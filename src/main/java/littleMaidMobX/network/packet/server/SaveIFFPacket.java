package littleMaidMobX.network.packet.server;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import littleMaidMobX.entity.modes.IFF;

public class SaveIFFPacket implements IMessage {

    public SaveIFFPacket(){}
    @Override
    public void fromBytes(ByteBuf buf) {

    }

    @Override
    public void toBytes(ByteBuf buf) {

    }


    public static class Handler implements IMessageHandler<SaveIFFPacket, IMessage> {

        @Override
        public IMessage onMessage(SaveIFFPacket message, MessageContext ctx) {
            if (ctx.side == Side.SERVER){
                IFF.saveIFF(ctx.getServerHandler().playerEntity.getGameProfile().getName());
                if (!ctx.getServerHandler().playerEntity.worldObj.isRemote){
                    IFF.saveIFF("");
                }
            }
            return null;
        }
    }
}
