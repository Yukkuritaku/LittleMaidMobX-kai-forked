package littleMaidMobX.network.packet.client;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import littleMaidMobX.entity.modes.IFF;

public class SetClientIFFPacket implements IMessage {

    private int value;
    private int index;

    public SetClientIFFPacket(){}
    public SetClientIFFPacket(int value, int index){
        this.value = value;
        this.index = index;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.value = buf.readInt();
        this.index = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.value);
        buf.writeInt(this.index);
    }


    public static class Handler implements IMessageHandler<SetClientIFFPacket, IMessage> {

        @Override
        public IMessage onMessage(SetClientIFFPacket message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT){
                String name = (String) IFF.DefaultIFF.keySet().toArray()[message.index];
                IFF.setIFFValue(null, name, message.value);
            }
            return null;
        }
    }
}
