package littleMaidMobX.network;

import java.util.List;

import littleMaidMobX.client.audio.LittleMaidSoundManager;
import littleMaidMobX.client.renderer.entity.RenderLittleMaid;
import littleMaidMobX.client.resources.OldZipTexturesLoader;
import littleMaidMobX.entity.EntityLittleMaid;
import littleMaidMobX.network.packet.old.Message;
import mmmlibx.lib.MMM_EntityDummy;
import mmmlibx.lib.MMM_EntitySelect;
import mmmlibx.lib.MMM_Helper;
import mmmlibx.lib.MMM_RenderDummy;
import mmmlibx.lib.multiModel.model.mc162.RenderModelMulti;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityCrit2FX;
import net.minecraft.client.particle.EntityPickupFX;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.ObfuscationReflectionHelper;

/**
 * クライアント専用処理。
 * マルチ用に分離。
 * 分離しとかないとNoSuchMethodで落ちる。
 */
public class ProxyClient extends ProxyCommon {

	public void init() {
		RenderingRegistry.registerEntityRenderingHandler(EntityLittleMaid.class, new RenderLittleMaid(0.3F));
		RenderingRegistry.registerEntityRenderingHandler(MMM_EntitySelect.class, new RenderModelMulti(0.0F));
		RenderingRegistry.registerEntityRenderingHandler(MMM_EntityDummy.class, new MMM_RenderDummy());
		// TODO ★		RenderingRegistry.registerEntityRenderingHandler(EntityItem.class,			new MMM_RenderItem());
	}

	/* 呼び出し箇所なし
	public GuiContainer getContainerGUI(EntityClientPlayerMP var1, int var2,
			int var3, int var4, int var5) {
		Entity lentity = var1.worldObj.getEntityByID(var3);
		if (lentity instanceof LMM_EntityLittleMaid) {
			LMM_GuiInventory lgui = new LMM_GuiInventory(var1, (LMM_EntityLittleMaid)lentity);
//			var1.openContainer = lgui.inventorySlots;
			return lgui;
		} else {
			return null;
		}
	}
	*/

	// Avatar

	public void onItemPickup(EntityPlayer pAvatar, Entity entity, int i) {
		// アイテム回収のエフェクト
		// TODO:こっちを使うか？
		//        mc.effectRenderer.addEffect(new EntityPickupFX(mc.theWorld, entity, avatar, -0.5F));
		MMM_Helper.mc.effectRenderer.addEffect(new EntityPickupFX(MMM_Helper.mc.theWorld, entity, pAvatar, 0.1F));
	}

	public void onCriticalHit(EntityPlayer pAvatar, Entity par1Entity) {
		MMM_Helper.mc.effectRenderer.addEffect(new EntityCrit2FX(MMM_Helper.mc.theWorld, par1Entity));
	}

	public void onEnchantmentCritical(EntityPlayer pAvatar, Entity par1Entity) {
		EntityCrit2FX entitycrit2fx = new EntityCrit2FX(MMM_Helper.mc.theWorld, par1Entity, "magicCrit");
		MMM_Helper.mc.effectRenderer.addEffect(entitycrit2fx);
	}

	// Network

	public void clientCustomPayload(Message message) {
		// クライアント側の特殊パケット受信動作
		/*byte mode = message.data[0];
		int leid = 0;
		EntityLittleMaid lemaid = null;
		if ((mode & 0x80) != 0) {
			leid = MMM_Helper.getInt(message.data, 1);
			lemaid = Net.getLittleMaid(message.data, 1, MMM_Helper.mc.theWorld);
			if (lemaid == null)
				return;
		}
		LittleMaidMobX.debug(String.format("LMM|Upd Clt Call[%2x:%d].", mode, leid));

		switch (mode) {
			case LMN_Client_SwingArm:
				// 腕振り
				byte larm = message.data[5];
				EnumSound lsound = EnumSound.getEnumSound(MMM_Helper.getInt(message.data, 6));
				lemaid.setSwinging(larm, lsound);
				//			mod_LMM_littleMaidMob.Debug(String.format("SwingSound:%s", lsound.name()));
				break;

			case LMN_Client_SetIFFValue:
				// IFFの設定値を受信
				int lval = message.data[1];
				int lindex = MMM_Helper.getInt(message.data, 2);
				String lname = (String) IFF.DefaultIFF.keySet().toArray()[lindex];
				LittleMaidMobX.debug("setIFF-CL %s(%d)=%d", lname, lindex, lval);
				IFF.setIFFValue(null, lname, lval);
				break;

			case LMN_Client_PlaySound:
				// 音声再生
				EnumSound lsound9 = EnumSound.getEnumSound(MMM_Helper.getInt(message.data, 5));
				lemaid.playLittleMaidSound(lsound9, true);
				LittleMaidMobX.debug(String.format("playSound:%s", lsound9.name()));
				break;

		}*/
	}

	public EntityPlayer getClientPlayer() {
		return Minecraft.getMinecraft().thePlayer;
	}

	/* 呼び出し箇所なし
	public static void setAchievement() {
// MinecraftクラスからstatFileWriterが消えてる
//		MMM_Helper.mc.statFileWriter.readStat(mod_LMM_littleMaidMob.ac_Contract, 1);
	}
	*/

	public void loadSounds() {
		// 音声の解析
		LittleMaidSoundManager.init();
		// サウンドパック
		LittleMaidSoundManager.loadDefaultSoundPack();
		LittleMaidSoundManager.loadSoundPack();
	}

	public boolean isSinglePlayer() {
		return Minecraft.getMinecraft().isSingleplayer();
	}

	public void postInit() {
		List<IResourcePack> defaultResourcePacks = ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "defaultResourcePacks", "field_110449_ao");
		defaultResourcePacks.add(new OldZipTexturesLoader());
	}
}