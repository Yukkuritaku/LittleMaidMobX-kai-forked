package littleMaidMobX.network.packet.client;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import littleMaidMobX.client.audio.EnumSound;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class PlayLittleMaidSoundPacket implements IMessage {

    private int entityId;
    private int soundName;

    public PlayLittleMaidSoundPacket(){}
    public PlayLittleMaidSoundPacket(Entity maid, EnumSound soundName){
        this.entityId = maid.getEntityId();
        this.soundName = soundName.index;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.soundName = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.soundName);
    }

    public static class Handler implements IMessageHandler<PlayLittleMaidSoundPacket, IMessage> {

        @Override
        public IMessage onMessage(PlayLittleMaidSoundPacket message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(message.entityId);
                /*if (entity instanceof ISoundPlayable){
                    ((ISoundPlayable) entity).play(message.soundName);
                }*/
            }
            return null;
        }
    }
}
