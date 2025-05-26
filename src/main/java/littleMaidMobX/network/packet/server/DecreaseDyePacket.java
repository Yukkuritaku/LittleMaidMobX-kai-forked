package littleMaidMobX.network.packet.server;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class DecreaseDyePacket implements IMessage {

    private int selectColor;

    public DecreaseDyePacket(){}
    public DecreaseDyePacket(int selectColor) {
        this.selectColor = selectColor;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.selectColor = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.selectColor);
    }

    public static class Handler implements IMessageHandler<DecreaseDyePacket, IMessage>{

        @Override
        public IMessage onMessage(DecreaseDyePacket message, MessageContext ctx) {
            if (ctx.side == Side.SERVER){
                EntityPlayerMP playerEntity = ctx.getServerHandler().playerEntity;
                if (!playerEntity.capabilities.isCreativeMode){
                    for (int i = 0; i < playerEntity.inventory.mainInventory.length; i++) {
                        ItemStack stack = playerEntity.inventory.mainInventory[i];
                        if (stack != null && stack.getItem() == Items.dye) {
                            if (stack.getItemDamage() == (15 - message.selectColor)) {
                                //MMM_Helper.decPlayerInventory(playerEntity, i, 1);
                                stack.stackSize--;
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
}
