package littleMaidMobX.config;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.Configuration;

public class LittleMaidConfig {

    public static final String CATEGORY_CLIENT = "client";
    public static final String CATEGORY_SPAWNING = "spawning";
    public static final String CATEGORY_MAIDS = "maids";
    public static final String CATEGORY_ITEMS = "items";
    public static final String CATEGORY_DEBUG = "debug";

    private static Configuration configuration;
    public static boolean oldStatsRendering;

    public static int spawnWeight;
    public static int minGroupSize;
    public static int maxGroupSize;
    public static boolean spawnMaidsEverywhere;

    public static boolean canDespawn;
    public static boolean checkOwnerName;
    public static boolean fixLittleMaidDupe;
    public static boolean enableSpawnEggRecipe;
    public static boolean enableDisplaySugarCount;
    public static boolean voiceDistortion;
    public static int voiceSoundInterval;
    public static String defaultTexture;
    public static boolean deathMessage;
    public static boolean setDefaultIFFFriendly;
    public static String[] ignoreItemList;
    public static boolean enableFarmerMode;
    public static int talkInterval;
    public static int maidContractLimit;
    public static int maxMaidContractLimit;
    public static boolean dropNametagOnTamed;

    public static boolean printDebugMessage;
//	public static boolean AlphaBlend = true;

    public static void init(FMLPreInitializationEvent event){
        configuration = new Configuration(event.getSuggestedConfigurationFile());
    }

    public static void save(){
        configuration.save();
    }

    public static void load(){
        configuration.load();
        configuration.setCategoryRequiresMcRestart(CATEGORY_SPAWNING, true);
        configuration.setCategoryComment(CATEGORY_SPAWNING, "Spawning Options (Required Restart)");
        configuration.setCategoryRequiresMcRestart(CATEGORY_ITEMS, true);
    }

    public static void sync(){
        oldStatsRendering = configuration.getBoolean("oldStatsRendering", CATEGORY_CLIENT, false, "[WIP] Set maid gui stats drawing to old version of LittleMaid.");
        spawnWeight = configuration.getInt("spawnWeight", CATEGORY_SPAWNING, 5, 0, 10, "Set the spawn chance of LittleMaid. set 0 to disable the spawn.");
        minGroupSize = configuration.getInt("minGroupSize", CATEGORY_SPAWNING, 1, 1, 10, "Minimum spawn group count.");
        maxGroupSize = configuration.getInt("maxGroupSize", CATEGORY_SPAWNING, 3, 1, 10, "Maximum spawn group count.");
        spawnMaidsEverywhere = configuration.getBoolean("spawnMaidsEverywhere", CATEGORY_SPAWNING, false, "If true maids will spawn in all biomes, if false maids will only spawn in biomes of approved types.");

        canDespawn = configuration.getBoolean("canDespawn", CATEGORY_MAIDS, false, "Whether or not a Maid without a contract can despawn.");
        checkOwnerName = configuration.getBoolean("checkOwnerName", CATEGORY_MAIDS, true, "Checks the name of owner, if you do multiplayer keep this true.");
        fixLittleMaidDupe = configuration.getBoolean("fixLittleMaidDupe", CATEGORY_MAIDS, true, "Fix LittleMaid duplication on load.");
        enableDisplaySugarCount = configuration.getBoolean("enableDisplaySugarCount", CATEGORY_MAIDS, true, "Enable the sugar count display on LittleMaid's head.");
        voiceDistortion = configuration.getBoolean("voiceDistortion", CATEGORY_MAIDS, true, "Enables LittleMaid voices to distort based of hair color.");
        voiceSoundInterval = configuration.getInt("voiceSoundInterval", CATEGORY_MAIDS, 20, 0, Integer.MAX_VALUE, "Sets LittleMaid voice cooldown, mean in minecraft tick.");
        defaultTexture = configuration.getString("defaultTexture", CATEGORY_MAIDS, "", "Default selected Texture Package. if set to empty, uses random selection");
        deathMessage = configuration.getBoolean("deathMessage", CATEGORY_MAIDS, true, "Prints message on the death of your maid.");
        setDefaultIFFFriendly = configuration.getBoolean("setDefaultIFFFriendly", CATEGORY_MAIDS, true, "Set non enemy mobs IFF value. if set to true, non enemy default IFF values to Friendly. if set to false, non enemy default IFF values to Enemy.");
        ignoreItemList = configuration.getStringList("ignoreItemList", CATEGORY_MAIDS, new String[]{"arsmagica2"}, "");
        enableFarmerMode = configuration.getBoolean("enableFarmerMode", CATEGORY_MAIDS, true, "[Deprecated] Enable/Disable farmer mode. if cause the lag in farmer mode, maybe you should set to false to fix this.\n§c(Deprecated reason: Maybe this bug fixed later, i'll delete this config, but no eta)§e");
        talkInterval = configuration.getInt("talkInterval", CATEGORY_MAIDS, 120, 0, 200, "Change LittleMaid living sound times");
        maidContractLimit = configuration.getInt("maidContractLimit", CATEGORY_MAIDS, 24000, 1, Integer.MAX_VALUE, "Change LittleMaid Contract add duration. (24000 is 1 day in Minecraft)");
        maxMaidContractLimit = configuration.getInt("maxMaidContractLimit", CATEGORY_MAIDS, 24000 * 7, 1, Integer.MAX_VALUE, "Change LittleMaid Contract max duration. (24000 is 1 day in Minecraft)");
        dropNametagOnTamed = configuration.getBoolean("dropNametagOnTamed", CATEGORY_MAIDS, false, "Change Nametag drop on LittleMaid tamed.");

        enableSpawnEggRecipe = configuration.getBoolean("enableSpawnEggRecipe", CATEGORY_ITEMS, true, "Enable LittleMaid spawn egg recipe.");

        printDebugMessage = configuration.getBoolean("printDebugMessage", CATEGORY_DEBUG, false, "If true will output debug messages.");
    }


    public static void check(){
        load();
        sync();
        if (configuration.hasChanged()){
            save();
        }
    }

    public static Configuration configuration(){
        return configuration;
    }
}
