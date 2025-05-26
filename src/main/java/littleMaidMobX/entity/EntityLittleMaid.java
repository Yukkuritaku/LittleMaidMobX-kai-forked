package littleMaidMobX.entity;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import littleMaidMobX.*;
import littleMaidMobX.client.audio.EnumSound;
import littleMaidMobX.client.audio.LittleMaidSoundManager;
import littleMaidMobX.client.gui.GuiCommonHandler;
import littleMaidMobX.client.model.EntityCaps;
import littleMaidMobX.config.LittleMaidConfig;
import littleMaidMobX.entity.ai.*;
import littleMaidMobX.entity.ai.EntityAIAttackOnCollide;
import littleMaidMobX.entity.ai.EntityAIBeg;
import littleMaidMobX.entity.ai.EntityAIFollowOwner;
import littleMaidMobX.entity.ai.EntityAISwimming;
import littleMaidMobX.entity.ai.EntityAIWander;
import littleMaidMobX.entity.modes.*;
import littleMaidMobX.inventory.InventoryLittleMaid;
import littleMaidMobX.network.NetworkHandler;
import littleMaidMobX.network.packet.client.PlayLittleMaidSoundPacket;
import littleMaidMobX.network.packet.client.SwingArmPacket;
import littleMaidMobX.network.packet.server.UpdateLittleMaidSlotPacket;
import littleMaidMobX.util.Statics;
import mmmlibx.lib.*;
import mmmlibx.lib.multiModel.model.mc162.EquippedStabilizer;
import mmmlibx.lib.multiModel.model.mc162.IModelCaps;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1EPacketRemoveEntityEffect;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import littleMaidMobX.wrapper.W_Common;

import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

import static littleMaidMobX.util.Statics.*;

public class EntityLittleMaid extends EntityTameable implements ITextureEntity {

    // 定数はStaticsへ移動
//	protected static final UUID maidUUID = UUID.nameUUIDFromBytes("lmm.littleMaidMob".getBytes());
    protected static final UUID maidUUID = UUID.fromString("e2361272-644a-3028-8416-8536667f0efb");
    //	protected static final UUID maidUUIDSneak = UUID.nameUUIDFromBytes("lmm.littleMaidMob.sneak".getBytes());
    protected static final UUID maidUUIDSneak = UUID.fromString("5649cf91-29bb-3a0c-8c31-b170a1045560");
    protected static AttributeModifier attCombatSpeed = (new AttributeModifier(maidUUID, "Combat speed boost", 0.07D, 0)).setSaved(false);
    protected static AttributeModifier attAxeAmp = (new AttributeModifier(maidUUID, "Axe Attack boost", 0.5D, 1)).setSaved(false);
    protected static AttributeModifier attSneakingSpeed = (new AttributeModifier(maidUUIDSneak, "Sneking speed ampd", -0.4D, 2)).setSaved(false);


    // 変数減らしたいなぁ
//	protected long maidContractLimit;		// 契約失効日
    public int maidContractLimit;        // 契約期間
    protected long maidAnniversary;            // 契約日UIDとして使用
    public int maidDominantArm;            // 利き腕、1Byte
    /**
     * テクスチャ関連のデータを管理
     **/
    public MMM_TextureData textureData;
    public Map<String, EquippedStabilizer> maidStabilizer = new HashMap<String, EquippedStabilizer>();


    public InventoryLittleMaid maidInventory;
    public EntityPlayer maidAvatar;
    public EntityCaps maidCaps;    // Client側のみ

    public List<EntityModeBase> maidEntityModeList;
    public Map<Integer, EntityAITasks[]> maidModeList;
    public Map<String, Integer> maidModeIndexList;
    public int maidMode;        // 2Byte
    public boolean maidTracer;
    public boolean maidFreedom;
    public boolean maidWait;
    public int homeWorld;
    public int[][] maidTiles = new int[9][3];
    public int[] maidTile = new int[3];
    public TileEntity maidTileEntity;

    // 動的な状態
    public EntityPlayer mstatMasterEntity;    // 主
    public double mstatMasterDistanceSq;        // 主との距離、計算軽量化用
    public Entity mstatgotcha;                // ワイヤード用
    protected boolean mstatBloodsuck;
    protected boolean mstatClockMaid;
    // マスク判定
    protected int mstatMaskSelect;
    // 追加の頭部装備
    protected boolean mstatCamouflage;
    protected boolean mstatPlanter;
    //	protected boolean isMaidChaseWait;
    protected int mstatWaitCount;
    protected int mstatTime;
    public MMM_Counter maidOverDriveTime;
    protected boolean mstatFirstLook;
    protected boolean mstatLookSuger;
    protected MMM_Counter mstatWorkingCount;
    protected int mstatPlayingRole;
    protected int mstatWorkingInt;
    protected String mstatModeName;
    protected boolean mstatOpenInventory;
    // 腕振り
    public SwingStatus[] mstatSwingStatus;
    public boolean mstatAimeBow;
    // 首周り
    private boolean looksWithInterest;
    private boolean looksWithInterestAXIS;
    private float rotateAngleHead;            // Angle
    private float prevRotateAngleHead;        // prevAngle

    /**
     * 個体ごとに値をバラつかせるのに使う。
     */
    public float entityIdFactor;

    public boolean weaponFullAuto;    // 装備がフルオート武器かどうか
    public boolean weaponReload;    // 装備がリロードを欲しているかどうか
    public boolean maidCamouflage;


    // 音声
//	protected LMM_EnumSound maidAttackSound;
    public EnumSound maidDamageSound;
    protected int maidSoundInterval;
    protected float maidSoundRate;

    // 実験用
    private int firstload = 1;
    public String statusMessage = "";


    // AI
    public EntityAITempt aiTempt;
    public EntityAIBeg aiBeg;
    public EntityAIBegMove aiBegMove;
    public EntityAIOpenDoor aiOpenDoor;
    public EntityAIRestrictOpenDoor aiCloseDoor;
    public EntityAIAvoidPlayer aiAvoidPlayer;
    public EntityAIFollowOwner aiFollow;
    public EntityAIAttackOnCollide aiAttack;
    public EntityAIAttackArrow aiShooting;
    public EntityAICollectItem aiCollectItem;
    public EntityAIRestrictRain aiRestrictRain;
    public EntityAIFleeRain aiFreeRain;
    public EntityAIWander aiWander;
    public EntityAIJumpToMaster aiJumpTo;
    public EntityAIFindBlock aiFindBlock;
    public EntityAITracerMove aiTracer;
    public net.minecraft.entity.ai.EntityAISwimming aiSwiming;
    public EntityAIPanic aiPanic;
    // ActiveModeClass
    public EntityModeBase maidActiveModeClass;
    public Profiler aiProfiler;

    //攻撃された回数
    private int attackCount = 0;
    public boolean IsDismissalNotice = false;

    public EntityLittleMaid(World world) {
        super(world);
        // 初期設定
        maidInventory = new InventoryLittleMaid(this);
        if (world != null) {
            if (world.isRemote) {
                maidAvatar = new EntityLittleMaidAvatar(world, this);
            } else {
                maidAvatar = new EntityLittleMaidAvatarMP(world, this);
            }
        }
        mstatOpenInventory = false;
//		isMaidChaseWait = false;
        mstatTime = 6000;
        maidOverDriveTime = new MMM_Counter(5, 300, -100);
        mstatWorkingCount = new MMM_Counter(11, 10, -10);

        // モデルレンダリング用のフラグ獲得用ヘルパー関数
        maidCaps = new EntityCaps(this);

        // 形態形成場
        textureData = new MMM_TextureData(this, maidCaps);
        textureData.setColor(12);
        MMM_TextureBox[] ltb = new MMM_TextureBox[2];
        ltb[0] = ltb[1] = MMM_TextureManager.instance.getDefaultTexture(this);
        setTexturePackName(ltb);

        entityIdFactor = (float) (getEntityId() * 70);
        // 腕振り
        mstatSwingStatus = new SwingStatus[]{new SwingStatus(), new SwingStatus()};
        setDominantArm(rand.nextInt(mstatSwingStatus.length));

        // 再生音声
//		maidAttackSound = LMM_EnumSound.attack;
        maidDamageSound = EnumSound.hurt;
        maidSoundInterval = 0;

        // 野生種用初期値設定
        setHealth(15F);

        // 移動用フィジカル設定
        getNavigator().setAvoidsWater(true);
        getNavigator().setBreakDoors(true);

        // TODO:これはテスト
//		maidStabilizer.put("HeadTop", MMM_StabilizerManager.getStabilizer("WitchHat", "HeadTop"));

        // EntityModeの追加
        maidEntityModeList = EntityModeManager.getModeList(this);
        // モードリスト
        maidActiveModeClass = null;
        maidModeList = new HashMap<>();
        maidModeIndexList = new HashMap<>();
        initModeList();
        mstatModeName = "";
        maidMode = 65535;
        // 初期化時実行コード
        for (EntityModeBase lem : maidEntityModeList) {
            lem.initEntity();
        }
    }

