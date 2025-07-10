package littleMaidMobX.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import littleMaidMobX.LittleMaidMobX;
import littleMaidMobX.entity.EntityLittleMaid;
import littleMaidMobX.network.packet.client.PlayLittleMaidSoundPacket;
import littleMaidMobX.network.packet.client.SetClientIFFPacket;
import littleMaidMobX.network.packet.client.SwingArmPacket;
import littleMaidMobX.network.packet.old.Message;
import littleMaidMobX.network.packet.server.*;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import java.util.concurrent.atomic.AtomicInteger;

public class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(LittleMaidMobX.MOD_ID);

    public static void init(){
        // mmmlib
        INSTANCE.registerMessage(Message.MessageHandler.class, Message.class, 0, Side.SERVER);
        INSTANCE.registerMessage(Message.MessageHandler.class, Message.class, 0, Side.CLIENT);
        AtomicInteger discriminator = new AtomicInteger(1);
        // client
        INSTANCE.registerMessage(PlayLittleMaidSoundPacket.Handler.class, PlayLittleMaidSoundPacket.class, discriminator.getAndIncrement(), Side.CLIENT);
        INSTANCE.registerMessage(SetClientIFFPacket.Handler.class, SetClientIFFPacket.class, discriminator.getAndIncrement(), Side.CLIENT);
        INSTANCE.registerMessage(SwingArmPacket.Handler.class, SwingArmPacket.class, discriminator.getAndIncrement(), Side.CLIENT);
        // server
        INSTANCE.registerMessage(DecreaseDyePacket.Handler.class, DecreaseDyePacket.class, discriminator.getAndIncrement(), Side.SERVER);
        INSTANCE.registerMessage(GetIFFPacket.Handler.class, GetIFFPacket.class, discriminator.getAndIncrement(), Side.SERVER);
        INSTANCE.registerMessage(SaveIFFPacket.Handler.class, SaveIFFPacket.class, discriminator.getAndIncrement(), Side.SERVER);
        INSTANCE.registerMessage(SetServerIFFPacket.Handler.class, SetServerIFFPacket.class, discriminator.getAndIncrement(), Side.SERVER);
        INSTANCE.registerMessage(UpdateLittleMaidSlotPacket.Handler.class, UpdateLittleMaidSlotPacket.class, discriminator.getAndIncrement(), Side.SERVER);
    }

    public static void sendToServer(IMessage message){
        INSTANCE.sendToServer(message);
    }

    public static void sendPacketToPlayer(IMessage packet, EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            INSTANCE.sendTo(packet, (EntityPlayerMP) player);
        }
    }

    public static void sendPacketToAllTracking(IMessage packet, EntityLittleMaid entity){
        if (entity.worldObj instanceof WorldServer) {
            EntityTracker tracker = ((WorldServer) entity.worldObj).getEntityTracker();
            for (EntityPlayer player : tracker.getTrackingPlayers(entity)) {
                sendPacketToPlayer(packet, player);
            }
        }
    }

    public static void sendPacketToServer(int ch, byte[] data) {
        INSTANCE.sendToServer(new Message(ch, data));
    }

    public static void sendPacketToPlayer(int ch, EntityPlayer player, byte[] data) {
        if (player instanceof EntityPlayerMP) {
            INSTANCE.sendTo(new Message(ch, data), (EntityPlayerMP) player);
        }
    }

    public static void sendPacketToAllPlayer(int ch, byte[] data) {
        INSTANCE.sendToAll(new Message(ch, data));
    }
}
