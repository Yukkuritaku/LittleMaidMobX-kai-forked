package littleMaidMobX.network.packet.client;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import littleMaidMobX.client.audio.EnumSound;
import littleMaidMobX.entity.EntityLittleMaid;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class SwingArmPacket implements IMessage {

    private int entityId;
    private int arm;
    private int soundName;

    public SwingArmPacket(){}
    public SwingArmPacket(EntityLittleMaid maid, int arm, EnumSound soundName){
        this.entityId = maid.getEntityId();
        this.arm = arm;
        this.soundName = soundName.index;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.arm = buf.readInt();
        this.soundName = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeInt(this.arm);
        buf.writeInt(this.soundName);
    }

    public static class Handler implements IMessageHandler<SwingArmPacket, IMessage> {

        @Override
        public IMessage onMessage(SwingArmPacket message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(message.entityId);
                if (entity instanceof EntityLittleMaid){
                    ((EntityLittleMaid) entity).setSwinging(message.arm, EnumSound.getEnumSound(message.soundName));
                }
            }
            return null;
        }
    }
}
