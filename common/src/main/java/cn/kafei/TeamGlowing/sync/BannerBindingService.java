package cn.kafei.TeamGlowing.sync;

import cn.kafei.TeamGlowing.localization.Localization;
import cn.kafei.TeamGlowing.network.ClientBannerVisibilityMessage;
import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import cn.kafei.TeamGlowing.party.BannerMarker;
import cn.kafei.TeamGlowing.party.BannerMarkerEntryIds;
import cn.kafei.TeamGlowing.party.PartyManager;
import cn.kafei.TeamGlowing.persistence.PartyPersistence;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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

    public InteractionResult handleUseBlock(ServerPlayer player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (player == null || world.isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResultCompat.pass();
        }
        if (!(world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof AbstractBannerBlock)
            || !(world.getBlockEntity(hitResult.getBlockPos()) instanceof BannerBlockEntity bannerBlockEntity)) {
            return InteractionResultCompat.pass();
        }

        BannerMarker marker = this.createMarker(player, world, hitResult, bannerBlockEntity);
        if (!player.isShiftKeyDown()) {
            return this.handleDirectHide(player, marker);
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.RECOVERY_COMPASS)) {
            return this.handleSneakingPartyBanner(player, marker);
        }
        if (stack.is(Items.COMPASS)) {
            return this.handleSneakingLocatorBanner(player, marker);
        }
        return InteractionResultCompat.pass();
    }

    public InteractionResult handleUseItem(ServerPlayer player, Level world, InteractionHand hand) {
        if (player == null || world == null || world.isClientSide() || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return InteractionResultCompat.pass();
        }
        if (isLookingAtBanner(player, world)) {
            return InteractionResultCompat.pass();
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.RECOVERY_COMPASS)) {
            return this.handleSneakingPartyCompass(player);
        }
        if (stack.is(Items.COMPASS)) {
            if (this.tryStartTeleport(player, stack)) {
                return InteractionResultCompat.success();
            }
            return this.handleSneakingLocatorCompass(player);
        }
        return InteractionResultCompat.pass();
    }

    public void handleBannerBroken(Level world, BlockPos pos) {
        if (world == null || world.isClientSide()) {
            return;
        }
        if (this.partyManager.clearBrokenBanner(world.dimension().location().toString(), pos)) {
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
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                pending.bossBar().removeAllPlayers();
                iterator.remove();
                continue;
            }

            if (!player.level().dimension().location().toString().equals(pending.dimensionId())
                || player.position().distanceToSqr(pending.startPos()) > TELEPORT_MOVE_THRESHOLD_SQUARED) {
                player.displayClientMessage(Component.literal(this.localization.translate(player, "party.teleport.cancelled")), true);
                pending.bossBar().removePlayer(player);
                iterator.remove();
                continue;
            }

            float progress = Math.min(1.0F, (float) (now - pending.startedAtMillis()) / (float) TELEPORT_WAIT_MILLIS);
            pending.bossBar().setProgress(progress);
            pending.bossBar().setName(Component.literal(this.localization.translate(player, "party.teleport.progress", Math.round(progress * 100.0F))));
            if (progress < 1.0F) {
                continue;
            }

            ServerLevel targetWorld = server.getLevel(pending.target().dimension());
            if (targetWorld == null) {
                player.displayClientMessage(Component.literal(this.localization.translate(player, "party.teleport.failed")), true);
                pending.bossBar().removePlayer(player);
                iterator.remove();
                continue;
            }

            Vec3 destination = findTeleportTarget(targetWorld, pending.bannerPos(), pending.target().pos());
            if (destination == null) {
                player.displayClientMessage(Component.literal(this.localization.translate(player, "party.teleport.failed")), true);
                pending.bossBar().removePlayer(player);
                iterator.remove();
                continue;
            }

            if (ServerTeleportCompat.teleport(player, targetWorld, destination)) {
                player.displayClientMessage(Component.literal(this.localization.translate(player, "party.teleport.completed", pending.bannerName())), true);
            } else {
                player.displayClientMessage(Component.literal(this.localization.translate(player, "party.teleport.failed")), true);
            }
            pending.bossBar().removePlayer(player);
            iterator.remove();
        }
    }

    private InteractionResult handleSneakingPartyBanner(ServerPlayer player, BannerMarker marker) {
        try {
            if (this.partyManager.isPartyBanner(marker)) {
                this.sendClientPartyBannerToggle(player, marker);
                return InteractionResultCompat.success();
            }
            this.partyManager.setPartyBanner(PartyManager.getPlayerName(player), marker);
            this.persistence.save(this.partyManager);
            player.displayClientMessage(Component.literal(this.localization.translate(player, "party.banner.bound", marker.name())), true);
            return InteractionResultCompat.success();
        } catch (IllegalStateException exception) {
            player.displayClientMessage(Component.literal(this.localization.translate(player, exception.getMessage())), true);
            return InteractionResultCompat.fail();
        }
    }

    private InteractionResult handleSneakingPartyCompass(ServerPlayer player) {
        BannerMarker marker = this.partyManager.getOwnedPartyBanner(PartyManager.getPlayerName(player));
        if (marker == null) {
            return InteractionResultCompat.pass();
        }
        this.sendClientPartyBannerToggle(player, marker);
        return InteractionResultCompat.success();
    }

    private InteractionResult handleSneakingLocatorBanner(ServerPlayer player, BannerMarker marker) {
        try {
            if (this.partyManager.isTrackedLocatorBanner(PartyManager.getPlayerName(player), marker)) {
                this.sendClientLocatorBannersToggle(player);
                return InteractionResultCompat.success();
            }
            this.partyManager.setLocatorBanner(PartyManager.getPlayerName(player), marker);
            this.persistence.save(this.partyManager);
            player.displayClientMessage(Component.literal(this.localization.translate(player, "party.locator_banner.bound", marker.name())), true);
            return InteractionResultCompat.success();
        } catch (IllegalStateException exception) {
            player.displayClientMessage(Component.literal(this.localization.translate(player, exception.getMessage())), true);
            return InteractionResultCompat.fail();
        }
    }

    private InteractionResult handleSneakingLocatorCompass(ServerPlayer player) {
        List<BannerMarker> markers = this.partyManager.getAllLocatorBanners(PartyManager.getPlayerName(player));
        if (markers.isEmpty()) {
            return InteractionResultCompat.pass();
        }
        this.sendClientLocatorBannersToggle(player, markers);
        return InteractionResultCompat.success();
    }

    private InteractionResult handleDirectHide(ServerPlayer player, BannerMarker marker) {
        if (this.partyManager.isPartyBanner(marker)) {
            this.sendClientPartyBannerToggle(player, marker);
            return InteractionResultCompat.success();
        }
        if (this.partyManager.isTrackedLocatorBanner(PartyManager.getPlayerName(player), marker)) {
            this.sendClientLocatorBannerToggle(player, marker);
            return InteractionResultCompat.success();
        }
        return InteractionResultCompat.pass();
    }

    private boolean tryStartTeleport(ServerPlayer player, ItemStack stack) {
        LodestoneTracker tracker = ItemStackComponentCompat.get(stack, DataComponents.LODESTONE_TRACKER);
        if (tracker == null || tracker.target().isEmpty()) {
            return false;
        }

        GlobalPos target = tracker.target().get();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        ServerLevel targetWorld = server.getLevel(target.dimension());
        if (targetWorld == null) {
            return false;
        }
        BlockPos lodestonePos = target.pos();
        if (!targetWorld.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            return false;
        }
        BannerMarker bannerMarker = this.partyManager.findLocatorBannerForLodestone(
            PartyManager.getPlayerName(player),
            target.dimension().location().toString(),
            lodestonePos
        );
        if (bannerMarker == null) {
            return false;
        }

        BlockPos bannerPos = bannerMarker.toBlockPos();
        Vec3 destination = findTeleportTarget(targetWorld, bannerPos, lodestonePos);
        if (destination == null) {
            player.displayClientMessage(Component.literal(this.localization.translate(player, "party.teleport.failed")), true);
            return true;
        }

        this.cancelPendingTeleport(player);
        ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal(this.localization.translate(player, "party.teleport.progress", 0)),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS
        );
        bossBar.setProgress(0.0F);
        bossBar.addPlayer(player);
        this.pendingTeleports.put(player.getUUID(), new PendingTeleport(
            player.position(),
            player.level().dimension().location().toString(),
            GlobalPos.of(targetWorld.dimension(), lodestonePos),
            bannerPos,
            System.currentTimeMillis(),
            bossBar,
            bannerMarker.name(),
            destination
        ));
        player.displayClientMessage(Component.literal(this.localization.translate(player, "party.teleport.started", bannerMarker.name())), true);
        return true;
    }

    private void cancelPendingTeleport(ServerPlayer player) {
        PendingTeleport pending = this.pendingTeleports.remove(player.getUUID());
        if (pending != null) {
            pending.bossBar().removePlayer(player);
        }
    }

    private void sendClientPartyBannerToggle(ServerPlayer player, BannerMarker marker) {
        TeamGlowingNetwork.sendTo(player, ClientBannerVisibilityMessage.togglePartyBanner(BannerMarkerEntryIds.partyBanner(marker), marker.name()));
    }

    private void sendClientLocatorBannerToggle(ServerPlayer player, BannerMarker marker) {
        TeamGlowingNetwork.sendTo(player, ClientBannerVisibilityMessage.toggleLocatorBanner(BannerMarkerEntryIds.locatorBanner(marker), marker.name()));
    }

    private void sendClientLocatorBannersToggle(ServerPlayer player) {
        this.sendClientLocatorBannersToggle(player, this.partyManager.getAllLocatorBanners(PartyManager.getPlayerName(player)));
    }

    private void sendClientLocatorBannersToggle(ServerPlayer player, List<BannerMarker> markers) {
        TeamGlowingNetwork.sendTo(player, ClientBannerVisibilityMessage.toggleLocatorBanners(
            markers.stream()
                .map(BannerMarkerEntryIds::locatorBanner)
                .toList()
        ));
    }

    private BannerMarker createMarker(ServerPlayer player, Level world, BlockHitResult hitResult, BannerBlockEntity bannerBlockEntity) {
        Component customName = bannerBlockEntity.getCustomName();
        String name = customName == null ? "" : customName.getString();
        return new BannerMarker(
            name == null || name.isBlank() ? this.localization.translate(player, "teamglowing.banner.default") : name,
            world.dimension().location().toString(),
            hitResult.getBlockPos().getX(),
            hitResult.getBlockPos().getY(),
            hitResult.getBlockPos().getZ(),
            bannerBlockEntity.getBaseColor().ordinal()
        );
    }

    private static Vec3 findTeleportTarget(ServerLevel world, BlockPos bannerPos, BlockPos lodestonePos) {
        for (int radius = 0; radius <= 2; radius++) {
            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                    for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                        if (radius > 0 && Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                            continue;
                        }
                        BlockPos feetPos = bannerPos.offset(xOffset, yOffset, zOffset);
                        if (feetPos.equals(bannerPos) || feetPos.equals(lodestonePos)) {
                            continue;
                        }
                        if (isSafeTeleportPos(world, feetPos)) {
                            return new Vec3(feetPos.getX() + 0.5D, feetPos.getY(), feetPos.getZ() + 0.5D);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeTeleportPos(ServerLevel world, BlockPos feetPos) {
        BlockPos headPos = feetPos.above();
        BlockPos groundPos = feetPos.below();
        return isPassable(world, feetPos)
            && isPassable(world, headPos)
            && isStandable(world, groundPos);
    }

    private static boolean isPassable(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos).isEmpty() && world.getFluidState(pos).isEmpty();
    }

    private static boolean isStandable(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos).isEmpty() && world.getFluidState(pos).isEmpty();
    }

    private record PendingTeleport(
        Vec3 startPos,
        String dimensionId,
        GlobalPos target,
        BlockPos bannerPos,
        long startedAtMillis,
        ServerBossEvent bossBar,
        String bannerName,
        Vec3 previewTarget
    ) {
    }

    private static boolean isLookingAtBanner(ServerPlayer player, Level world) {
        HitResult hitResult = player.pick(4.5D, 1.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return false;
        }
        return world.getBlockState(blockHitResult.getBlockPos()).getBlock() instanceof AbstractBannerBlock;
    }
}
