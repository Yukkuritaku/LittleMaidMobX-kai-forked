package littleMaidMobX.client.gui;

import littleMaidMobX.network.NetworkHandler;
import littleMaidMobX.network.packet.server.DecreaseDyePacket;
import littleMaidMobX.util.Statics;
import mmmlibx.lib.ITextureEntity;
import mmmlibx.lib.MMM_GuiTextureSelect;
import mmmlibx.lib.MMM_TextureManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/**
 * 選択時にサーバーへ染料の使用を通知するための処理。
 */
public class GuiTextureSelect extends MMM_GuiTextureSelect {

	public GuiTextureSelect(GuiScreen owner, ITextureEntity target,
							int color, boolean toServer) {
		super(owner, target, color, toServer);
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		super.actionPerformed(button);
        if (button.id == 200) {
            if (toServer) {
                MMM_TextureManager.instance.postSetTexturePack(target, selectColor, target.getTextureBox());
                if (selectColor != selectPanel.color) {
                    // 色情報の設定
//					theMaid.maidColor = selectPanel.color | 0x010000 | (selectColor << 8);
                    // サーバーへ染料の使用を通知
                    NetworkHandler.sendToServer(new DecreaseDyePacket(this.selectColor));
                }
            }
        }
	}

}
