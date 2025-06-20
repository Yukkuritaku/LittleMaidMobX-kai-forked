package littleMaidMobX.network;

import littleMaidMobX.network.packet.old.Message;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

public class ProxyCommon
{
	public void init() {}
	public void postInit() {}
	public void onItemPickup(EntityPlayer lmm_EntityLittleMaidAvatar,Entity entity, int i) {}
	public void onCriticalHit(EntityPlayer pAvatar, Entity par1Entity) {}
	public void onEnchantmentCritical(EntityPlayer pAvatar, Entity par1Entity) {}
	public void clientCustomPayload(Message var2) {}
	public EntityPlayer getClientPlayer(){ return null; }
	public void loadSounds(){}

	public boolean isSinglePlayer()
	{
		return MinecraftServer.getServer().isSinglePlayer();
	}
}
