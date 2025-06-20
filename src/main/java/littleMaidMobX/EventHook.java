package littleMaidMobX;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import littleMaidMobX.entity.EntityLittleMaidAvatar;
import littleMaidMobX.entity.IEntityLittleMaidAvatarBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

public class EventHook {

    @SubscribeEvent
    public void onEntityItemPickupEvent(EntityItemPickupEvent event) {
        if (event.entityPlayer instanceof EntityLittleMaidAvatar) {
            EntityLittleMaidAvatar avatar = (EntityLittleMaidAvatar) event.entityPlayer;
            if (event.item != null) {
                if (LittleMaidMobX.isMaidIgnoreItem(event.item.getEntityItem())) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntitySpawned(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityArrow) {
            EntityArrow arrow = (EntityArrow) event.entity;
            if (arrow.shootingEntity instanceof IEntityLittleMaidAvatarBase) {
                IEntityLittleMaidAvatarBase avatar = (IEntityLittleMaidAvatarBase) arrow.shootingEntity;
				/* if (arrow.isDead) {
					for (Object obj : arrow.worldObj.loadedEntityList) {
						if (obj instanceof EntityCreature && !(obj instanceof LMM_EntityLittleMaid)) {
							EntityCreature ecr = (EntityCreature)obj;
							if (ecr.getEntityToAttack() == avatar) {
								ecr.setTarget(avatar.getMaid());
							}
						}
					}
				} */
                arrow.shootingEntity = avatar.getMaid();
                LittleMaidMobX.debug("Set " + event.entity.getClass() + " field shootingEntity from avator to maid");
            }
        }
    }
}
