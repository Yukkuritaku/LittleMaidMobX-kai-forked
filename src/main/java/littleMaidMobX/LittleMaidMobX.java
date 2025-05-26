package littleMaidMobX;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import littleMaidMobX.client.gui.GuiCommonHandler;
import littleMaidMobX.config.LittleMaidConfig;
import littleMaidMobX.entity.EntityLittleMaid;
import littleMaidMobX.entity.modes.EntityModeManager;
import littleMaidMobX.entity.modes.IFF;
import littleMaidMobX.item.ItemDismissalNotice;
import littleMaidMobX.item.ItemSpawnEgg;
import littleMaidMobX.network.NetworkHandler;
import littleMaidMobX.network.ProxyCommon;
import mmmlibx.lib.MMM_Helper;
import mmmlibx.lib.MMM_TextureManager;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.AchievementPage;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = LittleMaidMobX.MOD_ID,
        name = LittleMaidMobX.MOD_ID,
        guiFactory = "littleMaidMobX.client.gui.LittleMaidMobXGuiFactory")
public class LittleMaidMobX {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "lmmx";

    public static Achievement contract;

    @SidedProxy(
            clientSide = "littleMaidMobX.network.ProxyClient",
            serverSide = "littleMaidMobX.network.ProxyCommon")
    public static ProxyCommon proxy;

    @Instance(MOD_ID)
    private static LittleMaidMobX instance;

    public static ItemSpawnEgg spawnEgg;

    public static ItemDismissalNotice dismissalNotice;

    public static LittleMaidMobX getInstance(){
        return instance;
    }

    public static void debug(String text, Object... params) {
        // デバッグメッセージ
        if (LittleMaidConfig.printDebugMessage) {
            LOGGER.debug("(littleMaidMobX) " + text, params);
        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        //FileManager.setSrcPath(event.getSourceFile());
        LittleMaidConfig.init(event);
        LittleMaidConfig.check();
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiCommonHandler());

        MMM_TextureManager.instance.init();
        EntityRegistry.registerModEntity(EntityLittleMaid.class, "LittleMaidX", 0, instance, 80, 3, true);
        // アイテム自体は登録しておき、レシピを隠して無効化
        spawnEgg = new ItemSpawnEgg();
        spawnEgg.setUnlocalizedName(MOD_ID + ":spawn_lmmx_egg");
        spawnEgg.setTextureName(MOD_ID + ":spawn_lmmx_egg");
        GameRegistry.registerItem(spawnEgg, "spawn_lmmx_egg");
        if (LittleMaidConfig.enableSpawnEggRecipe) {
            // 招喚用レシピを追加
            GameRegistry.addRecipe(new ItemStack(spawnEgg, 1), "scs",
                    "sbs",
                    " e ",
                    's', Items.sugar,
                    'c', new ItemStack(Items.dye, 1, 3),
                    'b', Items.slime_ball,
                    'e', Items.egg);
        }
        //解雇通知書を追加
        dismissalNotice = new ItemDismissalNotice();
        dismissalNotice.setUnlocalizedName(MOD_ID + ":dismissal_notice_paper");
        dismissalNotice.setTextureName(MOD_ID + ":dismissal_notice_paper");
        GameRegistry.registerItem(dismissalNotice, "dismissal_notice_paper");
        GameRegistry.addRecipe(new ItemStack(dismissalNotice, 1), "ppp",
                "pcp",
                "ppp",
                'p', Items.paper,
                'c', Items.cake);

        contract = new Achievement("achievement.contract", "contract", 0, 0, Items.cake, null).initIndependentStat().registerStat();
        Achievement[] achievements = new Achievement[]{contract};
        AchievementPage.registerAchievementPage(new AchievementPage("LittleMaidMobX", achievements));

        if (MMM_Helper.isClient) {
            // 名称変換テーブル
            // デフォルトモデルの設定
            proxy.init();
        }

        // AIリストの追加
        EntityModeManager.init();

        // アイテムスロット更新用のパケット
        NetworkHandler.init();

        // TODO ★ サウンドのロードを早くするテスト
        proxy.loadSounds();

        ChestGenHooks.addItem(ChestGenHooks.BONUS_CHEST, new WeightedRandomChestContent(new ItemStack(Items.cake), 1, 1, 10));
        ChestGenHooks.addItem(ChestGenHooks.BONUS_CHEST, new WeightedRandomChestContent(new ItemStack(spawnEgg), 1, 1, 10));
    }


