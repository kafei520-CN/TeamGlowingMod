package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.localization.Localization;
import cn.kafei.TeamGlowing.party.BannerMarker;
import cn.kafei.TeamGlowing.party.PartyManager;
import cn.kafei.TeamGlowing.persistence.PartyPersistence;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BannerBindingService {
    private static final long TELEPORT_WAIT_MILLIS = 3000L;
    private static final double TELEPORT_MOVE_THRESHOLD_SQUARED = 0.01D;

    private final PartyManager partyManager;
    private final Localization localization;
    private final PartyPersistence persistence;
    private final Map<UUID, PendingTeleport> pendingTeleports = new HashMap<>();

    public BannerBindingService(PartyManager partyManager, Localization localization, PartyPersistence persistence) {
        this.partyManager = partyManager;
        this.localization = localization;
        this.persistence = persistence;
    }

    public ActionResult handleUseBlock(ServerPlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (player == null || world.isClient() || hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (!(world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof AbstractBannerBlock)
            || !(world.getBlockEntity(hitResult.getBlockPos()) instanceof BannerBlockEntity bannerBlockEntity)) {
            return ActionResult.PASS;
        }

        BannerMarker marker = this.createMarker(player, world, hitResult, bannerBlockEntity);
        if (!player.isSneaking()) {
            return this.handleDirectHide(player, marker);
        }

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isOf(Items.RECOVERY_COMPASS)) {
            return this.handleSneakingPartyBanner(player, marker);
        }
        if (stack.isOf(Items.COMPASS)) {
            return this.handleSneakingLocatorBanner(player, marker);
        }
        return ActionResult.PASS;
    }

    public ActionResult handleUseItem(ServerPlayerEntity player, World world, Hand hand) {
        if (player == null || world == null || world.isClient() || hand != Hand.MAIN_HAND || !player.isSneaking()) {
            return ActionResult.PASS;
        }
        if (isLookingAtBanner(player, world)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isOf(Items.RECOVERY_COMPASS)) {
            return this.handleSneakingPartyCompass(player);
        }
        if (stack.isOf(Items.COMPASS)) {
            if (this.tryStartTeleport(player, stack)) {
                return ActionResult.SUCCESS_SERVER;
            }
            return this.handleSneakingLocatorCompass(player);
        }
        return ActionResult.PASS;
    }

    public void handleBannerBroken(World world, BlockPos pos) {
        if (world == null || world.isClient()) {
            return;
        }
        if (this.partyManager.clearBrokenBanner(world.getRegistryKey().getValue().toString(), pos)) {
            this.persistence.save(this.partyManager);
        }
    }

    public void tick(MinecraftServer server) {
        if (server == null || this.pendingTeleports.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, PendingTeleport>> iterator = this.pendingTeleports.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingTeleport> entry = iterator.next();
            PendingTeleport pending = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                pending.bossBar().clearPlayers();
                iterator.remove();
                continue;
            }

            if (!player.getWorld().getRegistryKey().getValue().toString().equals(pending.dimensionId())
                || player.getPos().squaredDistanceTo(pending.startPos()) > TELEPORT_MOVE_THRESHOLD_SQUARED) {
                player.sendMessage(Text.literal(this.localization.translate(player, "party.teleport.cancelled")), true);
                pending.bossBar().removePlayer(player);
                iterator.remove();
                continue;
            }

            float progress = Math.min(1.0F, (float) (now - pending.startedAtMillis()) / (float) TELEPORT_WAIT_MILLIS);
            pending.bossBar().setPercent(progress);
            pending.bossBar().setName(Text.literal(this.localization.translate(player, "party.teleport.progress", Math.round(progress * 100.0F))));
            if (progress < 1.0F) {
                continue;
            }

            ServerWorld targetWorld = server.getWorld(pending.target().dimension());
            if (targetWorld == null) {
                player.sendMessage(Text.literal(this.localization.translate(player, "party.teleport.failed")), true);
                pending.bossBar().removePlayer(player);
                iterator.remove();
                continue;
            }

            Vec3d destination = findTeleportTarget(targetWorld, pending.bannerPos(), pending.target().pos());
            if (destination == null) {
                player.sendMessage(Text.literal(this.localization.translate(player, "party.teleport.failed")), true);
                pending.bossBar().removePlayer(player);
                iterator.remove();
                continue;
            }

            player.teleport(targetWorld, destination.x, destination.y, destination.z, java.util.Set.<PositionFlag>of(), player.getYaw(), player.getPitch(), false);
            player.sendMessage(Text.literal(this.localization.translate(player, "party.teleport.completed", pending.bannerName())), true);
            pending.bossBar().removePlayer(player);
            iterator.remove();
        }
    }

    private ActionResult handleSneakingPartyBanner(ServerPlayerEntity player, BannerMarker marker) {
        try {
            if (this.partyManager.isPartyBanner(marker)) {
                boolean hidden = this.partyManager.togglePartyBannerHidden(PartyManager.getPlayerName(player));
                this.persistence.save(this.partyManager);
                player.sendMessage(Text.literal(this.localization.translate(player, hidden ? "party.banner.hidden" : "party.banner.shown", marker.name())), true);
                return ActionResult.SUCCESS_SERVER;
            }
            this.partyManager.setPartyBanner(PartyManager.getPlayerName(player), marker);
            this.persistence.save(this.partyManager);
            player.sendMessage(Text.literal(this.localization.translate(player, "party.banner.bound", marker.name())), true);
            return ActionResult.SUCCESS_SERVER;
        } catch (IllegalStateException exception) {
            player.sendMessage(Text.literal(this.localization.translate(player, exception.getMessage())), true);
            return ActionResult.FAIL;
        }
    }

    private ActionResult handleSneakingPartyCompass(ServerPlayerEntity player) {
        BannerMarker marker = this.partyManager.getOwnedPartyBanner(PartyManager.getPlayerName(player));
        if (marker == null) {
            return ActionResult.PASS;
        }
        boolean hidden = this.partyManager.togglePartyBannerHidden(PartyManager.getPlayerName(player));
        this.persistence.save(this.partyManager);
        player.sendMessage(Text.literal(this.localization.translate(player, hidden ? "party.banner.hidden" : "party.banner.shown", marker.name())), true);
        return ActionResult.SUCCESS_SERVER;
    }

    private ActionResult handleSneakingLocatorBanner(ServerPlayerEntity player, BannerMarker marker) {
        try {
            if (this.partyManager.isTrackedLocatorBanner(PartyManager.getPlayerName(player), marker)) {
                boolean hidden = this.partyManager.toggleAllLocatorBannersHidden(PartyManager.getPlayerName(player));
                this.persistence.save(this.partyManager);
                player.sendMessage(Text.literal(this.localization.translate(player, hidden ? "party.locator_banner.hidden_all" : "party.locator_banner.shown_all")), true);
                return ActionResult.SUCCESS_SERVER;
            }
            this.partyManager.setLocatorBanner(PartyManager.getPlayerName(player), marker);
            this.persistence.save(this.partyManager);
            player.sendMessage(Text.literal(this.localization.translate(player, "party.locator_banner.bound", marker.name())), true);
            return ActionResult.SUCCESS_SERVER;
        } catch (IllegalStateException exception) {
            player.sendMessage(Text.literal(this.localization.translate(player, exception.getMessage())), true);
            return ActionResult.FAIL;
        }
    }

    private ActionResult handleSneakingLocatorCompass(ServerPlayerEntity player) {
        List<BannerMarker> markers = this.partyManager.getAllLocatorBanners(PartyManager.getPlayerName(player));
        if (markers.isEmpty()) {
            return ActionResult.PASS;
        }
        boolean hidden = this.partyManager.toggleAllLocatorBannersHidden(PartyManager.getPlayerName(player));
        this.persistence.save(this.partyManager);
        player.sendMessage(Text.literal(this.localization.translate(player, hidden ? "party.locator_banner.hidden_all" : "party.locator_banner.shown_all")), true);
        return ActionResult.SUCCESS_SERVER;
    }

    private ActionResult handleDirectHide(ServerPlayerEntity player, BannerMarker marker) {
        if (this.partyManager.isPartyBanner(marker)) {
            boolean hidden = this.partyManager.togglePartyBannerHidden(PartyManager.getPlayerName(player));
            this.persistence.save(this.partyManager);
            player.sendMessage(Text.literal(this.localization.translate(player, hidden ? "party.banner.hidden" : "party.banner.shown", marker.name())), true);
            return ActionResult.SUCCESS_SERVER;
        }
        if (this.partyManager.isTrackedLocatorBanner(PartyManager.getPlayerName(player), marker)) {
            boolean hidden = this.partyManager.toggleLocatorBannerHidden(PartyManager.getPlayerName(player), marker);
            this.persistence.save(this.partyManager);
            player.sendMessage(Text.literal(this.localization.translate(player, hidden ? "party.locator_banner.hidden_single" : "party.locator_banner.shown_single", marker.name())), true);
            return ActionResult.SUCCESS_SERVER;
        }
        return ActionResult.PASS;
    }

    private boolean tryStartTeleport(ServerPlayerEntity player, ItemStack stack) {
        LodestoneTrackerComponent tracker = stack.get(DataComponentTypes.LODESTONE_TRACKER);
        if (tracker == null || tracker.target().isEmpty()) {
            return false;
        }

        GlobalPos target = tracker.target().get();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        ServerWorld targetWorld = server.getWorld(target.dimension());
        if (targetWorld == null) {
            return false;
        }
        BlockPos lodestonePos = target.pos();
        if (!targetWorld.getBlockState(lodestonePos).isOf(Blocks.LODESTONE)) {
            return false;
        }
        BannerMarker bannerMarker = this.partyManager.findLocatorBannerForLodestone(
            PartyManager.getPlayerName(player),
            target.dimension().getValue().toString(),
            lodestonePos
        );
        if (bannerMarker == null) {
            return false;
        }

        BlockPos bannerPos = bannerMarker.toBlockPos();
        Vec3d destination = findTeleportTarget(targetWorld, bannerPos, lodestonePos);
        if (destination == null) {
            player.sendMessage(Text.literal(this.localization.translate(player, "party.teleport.failed")), true);
            return true;
        }

        this.cancelPendingTeleport(player);
        ServerBossBar bossBar = new ServerBossBar(
            Text.literal(this.localization.translate(player, "party.teleport.progress", 0)),
            BossBar.Color.BLUE,
            BossBar.Style.PROGRESS
        );
        bossBar.setPercent(0.0F);
        bossBar.addPlayer(player);
        this.pendingTeleports.put(player.getUuid(), new PendingTeleport(
            player.getPos(),
            player.getWorld().getRegistryKey().getValue().toString(),
            GlobalPos.create(targetWorld.getRegistryKey(), lodestonePos),
            bannerPos,
            System.currentTimeMillis(),
            bossBar,
            bannerMarker.name(),
            destination
        ));
        player.sendMessage(Text.literal(this.localization.translate(player, "party.teleport.started", bannerMarker.name())), true);
        return true;
    }

    private void cancelPendingTeleport(ServerPlayerEntity player) {
        PendingTeleport pending = this.pendingTeleports.remove(player.getUuid());
        if (pending != null) {
            pending.bossBar().removePlayer(player);
        }
    }

    private BannerMarker createMarker(ServerPlayerEntity player, World world, BlockHitResult hitResult, BannerBlockEntity bannerBlockEntity) {
        Text customName = bannerBlockEntity.getCustomName();
        String name = customName == null ? "" : customName.getString();
        return new BannerMarker(
            name == null || name.isBlank() ? this.localization.translate(player, "teamglowing.banner.default") : name,
            world.getRegistryKey().getValue().toString(),
            hitResult.getBlockPos().getX(),
            hitResult.getBlockPos().getY(),
            hitResult.getBlockPos().getZ(),
            bannerBlockEntity.getColorForState().ordinal()
        );
    }

    private static Vec3d findTeleportTarget(ServerWorld world, BlockPos bannerPos, BlockPos lodestonePos) {
        for (int radius = 0; radius <= 2; radius++) {
            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                    for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                        if (radius > 0 && Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                            continue;
                        }
                        BlockPos feetPos = bannerPos.add(xOffset, yOffset, zOffset);
                        if (feetPos.equals(bannerPos) || feetPos.equals(lodestonePos)) {
                            continue;
                        }
                        if (isSafeTeleportPos(world, feetPos)) {
                            return new Vec3d(feetPos.getX() + 0.5D, feetPos.getY(), feetPos.getZ() + 0.5D);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeTeleportPos(ServerWorld world, BlockPos feetPos) {
        BlockPos headPos = feetPos.up();
        BlockPos groundPos = feetPos.down();
        return isPassable(world, feetPos)
            && isPassable(world, headPos)
            && isStandable(world, groundPos);
    }

    private static boolean isPassable(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos).isEmpty() && world.getFluidState(pos).isEmpty();
    }

    private static boolean isStandable(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos).isEmpty() && world.getFluidState(pos).isEmpty();
    }

    private record PendingTeleport(
        Vec3d startPos,
        String dimensionId,
        GlobalPos target,
        BlockPos bannerPos,
        long startedAtMillis,
        ServerBossBar bossBar,
        String bannerName,
        Vec3d previewTarget
    ) {
    }

    private static boolean isLookingAtBanner(ServerPlayerEntity player, World world) {
        HitResult hitResult = player.raycast(4.5D, 1.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return false;
        }
        return world.getBlockState(blockHitResult.getBlockPos()).getBlock() instanceof AbstractBannerBlock;
    }
}
