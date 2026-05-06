package cn.kafei.TeamGlowing.core;

import org.apache.logging.log4j.Logger;

public final class TeamGlowingConstants
{
    public static final String MODID = "teamglowing";
    public static final String NAME = "TeamGlowing";
    public static final String VERSION = "1.0";
    public static final byte ENTITY_GLOWING_FLAG = 0x40;
    public static final int TRACKER_UPDATE_INTERVAL = 2;

    private static Logger logger;

    private TeamGlowingConstants()
    {
    }

    public static void setLogger(Logger modLogger)
    {
        logger = modLogger;
    }

    public static Logger getLogger()
    {
        return logger;
    }
}