    @EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(instance);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent evt) {
        proxy.postInit();
        MinecraftForge.EVENT_BUS.register(new EventHook());
        // デフォルトモデルの設定
        MMM_TextureManager.instance.setDefaultTexture(EntityLittleMaid.class, MMM_TextureManager.instance.getTextureBox("default_Orign"));

        // Dominant
        if (LittleMaidConfig.spawnWeight > 0) {
            if (LittleMaidConfig.spawnMaidsEverywhere) {
                BiomeGenBase[] biomeList = BiomeGenBase.getBiomeGenArray();
                for (BiomeGenBase biome : biomeList) {
                    EntityRegistry.addSpawn(EntityLittleMaid.class,
                            LittleMaidConfig.spawnWeight, LittleMaidConfig.minGroupSize, LittleMaidConfig.maxGroupSize, EnumCreatureType.creature, biome);
                }
            } else {

                BiomeGenBase[] biomeList = BiomeGenBase.getBiomeGenArray();
                for (BiomeGenBase biome : biomeList) {
                    if (biome != null &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.OCEAN) &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.MOUNTAIN) &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.HILLS) &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.RIVER) &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.MAGICAL) &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.END) &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.NETHER) &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.JUNGLE) &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.DEAD) &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.SPOOKY) &&
                            !BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.MESA)) {
                        if (BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.HOT) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.COLD) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.WET) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.DRY) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.SAVANNA) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.CONIFEROUS) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.LUSH) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.MUSHROOM) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.FOREST) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.PLAINS) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.SANDY) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.SNOWY) ||
                                BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.BEACH)) {
                            EntityRegistry.addSpawn(EntityLittleMaid.class,
                                    LittleMaidConfig.spawnWeight, LittleMaidConfig.minGroupSize, LittleMaidConfig.maxGroupSize, EnumCreatureType.creature, biome);
                        }

                    }
                }
                // 通常スポーン設定バイオームは適当
                /*BiomeGenBase[] biomeList = new BiomeGenBase[]{
                        BiomeGenBase.desert,
                        BiomeGenBase.plains,
                        BiomeGenBase.savanna,
                        BiomeGenBase.mushroomIsland,
                        BiomeGenBase.forest,
                        BiomeGenBase.birchForest,
                        BiomeGenBase.swampland,
                        BiomeGenBase.taiga,
                };
                for (BiomeGenBase biome : biomeList) {
                    if (biome != null) {
                        EntityRegistry.addSpawn(EntityLittleMaid.class,
                                LittleMaidConfig.spawnWeight, LittleMaidConfig.minGroupSize, LittleMaidConfig.maxGroupSize, EnumCreatureType.creature, biome);
                    }
                }*/
            }
        }

        // モードリストを構築
        EntityModeManager.loadEntityMode();
        EntityModeManager.showLoadedModes();

        // サウンドのロード
// TODO ★		proxy.loadSounds();

        // IFFのロード
        IFF.loadIFFs();
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(LittleMaidMobX.MOD_ID)) {
            LittleMaidConfig.sync();
            LittleMaidConfig.save();
        }
    }


    // 特定のMODのアイテムを持つとクラッシュする不具合対策====================================

    public static boolean isMaidIgnoreItem(ItemStack item) {
        return item != null && item.getItem() != null && isMaidIgnoreItem(item.getItem());
    }

    public static boolean isMaidIgnoreItem(Item item) {
        if (item != null) {
            String name = Item.itemRegistry.getNameForObject(item);
            for (String ignoreItemName : LittleMaidConfig.ignoreItemList) {
                if (name.contains(ignoreItemName)) {
                    return true;
                }
            }
        }
        return false;
    }
    // 特定のMODのアイテムを持つとクラッシュする不具合対策====================================
}
