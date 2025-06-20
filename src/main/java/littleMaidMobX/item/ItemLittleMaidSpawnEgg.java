package littleMaidMobX.item;

import java.util.List;

import littleMaidMobX.LittleMaidMobX;
import littleMaidMobX.entity.EntityLittleMaid;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemLittleMaidSpawnEgg extends Item {

    public ItemLittleMaidSpawnEgg() {
        this.setUnlocalizedName(LittleMaidMobX.MOD_ID + ":spawn_lmmx_egg");
        this.setTextureName(LittleMaidMobX.MOD_ID + ":spawn_lmmx_egg");
        this.setHasSubtypes(true);
        this.setCreativeTab(CreativeTabs.tabMisc);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int par4, int par5, int par6, int par7, float par8, float par9, float par10) {
        if (!world.isRemote) {
            Block block = world.getBlock(par4, par5, par6);
            par4 += Facing.offsetsXForSide[par7];
            par5 += Facing.offsetsYForSide[par7];
            par6 += Facing.offsetsZForSide[par7];
            double d0 = 0.0D;
            if (par7 == 1 && block.getRenderType() == 11) {
                d0 = 0.5D;
            }
            Entity entity = spawnMaid(world, stack.getItemDamage(), (double) par4 + 0.5D, (double) par5 + d0, (double) par6 + 0.5D);
            if (entity != null) {
                if (entity instanceof EntityLivingBase && stack.hasDisplayName()) {
                    ((EntityLiving) entity).setCustomNameTag(stack.getDisplayName());
                }
                if (!player.capabilities.isCreativeMode) {
                    --stack.stackSize;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            MovingObjectPosition position = this.getMovingObjectPositionFromPlayer(world, player, true);
            if (position != null) {
                if (position.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    int x = position.blockX;
                    int y = position.blockY;
                    int z = position.blockZ;
                    if (!world.canMineBlock(player, x, y, z)) {
                        return stack;
                    }
                    if (!player.canPlayerEdit(x, y, z, position.sideHit, stack)) {
                        return stack;
                    }
                    if (world.getBlock(x, y, z) instanceof BlockLiquid) {
                        Entity entity = spawnMaid(world, stack.getItemDamage(), x, y, z);
                        if (entity != null) {
                            if (entity instanceof EntityLivingBase && stack.hasDisplayName()) {
                                ((EntityLiving) entity).setCustomNameTag(stack.getDisplayName());
                            }
                            if (!player.capabilities.isCreativeMode) {
                                --stack.stackSize;
                            }
                        }
                    }
                }
            }
        }
        return stack;
    }

    public static Entity spawnMaid(World par0World, int par1, double par2, double par4, double par6) {
        EntityLiving entity = null;
        try {
            entity = new EntityLittleMaid(par0World);
            entity.setLocationAndAngles(par2, par4, par6, MathHelper.wrapAngleTo180_float(par0World.rand.nextFloat() * 360.0F), 0.0F);
            entity.rotationYawHead = entity.rotationYaw;
            entity.renderYawOffset = entity.rotationYaw;
            entity.onSpawnWithEgg(null);
            par0World.spawnEntityInWorld(entity);
            entity.playLivingSound();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entity;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(Item item, CreativeTabs tabs, List par3) {
        par3.add(new ItemStack(item, 1));
    }
}
