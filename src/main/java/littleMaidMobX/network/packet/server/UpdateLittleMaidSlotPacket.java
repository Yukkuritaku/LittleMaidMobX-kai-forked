package littleMaidMobX.network.packet.server;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import littleMaidMobX.entity.EntityLittleMaid;
import littleMaidMobX.entity.modes.SwingStatus;
import net.minecraft.entity.Entity;

public class UpdateLittleMaidSlotPacket implements IMessage {

    private int entityId;

    public UpdateLittleMaidSlotPacket(){}

    public UpdateLittleMaidSlotPacket(EntityLittleMaid maid) {
        this.entityId = maid.getEntityId();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
    }


    public static class Handler implements IMessageHandler<UpdateLittleMaidSlotPacket, IMessage>{

        @Override
        public IMessage onMessage(UpdateLittleMaidSlotPacket message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                Entity entity = ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.entityId);
                if (entity instanceof EntityLittleMaid){
                    EntityLittleMaid maid = (EntityLittleMaid) entity;
                    maid.maidInventory.clearChanged();
                    for (SwingStatus swingStatus : maid.mstatSwingStatus) {
                        swingStatus.lastIndex = -1;
                    }
                }
            }
            return null;
        }
    }
}
