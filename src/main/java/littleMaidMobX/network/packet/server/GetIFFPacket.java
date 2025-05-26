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

public class GetIFFPacket implements IMessage {

    private int index;
    private String entityName;

    public GetIFFPacket(){}
    public GetIFFPacket(int index, String entityName) {
        this.index = index;
        this.entityName = entityName;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.index = buf.readInt();
        this.entityName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.index);
        ByteBufUtils.writeUTF8String(buf, this.entityName);
    }

    public static class Handler implements IMessageHandler<GetIFFPacket, IMessage> {

        @Override
        public IMessage onMessage(GetIFFPacket message, MessageContext ctx) {
            if (ctx.side == Side.SERVER){
                int value = IFF.getIFF(ctx.getServerHandler().playerEntity.getGameProfile().getName(), message.entityName, ctx.getServerHandler().playerEntity.worldObj);
                NetworkHandler.sendPacketToPlayer(new SetClientIFFPacket(value, message.index), ctx.getServerHandler().playerEntity);
            }
            return null;
        }
    }
}
