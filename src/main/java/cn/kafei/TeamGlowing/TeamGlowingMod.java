package cn.kafei.TeamGlowing;

import cn.kafei.TeamGlowing.client.ClientBootstrap;
import cn.kafei.TeamGlowing.command.TeamGlowingPartyCommand;
import cn.kafei.TeamGlowing.core.TeamGlowingConstants;
import cn.kafei.TeamGlowing.localization.Localization;
import cn.kafei.TeamGlowing.network.TeamGlowingNetwork;
import cn.kafei.TeamGlowing.party.PartyManager;
import cn.kafei.TeamGlowing.persistence.PartyPersistence;
import cn.kafei.TeamGlowing.sync.GlowSyncService;
import cn.kafei.TeamGlowing.sync.LocatorSyncService;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = TeamGlowingConstants.MODID, name = TeamGlowingConstants.NAME, version = TeamGlowingConstants.VERSION)
public class TeamGlowingMod
{
    private final PartyManager partyManager = new PartyManager();
    private final Localization localization = new Localization();
    private final PartyPersistence persistence = new PartyPersistence();
    private final GlowSyncService glowSyncService = new GlowSyncService();
    private final LocatorSyncService locatorSyncService = new LocatorSyncService();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        TeamGlowingConstants.setLogger(event.getModLog());
        this.glowSyncService.initReflection();
        TeamGlowingNetwork.register();
        if (event.getSide() == Side.CLIENT)
        {
            ClientBootstrap.init();
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
        TeamGlowingConstants.getLogger().info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new TeamGlowingPartyCommand(this.partyManager, this.localization, this.persistence));
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event)
    {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null)
        {
            return;
        }

        this.persistence.setSaveFilePath(server.getFile("teamglowing-parties.json").toPath());
        this.persistence.load(this.partyManager);
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event)
    {
        this.persistence.save(this.partyManager);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || !(event.player instanceof EntityPlayerMP))
        {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        this.glowSyncService.syncVisibilityForPlayer(player, this.partyManager);
        this.locatorSyncService.syncToPlayer(player, this.partyManager);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event)
    {
        if (event.player instanceof EntityPlayerMP)
        {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            this.glowSyncService.syncSinglePlayer(player, this.partyManager);
            this.locatorSyncService.syncToPlayer(player, this.partyManager);
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event)
    {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP))
        {
            return;
        }

        this.glowSyncService.syncVisibilityForPlayer((EntityPlayerMP) event.getEntityLiving(), this.partyManager);
    }
}
