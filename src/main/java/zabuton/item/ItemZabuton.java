package zabuton.item;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import zabuton.Zabuton;
import zabuton.entity.EntityZabuton;

public class ItemZabuton extends Item {

	public static final int[] colorValues = {
		0x1e1b1b, 0xb3312c, 0x3b511a, 0x51301a,
		0x253192, 0x7b2fbe, 0x287697, 0xa0a0af,
		0x434343, 0xd88198, 0x41cd34, 0xdecf2a,
		0x6689d3, 0xc354cd, 0xeb8844, 0xf0f0f0 };

	public ItemZabuton() {
		super();
		setUnlocalizedName(Zabuton.MOD_ID + ":zabuton");
		setTextureName(Zabuton.MOD_ID + ":zabuton");
		setCreativeTab(CreativeTabs.tabTransport);
		setMaxStackSize(8);
		setHasSubtypes(true);
		setMaxDamage(0);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack itemstack, World world,
			EntityPlayer entityplayer) {
		float f = 1.0F;
		float f1 = entityplayer.prevRotationPitch
				+ (entityplayer.rotationPitch - entityplayer.prevRotationPitch) * f;
		float f2 = entityplayer.prevRotationYaw
				+ (entityplayer.rotationYaw - entityplayer.prevRotationYaw) * f;
		double d = entityplayer.prevPosX
				+ (entityplayer.posX - entityplayer.prevPosX) * (double) f;
		double d1 = (entityplayer.prevPosY
				+ (entityplayer.posY - entityplayer.prevPosY) * (double) f + 1.6200000000000001D)
				- (double) entityplayer.yOffset;
		double d2 = entityplayer.prevPosZ
				+ (entityplayer.posZ - entityplayer.prevPosZ) * (double) f;
		Vec3 vec3d = Vec3.createVectorHelper(d, d1, d2);
		float f3 = MathHelper.cos(-f2 * 0.01745329F - 3.141593F);
		float f4 = MathHelper.sin(-f2 * 0.01745329F - 3.141593F);
		float f5 = -MathHelper.cos(-f1 * 0.01745329F);
		float f6 = MathHelper.sin(-f1 * 0.01745329F);
		float f7 = f4 * f5;
		float f8 = f6;
		float f9 = f3 * f5;
		double d3 = 5D;
		Vec3 vec3d1 = vec3d.addVector((double) f7 * d3, (double) f8 * d3,
				(double) f9 * d3);
		MovingObjectPosition movingobjectposition = world.rayTraceBlocks(vec3d, vec3d1, true);
		if (movingobjectposition == null) {
			return itemstack;
		}
		if (movingobjectposition.typeOfHit == MovingObjectType.BLOCK) {
			int i = movingobjectposition.blockX;
			int j = movingobjectposition.blockY;
			int k = movingobjectposition.blockZ;
			if (world.getBlock(i, j + 1, k).getMaterial() == Material.air) {
				if (!world.isRemote) {
					EntityZabuton ez = new EntityZabuton(world,
							(float) i + 0.5F, (float) j + 1.0F,
							(float) k + 0.5F,
							(byte) (itemstack.getItemDamage() & 0x0f));
					
					// 方向ぎめはここに入れる
					ez.rotationYaw = (MathHelper.floor_double((double) ((entityplayer.rotationYaw * 4F) / 360F) + 2.50D) & 3) * 90;
					world.spawnEntityInWorld(ez);
				}
				if (!entityplayer.capabilities.isCreativeMode) {
					itemstack.stackSize--;
				}
			}
		}
		return itemstack;
	}

	public int getColorFromDamage(int damage) {
        return colorValues[damage];
	}

	@Override
	public int getColorFromItemStack(ItemStack stack, int pass) {
		return getColorFromDamage(stack.getItemDamage());
	}

	@Override
	public String getUnlocalizedName(ItemStack par1ItemStack) {
		return super.getUnlocalizedName() + "." +
                ItemDye.field_150923_a[par1ItemStack.getItemDamage()];
	}

	@Override
	public boolean requiresMultipleRenderPasses() {
		return true;
	}

	@Override
	public void getSubItems(Item item, CreativeTabs creativeTabs, List list) {
		for (int metadata = 0; metadata < 16; metadata++) {
			list.add(new ItemStack(item, 1, metadata));
		}
	}
}
