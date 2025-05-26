package littleMaidMobX.client.gui;

import littleMaidMobX.entity.EntityLittleMaid;
import littleMaidMobX.entity.modes.IFF;
import littleMaidMobX.network.NetworkHandler;
import littleMaidMobX.network.packet.server.GetIFFPacket;
import littleMaidMobX.network.packet.server.SaveIFFPacket;
import littleMaidMobX.network.packet.server.SetServerIFFPacket;
import mmmlibx.lib.Client;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public class GuiIFF extends GuiMobSelect {

	public static final String[] IFFString = {
		"ENEMY", // 反撃、狩
		"UNKNOWN", // 反撃
		"FRIENDLY" // 攻撃しない
	};

	protected EntityLittleMaid target;


	public GuiIFF(World world, EntityLittleMaid pEntity) {
		super(world);
		screenTitle = "LittleMaid IFF";
		target = pEntity;
		
		// IFFをサーバーから取得
		if (!Client.isIntegratedServerRunning()) {
			int li = 0;
			for (String ls : IFF.DefaultIFF.keySet()) {
				NetworkHandler.sendToServer(new GetIFFPacket(li, ls));
				li++;
			}
		}
	}

	@Override
	protected boolean checkEntity(String entityName, Entity entity, int index) {
		boolean lf = false;
		// Entityの値を設定
        IFF.checkEntityStatic(entityName, entity, index, this.entityMap);
        if (entity instanceof EntityLivingBase) {
			if (entity instanceof EntityLittleMaid) {
				if (index == 0 || index == 1) {
					// 野生種、自分契約者
					lf = true;
				} else {
					// 他人の契約者
				}
			} else if (entity instanceof IEntityOwnable) {
				if (index == 0 || index == 1) {
					// 野生種、自分の
					lf = true;
				} else {
					// 他人の家畜
				}
			}
		}
		
		return lf;
	}
	@SuppressWarnings("unchecked")
	@Override
	public void initGui() {
		super.initGui();
		
		this.buttonList.add(new GuiButton(200, this.width / 2 - 130, this.height - 40, 120, 20,
				StatCollector.translateToLocal("gui.done")));
		this.buttonList.add(new GuiButton(201, this.width / 2 + 10, this.height - 40, 120, 20,
				"Trigger Select"));
	}

	@Override
	protected void actionPerformed(GuiButton guibutton) {
		if (!guibutton.enabled) {
			return;
		}
		if (guibutton.id == 200) {
			mc.displayGuiScreen(null);
		}
		if (guibutton.id == 201) {
			mc.displayGuiScreen(new GuiTriggerSelect(mc.thePlayer, this));
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
        return super.doesGuiPauseGame();
    }

	@Override
	public void onGuiClosed() {
		NetworkHandler.sendToServer(new SaveIFFPacket());
		//Net.saveIFF();
		super.onGuiClosed();
	}

	@Override
	public void clickSlot(int pIndex, boolean pDoubleClick, String pName, EntityLivingBase pEntity) {
		if (pDoubleClick) {
			int tt = IFF.getIFF(null, pName, pEntity.worldObj);
			tt++;
			if (tt > 2) {
				tt = 0;
			}
			
			if (!mc.isIntegratedServerRunning()) {
				// サーバーへ変更値を送る。
				int li = 0;
				for (String ls : IFF.DefaultIFF.keySet()) {
					if (ls.contains(pName)) {
						NetworkHandler.sendToServer(new SetServerIFFPacket(li, tt, pName));
					}
					li++;
				}
			} else {
				IFF.setIFFValue(null, pName, tt);
			}
			
			Entity player = mc.thePlayer;
			pEntity.worldObj.playSound(player.posX+0.5, player.posY+0.5, player.posZ+0.5, "random.click", 1, 1, false);
		}
	}

	@Override
	public void drawSlot(int pSlotindex, int pX, int pY, int pDrawheight,
			Tessellator pTessellator, String pName, Entity pEntity) {
		// 名前と敵味方識別の描画
		int tt = IFF.getIFF(null, pName, pEntity.worldObj);
		int c = 0xffffff;
		switch (tt) {
		case IFF.iff_Friendry:
			c = 0x3fff3f;
			break;
		case IFF.iff_Unknown:
			c = 0xffff00;
			break;
		case IFF.iff_Enemy:
			c = 0xff3f3f;
			break;
		}
		drawString(this.mc.fontRenderer, GuiIFF.IFFString[tt],
				(width - this.mc.fontRenderer.getStringWidth(GuiIFF.IFFString[tt])) / 2, pY + 18, c);
		drawString(this.mc.fontRenderer, pName,
				(width - this.mc.fontRenderer.getStringWidth(pName)) / 2, pY + 6, 0xffffff);
	}

}