    public IEntityLittleMaidAvatarBase getAvatarIF() {
        return (IEntityLittleMaidAvatarBase) maidAvatar;
    }

    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData par1EntityLivingData) {
        // テクスチャーをランダムで選択
        String ls;
        if (LittleMaidConfig.defaultTexture.isEmpty()) {
            ls = MMM_TextureManager.instance.getRandomTextureString(rand);
        } else {
            ls = LittleMaidConfig.defaultTexture;
        }
        textureData.setTextureInitServer(ls);
        LittleMaidMobX.debug("init-ID:%d, %s:%d", getEntityId(), textureData.textureBox[0].textureName, textureData.getColor());
        setTexturePackIndex(textureData.getColor(), textureData.textureIndex);
        setMaidMode("Wild");
        //新しいメイドの能力値を決める。
        //ここは体力
		/*
		if (this.rand.nextInt(4) == 0)
		{
			this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(22.0D);
		}else {
			this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(20.0D);
		}
		*/
        this.setHealth(this.getMaxHealth());
        return super.onSpawnWithEgg(par1EntityLivingData);
    }

    protected void applyEntityAttributes() {
        // 初期パラメーター
        super.applyEntityAttributes();
        // 対象移動可能範囲
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(20.0D);
        // 基本移動速度
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.23000000417232513D);
        // 標準攻撃力１
        getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(1.0D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        /*
         * DataWatcherはクライアントからサーバーへは値を渡さない、渡せない。
         */

        // 使用中リスト
        // 0:Flags
        // 1:Air
        // 2, 3, 4, 5,
        // 6: HP
        // 7, 8:PotionMap
        // 9: ArrowCount
        // 10: 固有名称
        // 11: 名付判定
        // 12: GrowingAge
        // 16: Tame(4), Sit(1)
        // 17: ownerName

        // maidAvater用EntityPlayer互換変数
        // 17 -> 18
        // 18 : Absoption効果をクライアント側へ転送するのに使う
        dataWatcher.addObject(dataWatch_Absoption, 0.0f);

        // 独自分
        // 19:maidColor
        dataWatcher.addObject(dataWatch_Color, (byte) 0);
        // 20:選択テクスチャインデックス
        dataWatcher.addObject(dataWatch_Texture, 0);
        // 21:モデルパーツの表示フラグ
        dataWatcher.addObject(dataWatch_Parts, 0);
        // 22:状態遷移フラグ群(32Bit)、詳細はStatics参照
        dataWatcher.addObject(dataWatch_Flags, 0);
        // 23:GotchaID
        dataWatcher.addObject(dataWatch_Gotcha, 0);
        // 24:メイドモード
        dataWatcher.addObject(dataWatch_Mode, (short) 0);
        // 25:利き腕
        dataWatcher.addObject(dataWatch_DominamtArm, (byte) 0);
        // 26:アイテムの使用判定
        dataWatcher.addObject(dataWatch_ItemUse, 0);
        // 27:保持経験値
        dataWatcher.addObject(dataWatch_ExpValue, 0);

        // TODO:test
        // 31:自由変数、EntityMode等で使用可能な変数。
        dataWatcher.addObject(dataWatch_Free, 0);
    }

    public void initModeList() {
        // AI
        aiBeg = new EntityAIBeg(this, 8F);
        aiBegMove = new EntityAIBegMove(this, 1.2F);
        aiOpenDoor = new EntityAIOpenDoor(this, true);
        aiCloseDoor = new EntityAIRestrictOpenDoor(this);
        aiAvoidPlayer = new EntityAIAvoidPlayer(this, 1.0F, 3);
        aiFollow = new EntityAIFollowOwner(this, 1.0F, 36D, 25D, 81D);
        aiAttack = new EntityAIAttackOnCollide(this, 1.0F, true);
        aiShooting = new EntityAIAttackArrow(this);
        aiCollectItem = new EntityAICollectItem(this, 1.0F);
        aiRestrictRain = new EntityAIRestrictRain(this);
        aiFreeRain = new EntityAIFleeRain(this, 1.0F);
        aiWander = new EntityAIWander(this, 1.0F);
        aiJumpTo = new EntityAIJumpToMaster(this);
        aiFindBlock = new EntityAIFindBlock(this);
        aiSwiming = new EntityAISwimming(this);
        aiPanic = new EntityAIPanic(this, 2.0F);
        aiTracer = new EntityAITracerMove(this);
        aiSit = new EntityAIWait(this);

        // TODO:これいらなくね？
        aiProfiler = worldObj != null && worldObj.theProfiler != null ? worldObj.theProfiler : null;

        // 動作モード用のTasksListを初期化
        EntityAITasks[] ltasks = new EntityAITasks[2];
        ltasks[0] = new EntityAITasks(aiProfiler);
        ltasks[1] = new EntityAITasks(aiProfiler);

        // default
        ltasks[0].addTask(1, aiSwiming);
        ltasks[0].addTask(2, aiSit);
        ltasks[0].addTask(3, aiJumpTo);
        ltasks[0].addTask(4, aiFindBlock);
        ltasks[0].addTask(6, aiAttack);
        ltasks[0].addTask(7, aiShooting);
//		ltasks[0].addTask(8, aiPanic);
        ltasks[0].addTask(10, aiBeg);
        ltasks[0].addTask(11, aiBegMove);
        ltasks[0].addTask(20, aiAvoidPlayer);
        ltasks[0].addTask(21, aiFreeRain);
        ltasks[0].addTask(22, aiCollectItem);
        // 移動用AI
        ltasks[0].addTask(30, aiTracer);
        ltasks[0].addTask(31, aiFollow);
        ltasks[0].addTask(32, aiWander);
        ltasks[0].addTask(33, new EntityAILeapAtTarget(this, 0.3F));
        // Mutexの影響しない特殊行動
        ltasks[0].addTask(40, aiCloseDoor);
        ltasks[0].addTask(41, aiOpenDoor);
        ltasks[0].addTask(42, aiRestrictRain);
        // 首の動き単独
        ltasks[0].addTask(51, new EntityAIWatchClosest(this, EntityLivingBase.class, 10F));
        ltasks[0].addTask(52, new EntityAILookIdle(this));

        // 追加分
        for (EntityModeBase ieml : maidEntityModeList) {
            ieml.addEntityMode(ltasks[0], ltasks[1]);
        }
    }


    public void addMaidMode(EntityAITasks[] peaiTasks, String pmodeName, int pmodeIndex) {
        maidModeList.put(pmodeIndex, peaiTasks);
        maidModeIndexList.put(pmodeName, pmodeIndex);
    }


    public int getMaidModeInt() {
        return maidMode;
    }

    public int getStateWorkingInt() {
        return mstatWorkingInt;
    }

    public String getMaidModeString() {
        if (!isContract()) {
            return getMaidModeString(maidMode);
        } else if (!isRemainsContract()) {
            return "Strike";
        } else if (isMaidWait()) {
            return "Wait";
        } else if (isPlaying()) {
            return "Playing";
        } else {
            String ls = getMaidModeString(maidMode);
            if (maidOverDriveTime.isEnable()) {
                ls = "D-" + ls;
            } else if (isTracer()) {
                ls = "T-" + ls;
            } else if (isFreedom()) {
                ls = "F-" + ls;
            }
            return ls;
        }
    }

    public String getMaidModeString(int index) {
        // モード名称の獲得
        String ls = "";
        for (Entry<String, Integer> le : maidModeIndexList.entrySet()) {
            if (le.getValue() == index) {
                ls = le.getKey();
                break;
            }
        }
        return ls;
    }

    public boolean setMaidMode(String name) {
        return setMaidMode(name, false);
    }

    public boolean setMaidMode(String name, boolean playing) {
        if (!maidModeIndexList.containsKey(name)) {
            return false;
        }
        return setMaidMode(maidModeIndexList.get(name), playing);
    }

    public boolean setMaidMode(int index) {
        return setMaidMode(index, false);
    }


    public boolean setMaidMode(int index, boolean playing) {
        // モードに応じてAIを切り替える
        velocityChanged = true;
        if (!maidModeList.containsKey(index)) return false;
        if (maidMode == index) return true;

        if (playing) {

        } else {
            mstatWorkingInt = index;
        }
        mstatModeName = getMaidModeString(index);
        maidAvatar.addChatMessage(new ChatComponentText(mstatModeName));
        maidMode = index;
        dataWatcher.updateObject(dataWatch_Mode, (short) maidMode);
        EntityAITasks[] ltasks = maidModeList.get(index);

        // AIを根底から書き換える
        if (ltasks.length > 0 && ltasks[0] != null) {
            setMaidModeAITasks(ltasks[0], tasks);
        } else {
            setMaidModeAITasks(null, tasks);
        }
        if (ltasks.length > 1 && ltasks[1] != null) {
            setMaidModeAITasks(ltasks[1], targetTasks);
        } else {
            setMaidModeAITasks(null, targetTasks);
        }

        // モード切替に応じた処理系を確保
        maidAvatar.stopUsingItem();
        setSitting(false);
        setSneaking(false);
        setActiveModeClass(null);
        aiJumpTo.setEnable(true);
//		aiFollow.setEnable(true);
        aiAttack.setEnable(true);
        aiShooting.setEnable(false);
        aiAvoidPlayer.setEnable(true);
//		aiWander.setEnable(maidFreedom);
        setBloodsuck(false);
        clearTilePosAll();
        for (int li = 0; li < maidEntityModeList.size(); li++) {
            EntityModeBase iem = maidEntityModeList.get(li);
            if (iem.setMode(maidMode)) {
                setActiveModeClass(iem);
                aiFollow.minDist = iem.getRangeToMaster(0);
                aiFollow.maxDist = iem.getRangeToMaster(1);
                break;
            }
        }
        getNextEquipItem();

        return true;
    }

    protected void setMaidModeAITasks(EntityAITasks taskSrc, EntityAITasks taskDest) {
        // 既存のAIを削除して置き換える。
        // 動作をクリア
        try {
            ArrayList<EntityAITaskEntry> taskEntriesDest = getTaskEntries(taskDest);
            ArrayList<EntityAITaskEntry> executingTaskEntriesDest = getExecutingTaskEntries(taskDest);

            if (taskSrc == null) {
                if (taskEntriesDest != null) taskEntriesDest.clear();
                if (executingTaskEntriesDest != null) executingTaskEntriesDest.clear();
            } else {
                ArrayList<EntityAITaskEntry> taskEntriesSrc = getTaskEntries(taskSrc);
                ArrayList<EntityAITaskEntry> executingTaskEntriesSrc = getExecutingTaskEntries(taskSrc);

                Iterator<EntityAITaskEntry> iterator;
                iterator = executingTaskEntriesDest.iterator();
                while (iterator.hasNext()) {
                    EntityAITaskEntry taskEntry = iterator.next();
                    taskEntry.action.resetTask();
                }
                executingTaskEntriesDest.clear();

                taskEntriesDest.clear();
                taskEntriesDest.addAll(taskEntriesSrc);
                // TODO: 未実装の機能、モードチェンジ時の初期化を行う。
                for (EntityAITaskEntry task : taskEntriesSrc) {
                    if (task instanceof IEntityAI) {
//						((LMM_IEntityAI)task).setDefaultEnable();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static ArrayList<EntityAITaskEntry> getTaskEntries(EntityAITasks task) {
        try {
            return ObfuscationReflectionHelper.getPrivateValue(EntityAITasks.class, task, "field_75782_a", "taskEntries");
        } catch (Exception e) {
            return null;
        }
    }

    public static ArrayList<EntityAITaskEntry> getExecutingTaskEntries(EntityAITasks task) {
        try {
            return ObfuscationReflectionHelper.getPrivateValue(EntityAITasks.class, task, "field_75780_b", "executingTaskEntries");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 適用されているモードクラス
     */
    public EntityModeBase getActiveModeClass() {
        return maidActiveModeClass;
    }

    public void setActiveModeClass(EntityModeBase pEntityMode) {
        maidActiveModeClass = pEntityMode;
    }

    public boolean isActiveModeClass() {
        return maidActiveModeClass != null;
    }

    // 効果音の設定
    @Override
    protected String getHurtSound() {
        playSound(maidDamageSound, true);
        return null;
    }

    @Override
    protected String getDeathSound() {
        playSound(EnumSound.death, true);
        return null;
    }

    @Override
    protected String getLivingSound() {
        // 普段の声
        EnumSound so = EnumSound.Null;
        if (getHealth() < 10)
            so = EnumSound.living_whine;
        else if (rand.nextFloat() < maidSoundRate) {
            if (mstatTime > 23500 || mstatTime < 1500) {
                so = EnumSound.living_morning;
            } else if (mstatTime < 12500) {
                if (isContract()) {
                    BiomeGenBase biomegenbase = worldObj.getBiomeGenForCoords(MathHelper.floor_double(posX + 0.5D), MathHelper.floor_double(posZ + 0.5D));
                    BiomeGenBase.TempCategory temperature = biomegenbase.getTempCategory();
                    if (temperature == BiomeGenBase.TempCategory.COLD) {
                        so = EnumSound.living_cold;
                    } else if (temperature == BiomeGenBase.TempCategory.WARM) {
                        so = EnumSound.living_hot;
                    } else {
                        so = EnumSound.living_daytime;
                    }
                    if (worldObj.isRaining()) {
                        if (biomegenbase.canSpawnLightningBolt()) {
                            so = EnumSound.living_rain;
                        } else if (biomegenbase.getEnableSnow()) {
                            so = EnumSound.living_snow;
                        }
                    }
                } else {
                    so = EnumSound.living_daytime;
                }
            } else {
                so = EnumSound.living_night;
            }
        }

        LittleMaidMobX.debug("id:%d LivingSound:%s", getEntityId(), worldObj == null ? "null" : worldObj.isRemote ? "Client" : "Server");
        playLittleMaidSound(so, false);
        return null;
    }

    /**
     * 簡易音声再生、標準の音声のみ使用すること。
     */
    public void playSound(String pname) {
        playSound(pname, 0.5F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);
    }

    /**
     * ネットワーク対応音声再生
     */
    public void playSound(EnumSound enumsound, boolean force) {
        if ((maidSoundInterval > 0 && !force) || enumsound == EnumSound.Null) return;
        maidSoundInterval = LittleMaidConfig.voiceSoundInterval;
        if (worldObj.isRemote) {
            // Client
//			String lsound = LMM_SoundManager.getSoundValue(enumsound, textureName, maidColor & 0x00ff);
//			float lpitch = mod_LMM_littleMaidMob.VoiceDistortion ? (rand.nextFloat() * 0.2F) + 0.95F : 1.0F;
//			worldObj.playSound(posX, posY, posZ, lsound, getSoundVolume(), lpitch, false);
        } else {
            // Server
            LittleMaidMobX.debug("id:%d-%s, seps:%04x-%s", getEntityId(), worldObj.isRemote ? "Client" : "Server", enumsound.index, enumsound.name());
            NetworkHandler.sendPacketToAllTracking(new PlayLittleMaidSoundPacket(this, enumsound), this);
        }
    }

    /**
     * 音声再生用。
     * 通常の再生ではネットワーク越しになるのでその対策。
     */
    public void playLittleMaidSound(EnumSound enumsound, boolean force) {
        // 音声の再生
        if ((maidSoundInterval > 0 && !force) || enumsound == EnumSound.Null) return;
        maidSoundInterval = LittleMaidConfig.voiceSoundInterval;
        if (worldObj.isRemote) {
            // Client
            String s = LittleMaidSoundManager.getSoundValue(enumsound, textureData.getTextureName(0), textureData.getColor());
            if (s != null && !s.isEmpty() && !s.startsWith("minecraft:")) {
                s = LittleMaidMobX.MOD_ID + ":" + s;
            }
            LittleMaidMobX.debug(String.format("id:%d, se:%04x-%s (%s)", getEntityId(), enumsound.index, enumsound.name(), s));
            if (s != null && !s.isEmpty()) {
                float lpitch = LittleMaidConfig.voiceDistortion ? (rand.nextFloat() * 0.2f) + 0.95f : 1.0f;
                worldObj.playSound(posX, posY, posZ, s, getSoundVolume(), lpitch, false);
            }
        }
    }

    @Override
    public void onKillEntity(EntityLivingBase par1EntityLiving) {
        super.onKillEntity(par1EntityLiving);
        if (isBloodsuck()) {
//			mod_LMM_littleMaidMob.Debug("nice Kill.");
            playSound(EnumSound.laughter, true);
        } else {
            setTarget(null);
            setAttackTarget(null);
        }
    }

    @Override
    protected boolean canDespawn() {
        return !this.isTamed() && LittleMaidConfig.canDespawn;
    }

    @Override
    public boolean getCanSpawnHere() {
        // スポーン可能か？
        // TODO Cause a infinite dupe bug
        /*if (LittleMaidConfig.spawnLimit <= getMaidCount()) {
            LittleMaidMobX.Debug("Spawn Limit.");
            return false;
        }*/
        int lx = MathHelper.floor_double(this.posX);
        int ly = MathHelper.floor_double(this.boundingBox.minY);
        int lz = MathHelper.floor_double(this.posZ);
        // TODO:サーバー側で判定できないので意味なし?
		/* MMM_TextureBox lbox = MMM_TextureManager.instance.getTextureBox(textureData.textureBox[0]);
		if (worldObj == null || textureData.textureModel == null
				|| !textureData.textureBox[0].getCanSpawnHere(worldObj, lx, ly, lz, this)) {
			mod_LMM_littleMaidMob.Debug(String.format("%s is can't spawn hear.", textureName));
			return false;
		} */
        if (LittleMaidConfig.spawnMaidsEverywhere) {
            // ドミナント
            return this.worldObj.checkNoEntityCollision(this.boundingBox)
                    && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox).isEmpty()
                    && !this.worldObj.isAnyLiquid(this.boundingBox)
                    && this.getBlockPathWeight(lx, ly, lz) >= 0.0F;
        } else {
            return super.getCanSpawnHere();
        }
    }

    @Override
    public void setDead() {
        if (mstatgotcha != null) {
            // 首紐をドロップ
            EntityItem entityitem = new EntityItem(worldObj, mstatgotcha.posX, mstatgotcha.posY, mstatgotcha.posZ, new ItemStack(Items.string));
            worldObj.spawnEntityInWorld(entityitem);
            mstatgotcha = null;
        }
        super.setDead();
    }

    /**
     * 読み込み領域内のメイドさんの数
     */
    public int getMaidCount() {
        int lj = 0;
        for (int li = 0; li < worldObj.loadedEntityList.size(); li++) {
            if (worldObj.loadedEntityList.get(li) instanceof EntityLittleMaid) {
                lj++;
            }
        }
        return lj;
    }

    @Override
    public EntityAgeable createChild(EntityAgeable var1) {
        // お子さんの設定
        return null;
    }

    // エフェクト表示
    protected void showParticleFX(String s) {
        showParticleFX(s, 1D, 1D, 1D);
    }

    protected void showParticleFX(String s, double d, double d1, double d2) {
        showParticleFX(s, d, d1, d2, 0D, 0D, 0D);
    }

    public void showParticleFX(String s, double d, double d1, double d2, double d3, double d4, double d5) {
        for (int i = 0; i < 7; i++) {
            double d6 = rand.nextGaussian() * d + d3;
            double d7 = rand.nextGaussian() * d1 + d4;
            double d8 = rand.nextGaussian() * d2 + d5;
            worldObj.spawnParticle(s, (posX + (double) (rand.nextFloat() * width * 2.0F)) - (double) width, posY + 0.5D + (double) (rand.nextFloat() * height), (posZ + (double) (rand.nextFloat() * width * 2.0F)) - (double) width, d6, d7, d8);
        }
    }

    @Override
    public void handleHealthUpdate(byte par1) {
        // worldObj.setEntityState(this, (byte))で指定されたアクションを実行
        switch (par1) {
            case 10:
                // 不機嫌
                showParticleFX("smoke", 0.02D, 0.02D, 0.02D);
                break;
            case 11:
                // ゴキゲン
                double a = getContractLimitDays() / 7D;
                double d6 = a * 0.3D;
                double d7 = a;
                double d8 = a * 0.3D;
                worldObj.spawnParticle("note", posX, posY + height + 0.1D, posZ, d6, d7, d8);
                break;
            case 12:
                // 自由行動
                showParticleFX("reddust", 0.5D, 0.5D, 0.5D, 1.0D, 1.0D, 1.0D);
                break;
            case 13:
                // 不自由行動
                showParticleFX("smoke", 0.02D, 0.02D, 0.02D);
                break;
            case 14:
                // トレーサー
                showParticleFX("explode", 0.3D, 0.3D, 0.3D, 0.0D, 0.0D, 0.0D);
                break;

            default:
                super.handleHealthUpdate(par1);
        }
    }

    // ポーション効果のエフェクト
    public void setAbsorptionAmount(float par1) {
        // AbsorptionAmount
        if (par1 < 0.0F) {
            par1 = 0.0F;
        }

        this.getDataWatcher().updateObject(dataWatch_Absoption, Float.valueOf(par1));
    }

    public float getAbsorptionAmount() {
        return this.getDataWatcher().getWatchableObjectFloat(dataWatch_Absoption);
    }


    public int colorMultiplier(float pLight, float pPartialTicks) {
        // 発光処理用
        int lbase = 0;
        if (maidOverDriveTime.isDelay()) {
            int i;
            if (maidOverDriveTime.isEnable()) {
                i = 100;
            } else {
                i = 100 + maidOverDriveTime.getValue();
            }
            lbase = i << 24 | 0x00df0f0f;
        }

        if (isActiveModeClass()) {
            lbase = lbase | getActiveModeClass().colorMultiplier(pLight, pPartialTicks);
        }

        return lbase;
    }


    // AI関連
    @Override
    protected boolean isAIEnabled() {
        // 新AI対応
        return true;
    }

    /**
     * 敵味方識別
     */
    public boolean getIFF(Entity pEntity) {
        // 敵味方識別(敵=false)
        if (pEntity == null || pEntity == mstatMasterEntity) {
            return true;
        }

        int tt = IFF.getIFF(getMaidMaster(), pEntity);
        switch (tt) {
            case IFF.iff_Enemy:
                return false;
            case IFF.iff_Friendry:
                return true;
            case IFF.iff_Unknown:
                if (isBloodsuck()) {
                    // 血に餓えている時は敵
                    return false;
                }
                if (pEntity instanceof EntityLittleMaid) {
                    // お遊びモードのメイドには敵対しない
                    if (((EntityLittleMaid) pEntity).mstatPlayingRole > EntityModePlaying.mpr_NULL) {
                        return true;
                    }
                }
                if (pEntity instanceof EntityCreature) {
                    // 相手が何をターゲットにしているかで決まる
                    Entity et = ((EntityCreature) pEntity).getEntityToAttack();
                    if (et != null && et == mstatMasterEntity) {
                        return false;
                    }
                    if (et == this) {
                        return false;
                    }
                    if (et instanceof EntityLittleMaid) {
                        // 同じマスターのメイドを攻撃対象としている
                        if (((EntityLittleMaid) et).getMaidMasterEntity() == mstatMasterEntity) {
                            return false;
                        }
                    }
                }
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean canAttackClass(Class par1Class) {
        // IFFの設定、クラス毎の判定しかできないので使わない。
        return true;
    }

    @Override
    public boolean attackEntityAsMob(Entity par1Entity) {

        // 正常時は回復優先処理
        if (getHealth() < 10 && !isBloodsuck() && maidInventory.hasItem(Items.sugar)) {
            return true;
        }

        // 特殊な攻撃処理
        if (isActiveModeClass() && getActiveModeClass().attackEntityAsMob(maidMode, par1Entity)) {
            return true;
        }

        // 標準処理
        setSwing(20, isBloodsuck() ? EnumSound.attack_bloodsuck : EnumSound.attack);
        maidAvatar.attackTargetEntityWithCurrentItem(par1Entity);
        return true;
    }

    @Override
    public boolean isBreedingItem(ItemStack par1ItemStack) {
        // お好みは何？
        if (isContractEX()) {
            return ItemHelper.isSugar(par1ItemStack);
        } else {
            return ItemHelper.isCake(par1ItemStack);
        }
    }


    @Override
    public void writeEntityToNBT(NBTTagCompound par1nbtTagCompound) {
        // データセーブ
        super.writeEntityToNBT(par1nbtTagCompound);
        if (this.getMaidMasterEntity() != null) {
            par1nbtTagCompound.setString("OwnerUUID", getMaidMasterEntity().getUniqueID().toString());
            par1nbtTagCompound.setString("MaidOwner", this.getMaidMaster());
        }
        par1nbtTagCompound.setTag("Inventory", maidInventory.writeToNBT(new NBTTagList()));
        par1nbtTagCompound.setString("Mode", getMaidModeString(mstatWorkingInt));
        par1nbtTagCompound.setBoolean("Wait", isMaidWait());
        par1nbtTagCompound.setBoolean("Freedom", isFreedom());
        par1nbtTagCompound.setBoolean("Tracer", isTracer());
        par1nbtTagCompound.setInteger("LimitCount", maidContractLimit);
        par1nbtTagCompound.setLong("Anniversary", maidAnniversary);
        par1nbtTagCompound.setInteger("EXP", experienceValue);
        par1nbtTagCompound.setInteger("DominantArm", maidDominantArm);
        par1nbtTagCompound.setInteger("Color", textureData.getColor());
        par1nbtTagCompound.setString("texName", textureData.getTextureName(0));
        par1nbtTagCompound.setString("texArmor", textureData.getTextureName(1));
        // HomePosition
        par1nbtTagCompound.setInteger("homeX", getHomePosition().posX);
        par1nbtTagCompound.setInteger("homeY", getHomePosition().posY);
        par1nbtTagCompound.setInteger("homeZ", getHomePosition().posZ);
        par1nbtTagCompound.setInteger("homeWorld", homeWorld);
        // Tiles
        NBTTagCompound lnbt = new NBTTagCompound();
        par1nbtTagCompound.setTag("Tiles", lnbt);
        for (int li = 0; li < maidTiles.length; li++) {
            if (maidTiles[li] != null) {
                lnbt.setIntArray(String.valueOf(li), maidTiles[li]);
            }
        }
        // 追加分
        for (int li = 0; li < maidEntityModeList.size(); li++) {
            maidEntityModeList.get(li).writeEntityToNBT(par1nbtTagCompound);
        }

        //攻撃された回数
        par1nbtTagCompound.setInteger("attackCount", attackCount);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound par1nbtTagCompound) {
        // データロード
        super.readEntityFromNBT(par1nbtTagCompound);

        if (par1nbtTagCompound.hasKey("MaidOwner")) {
            String maidOwner = par1nbtTagCompound.getString("MaidOwner");
            W_Common.setOwner(this, maidOwner);
        }
        if (par1nbtTagCompound.hasKey("ModeColor")) {
            // 旧版からの継承
            String s = par1nbtTagCompound.getString("Master");
            if (!s.isEmpty()) {
                W_Common.setOwner(this, s);
                setContract(true);
            }
            NBTTagList nbttaglist = par1nbtTagCompound.getTagList("Inventory", 10);
            maidInventory.readFromNBT(nbttaglist);
            // アーマースロット変更に対応するためのコード
            ItemStack[] armi = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                ItemStack is = maidInventory.armorItemInSlot(i);
                if (is != null) {
                    armi[3 - ((ItemArmor) is.getItem()).armorType] = is;
                }
            }
            maidInventory.armorInventory = armi;
            //
            setMaidWait(par1nbtTagCompound.getBoolean("Wait"));
            setFreedom(par1nbtTagCompound.getBoolean("Freedom"));
            setTracer(par1nbtTagCompound.getBoolean("Tracer"));
            textureData.textureIndex[0] = MMM_TextureManager.instance.getIndexTextureBoxServer(this, par1nbtTagCompound.getString("texName"));
            textureData.textureIndex[1] = MMM_TextureManager.instance.getIndexTextureBoxServer(this, par1nbtTagCompound.getString("texArmor"));
            textureData.textureBox[0] = MMM_TextureManager.instance.getTextureBoxServer(textureData.textureIndex[0]);
            textureData.textureBox[1] = MMM_TextureManager.instance.getTextureBoxServer(textureData.textureIndex[1]);
            byte b = par1nbtTagCompound.getByte("ModeColor");
            setColor(b & 0x0f);
            switch ((b & 0xf0) >> 4) {
                case 0:
                    setMaidMode(0x0000);    // Wild
                    break;
                case 2:
                    setMaidMode(0x0001);    // Escorter
                    break;
                case 4:
                    setMaidMode(0x0080);    // Fencer
                    break;
                case 5:
                    setMaidMode(0x0000);    // Healer
                    break;
                case 6:
                    setMaidMode(0x0021);    // Cooking
                    break;
                case 7:
                    setMaidMode(0x00c0);    // Bloodsucker
                    break;
                case 8:
                    setMaidMode(0x0083);    // Archer
                    break;
                case 9:
                    setMaidMode(0x00c3);    // Blazingstar
                    break;
                case 10:
                    setMaidMode(0x0081);    // Ripper
                    break;
                case 11:
                    setMaidMode(0x00c2);    // Detonator
                    break;
                case 12:
                    setMaidMode(0x00c1);    // TNT-D
                    break;
                case 13:
                    setMaidMode(0x0020);    // Torcher
                    break;
                case 15:
                    setMaidMode(0x0000);    // Pharmacist
                    break;
                default:
                    setMaidMode(0x0000);    // Wild
            }
//			setMaidMode((b & 0xf0) >> 4);
            int lhx = MathHelper.floor_double(posX);
            int lhy = MathHelper.floor_double(posY);
            int lhz = MathHelper.floor_double(posZ);
            ;
//			func_110172_bL().set(lhx, lhy, lhz);
            getHomePosition().set(lhx, lhy, lhz);
            long lcl = par1nbtTagCompound.getLong("Limit");
            if (isContract() && lcl == 0) {
                maidContractLimit = 24000;
            } else {
                maidContractLimit = (int) ((lcl - worldObj.getTotalWorldTime()));
            }
            maidAnniversary = par1nbtTagCompound.getLong("Anniversary");
            if (maidAnniversary == 0L && isContract()) {
                // ダミーの数値を入れる
                maidAnniversary = worldObj.getWorldTime() - getEntityId();
            }

        } else {
            // 新型
            LittleMaidMobX.debug("read." + worldObj.isRemote);

            maidInventory.readFromNBT(par1nbtTagCompound.getTagList("Inventory", 10));
            setMaidWait(par1nbtTagCompound.getBoolean("Wait"));
            setFreedom(par1nbtTagCompound.getBoolean("Freedom"));
            setTracer(par1nbtTagCompound.getBoolean("Tracer"));
            setMaidMode(par1nbtTagCompound.getString("Mode"));
            if (par1nbtTagCompound.hasKey("LimitCount")) {
                maidContractLimit = par1nbtTagCompound.getInteger("LimitCount");
            } else {
                long lcl = par1nbtTagCompound.getLong("Limit");
                if (isContract() && lcl == 0) {
                    maidContractLimit = 24000;
                } else {
                    maidContractLimit = (int) ((lcl - worldObj.getWorldTime()));
                }
            }
            if (isContract() && maidContractLimit == 0) {
                // 値がおかしい時は１日分
//				maidContractLimit = worldObj.getWorldTime() + 24000L;
                maidContractLimit = 24000;
            }
            maidAnniversary = par1nbtTagCompound.getLong("Anniversary");
            if (maidAnniversary == 0L && isContract()) {
                // ダミーの数値を入れる
                maidAnniversary = worldObj.getWorldTime() - getEntityId();
            }
            if (maidAvatar != null) {
                maidAvatar.experienceTotal = par1nbtTagCompound.getInteger("EXP");
            }
            setDominantArm(par1nbtTagCompound.getInteger("DominantArm"));
            if (mstatSwingStatus.length <= maidDominantArm) {
                maidDominantArm = 0;
            }
            textureData.textureIndex[0] = MMM_TextureManager.instance.getIndexTextureBoxServer(this, par1nbtTagCompound.getString("texName"));
            textureData.textureIndex[1] = MMM_TextureManager.instance.getIndexTextureBoxServer(this, par1nbtTagCompound.getString("texArmor"));
            textureData.textureBox[0] = MMM_TextureManager.instance.getTextureBoxServer(textureData.textureIndex[0]);
            textureData.textureBox[1] = MMM_TextureManager.instance.getTextureBoxServer(textureData.textureIndex[1]);
            textureData.setColor(par1nbtTagCompound.getInteger("Color"));
            setTexturePackIndex(textureData.color, textureData.getTextureIndex());

            // HomePosition
            int lhx = par1nbtTagCompound.getInteger("homeX");
            int lhy = par1nbtTagCompound.getInteger("homeY");
            int lhz = par1nbtTagCompound.getInteger("homeZ");
//			func_110172_bL().set(lhx, lhy, lhz);
            getHomePosition().set(lhx, lhy, lhz);
            homeWorld = par1nbtTagCompound.getInteger("homeWorld");

            // Tiles
            NBTTagCompound lnbt = par1nbtTagCompound.getCompoundTag("Tiles");
            for (int li = 0; li < maidTiles.length; li++) {
                int ltile[] = lnbt.getIntArray(String.valueOf(li));
                maidTiles[li] = ltile.length > 0 ? ltile : null;
            }

            // 追加分
            for (int li = 0; li < maidEntityModeList.size(); li++) {
                maidEntityModeList.get(li).readEntityFromNBT(par1nbtTagCompound);
            }
        }
        onInventoryChanged();

        // ドッペル対策
        if (LittleMaidConfig.fixLittleMaidDupe && maidAnniversary > 0L) {
            for (int i = 0; i < worldObj.loadedEntityList.size(); i++) {
                Entity entity1 = (Entity) worldObj.loadedEntityList.get(i);
                if (!entity1.isDead && entity1 instanceof EntityLittleMaid) {
                    EntityLittleMaid elm = (EntityLittleMaid) entity1;
                    if (elm != this && elm.isContract() && elm.maidAnniversary == maidAnniversary
                            && elm.getMaidMaster().equalsIgnoreCase(getMaidMaster())) {
                        // 新しい方を残す
                        if (getEntityId() > elm.getEntityId()) {
                            LittleMaidMobX.debug(String.format("Load Doppelganger ID:%d, %d", elm.getEntityId(), maidAnniversary));
                            //worldObj.getPlayerEntityByName(getMaidMaster()).addChatMessage(new ChatComponentText(String.format("Load Doppelganger ID:%d, %d" ,elm.getEntityId(), maidAnniversary)));
                            elm.setDead();
                        } else {
                            //worldObj.getPlayerEntityByName(getMaidMaster()).addChatMessage(new ChatComponentText(String.format("Load Doppelganger ID:%d, %d" ,getEntityId(), maidAnniversary)));
                            LittleMaidMobX.debug(String.format("Load Doppelganger ID:%d, %d", getEntityId(), maidAnniversary));
                            setDead();
                            break;
                        }
                    }
                }
            }
        } else {
            LittleMaidMobX.debug(String.format("Load ID:%d, MaidMaster:%s, x:%.1f, y:%.1f, z:%.1f, %d", getEntityId(), getMaidMaster(), posX, posY, posZ, maidAnniversary));
        }

        attackCount = par1nbtTagCompound.getInteger("attackCount");
    }

    @Override
    public IIcon getItemIcon(ItemStack par1ItemStack, int par2) {
        // アイテムの表示
        if (maidAvatar != null) {
            return maidAvatar.getItemIcon(par1ItemStack, par2);
        }

        if (par1ItemStack.getItem().requiresMultipleRenderPasses()) {
            return par1ItemStack.getItem().getIconFromDamageForRenderPass(par1ItemStack.getItemDamage(), par2);
        } else {
            return super.getItemIcon(par1ItemStack, par2);
        }
    }

    public boolean canBePushed() {
        // See LittleMaidMobNX@93256b55c
        if (isMaidWait()) return false;

        // --------------------------------------------
        // 肩車状態でプレイヤーが馬に乗っているときは、当たり判定をなくす。
        if (ridingEntity != null && ridingEntity == mstatMasterEntity) {
            if (ridingEntity.ridingEntity instanceof EntityHorse) {
                return false;
            }
        }
        // --------------------------------------------

        return !this.isDead;
    }

    // おんぶおばけは無敵
    @Override
    public boolean canBeCollidedWith() {
        if (ridingEntity != null && ridingEntity == mstatMasterEntity) {
            ItemStack litemstack = ((EntityPlayer) mstatMasterEntity).getCurrentEquippedItem();
            return (litemstack == null) || (litemstack.getItem() == Items.saddle);
        } else {
            return super.canBeCollidedWith();
        }
    }

    @Override
    public boolean canAttackWithItem() {
        if (ridingEntity != null && ridingEntity == mstatMasterEntity) {
            return false;
        } else {
            return super.canAttackWithItem();
        }
    }

    @Override
    public double getMountedYOffset() {
        // TODO:ここは要調整
        if (riddenByEntity instanceof EntityChicken) {
            return height + 0.03D;
        }
        if (riddenByEntity instanceof EntitySquid) {
            return height - 0.2D;
        }
        return super.getMountedYOffset() + 0.35D;
    }

    @Override
    public double getYOffset() {
        if (ridingEntity instanceof EntityPlayer) {
            // 姿勢制御
//			setSneaking(true);
//			mstatAimeBow = true;
//			updateAimebow();
//			return (double)(yOffset - 1.8F);

            // --------------------------------------------
            // プレイヤーが馬に乗っているときは、肩車ではなく馬の後ろに乗る
            if (ridingEntity.ridingEntity instanceof EntityHorse) {
                if (this.worldObj.isRemote) {
                    return (double) (yOffset - 2.8F);
                } else {
                    return (double) (yOffset - 1.0F);
                }
            }
            // プレイヤーに肩車
            else {
                return (double) (yOffset - 2.0F);
            }
            // --------------------------------------------
        }
        return (double) (yOffset - 0.25F);
    }

    @Override
    public void updateRidden() {
        // TODO:アップデート時にチェック
        ++ticksExisted;
        //

        if (ridingEntity instanceof EntityPlayer) {
            EntityPlayer lep = (EntityPlayer) ridingEntity;

            // ヘッドハガー
            renderYawOffset = lep.renderYawOffset;
            prevRenderYawOffset = lep.prevRenderYawOffset;
            double llpx = lastTickPosX;
            double llpy = lastTickPosY;
            double llpz = lastTickPosZ;

            // ★注意：水に触れると ridingEntity はnullになる ★
            super.updateRidden();

            renderYawOffset = lep.renderYawOffset;
            if (((rotationYaw - renderYawOffset) % 360F) > 90F) {
                rotationYaw = renderYawOffset + 90F;
            }
            if (((rotationYaw - renderYawOffset) % 360F) < -90F) {
                rotationYaw = renderYawOffset - 90F;
            }
            if (((rotationYawHead - renderYawOffset) % 360F) > 90F) {
                rotationYawHead = renderYawOffset + 90F;
            }
            if (((rotationYawHead - renderYawOffset) % 360F) < -90F) {
                rotationYawHead = renderYawOffset - 90F;
            }

            double dx, dz;
            // --------------------------------------------
            // プレイヤーが馬に乗っているときは、肩車ではなく馬の後ろに乗る
            // ridingEntity はsuper.updateRidden();によってNULLになる事があるので注意
            if (lep.ridingEntity instanceof EntityHorse) {
                EntityHorse horse = (EntityHorse) lep.ridingEntity;
                if (this.worldObj.isRemote) {
                    dx = Math.sin(((double) horse.renderYawOffset * Math.PI) / 180D) * 0.5;
                    dz = Math.cos(((double) horse.renderYawOffset * Math.PI) / 180D) * 0.5;
                } else {
                    dx = Math.sin(((double) horse.renderYawOffset * Math.PI) / 180D) * 0.9;
                    dz = Math.cos(((double) horse.renderYawOffset * Math.PI) / 180D) * 0.9;
                }
            } else {
                dx = Math.sin(((double) lep.renderYawOffset * Math.PI) / 180D) * 0.35;
                dz = Math.cos(((double) lep.renderYawOffset * Math.PI) / 180D) * 0.35;
            }
            // --------------------------------------------

            setPosition(lep.posX + dx, posY, lep.posZ - dz);
            lastTickPosX = llpx;
            lastTickPosY = llpy;
            lastTickPosZ = llpz;
        } else {
            super.updateRidden();
        }
    }

    @Override
    public void updateRiderPosition() {
        super.updateRiderPosition();
    }

    @Override
    public float getSwingProgress(float par1) {
        for (SwingStatus lswing : mstatSwingStatus) {
            lswing.getSwingProgress(par1);
        }
        return getSwingStatusDominant().onGround;
    }

    // 首周り
    public void setLooksWithInterest(boolean f) {
        if (looksWithInterest != f) {
            looksWithInterest = f;
            if (numTicksToChaseTarget <= 0) {
                looksWithInterestAXIS = rand.nextBoolean();
            }
            int li = dataWatcher.getWatchableObjectInt(dataWatch_Flags);
            li = looksWithInterest ? (li | dataWatch_Flags_looksWithInterest) : (li & ~dataWatch_Flags_looksWithInterest);
            li = looksWithInterestAXIS ? (li | dataWatch_Flags_looksWithInterestAXIS) : (li & ~dataWatch_Flags_looksWithInterestAXIS);
            dataWatcher.updateObject(dataWatch_Flags, Integer.valueOf(li));
        }
    }

    public boolean getLooksWithInterest() {
        looksWithInterest = (dataWatcher.getWatchableObjectInt(dataWatch_Flags) & dataWatch_Flags_looksWithInterest) > 0;
        looksWithInterestAXIS = (dataWatcher.getWatchableObjectInt(dataWatch_Flags) & dataWatch_Flags_looksWithInterestAXIS) > 0;

        return looksWithInterest;
    }

    public float getInterestedAngle(float f) {
        return (prevRotateAngleHead + (rotateAngleHead - prevRotateAngleHead) * f) * ((looksWithInterestAXIS ? 0.08F : -0.08F) * (float) Math.PI);
    }


    // ダメージコントロール
//	@Override
    public boolean isBlocking() {
        return getSwingStatusDominant().isBlocking();
//		return maidAvatar.isBlocking();
    }

    @Override
    protected void damageArmor(float pDamage) {
        maidInventory.damageArmor(pDamage);
        getAvatarIF().W_damageArmor(pDamage);
    }

    @Override
    public int getTotalArmorValue() {
        return maidAvatar.getTotalArmorValue();
    }

    @Override
    protected float applyArmorCalculations(DamageSource par1DamageSource, float par2) {
        return getAvatarIF().W_applyArmorCalculations(par1DamageSource, par2);
    }

    @Override
    protected float applyPotionDamageCalculations(DamageSource par1DamageSource, float par2) {
        return getAvatarIF().W_applyPotionDamageCalculations(par1DamageSource, par2);
    }

    @Override
    protected void damageEntity(DamageSource par1DamageSource, float par2) {
        // ダメージソースに応じて音声変更
        if (par1DamageSource == DamageSource.fall) {
            maidDamageSound = EnumSound.hurt_fall;
        }
        if (!par1DamageSource.isUnblockable() && isBlocking()) {
            // ブロッキング
//			par2 = (1.0F + par2) * 0.5F;
            LittleMaidMobX.debug(String.format("Blocking success ID:%d, %f -> %f", this.getEntityId(), par2, (par2 = (1.0F + par2) * 0.5F)));
            maidDamageSound = EnumSound.hurt_guard;
        }

        // 被ダメ
        float llasthealth = getHealth();
        if (par2 > 0 && getActiveModeClass() != null && !getActiveModeClass().damageEntity(maidMode, par1DamageSource, par2)) {
            getAvatarIF().W_damageEntity(par1DamageSource, par2);

            // ダメージを受けると待機を解除
            setMaidWait(false);
        }

        if (llasthealth == getHealth() && maidDamageSound == EnumSound.hurt) {
            maidDamageSound = EnumSound.hurt_nodamege;
        }
        LittleMaidMobX.debug(String.format("GetDamage ID:%d, %s, %f/ %f", this.getEntityId(), par1DamageSource.damageType, llasthealth - getHealth(), par2));
//		super.damageEntity(par1DamageSource, par2);
    }

    @Override
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2) {
        Entity entity = par1DamageSource.getEntity();

        if (par1DamageSource.getDamageType().equalsIgnoreCase("thrown")) {
            if (entity != null && this.maidAvatar != null && entity.getEntityId() == this.maidAvatar.getEntityId()) {
                return false;
            }
        }

        LittleMaidMobX.debug("LMM_EntityLittleMaid.attackEntityFrom " + this + "(" + this.maidAvatar + ") <= " + entity);

        // ダメージソースを特定して音声の設定
        maidDamageSound = EnumSound.hurt;
        if (par1DamageSource == DamageSource.inFire || par1DamageSource == DamageSource.onFire || par1DamageSource == DamageSource.lava) {
            maidDamageSound = EnumSound.hurt_fire;
        }
        for (EntityModeBase lm : maidEntityModeList) {
            float li = lm.attackEntityFrom(par1DamageSource, par2);
            if (li > 0) return li == 1 ? false : true;
        }

        setMaidWait(false);
        setMaidWaitCount(0);
        if (par2 > 0) {
            // 遊びは終わりだ！
            setPlayingRole(0);
            getNextEquipItem();
        }
        // ゲーム難易度によるダメージの補正
        if (isContract() && (entity instanceof EntityLivingBase) || (entity instanceof EntityArrow)) {
            if (worldObj.difficultySetting == EnumDifficulty.PEACEFUL) {
                par2 = 0;
            }
            if (worldObj.difficultySetting == EnumDifficulty.EASY && par2 > 0) {
                par2 = par2 / 2 + 1;
            }
            if (worldObj.difficultySetting == EnumDifficulty.HARD) {
                par2 = (par2 * 3) / 2;
            }
        }

//		if (par2 == 0 && maidMode != mmode_Detonator) {
        if (par2 == 0) {
            // ノーダメージ
            if (maidDamageSound == EnumSound.hurt) {
                maidDamageSound = EnumSound.hurt_nodamege;
            }
            playSound(maidDamageSound, true);
            return false;
        }

        if (super.attackEntityFrom(par1DamageSource, par2)) {
            //契約者の名前チェックはマルチ用
            if (isContract() && entity != null) {
                if (getIFF(entity) && !isPlaying()) {
                    fleeingTick = 0;
                    return true;
                }
            } else if (maidInventory.getCurrentItem() == null) {
                return true;
            }
            fleeingTick = 0;
//			entityToAttack = entity;
			/*
			if (entity != null) {
				setPathToEntity(worldObj.getPathEntityToEntity(this, entityToAttack, 16F, true, false, false, true));
			}
			if (maidMode == mmode_Healer && entity instanceof EntityLiving) {
				// ヒーラーは薬剤で攻撃
				maidInventory.currentItem = maidInventory.getInventorySlotContainItemPotion(true, 0, ((EntityLiving)entity).isEntityUndead() & isMaskedMaid);
			}
			*/
            return true;
        } else {
            return false;
        }


//		return maidAvatar.attackEntityFrom(par1DamageSource, par2);
    }

    /**
     * 対象にポーションを使う。
     */
    public void usePotionTotarget(EntityLivingBase entityliving) {
        ItemStack itemstack = maidInventory.getCurrentItem();
        if (itemstack != null && itemstack.getItem() instanceof ItemPotion) {
            // ポーション効果の発動
            itemstack.stackSize--;
            List list = ((ItemPotion) itemstack.getItem()).getEffects(itemstack);
            if (list != null) {
                PotionEffect potioneffect;
                for (Iterator iterator = list.iterator(); iterator.hasNext(); entityliving.addPotionEffect(new PotionEffect(potioneffect))) {
                    potioneffect = (PotionEffect) iterator.next();
                }
            }
            if (itemstack.stackSize <= 0) {
                maidInventory.setInventoryCurrentSlotContents(null);
            }
            maidInventory.addItemStackToInventory(new ItemStack(Items.glass_bottle));
        }
    }

    @Override
    protected void dropFewItems(boolean par1, int par2) {
        // メイドさんはお砂糖とココアと不定形の何かでできてるの！
        int k = rand.nextInt(3 + par2);
        for (int j = 0; j <= k; j++) {
            if (rand.nextInt(30) == 0) {
                dropItem(Items.slime_ball, 1);
            }
            if (rand.nextInt(50) == 0) {
                entityDropItem(new ItemStack(Items.dye, 1, 3), 0F);
            }
            dropItem(Items.sugar, 1);
        }

        // インベントリをブチマケロ！
        maidInventory.dropAllItems();
    }

    @Override
    protected Item getDropItem() {
        return Items.sugar;
    }

    @Override
    public int getExperiencePoints(EntityPlayer par1EntityPlayer) {
        return experienceValue;
    }


    @Override
    public void applyEntityCollision(Entity par1Entity) {
        // 閉所接触回避用
        super.applyEntityCollision(par1Entity);

        if (par1Entity instanceof EntityLittleMaid) {
            if (((EntityLittleMaid) par1Entity).aiAvoidPlayer.isActive) {
                aiAvoidPlayer.isActive = true;
            }
        } else if (par1Entity == mstatMasterEntity) {
            aiAvoidPlayer.setActive();
        }
    }

    @Override
    protected void updateAITick() {
//		// AI対応型はこっちが呼ばれる
//		dataWatcher.updateObject(dataWatch_Health, Integer.valueOf(getHealth()));

        // 追加分
        for (EntityModeBase ieml : maidEntityModeList) {
            ieml.updateAITick(getMaidModeInt());
        }
    }

    public void updateAITasks() {
        super.updateAITasks();
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
    }

    @Override
    public int getTalkInterval() {
        return LittleMaidConfig.talkInterval;
    }

    /**
     * 埋葬対策コピー
     */
    private boolean isBlockTranslucent(int par1, int par2, int par3) {
        return this.worldObj.getBlock(par1, par2, par3).isNormalCube();
    }

    /**
     * 埋葬対策コピー
     */
    @Override
    protected boolean func_145771_j(double par1, double par3, double par5) {
        return pushOutOfBlocks(par1, par3, par5);
    }

    protected boolean pushOutOfBlocks(double par1, double par3, double par5) {
        // EntityPlayerSPのを引っ張ってきた
        int var7 = MathHelper.floor_double(par1);
        int var8 = MathHelper.floor_double(par3);
        int var9 = MathHelper.floor_double(par5);
        double var10 = par1 - (double) var7;
        double var12 = par5 - (double) var9;

        boolean lflag = false;
        for (int li = 0; (float) li < height; li++) {
            lflag |= this.isBlockTranslucent(var7, var8 + li, var9);
        }
        if (lflag) {
            boolean var14 = !this.isBlockTranslucent(var7 - 1, var8, var9) && !this.isBlockTranslucent(var7 - 1, var8 + 1, var9);
            boolean var15 = !this.isBlockTranslucent(var7 + 1, var8, var9) && !this.isBlockTranslucent(var7 + 1, var8 + 1, var9);
            boolean var16 = !this.isBlockTranslucent(var7, var8, var9 - 1) && !this.isBlockTranslucent(var7, var8 + 1, var9 - 1);
            boolean var17 = !this.isBlockTranslucent(var7, var8, var9 + 1) && !this.isBlockTranslucent(var7, var8 + 1, var9 + 1);
            byte var18 = -1;
            double var19 = 9999.0D;

            if (var14 && var10 < var19) {
                var19 = var10;
                var18 = 0;
            }

            if (var15 && 1.0D - var10 < var19) {
                var19 = 1.0D - var10;
                var18 = 1;
            }

            if (var16 && var12 < var19) {
                var19 = var12;
                var18 = 4;
            }

            if (var17 && 1.0D - var12 < var19) {
                var19 = 1.0D - var12;
                var18 = 5;
            }

            float var21 = 0.1F;

            if (var18 == 0) {
                this.motionX = (double) (-var21);
            }

            if (var18 == 1) {
                this.motionX = (double) var21;
            }

            if (var18 == 4) {
                this.motionZ = (double) (-var21);
            }

            if (var18 == 5) {
                this.motionZ = (double) var21;
            }

            return !(var14 | var15 | var16 | var17);
        }

        return false;
    }

    @Override
    public void onLivingUpdate() {
        // 回復判定
        float lhealth = getHealth();
        if (lhealth > 0) {
            if (!worldObj.isRemote) {
                if (getSwingStatusDominant().canAttack()) {
                    if (!isBloodsuck()) {
                        // 通常時は回復優先
                        if (lhealth < getMaxHealth()) {
                            if (maidInventory.consumeInventoryItem(Items.sugar)) {
                                eatSugar(true, false);
                            }
                        }
                    }
                }
            }
        }

        super.onLivingUpdate();

        maidInventory.decrementAnimations();
        // 埋葬対策
        boolean grave = true;
        grave &= pushOutOfBlocks(posX - (double) width * 0.34999999999999998D, boundingBox.minY, posZ + (double) width * 0.34999999999999998D);
        grave &= pushOutOfBlocks(posX - (double) width * 0.34999999999999998D, boundingBox.minY, posZ - (double) width * 0.34999999999999998D);
        grave &= pushOutOfBlocks(posX + (double) width * 0.34999999999999998D, boundingBox.minY, posZ - (double) width * 0.34999999999999998D);
        grave &= pushOutOfBlocks(posX + (double) width * 0.34999999999999998D, boundingBox.minY, posZ + (double) width * 0.34999999999999998D);
        if (grave && onGround) {
            jump();
        }
        if (lhealth > 0) {
            // 近接監視の追加はここ
            // アイテムの回収
            if (!worldObj.isRemote) {
                List list = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.expand(1.0D, 0.0D, 1.0D));
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        Entity entity = (Entity) list.get(i);
                        if (!entity.isDead) {
                            if (entity instanceof EntityArrow) {
                                // 特殊回収
                                ((EntityArrow) entity).canBePickedUp = 1;
                            }
                            if (!(entity instanceof EntityItem)) {
                                entity.onCollideWithPlayer(maidAvatar);
                            } else {
                                EntityItem item = (EntityItem) entity;
                                if ((maidInventory.getFirstEmptyStack() != -1) ||
                                        (maidInventory.getInventorySlotContainItem(item.getEntityItem().getItem()) != -1)) {
                                    entity.onCollideWithPlayer(maidAvatar);
                                }
                            }
                        }
                    }
                    // アイテムが一杯になっていてアイテムにタゲをとっている場合はタゲをクリア
                    if (entityToAttack instanceof EntityItem && maidInventory.getFirstEmptyStack() == -1) {
                        setTarget(null);
                    }
                }
            }
            // 時計を持っている
            // TODO:多分この辺りの処理はおかしい
            if (isContractEX() && mstatClockMaid) {
                // ゲーム内時間に合わせた音声の再生
                mstatTime = (int) (worldObj.getWorldTime() % 24000);
                if (mstatMasterEntity != null) {
                    boolean b = mstatMasterEntity.isPlayerSleeping();

                    if (mstatMasterDistanceSq < 25D && getEntitySenses().canSee(mstatMasterEntity)) {
                        EnumSound lsound = EnumSound.Null;
                        if (mstatFirstLook && (mstatTime > 23500 || mstatTime < 1500)) {
                            lsound = EnumSound.goodmorning;
                            mstatFirstLook = false;
                        } else if (!mstatFirstLook && b) {
                            lsound = EnumSound.goodnight;
                            mstatFirstLook = true;
                        } else if (mstatFirstLook && !b) {
                            mstatFirstLook = false;
                        }

                        if (lsound != EnumSound.Null) {
                            playSound(lsound, true);
                            setLooksWithInterest(true);
                        }
                    } else {
                        if (!mstatFirstLook && (b || (mstatTime > 18000 && mstatTime < 23500))) {
                            mstatFirstLook = true;
                        }
                    }
                }
            } else {
                mstatTime = 6000;
            }

            // TNT-D System
            maidOverDriveTime.onUpdate();
            if (maidOverDriveTime.isDelay()) {
                for (int li = 0; li < mstatSwingStatus.length; li++) {
                    mstatSwingStatus[li].attackTime--;
                }
                if (maidOverDriveTime.isEnable()) {
                    worldObj.spawnParticle("reddust", (posX + (double) (rand.nextFloat() * width * 2.0F)) - (double) width, posY + 0.5D + (double) (rand.nextFloat() * height), (posZ + (double) (rand.nextFloat() * width * 2.0F)) - (double) width, 1.2D, 0.4D, 0.4D);
                }
                if (!worldObj.isRemote) {
                    Entity lattackentity = getAttackTarget();
                    if (lattackentity == null) {
                        lattackentity = getEntityToAttack();
                    }
                    if (lattackentity != null) {
                        PathEntity pe = worldObj.getPathEntityToEntity(this, lattackentity, 16F, true, false, false, true);

                        if (pe != null) {
                            pe.incrementPathIndex();
                            if (!pe.isFinished()) {
                                Vec3 v = pe.getPosition(this);
                                setPosition(v.xCoord, v.yCoord, v.zCoord);
                            }
                        }
                    }
                }
            }

            if (!worldObj.isRemote) {
                if (getSwingStatusDominant().canAttack()) {
//					mod_LMM_littleMaidMob.Debug("isRemort:" + worldObj.isRemote);
                    // 回復
                    if (getHealth() < getMaxHealth()) {
                        if (maidInventory.consumeInventoryItem(Items.sugar)) {
                            eatSugar(true, false);
                        }
                    }
                    // つまみ食い
                    if (rand.nextInt(50000) == 0 && maidInventory.consumeInventoryItem(Items.sugar)) {
                        eatSugar(true, false);
                    }
                    // 契約更新
                    if (isContractEX()) {
                        float f = getContractLimitDays();
                        if (f <= 6 && maidInventory.consumeInventoryItem(Items.sugar)) {
                            // 契約更新
                            eatSugar(true, true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onUpdate() {
        int litemuse = 0;

        // Entity初回生成時のインベントリ更新用
        // サーバーの方が先に起動するのでクライアント側が更新を受け取れない
        if (firstload > 0) {
            // 初回更新用
            // サーバーの方が先に起動しているので強制読み込みの手順が必要
            if (--firstload == 0) {
                if (worldObj.isRemote) {
                    NetworkHandler.sendToServer(new UpdateLittleMaidSlotPacket(this));
                } else {
                }
            }
        }

        // 飛び道具用
        weaponFullAuto = false;
        weaponReload = false;

        // 主の確認など
        mstatMasterEntity = getMaidMasterEntity();
        if (mstatMasterEntity != null) {
            mstatMasterDistanceSq = getDistanceSqToEntity(mstatMasterEntity);
        }
        // モデルサイズのリアルタイム変更有り？
        textureData.onUpdate();
        // リアルタイム変動値をアップデート
        if (worldObj.isRemote) {
            // クライアント側
            boolean lupd = false;
            lupd |= updateMaidContract();
            lupd |= updateMaidColor();
//			lupd |= updateTexturePack();
            updateTexturePack();
            if (lupd) {
                setTextureNames();
            }
            setMaidMode(dataWatcher.getWatchableObjectShort(dataWatch_Mode));
            setDominantArm(dataWatcher.getWatchableObjectByte(dataWatch_DominamtArm));
            updateMaidFlagsClient();
            updateGotcha();

            // 腕の挙動関連
            litemuse = dataWatcher.getWatchableObjectInt(dataWatch_ItemUse);
            for (int li = 0; li < mstatSwingStatus.length; li++) {
                ItemStack lis = mstatSwingStatus[li].getItemStack(this);
                if ((litemuse & (1 << li)) > 0 && lis != null) {
                    mstatSwingStatus[li].setItemInUse(lis, lis.getMaxItemUseDuration(), this);
                } else {
                    mstatSwingStatus[li].stopUsingItem(this);
                }
            }
        } else {
            boolean lf;
            // サーバー側
            updateRemainsContract();
            // Overdrive
            lf = maidOverDriveTime.isEnable();
            if (getMaidFlags(dataWatch_Flags_OverDrive) != lf) {
                if (lf) {
                    playSound(EnumSound.TNT_D, true);
                }
                setMaidFlags(lf, dataWatch_Flags_OverDrive);
            }
            // Working!
            lf = mstatWorkingCount.isEnable();
            if (getMaidFlags(dataWatch_Flags_Working) != lf) {
                setMaidFlags(lf, dataWatch_Flags_Working);
            }
            // 拗ねる
            if (!isContractEX() && !isFreedom()) {
                setFreedom(true);
                setMaidWait(false);
            }
            // 移動速度の変更
            IAttributeInstance latt = this.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
            // 属性を解除
            latt.removeModifier(attCombatSpeed);
            if (isContract()) {
                if (!isFreedom() || (entityToAttack != null || getAttackTarget() != null)) {
                    // 属性を設定
                    latt.applyModifier(attCombatSpeed);
                }
            }
            // スニーキング判定
            latt.removeModifier(attSneakingSpeed);
            if ((onGround && isSneaking()) || isUsingItem()) {
                latt.applyModifier(attSneakingSpeed);
            }
//			isSprinting()
        }

        // 独自処理用毎時処理
        for (EntityModeBase leb : maidEntityModeList) {
            leb.onUpdate(maidMode);
        }


        super.onUpdate();
        // SwingUpdate
        SwingStatus lmss1 = getSwingStatusDominant();
        prevSwingProgress = maidAvatar.prevSwingProgress = lmss1.prevSwingProgress;
        swingProgress = maidAvatar.swingProgress = lmss1.swingProgress;
        swingProgressInt = maidAvatar.swingProgressInt = lmss1.swingProgressInt;
        isSwingInProgress = maidAvatar.isSwingInProgress = lmss1.isSwingInProgress;

        // Aveterの毎時処理
        if (maidAvatar != null) {
            getAvatarIF().getValue();
            maidAvatar.onUpdate();
//			maidAvatar.setValue();
        }

        // カウンタ系
        if (mstatWaitCount > 0) {
            if (hasPath()) {
                mstatWaitCount = 0;
            } else {
                mstatWaitCount--;
            }
        }
        if (maidSoundInterval > 0) {
            maidSoundInterval--;
        }

        // くびかしげ
        prevRotateAngleHead = rotateAngleHead;
        if (getLooksWithInterest()) {
            rotateAngleHead = rotateAngleHead + (1.0F - rotateAngleHead) * 0.4F;
            numTicksToChaseTarget = 10;
        } else {
            rotateAngleHead = rotateAngleHead + (0.0F - rotateAngleHead) * 0.4F;
            if (numTicksToChaseTarget > 0) numTicksToChaseTarget--;
        }

        if (getAttackTarget() != null || getEntityToAttack() != null) {
            setWorking(true);
        }
        // お仕事カウンター
        mstatWorkingCount.onUpdate();

        // 腕の挙動に関する処理
        litemuse = 0;
        for (int li = 0; li < mstatSwingStatus.length; li++) {
            mstatSwingStatus[li].onUpdate(this);
            if (mstatSwingStatus[li].isUsingItem()) {
                litemuse |= (1 << li);
            }
        }
        // 標準変数に対する数値の代入
        SwingStatus lmss = getSwingStatusDominant();
        prevSwingProgress = maidAvatar.prevSwingProgress = lmss.prevSwingProgress;
        swingProgress = maidAvatar.swingProgress = lmss.swingProgress;
        swingProgressInt = maidAvatar.swingProgressInt = lmss.swingProgressInt;
        isSwingInProgress = maidAvatar.isSwingInProgress = lmss.isSwingInProgress;

        // 持ち物の確認
        if (maidInventory.inventoryChanged) {
            onInventoryChanged();
            maidInventory.inventoryChanged = false;
        }
        if (!worldObj.isRemote) {
            // サーバー側処理
            // アイテム使用状態の更新
            dataWatcher.updateObject(dataWatch_ItemUse, litemuse);
            // インベントリの更新
            for (int li = 0; li < maidInventory.getSizeInventory(); li++) {
                boolean lchange = false;
                int lselect = 0xff;
                // 選択装備が変わった
                for (int lj = 0; lj < mstatSwingStatus.length; lj++) {
                    lchange = mstatSwingStatus[lj].checkChanged();
                    if (mstatSwingStatus[lj].index == li) {
                        lselect = lj;
                    }
                }
                // インベントリの中身が変わった
                if (lchange || maidInventory.isChanged(li)) {
                    ((WorldServer) worldObj).getEntityTracker().func_151247_a(this, new S04PacketEntityEquipment(this.getEntityId(), (li | lselect << 8) + 5, maidInventory.getStackInSlot(li)));
                    maidInventory.resetChanged(li);
                    LittleMaidMobX.debug(String.format("ID:%d-%s - Slot(%x:%d-%d,%d) Update.", getEntityId(), worldObj.isRemote ? "Client" : "Server", lselect, li, mstatSwingStatus[0].index, mstatSwingStatus[1].index));
                }
            }

            // 弓構え
            mstatAimeBow &= !getSwingStatusDominant().canAttack();
            // 構えの更新
            updateAimebow();

            // TODO:test
            if (dataWatcher.getWatchableObjectInt(dataWatch_ExpValue) != experienceValue) {
                dataWatcher.updateObject(dataWatch_ExpValue, Integer.valueOf(experienceValue));
            }

            // 自分より大きなものは乗っけない（イカ除く）
            if (riddenByEntity != null && !(riddenByEntity instanceof EntitySquid)) {
                if (height * width < riddenByEntity.height * riddenByEntity.width) {
                    if (riddenByEntity instanceof EntityLivingBase) {
                        attackEntityFrom(DamageSource.causeMobDamage((EntityLivingBase) riddenByEntity), 0);
                    }
                    riddenByEntity.mountEntity(null);
                    return;
                }
            }

            // 斧装備時は攻撃力が上がる
            IAttributeInstance latt = this.getEntityAttribute(SharedMonsterAttributes.attackDamage);
            // 属性を解除
            latt.removeModifier(attAxeAmp);
            ItemStack lis = getCurrentEquippedItem();
            if (lis != null && lis.getItem() instanceof ItemAxe) {
                // 属性を設定
                latt.applyModifier(attAxeAmp);
            }
        } else {
            // Client
            // TODO:test
            experienceValue = dataWatcher.getWatchableObjectInt(dataWatch_ExpValue);
        }

        // 紐で拉致
        if (mstatgotcha != null) {
            double d = mstatgotcha.getDistanceSqToEntity(this);
            if (entityToAttack == null) {
                // インコムごっこ用
                if (d > 4D) {
//					setPathToEntity(null);
                    getNavigator().clearPathEntity();
                    getLookHelper().setLookPositionWithEntity(mstatgotcha, 15F, 15F);
                }
                if (d > 12.25D) {
//					setPathToEntity(worldObj.getPathEntityToEntity(mstatgotcha, this, 16F, true, false, false, true));
                    getNavigator().tryMoveToXYZ(mstatgotcha.posX, mstatgotcha.posY, mstatgotcha.posZ, 1.0F);
                    getLookHelper().setLookPositionWithEntity(mstatgotcha, 15F, 15F);
                }
            }
            if (d > 25D) {
                double d1 = mstatgotcha.posX - posX;
                double d3 = mstatgotcha.posZ - posZ;
                double d5 = 0.125D / (Math.sqrt(d1 * d1 + d3 * d3) + 0.0625D);
                d1 *= d5;
                d3 *= d5;
                motionX += d1;
                motionZ += d3;
            }
            if (d > 42.25D) {
                double d2 = mstatgotcha.posX - posX;
                double d4 = mstatgotcha.posZ - posZ;
                double d6 = 0.0625D / (Math.sqrt(d2 * d2 + d4 * d4) + 0.0625D);
                d2 *= d6;
                d4 *= d6;
                mstatgotcha.motionX -= d2;
                mstatgotcha.motionZ -= d4;
            }
            if (d > 64D) {
                setGotcha(0);
                mstatgotcha = null;
                playSound("random.drr");
            }
            if (rand.nextInt(16) == 0) {
                List list = worldObj.getEntitiesWithinAABBExcludingEntity(this, boundingBox.expand(8D, 8D, 8D));
                for (int k = 0; k < list.size(); k++) {
                    Entity entity = (Entity) list.get(k);
                    if (!(entity instanceof EntityMob)) {
                        continue;
                    }
                    EntityMob entitymob = (EntityMob) entity;
                    if (entitymob.getEntityToAttack() == mstatgotcha) {
                        entitymob.setTarget(this);
                    }
                }
            }
        }

    }


    @Override
    public void onDeath(DamageSource par1DamageSource) {
        super.onDeath(par1DamageSource);

        // 死因を表示
        if (!worldObj.isRemote) {
            // マスター判定失敗するかも？
            if (LittleMaidConfig.deathMessage && mstatMasterEntity != null) {
                String ls = par1DamageSource.getDamageType();
                Entity lentity = par1DamageSource.getEntity();
                if (lentity != null) {
                    if (par1DamageSource.getEntity() instanceof EntityPlayer) {
                        ls += ":" + MMM_Helper.getPlayerName((EntityPlayer) lentity);
                    } else {
                        String lt = EntityList.getEntityString(lentity);
                        if (lt != null) {
                            ls += ":" + lt;
                        }
                    }
                }

                // 不具合対応
                // getFormattedText → getUnformattedTextForChat
                // getFormattedText はクライアント専用（描画用）。
                // http://forum.minecraftuser.jp/viewtopic.php?f=13&t=23347&p=212078#p211805
                String lt = func_145748_c_().getUnformattedTextForChat();

                ChatComponentText text = new ChatComponentText(String.format("your %s killed by %s", lt, ls));
                mstatMasterEntity.addChatMessage(text);
            }
        }
    }

    // ポーションエフェクト
    @Override
    protected void onNewPotionEffect(PotionEffect par1PotionEffect) {
        super.onNewPotionEffect(par1PotionEffect);
        if (mstatMasterEntity instanceof EntityPlayerMP) {
            ((EntityPlayerMP) mstatMasterEntity).playerNetServerHandler.sendPacket(new S1DPacketEntityEffect(this.getEntityId(), par1PotionEffect));
        }
    }

    @Override
    protected void onChangedPotionEffect(PotionEffect par1PotionEffect, boolean par2) {
        super.onChangedPotionEffect(par1PotionEffect, par2);
        // TODO:必要かどうかのチェック
//		if (mstatMasterEntity instanceof EntityPlayerMP) {
//			((EntityPlayerMP)mstatMasterEntity).playerNetServerHandler.sendPacketToPlayer(new Packet41EntityEffect(this.getEntityId(), par1PotionEffect));
//		}
    }

    @Override
    protected void onFinishedPotionEffect(PotionEffect par1PotionEffect) {
        super.onFinishedPotionEffect(par1PotionEffect);
        if (mstatMasterEntity instanceof EntityPlayerMP) {
            ((EntityPlayerMP) mstatMasterEntity).playerNetServerHandler.sendPacket(new S1EPacketRemoveEntityEffect(this.getEntityId(), par1PotionEffect));
        }
    }


    /**
     * インベントリが変更されました。
     */
    public void onInventoryChanged() {
        checkClockMaid();
        checkMaskedMaid();
        checkHeadMount();
        if ((getActiveModeClass() != null) && maidInventory.isChanged(0)) {
            worldObj.setEntityState(this, (byte) 11);
            if (!worldObj.isRemote) {
                setMaidModeAuto(getMaidMasterEntity());
                setMaidWait(false);
            }
        }
        getNextEquipItem();
//		setArmorTextureValue();
    }

    /**
     * インベントリにある次の装備品を選択
     */
    public boolean getNextEquipItem() {
        if (worldObj.isRemote) {
            // クライアント側は処理しない
            return false;
        }

        int li;
        if (isActiveModeClass()) {
            li = getActiveModeClass().getNextEquipItem(maidMode);
        } else {
            li = -1;
        }
        setEquipItem(maidDominantArm, li);
        return li > -1;
    }

    public ItemStack getHandSlotForModeChange() {
        return maidInventory.getStackInSlot(0);
    }

    public void setEquipItem(int pArm, int pIndex) {
        if (pArm == maidDominantArm) {
            maidInventory.currentItem = pIndex;
        }
        //maidInventory.currentItem = pIndex;
        int li = mstatSwingStatus[pArm].index;
        if (li != pIndex) {
            if (li > -1) {
                maidInventory.setChanged(li);
            }
            if (pIndex > -1) {
                maidInventory.setChanged(pIndex);
            }
            mstatSwingStatus[pArm].setSlotIndex(pIndex);
        }
    }

    public void setEquipItem(int pIndex) {
        setEquipItem(maidDominantArm, pIndex);
    }


    /**
     * 対応型射撃武器のリロード判定
     */
    public void getWeaponStatus() {
        // 飛び道具用の特殊処理
        ItemStack is = maidInventory.getCurrentItem();
        if (is == null) return;

        try {
            Method me = is.getItem().getClass().getMethod("isWeaponReload", ItemStack.class, EntityPlayer.class);
            weaponReload = (Boolean) me.invoke(is.getItem(), is, maidAvatar);
        } catch (Exception ignored) {
        }

        try {
            Method me = is.getItem().getClass().getMethod("isWeaponFullAuto", ItemStack.class);
            weaponFullAuto = (Boolean) me.invoke(is.getItem(), is);
        } catch (Exception ignored) {
        }
    }

    // 保持アイテム関連

    /**
     * 現在の装備品
     */
    public ItemStack getCurrentEquippedItem() {
        return maidInventory.getCurrentItem();
    }

    @Override
    public ItemStack getHeldItem() {
        return maidInventory.getCurrentItem();
    }

    @Override
    public ItemStack getEquipmentInSlot(int par1) {
        if (par1 == 0) {
            return getHeldItem();
        } else if (par1 >= 0 && par1 < 4) {
            return maidInventory.armorItemInSlot(par1);
        } else {
            return maidInventory.getStackInSlot(par1);
        }
        //return maidInventory.getStackInSlot(par1);
    }

    @Override
    public ItemStack func_130225_q(int par1) {
        return maidInventory.armorItemInSlot(par1);
    }

    @Override
    public void setCurrentItemOrArmor(int par1, ItemStack par2ItemStack) {
        par1 &= 0x0000ffff;
        if (par1 == 0) {
            maidInventory.setInventoryCurrentSlotContents(par2ItemStack);
        } else {
            par1 -= 5;
            // 持ち物のアップデート
            // 独自拡張:普通にスロット番号の通り、上位８ビットは装備スロット
            // par1はShortで渡されるのでそのように。
            int lslotindex = par1 & 0x7f;
            int lequip = (par1 >>> 8) & 0xff;
            maidInventory.setInventorySlotContents(lslotindex, par2ItemStack);
            maidInventory.inventoryChanged = true;

            for (SwingStatus lss : mstatSwingStatus) {
                if (lslotindex == lss.index) {
                    lss.index = -1;
                }
            }
            if (lequip != 0xff) {
                setEquipItem(lequip, lslotindex);
//				mstatSwingStatus[lequip].index = lslotindex;
            }
            if (lslotindex >= maidInventory.maxInventorySize) {
                setTextureNames();
            }
        }
		/*
		par1 -= 5;
		int lslotindex = par1 & 0x7f;
		int lequip = (par1 >>> 8) & 0xff;
		maidInventory.setInventorySlotContents(lslotindex, par2ItemStack);
		String s = par2ItemStack == null ? null : par2ItemStack.getDisplayName();
		if(mstatMasterEntity != null) {
			//mstatMasterEntity.addChatMessage(new ChatComponentText(String.format("ID:%d Slot(%2d:%d):%s", par1, lslotindex, LMM_InventoryLittleMaid.handInventoryOffset, s == null ? "NoItem" : s)));
		}
		maidInventory.inventoryChanged = true;
		if((lslotindex == LMM_InventoryLittleMaid.handInventoryOffset)) {
			worldObj.setEntityState(this, (byte)11);
			if(!worldObj.isRemote) {
				setFreedom(isFreedom());
				setMaidModeAuto(mstatMasterEntity);
				if(mstatMasterEntity != null) {
					mstatMasterEntity.addChatMessage(new ChatComponentText(String.format("not Remote.")));
				}
			}
		}
		if (par1 > 0 && par1 < 4) {
			int lslotindex = par1 & 0x7f;
			int lequip = (par1 >>> 8) & 0xff;
			String s = par2ItemStack == null ? null : par2ItemStack.getDisplayName();
			if(mstatMasterEntity!=null) {
				mstatMasterEntity.addChatMessage(new ChatComponentText(String.format("ID:%d Slot(%2d:%d):%s", par1, lslotindex, lequip, s == null ? "NoItem" : s)));
			}
			maidInventory.setInventorySlotContents(par1, par2ItemStack);
			//maidInventory.armorInventory[par1] = par2ItemStack;
			setTextureNames();
		}
		else {
			par1 -= 5;
			// 持ち物のアップデート
			// 独自拡張:普通にスロット番号の通り、上位８ビットは装備スロット
			// par1はShortで渡されるのでそのように。
			int lslotindex = par1 & 0x7f;
			int lequip = (par1 >>> 8) & 0xff;
			//maidInventory.setInventorySlotContents(lslotindex, par2ItemStack);
			//maidInventory.resetChanged(lslotindex);	// これは意味ないけどな。
			maidInventory.inventoryChanged = true;
//			if (par1 >= maidInventory.mainInventory.length) {
//				LMM_Client.setArmorTextureValue(this);
//			}

			for (LMM_SwingStatus lss: mstatSwingStatus) {
				if (lslotindex == lss.index) {
					lss.index = -1;
				}
			}
			if (lequip != 0xff) {
				//setEquipItem(lequip, lslotindex);
//				mstatSwingStatus[lequip].index = lslotindex;
			}
			if (lslotindex >= maidInventory.maxInventorySize) {
				setTextureNames();
			}
			String s = par2ItemStack == null ? null : par2ItemStack.getDisplayName();
			if(mstatMasterEntity!=null) {
				mstatMasterEntity.addChatMessage(new ChatComponentText(String.format("ID:%d Slot(%2d:%d):%s", getEntityId(), lslotindex, lequip, s == null ? "NoItem" : s)));
			}
			LMM_LittleMaidMobX.Debug(String.format("ID:%d Slot(%2d:%d):%s", getEntityId(), lslotindex, lequip, s == null ? "NoItem" : s));
		}
		*/
    }

    @Override
    public ItemStack[] getLastActiveItems() {
        return maidInventory.armorInventory;
    }

    protected void checkClockMaid() {
        // 時計を持っているか？
        mstatClockMaid = maidInventory.getInventorySlotContainItem(Items.clock) > -1;
    }

    /**
     * 時計を持っているか?
     */
    public boolean isClockMaid() {
        return mstatClockMaid;
    }

    protected void checkMaskedMaid() {
        // インベントリにヘルムがあるか？
		/*
		for (int i = maidInventory.mainInventory.length - 1; i >= 0; i--) {
			ItemStack is = maidInventory.getStackInSlot(i);
			if (is != null && is.getItem() instanceof ItemArmor && ((ItemArmor)is.getItem()).armorType == 0) {
				// ヘルムを持ってる
				mstatMaskSelect = i;
				maidInventory.armorInventory[3] = is;
				if (worldObj.isRemote) {
					setTextureNames();
				}
				return;
			}
		}
		*/

        if (maidInventory.armorInventory[3] != null) {
            mstatMaskSelect = 3;
        } else {
            mstatMaskSelect = -1;
        }
        //maidInventory.armorInventory[3] = null;
        return;
    }

    /**
     * メットを被ってるか
     */
    public boolean isMaskedMaid() {
        return mstatMaskSelect > -1;

    }

    protected void checkHeadMount() {
        // 追加の頭部装備の判定
        ItemStack lis = maidInventory.getHeadMount();
        mstatPlanter = false;
        mstatCamouflage = false;
        if (lis != null) {
            if (lis.getItem() instanceof ItemBlock) {
                Block lblock = Block.getBlockFromItem(lis.getItem());
//				mstatPlanter =	(lblock instanceof BlockFlower      && lblock.getRenderType() ==  1) ||
                mstatPlanter = (lblock.getRenderType() == 1) ||
                        (lblock instanceof BlockDoublePlant && lblock.getRenderType() == 40);
                mstatCamouflage = (lblock instanceof BlockLeaves) || (lblock instanceof BlockPumpkin) || (lblock instanceof BlockStainedGlass);
            } else if (lis.getItem() instanceof ItemSkull) {
                mstatCamouflage = true;
            }
        }
    }

    /**
     * カモフラージュ！
     */
    public boolean isCamouflage() {
        return mstatCamouflage;
    }

    /**
     * 鉢植え状態
     */
    public boolean isPlanter() {
        return mstatPlanter;
    }

    /**
     * ポーション等による腕振りモーションの速度補正
     */
    public int getSwingSpeedModifier() {
        if (isPotionActive(Potion.digSpeed)) {
            return 6 - (1 + getActivePotionEffect(Potion.digSpeed).getAmplifier()) * 1;
        }

        if (isPotionActive(Potion.digSlowdown)) {
            return 6 + (1 + getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2;
        } else {
            return 6;
        }
    }

    /**
     * 手持ちアイテムの破壊
     */
    public void destroyCurrentEquippedItem() {
        maidInventory.setInventoryCurrentSlotContents(null);
    }

    /**
     * メイドインベントリを開く
     *
     * @param pEntityPlayer
     */
    public void displayGUIMaidInventory(EntityPlayer pEntityPlayer) {
        if (!worldObj.isRemote) {
            GuiCommonHandler.maidServer = this;
            pEntityPlayer.openGui(LittleMaidMobX.getInstance(), GuiCommonHandler.GUI_ID_INVVENTORY, this.worldObj,
                    (int) this.posX, (int) this.posY, (int) this.posZ);
        } else {
            GuiCommonHandler.maidClient = this;
        }
    }

    @Override
    public boolean interact(EntityPlayer par1EntityPlayer) {
        MMMLib.Debug(this.worldObj.isRemote, "LMM_EntityLittleMaid.interact:" + par1EntityPlayer.getGameProfile().getName());
        float lhealth = getHealth();
        ItemStack itemstack1 = par1EntityPlayer.getCurrentEquippedItem();

        // プラグインでの処理を先に行う
        for (int li = 0; li < maidEntityModeList.size(); li++) {
            if (maidEntityModeList.get(li).preInteract(par1EntityPlayer, itemstack1)) {
                return true;
            }
        }
        // しゃがみ時は処理無効
        if (par1EntityPlayer.isSneaking()) {
            return false;
        }
        // ナデリ判定
        if (lhealth > 0F && par1EntityPlayer.riddenByEntity != null && !(par1EntityPlayer.riddenByEntity instanceof EntityLittleMaid)) {
            // 載せ替え
            par1EntityPlayer.riddenByEntity.mountEntity(this);
            return true;
        }


        if (mstatgotcha == null && par1EntityPlayer.fishEntity == null) {
            if (itemstack1 != null && itemstack1.getItem() == Items.string) {
                // 紐で繋ぐ
                setGotcha(par1EntityPlayer.getEntityId());
                mstatgotcha = par1EntityPlayer;
                MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                playSound("random.pop");
                return true;
            }

            if (isContract()) {
                // 契約状態
                if (lhealth > 0F && isMaidContractOwner(par1EntityPlayer)) {
                    if (itemstack1 != null) {
                        // 追加分の処理
                        setPathToEntity(null);
                        // プラグインでの処理を先に行う
                        for (int li = 0; li < maidEntityModeList.size(); li++) {
                            if (maidEntityModeList.get(li).interact(par1EntityPlayer, itemstack1)) {
                                return true;
                            }
                        }
                        if (isRemainsContract()) {
                            // 通常
                            if (ItemHelper.isSugar(itemstack1.getItem())) {
                                // モード切替
                                MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                                eatSugar(false, true);
                                worldObj.setEntityState(this, (byte) 11);

                                LittleMaidMobX.debug("give suger." + worldObj.isRemote);
                                if (!worldObj.isRemote) {
                                    setFreedom(isFreedom());
                                    if (isMaidWait()) {
                                        setMaidModeAuto(par1EntityPlayer);
                                        setMaidWait(false);
                                    } else {
                                        // 待機
                                        setMaidWait(true);
                                    }
                                }
                                return true;
                            } else if (itemstack1.getItem() == Items.dye) {
                                // カラーメイド
                                if (!worldObj.isRemote) {
                                    setColor(15 - itemstack1.getItemDamage());
                                }
                                MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                                return true;
                            } else if (itemstack1.getItem() == Items.feather) {
                                // 自由行動
                                //MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                                setFreedom(!isFreedom());
                                worldObj.setEntityState(this, isFreedom() ? (byte) 12 : (byte) 13);
                                return true;
                            } else if (itemstack1.getItem() == Items.saddle) {
                                // 肩車
                                if (!worldObj.isRemote) {
                                    if (ridingEntity == par1EntityPlayer) {
                                        this.mountEntity(null);
                                    } else {
                                        this.mountEntity(par1EntityPlayer);
                                    }
                                    return true;
                                }
                            } else if (itemstack1.getItem() == Items.gunpowder) {
                                // test TNT-D
//								playSound(LMM_EnumSound.eatGunpowder, false);
                                maidOverDriveTime.setValue(itemstack1.stackSize * 10);
                                MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, itemstack1.stackSize);
                                return true;
                            } else if (itemstack1.getItem() == Items.book) {
                                // IFFのオープン
                                //MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                                if (worldObj.isRemote) {
                                    par1EntityPlayer.openGui(LittleMaidMobX.getInstance(),
                                            GuiCommonHandler.GUI_ID_IFF,
                                            this.worldObj,
                                            (int) this.posX,
                                            (int) this.posY,
                                            (int) this.posZ);
                                }
                                return true;
                            } else if ((itemstack1.getItem() == Items.glass_bottle) && (experienceValue >= 5)) {
                                // Expボトル
                                MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                                if (!worldObj.isRemote) {
                                    entityDropItem(new ItemStack(Items.experience_bottle), 0.5F);
                                    experienceValue -= 5;
                                    if (maidAvatar != null) {
                                        maidAvatar.experienceTotal -= 5;
                                    }
                                }
                                return true;
                            } else if (itemstack1.getItem() instanceof ItemPotion) {
                                // ポーション
                                if (!worldObj.isRemote) {
                                    List list = ((ItemPotion) itemstack1.getItem()).getEffects(itemstack1);
                                    if (list != null) {
                                        PotionEffect potioneffect;
                                        for (Iterator iterator = list.iterator(); iterator.hasNext(); addPotionEffect(new PotionEffect(potioneffect))) {
                                            potioneffect = (PotionEffect) iterator.next();
                                        }
                                    }
                                }
                                MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                                return true;
                            } else if (isFreedom() && itemstack1.getItem() == Items.redstone) {
                                // Tracer
                                //MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                                setPathToEntity(null);
                                setMaidWait(false);
                                setTracer(!isTracer());
                                if (isTracer()) {
                                    worldObj.setEntityState(this, (byte) 14);
                                } else {
                                    worldObj.setEntityState(this, (byte) 12);
                                }

                                return true;
                            } else if (itemstack1.getItem() == Items.stick) {
                                setDominantArm(maidDominantArm == 0 ? 1 : 0);
                                return true;
                            } else if (itemstack1.getItem() == LittleMaidMobX.dismissalNotice) {
                                MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                                //IsDismissalNotice = true;
                                setTamed(false);
                                //par1EntityPlayer.addChatMessage(new ChatComponentText("DismissalNotice"));
                                return true;
                            } else if (itemstack1.getItem() == Items.bucket) {
                                //乳搾りします
                                //胸当て着けてたら絞れませんよ
                                boolean isArmor = false;
                                ItemStack[] items = getLastActiveItems();
                                for (int i = 0; i < 4; i++) {
                                    ItemStack is = maidInventory.armorItemInSlot(i);
                                    if ((is != null) && (((ItemArmor) is.getItem()).armorType == 1)) {
                                        isArmor = true;
                                        break;
                                    }
                                }
                                if (isArmor) {
                                    showParticleFX("smoke", 0.05D, 0.04D, 0.05D);
                                } else {
                                    if (!worldObj.isRemote) {
                                        MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                                        ItemStack milkBucket = new ItemStack(Items.milk_bucket, 1);
                                        if (this.hasCustomNameTag()) {
                                            milkBucket.setStackDisplayName(StatCollector.translateToLocalFormatted("item.lmmx.milk_bucket_custom", this.getCustomNameTag()));
                                        } else {
                                            milkBucket.setStackDisplayName(StatCollector.translateToLocal("item.lmmx.milk_bucket"));
                                        }
                                        par1EntityPlayer.inventory.addItemStackToInventory(milkBucket);
                                    }
                                    showParticleFX("heart", 0.2D, 0.5D, 0.2D);
                                }
                                return true;
                            }
                        } else {
                            // ストライキ
                            if (ItemHelper.isSugar(itemstack1)) {
                                // 受取拒否
                                worldObj.setEntityState(this, (byte) 10);
                                return true;
                            } else if (ItemHelper.isCake(itemstack1)) {
                                // 再契約
                                MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                                maidContractLimit = (24000 * 7);
                                setFreedom(false);
                                setTracer(false);
                                setMaidWait(false);
                                setMaidMode("Escorter");
                                worldObj.setEntityState(this, (byte) 11);
                                playSound(EnumSound.Recontract, true);
                                return true;
                            }
                        }
                    }
                    // メイドインベントリ
                    W_Common.setOwner(this, MMM_Helper.getPlayerName(par1EntityPlayer));
                    getNavigator().clearPathEntity();
                    isJumping = false;
                    displayGUIMaidInventory(par1EntityPlayer);
                    return true;
                }
            } else {
                // 未契約
                if (itemstack1 != null) {
                    if (ItemHelper.isCake(itemstack1)) {
                        // 契約
                        MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);

                        deathTime = 0;
                        if (!worldObj.isRemote) {
                            dropItem(Items.name_tag, 1);

                            if (LittleMaidMobX.contract != null) {
                                par1EntityPlayer.triggerAchievement(LittleMaidMobX.contract);
                            }
                            setContract(true);
                            W_Common.setOwner(this, MMM_Helper.getPlayerName(par1EntityPlayer));
                            setHealth(20);
                            setMaidMode("Escorter");
                            setMaidWait(false);
                            setFreedom(false);
                            playSound(EnumSound.getCake, true);
//							playLittleMaidSound(LMM_EnumSound.getCake, true);
//							playTameEffect(true);
                            worldObj.setEntityState(this, (byte) 7);
                            // 契約記念日と、初期契約期間
                            maidContractLimit = (24000 * 7);
                            maidAnniversary = worldObj.getTotalWorldTime();

                            // テクスチャのアップデート:いらん？
//							LMM_Net.sendToAllEClient(this, new byte[] {LMM_Net.LMN_Client_UpdateTexture, 0, 0, 0, 0});

                        }
                        return true;
                    } else if (itemstack1.getItem() == Items.bucket) {
                        //見ず知らずのメイドさんに乳搾りしたらお仕置きです
                        MMM_Helper.decPlayerInventory(par1EntityPlayer, -1, 1);
                        setSwing(10, EnumSound.attack);
                        par1EntityPlayer.attackEntityFrom(DamageSource.generic, 2);
                        PotionEffect moveSpeedEffect = new PotionEffect(Potion.moveSpeed.id, 50, 5);
                        this.addPotionEffect(moveSpeedEffect);
                        if (!worldObj.isRemote) {
                            entityDropItem(new ItemStack(Items.bucket), 0.5F);
                        }

                        getNavigator().clearPathEntity();
                        getNavigator().tryMoveToXYZ(par1EntityPlayer.posX, par1EntityPlayer.posY, par1EntityPlayer.posZ, 1.0F);
                        getLookHelper().setLookPosition(par1EntityPlayer.posX + 0.5D, par1EntityPlayer.posY + 0.5D, par1EntityPlayer.posZ + 0.5D, 10F, getVerticalFaceSpeed());
                        maidOverDriveTime.setValue(100);
                    } else {
//						worldObj.setEntityState(this, (byte)6);
                    }
                }
            }
        } else if (lhealth > 0F && mstatgotcha != null) {
            if (!worldObj.isRemote) {
                EntityItem entityitem = new EntityItem(worldObj, mstatgotcha.posX, mstatgotcha.posY, mstatgotcha.posZ, new ItemStack(Items.string));
                worldObj.spawnEntityInWorld(entityitem);
                setGotcha(0);
                mstatgotcha = null;
            }
            return true;
        }

        return false;
    }

    /**
     * Set maid's mode automatically.
     *
     * @return whether the mode was changed
     */
    private boolean setMaidModeAuto(EntityPlayer par1EntityPlayer) {
        if (par1EntityPlayer == null) return false;

        boolean lflag = false;
        String orgnMode = getMaidModeString();

        setActiveModeClass(null);
        for (int li = 0; li < maidEntityModeList.size() && !lflag; li++) {
            lflag = maidEntityModeList.get(li).changeMode(par1EntityPlayer);
            if (lflag) {
                setActiveModeClass(maidEntityModeList.get(li));
            }
        }
        //par1EntityPlayer.addChatMessage(new ChatComponentText(String.format("ID:%d",lflag == true ? 1 : 0)));
        if (!lflag) {
            setMaidMode("Escorter");
            setEquipItem(-1);
//			maidInventory.currentItem = -1;
        }
        getNextEquipItem();

        if (!orgnMode.equals(getMaidModeString())) {
            clearTilePosAll();
            getNavigator().clearPathEntity();
            setAttackTarget(null);
            return true;
        }
        return false;
    }

    // メイドの契約設定
    @Override
    public boolean isTamed() {
        return isContract();
    }

    public boolean isContract() {
        return super.isTamed();
    }

    public boolean isContractEX() {
        return isContract() && isRemainsContract();
    }

    @Override
    public void setTamed(boolean par1) {
        setContract(par1);
    }

    @Override
    public void setContract(boolean flag) {
        super.setTamed(flag);
        textureData.setContract(flag);
        if (flag) {
//			maidMode = mmode_Escorter;
        } else {
        }
    }

    /**
     * 契約期間の残りがあるかを確認
     */
    protected void updateRemainsContract() {
        boolean lflag = false;
        if (maidContractLimit > 0) {
            maidContractLimit--;
            lflag = true;
        }
        if (getMaidFlags(dataWatch_Flags_remainsContract) != lflag) {
            setMaidFlags(lflag, dataWatch_Flags_remainsContract);
        }
    }

    /**
     * ストライキに入っていないか判定
     *
     * @return
     */
    public boolean isRemainsContract() {
        return getMaidFlags(dataWatch_Flags_remainsContract);
    }

    public float getContractLimitDays() {
        return maidContractLimit > 0 ? ((float) maidContractLimit / 24000F) : -1F;
    }

    public boolean updateMaidContract() {
        // 同一性のチェック
        boolean lf = isContract();
        if (textureData.isContract() != lf) {
            textureData.setContract(lf);
            return true;
        }
        return false;
    }

    @Override
    public EntityLivingBase getOwner() {
        return getMaidMasterEntity();
    }

    public String getMaidMaster() {
        return W_Common.getOwnerName(this);
    }

    public EntityPlayer getMaidMasterEntity() {
        // 主を獲得
        if (isContract()) {
            EntityPlayer entityplayer = mstatMasterEntity;
            if (mstatMasterEntity == null || mstatMasterEntity.isDead) {
                String lname;
                // サーバー側ならちゃんとオーナ判定する

                // Minecraftクラスのプレイヤーを取得していたが、サーバには存在しないためプロキシをかませる。サーバならNULL固定
                EntityPlayer clientPlayer = LittleMaidMobX.proxy.getClientPlayer();

                if (!LittleMaidMobX.proxy.isSinglePlayer()
                        || LittleMaidConfig.checkOwnerName
                        || clientPlayer == null) {
                    lname = getMaidMaster();
                } else {
                    lname = MMM_Helper.getPlayerName(clientPlayer);
                }
                entityplayer = worldObj.getPlayerEntityByName(lname);
                // とりあえず主の名前を入れてみる
                // TODO:再設定は不可になったので経過観察
//				maidAvatar.username = lname;

                if (entityplayer != null && maidAvatar != null) {
                    maidAvatar.capabilities.isCreativeMode = entityplayer.capabilities.isCreativeMode;
                }

            }
            return entityplayer;
        } else {
            return null;
        }
    }

    public boolean isMaidContractOwner(String pname) {
        return pname.equalsIgnoreCase(MMM_Helper.getPlayerName(mstatMasterEntity));
    }

    public boolean isMaidContractOwner(EntityPlayer pentity) {
        return pentity == getMaidMasterEntity();

//		return pentity == mstatMasterEntity;
    }

    // メイドの待機設定
    public boolean isMaidWait() {
        return maidWait;
    }

    public boolean isMaidWaitEx() {
        return isMaidWait() | (mstatWaitCount > 0) | isOpenInventory();
    }

    public void setMaidWait(boolean pflag) {
        // 待機常態の設定、 isMaidWait系でtrueを返すならAIが勝手に移動を停止させる。
        maidWait = pflag;
        setMaidFlags(pflag, dataWatch_Flags_Wait);

        aiSit.setSitting(pflag);
        isJumping = false;
        setAttackTarget(null);
        setRevengeTarget(null);
        setPathToEntity(null);
        getNavigator().clearPathEntity();
        velocityChanged = true;
    }

    public void setMaidWaitCount(int count) {
        mstatWaitCount = count;
    }

    // インベントリの表示関係
    // まさぐれるのは一人だけ
    public void setOpenInventory(boolean flag) {
        mstatOpenInventory = flag;
    }

    public boolean isOpenInventory() {
        return mstatOpenInventory;
    }

    /**
     * GUIを開いた時にサーバー側で呼ばれる。
     */
    public void onGuiOpened() {
        setOpenInventory(true);
    }

    /**
     * GUIを閉めた時にサーバー側で呼ばれる。
     */
    public void onGuiClosed() {
        setOpenInventory(false);
        int li = maidMode & 0x0080;
        setMaidWaitCount((li == 0) ? 50 : 0);
    }

    // 腕振り
    public void setSwing(int attacktime, EnumSound enumsound) {
        setSwing(attacktime, enumsound, maidDominantArm);
    }

    public void setSwing(int pattacktime, EnumSound enumsound, int pArm) {
        mstatSwingStatus[pArm].attackTime = pattacktime;
//		maidAttackSound = enumsound;
//		soundInterval = 0;// いるか？
        if (!weaponFullAuto) {
            setSwinging(pArm, enumsound);
        }
        if (!worldObj.isRemote) {
            NetworkHandler.sendPacketToAllTracking(new SwingArmPacket(this, pArm, enumsound), this);
        }
    }

    public void setSwinging(EnumSound pSound) {
        setSwinging(maidDominantArm, pSound);
    }

    public void setSwinging(int pArm, EnumSound pSound) {
        if (mstatSwingStatus[pArm].setSwinging()) {
            playLittleMaidSound(pSound, true);
            maidAvatar.swingProgressInt = -1;
//			maidAvatar.swingProgressInt = -1;
            maidAvatar.isSwingInProgress = true;
        }
    }

    public boolean getSwinging() {
        return getSwinging(maidDominantArm);
    }

    public boolean getSwinging(int pArm) {
        return mstatSwingStatus[pArm].isSwingInProgress;
    }

    /**
     * 利き腕のリロードタイム
     */
    public SwingStatus getSwingStatusDominant() {
        return mstatSwingStatus[maidDominantArm];
    }

    public SwingStatus getSwingStatus(int pindex) {
        return mstatSwingStatus[pindex];
    }


    // 今宵のメイドは血に飢えておる
    public void setBloodsuck(boolean pFlag) {
        mstatBloodsuck = pFlag;
        setMaidFlags(pFlag, dataWatch_Flags_Bloodsuck);
    }

    public boolean isBloodsuck() {
        return mstatBloodsuck;
    }


    // 砂糖関連
    public void setLookSuger(boolean pFlag) {
        mstatLookSuger = pFlag;
        setMaidFlags(pFlag, dataWatch_Flags_LooksSugar);
    }

    public boolean isLookSuger() {
        return mstatLookSuger;
    }

    /**
     * ペロッ・・・これは・・・砂糖ッ！！
     * motion : 腕を振るか？
     * recontract : 契約延長効果アリ？
     */
    public void eatSugar(boolean motion, boolean recontract) {
        if (motion) {
            setSwing(2, (getMaxHealth() - getHealth() <= 1F) ? EnumSound.eatSugar_MaxPower : EnumSound.eatSugar);
        }
        int h = hurtResistantTime;
        heal(1);
        hurtResistantTime = h;
        playSound("random.pop");
        LittleMaidMobX.debug(("eat Suger." + worldObj.isRemote));

        if (recontract) {
            // 契約期間の延長
            maidContractLimit += LittleMaidConfig.maidContractLimit;
            if (maidContractLimit > LittleMaidConfig.maxMaidContractLimit) {
                maidContractLimit = LittleMaidConfig.maxMaidContractLimit;    // 24000 * 7
            }
        }

        // 暫定処理
        if (maidAvatar != null) {
            maidAvatar.getFoodStats().addStats(20, 20F);
        }
    }


    // お仕事チュ

    /**
     * 仕事中かどうかの設定
     */
    public void setWorking(boolean pFlag) {
        mstatWorkingCount.setEnable(pFlag);
    }

    /**
     * 仕事中かどうかを返す
     */
    public boolean isWorking() {
        return mstatWorkingCount.isEnable();
    }

    /**
     * 仕事が終了しても余韻を含めて返す
     */
    public boolean isWorkingDelay() {
        return mstatWorkingCount.isDelay();
    }

    /**
     * トレーサーモードの設定
     */
    public void setTracer(boolean pFlag) {
        maidTracer = pFlag;
        setMaidFlags(pFlag, dataWatch_Flags_Tracer);
        if (maidTracer) {
            setFreedom(true);
        }
        aiTracer.setEnable(pFlag);
    }

    /**
     * トレーサーモードであるか？
     */
    public boolean isTracer() {
        return maidTracer;
    }


    // お遊びモード
    public void setPlayingRole(int pValue) {
        if (mstatPlayingRole != pValue) {
            mstatPlayingRole = pValue;
            if (pValue == 0) {
                setAttackTarget(null);
                setMaidMode(mstatWorkingInt, true);
            } else {
                setMaidMode(0x00ff, true);
            }
        }
    }

    public int getPlayingRole() {
        return mstatPlayingRole;
    }

    public boolean isPlaying() {
        return mstatPlayingRole != 0;
    }


    // 自由行動
    public void setFreedom(boolean pFlag) {
        // AI関連のリセットもここで。
        maidFreedom = pFlag;
        aiRestrictRain.setEnable(pFlag);
        aiFreeRain.setEnable(pFlag);
        aiWander.setEnable(pFlag);
//		aiJumpTo.setEnable(!pFlag);
        aiAvoidPlayer.setEnable(!pFlag);
        aiFollow.setEnable(!pFlag);
        aiTracer.setEnable(false);
//		setAIMoveSpeed(pFlag ? moveSpeed_Nomal : moveSpeed_Max);
//		setMoveForward(0.0F);

        if (maidFreedom && isContract()) {
            setHomeArea(
//			setHomeArea(
                    MathHelper.floor_double(posX),
                    MathHelper.floor_double(posY),
                    MathHelper.floor_double(posZ), 16);
        } else {
            detachHome();
            setPlayingRole(0);
        }

        setMaidFlags(maidFreedom, dataWatch_Flags_Freedom);
    }

    public boolean isFreedom() {
        return maidFreedom;
    }


    /**
     * サーバーへテクスチャパックのインデックスを送る。
     * クライアント側の処理
     */
    public boolean sendTextureToServer() {
        // 16bitあればテクスチャパックの数にたりんべ
        MMM_TextureManager.instance.postSetTexturePack(this, textureData.getColor(), textureData.getTextureBox());
        return true;
    }


    public boolean updateTexturePack() {
        // テクスチャパックが更新されていないかをチェック
        // クライアント側の
        boolean lflag = false;
        MMM_TextureBoxServer lbox;

        int ltexture = dataWatcher.getWatchableObjectInt(dataWatch_Texture);
        int larmor = (ltexture >>> 16) & 0xffff;
        ltexture &= 0xffff;
        if (textureData.textureIndex[0] != ltexture) {
            textureData.textureIndex[0] = ltexture;
            lflag = true;
        }
        if (textureData.textureIndex[1] != larmor) {
            textureData.textureIndex[1] = larmor;
            lflag = true;
        }
        if (lflag) {
            MMM_TextureManager.instance.postGetTexturePack(this, textureData.getTextureIndex());
        }
        return lflag;
    }

    @Override
    public int getColor() {
//		return textureData.getColor();
        return dataWatcher.getWatchableObjectByte(dataWatch_Color);
    }

    @Override
    public void setColor(int index) {
        textureData.setColor(index);
        dataWatcher.updateObject(dataWatch_Color, (byte) index);
    }

    public boolean updateMaidColor() {
        // 同一性のチェック
        int lc = getColor();
        if (textureData.getColor() != lc) {
            textureData.setColor(lc);
            return true;
        }
        return false;
    }

    /**
     * 紐の持ち主
     */
    public void updateGotcha() {
        int lid = dataWatcher.getWatchableObjectInt(dataWatch_Gotcha);
        if (lid == 0) {
            mstatgotcha = null;
            return;
        }
        if (mstatgotcha != null && mstatgotcha.getEntityId() == lid) {
            return;
        }
        for (int li = 0; li < worldObj.loadedEntityList.size(); li++) {
            if (((Entity) worldObj.loadedEntityList.get(li)).getEntityId() == lid) {
                mstatgotcha = (Entity) worldObj.loadedEntityList.get(li);
                break;
            }
        }
    }

    public void setGotcha(int pEntityID) {
        dataWatcher.updateObject(dataWatch_Gotcha, Integer.valueOf(pEntityID));
    }

    public void setGotcha(Entity pEntity) {
        setGotcha(pEntity == null ? 0 : pEntity.getEntityId());
    }


    /**
     * 弓構えを更新
     */
    public void updateAimebow() {
        boolean lflag = (maidAvatar != null && getAvatarIF().isUsingItemLittleMaid()) || mstatAimeBow;
        setMaidFlags(lflag, dataWatch_Flags_Aimebow);
    }

    public boolean isAimebow() {
        return (dataWatcher.getWatchableObjectInt(dataWatch_Flags) & dataWatch_Flags_Aimebow) > 0;
    }


    /**
     * 各種フラグのアップデート
     */
    public void updateMaidFlagsClient() {
        int li = dataWatcher.getWatchableObjectInt(dataWatch_Flags);
        maidFreedom = (li & dataWatch_Flags_Freedom) > 0;
        maidTracer = (li & dataWatch_Flags_Tracer) > 0;
        maidWait = (li & dataWatch_Flags_Wait) > 0;
        mstatAimeBow = (li & dataWatch_Flags_Aimebow) > 0;
        mstatLookSuger = (li & dataWatch_Flags_LooksSugar) > 0;
        mstatBloodsuck = (li & dataWatch_Flags_Bloodsuck) > 0;
        looksWithInterest = (li & dataWatch_Flags_looksWithInterest) > 0;
        looksWithInterestAXIS = (li & dataWatch_Flags_looksWithInterestAXIS) > 0;
        maidOverDriveTime.updateClient((li & dataWatch_Flags_OverDrive) > 0);
        mstatWorkingCount.updateClient((li & dataWatch_Flags_Working) > 0);
    }

    /**
     * フラグ群に値をセット。
     *
     * @param pFlag： 対象値。
     * @param pFlagvalue： 対象フラグ。
     */
    public void setMaidFlags(boolean pFlag, int pFlagvalue) {
        int li = dataWatcher.getWatchableObjectInt(dataWatch_Flags);
        li = pFlag ? (li | pFlagvalue) : (li & ~pFlagvalue);
        dataWatcher.updateObject(dataWatch_Flags, Integer.valueOf(li));
    }

    /**
     * 指定されたフラグを獲得
     */
    public boolean getMaidFlags(int pFlagvalue) {
        return (dataWatcher.getWatchableObjectInt(dataWatch_Flags) & pFlagvalue) > 0;
    }

    /**
     * 利き腕の設定
     */
    public void setDominantArm(int pindex) {
        if (mstatSwingStatus.length <= pindex) return;
        if (maidDominantArm == pindex) return;
        for (SwingStatus lss : mstatSwingStatus) {
            lss.index = lss.lastIndex = -1;
        }
        maidDominantArm = pindex;
        dataWatcher.updateObject(dataWatch_DominamtArm, (byte) maidDominantArm);
        LittleMaidMobX.debug("Change Dominant.");
    }

    @Override
    public void setHomeArea(int par1, int par2, int par3, int par4) {
        homeWorld = dimension;
        super.setHomeArea(par1, par2, par3, par4);
    }

    @Override
    public void setTexturePackIndex(int pColor, int[] pIndex) {
        // Server
        textureData.setTexturePackIndex(pColor, pIndex);
        dataWatcher.updateObject(dataWatch_Texture, ((textureData.textureIndex[0] & 0xffff) | (textureData.textureIndex[1] & 0xffff) << 16));
        LittleMaidMobX.debug("changeSize-ID:%d: %f, %f, %b", getEntityId(), width, height, worldObj.isRemote);
        setColor(pColor);
        setTextureNames();
    }

    @Override
    public void setTexturePackName(MMM_TextureBox[] pTextureBox) {
        if (worldObj.isRemote) {
            // Client
            textureData.setTexturePackName(pTextureBox);
            setTextureNames();
            LittleMaidMobX.debug("ID:%d, TextureModel:%s", getEntityId(), textureData.getTextureName(0));
            // モデルの初期化
            ((MMM_TextureBox) textureData.textureBox[0]).models[0].setCapsValue(IModelCaps.caps_changeModel, maidCaps);
            // スタビの付け替え
//		for (Entry<String, MMM_EquippedStabilizer> le : pEntity.maidStabilizer.entrySet()) {
//			if (le.getValue() != null) {
//				le.getValue().updateEquippedPoint(pEntity.textureModel0);
//			}
//		}
            maidSoundRate = LittleMaidSoundManager.getSoundRate(textureData.getTextureName(0), getColor());

        }
    }

    /**
     * Client用
     */
    public void setTextureNames() {
        textureData.setTextureNames();
		/* http://forum.minecraftuser.jp/posting.php?mode=reply&f=13&t=23347#pr231574
		if (!textureData.setTextureNames()) {
			// TODO:setDefaultTexture
//			if (worldObj.isRemote) {
				setNextTexturePackege(0);
//			}
		}
		*/
    }

    public void setNextTexturePackege(int pTargetTexture) {
        textureData.setNextTexturePackege(pTargetTexture);
    }

    public void setPrevTexturePackege(int pTargetTexture) {
        textureData.setPrevTexturePackege(pTargetTexture);
    }


    // textureEntity

    @Override
    public void setTextureBox(MMM_TextureBoxBase[] pTextureBox) {
        textureData.setTextureBox(pTextureBox);
    }

    @Override
    public MMM_TextureBoxBase[] getTextureBox() {
        return textureData.getTextureBox();
    }

    @Override
    public void setTextureIndex(int[] pTextureIndex) {
        textureData.setTextureIndex(pTextureIndex);
    }

    @Override
    public int[] getTextureIndex() {
        return textureData.getTextureIndex();
    }

    @Override
    public void setTextures(int pIndex, ResourceLocation[] pNames) {
        textureData.setTextures(pIndex, pNames);
    }

    @Override
    public ResourceLocation[] getTextures(int pIndex) {
        return textureData.getTextures(pIndex);
    }

    @Override
    public MMM_TextureData getTextureData() {
        return textureData;
    }

    // Tile関係

    /**
     * 使っているTileかどうか判定して返す。
     */
    public boolean isUsingTile(TileEntity pTile) {
        if (isActiveModeClass()) {
            return getActiveModeClass().isUsingTile(pTile);
        }
        for (int[] tile : maidTiles) {
            if (tile != null &&
                    pTile.xCoord == tile[0] &&
                    pTile.yCoord == tile[1] &&
                    pTile.zCoord == tile[2]) {
                return true;
            }
        }
        return false;
    }

    public boolean isEqualTile() {
        return worldObj.getTileEntity(maidTile[0], maidTile[1], maidTile[2]) == maidTileEntity;
    }

    public boolean isTilePos() {
        return maidTileEntity != null;
    }

    public boolean isTilePos(int pIndex) {
        if (pIndex < maidTiles.length) {
            return maidTiles[pIndex] != null;
        }
        return false;
    }

    /**
     * ローカル変数にTileの位置を入れる。
     */
    public boolean getTilePos(int pIndex) {
        if (pIndex < maidTiles.length && maidTiles[pIndex] != null) {
            maidTile[0] = maidTiles[pIndex][0];
            maidTile[1] = maidTiles[pIndex][1];
            maidTile[2] = maidTiles[pIndex][2];
            return true;
        }
        return false;
    }

    public void setTilePos(int pX, int pY, int pZ) {
        maidTile[0] = pX;
        maidTile[1] = pY;
        maidTile[2] = pZ;
    }

    public void setTilePos(TileEntity pEntity) {
        maidTile[0] = pEntity.xCoord;
        maidTile[1] = pEntity.yCoord;
        maidTile[2] = pEntity.zCoord;
        maidTileEntity = pEntity;
    }

    public void setTilePos(int pIndex) {
        if (pIndex < maidTiles.length) {
            if (maidTiles[pIndex] == null) {
                maidTiles[pIndex] = new int[3];
            }
            maidTiles[pIndex][0] = maidTile[0];
            maidTiles[pIndex][1] = maidTile[1];
            maidTiles[pIndex][2] = maidTile[2];
        }
    }

    public void setTilePos(int pIndex, int pX, int pY, int pZ) {
        if (pIndex < maidTiles.length) {
            if (maidTiles[pIndex] == null) {
                maidTiles[pIndex] = new int[3];
            }
            maidTiles[pIndex][0] = pX;
            maidTiles[pIndex][1] = pY;
            maidTiles[pIndex][2] = pZ;
        }
    }

    public TileEntity getTileEntity() {
        return maidTileEntity = worldObj.getTileEntity(maidTile[0], maidTile[1], maidTile[2]);
    }

    public TileEntity getTileEntity(int pIndex) {
        if (pIndex < maidTiles.length && maidTiles[pIndex] != null) {
            TileEntity ltile = worldObj.getTileEntity(
                    maidTiles[pIndex][0], maidTiles[pIndex][1], maidTiles[pIndex][2]);
            if (ltile == null) {
                clearTilePos(pIndex);
            }
            return ltile;
        }
        return null;
    }

    public void clearTilePos() {
        maidTileEntity = null;
    }

    public void clearTilePos(int pIndex) {
        if (pIndex < maidTiles.length) {
            maidTiles[pIndex] = null;
        }
    }

    public void clearTilePosAll() {
        for (int li = 0; li < maidTiles.length; li++) {
            maidTiles[li] = null;
        }
    }

    public double getDistanceTilePos() {
        return getDistance(
                (double) maidTile[0] + 0.5D,
                (double) maidTile[1] + 0.5D,
                (double) maidTile[2] + 0.5D);
    }

    public double getDistanceTilePosSq() {
        return getDistanceSq(
                (double) maidTile[0] + 0.5D,
                (double) maidTile[1] + 0.5D,
                (double) maidTile[2] + 0.5D);
    }

    public double getDistanceTilePos(int pIndex) {
        if (maidTiles.length > pIndex && maidTiles[pIndex] != null) {
            return getDistance(
                    (double) maidTiles[pIndex][0] + 0.5D,
                    (double) maidTiles[pIndex][1] + 0.5D,
                    (double) maidTiles[pIndex][2] + 0.5D);
        }
        return -1D;
    }

    public double getDistanceTilePosSq(int pIndex) {
        if (maidTiles.length > pIndex && maidTiles[pIndex] != null) {
            return getDistanceSq(
                    (double) maidTiles[pIndex][0] + 0.5D,
                    (double) maidTiles[pIndex][1] + 0.5D,
                    (double) maidTiles[pIndex][2] + 0.5D);
        }
        return -1D;
    }

    public double getDistanceTilePos(TileEntity pTile) {
        if (pTile != null) {
            return getDistance(
                    (double) pTile.xCoord + 0.5D,
                    (double) pTile.yCoord + 0.5D,
                    (double) pTile.zCoord + 0.5D);
        }
        return -1D;
    }

    public double getDistanceTilePosSq(TileEntity pTile) {
        if (pTile != null) {
            return getDistanceSq(
                    (double) pTile.xCoord + 0.5D,
                    (double) pTile.yCoord + 0.5D,
                    (double) pTile.zCoord + 0.5D);
        }
        return -1D;
    }

    public void looksTilePos() {
        getLookHelper().setLookPosition(
                maidTile[0] + 0.5D, maidTile[1] + 0.5D, maidTile[2] + 0.5D,
                10F, getVerticalFaceSpeed());
    }

    public void looksTilePos(int pIndex) {
        if (maidTiles.length > pIndex && maidTiles[pIndex] != null) {
            getLookHelper().setLookPosition(
                    maidTiles[pIndex][0] + 0.5D,
                    maidTiles[pIndex][1] + 0.5D,
                    maidTiles[pIndex][2] + 0.5D,
                    10F, getVerticalFaceSpeed());
        }
    }

    public boolean isUsingItem() {
        return dataWatcher.getWatchableObjectInt(dataWatch_ItemUse) > 0;
    }

    public boolean isUsingItem(int pIndex) {
        return (dataWatcher.getWatchableObjectInt(dataWatch_ItemUse) & (1 << pIndex)) > 0;
    }

    public void setExperienceValue(int val) {
        this.experienceValue = val;
    }

    public int getExperienceValue() {
        return this.experienceValue;
    }

    public void setFlag(int par1, boolean par2) {
        super.setFlag(par1, par2);
    }

    public void updateWanderPath() {
        super.updateWanderPath();
    }

    public boolean isMovementCeased() {
        return super.isMovementCeased();
    }

    public void setSize2(float par1, float par2) {
        super.setSize(par1, par2);
    }

    public float getEyeHeight() {
        return this.height * 0.95F;
    }

    /**
     * 攻撃された回数を増やす。
     */
    public void addAttackCount(int num) {
        if (num > 0) {
            attackCount += num;
        }
    }


    /**
     * 攻撃された回数を返す。
     */
    public int getAttackCount() {
        return attackCount;
    }

    /**
     * プレイヤーを攻撃し続ける時間を返す。
     */
    public int getAttackTime() {
        if (attackCount == 0) {
            return 30 * 20;
        } else {
            return attackCount * 30 * 20;
        }
    }
}
