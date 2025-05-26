package littleMaidMobX.network.packet.server;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import littleMaidMobX.entity.modes.IFF;
import littleMaidMobX.network.NetworkHandler;
import littleMaidMobX.network.packet.client.SetClientIFFPacket;

//TODO IFF Rework
public class SetServerIFFPacket implements IMessage {

    private int index;
    private int value;
    private String entityName;

    public SetServerIFFPacket(){}
    public SetServerIFFPacket(int index, int value, String entityName){
        this.index = index;
        this.value = value;
        this.entityName = entityName;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.index = buf.readInt();
        this.value = buf.readInt();
        this.entityName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.index);
        buf.writeInt(this.value);
        ByteBufUtils.writeUTF8String(buf, this.entityName);
    }


    public static class Handler implements IMessageHandler<SetServerIFFPacket, IMessage>{

        @Override
        public IMessage onMessage(SetServerIFFPacket message, MessageContext ctx) {
            if (ctx.side == Side.SERVER){
                IFF.setIFFValue(ctx.getServerHandler().playerEntity.getGameProfile().getName(), message.entityName, message.value);
                NetworkHandler.sendPacketToPlayer(new SetClientIFFPacket(message.value, message.index), ctx.getServerHandler().playerEntity);
            }
            return null;
        }
    }
}
