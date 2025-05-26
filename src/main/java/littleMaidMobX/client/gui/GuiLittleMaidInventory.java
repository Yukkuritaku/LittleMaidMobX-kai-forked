package littleMaidMobX.client.gui;

import littleMaidMobX.LittleMaidMobX;
import littleMaidMobX.config.LittleMaidConfig;
import littleMaidMobX.entity.EntityLittleMaid;
import littleMaidMobX.inventory.ContainerInventory;
import mmmlibx.lib.Client;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.Collection;
import java.util.Random;

public class GuiLittleMaidInventory extends GuiContainer {
    // Field
    private Random rand;
    private IInventory upperChestInventory;
    private IInventory lowerChestInventory;
    private float xSize_lo;
    private float ySize_lo;
    private int ySizebk;
    private int updateCounter;
    public EntityLittleMaid littleMaid;

    public GuiScreenBook.NextPageButton[] txbutton = new GuiScreenBook.NextPageButton[4];
    public GuiButton selectbutton;
    public boolean isChangeTexture;

    private int topTicks = 0;

    private static class RenderInfoPart {
        private static boolean shiftLock;

        /**
         * 0: Armor, 1: Mode, 2: Free
         */
        private static final boolean[] renderInfo = new boolean[]{
                true, false, true, false
        };

        private static int renderingPart = 0;

        public static int getEnabledCounts() {
            int count = 0;
            for (boolean s : renderInfo) {
                if (s) ++count;
            }
            return count;
        }

        public static void setEnabled(int index, boolean flag) {
            renderInfo[index] = flag;
            // Armorだけはtrueを維持
            renderInfo[1] = true;
            if (renderingPart == index && !flag) {
                shiftPart();
            }
        }

        public static boolean isEnabled(int index) {
            try {
                return renderInfo[index];
            } catch (IndexOutOfBoundsException exception) {
                return false;
            }
        }

        public static void shiftPart() {
            while (!isEnabled(++renderingPart)) {
                if (renderingPart >= renderInfo.length) {
                    renderingPart = -1;
                }
            }
        }

        public static int getRenderingPart() {
            return renderingPart;
        }

        public static void lock() {
            shiftLock = true;
        }

        public static void unlock() {
            shiftLock = false;
        }

        public static boolean islocked() {
            return shiftLock;
        }
    }

    protected static final ResourceLocation fguiTex =
            new ResourceLocation(LittleMaidMobX.MOD_ID, "textures/gui/container/littlemaidinventory.png");

    // Method
    public GuiLittleMaidInventory(EntityPlayer pPlayer, EntityLittleMaid elmaid) {
        super(new ContainerInventory(pPlayer.inventory, elmaid));
        rand = new Random();
        upperChestInventory = pPlayer.inventory;
        lowerChestInventory = elmaid.maidInventory;
        allowUserInput = false;
        updateCounter = 0;
        ySizebk = ySize;
        ySize = 207;
        isChangeTexture = true;

        littleMaid = elmaid;
        topTicks = littleMaid.ticksExisted;
        // entitylittlemaid.setOpenInventory(true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();
        if (!littleMaid.getActivePotionEffects().isEmpty()) {
            guiLeft = 160 + (width - xSize - 200) / 2;
        }
        buttonList.add(txbutton[0] = new GuiScreenBook.NextPageButton(100, guiLeft + 25, guiTop + 7, false));
        buttonList.add(txbutton[1] = new GuiScreenBook.NextPageButton(101, guiLeft + 55, guiTop + 7, true));
        buttonList.add(txbutton[2] = new GuiScreenBook.NextPageButton(110, guiLeft + 25, guiTop + 47, false));
        buttonList.add(txbutton[3] = new GuiScreenBook.NextPageButton(111, guiLeft + 55, guiTop + 47, true));
        buttonList.add(selectbutton = new GuiButton(200, guiLeft + 25, guiTop + 25, 53, 17, "select"));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        mc.fontRenderer.drawString(StatCollector.translateToLocal(
                lowerChestInventory.getInventoryName()), 114, 64, 0x404040);
        //mc.fontRenderer.drawString(StatCollector.translateToLocal(
        //		upperChestInventory.getInventoryName()), 8, 114, 0x404040);
//		fontRenderer.drawString(StatCollector.translateToLocal(
//				"littleMaidMob.text.Health"), 86, 8, 0x404040);
//		fontRenderer.drawString(StatCollector.translateToLocal(
//				"littleMaidMob.text.AP"), 86, 32, 0x404040);
        //mc.fontRenderer.drawString(StatCollector.translateToLocal(
        //		"littleMaidMob.text.STATUS"), 86, 8, 0x404040);
        if (!LittleMaidConfig.oldStatsRendering) {
            if (RenderInfoPart.getRenderingPart() == 1) {
                mc.fontRenderer.drawString(StatCollector.translateToLocal(
                        "littleMaidMob.mode.".concat(littleMaid.getMaidModeString())), 7, 64, 0x404040);
            }
        }else {
            mc.fontRenderer.drawString(StatCollector.translateToLocal(
                    "littleMaidMob.mode.".concat(littleMaid.getMaidModeString())), 7, 64, 0x404040);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // キャラ
        int lj = 0;
        int lk = 0;
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glPushMatrix();
        GL11.glTranslatef(lj + 51, lk + 57, 50F);
        float f1 = 30F;
        GL11.glScalef(-f1, f1, f1);
        GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);
        float f2 = littleMaid.renderYawOffset;
        float f3 = littleMaid.rotationYaw;
        float f4 = littleMaid.rotationYawHead;
        float f5 = littleMaid.rotationPitch;
//		float f8 = (float) (lj + 51) - xSize_lo;
//		float f9 = (float) (lk + 75) - 50 - ySize_lo;
        float f8 = (float) (guiLeft + 51 - par1);
        float f9 = (float) (guiTop + 22 - par2);
        GL11.glRotatef(135F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GL11.glRotatef(-135F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-(float) Math.atan(f9 / 40F) * 20F, 1.0F, 0.0F, 0.0F);
        littleMaid.renderYawOffset = (float) Math.atan(f8 / 40F) * 20F;
        littleMaid.rotationYawHead = littleMaid.rotationYaw = (float) Math.atan(f8 / 40F) * 40F;
        littleMaid.rotationPitch = -(float) Math.atan(f9 / 40F) * 20F;
        GL11.glTranslatef(0.0F, littleMaid.yOffset, 0.0F);
        RenderManager.instance.playerViewY = 180F;
        RenderManager.instance.renderEntityWithPosYaw(littleMaid, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        littleMaid.renderYawOffset = f2;
        littleMaid.rotationYaw = f3;
        littleMaid.rotationYawHead = f4;
        littleMaid.rotationPitch = f5;
        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        // 背景
        ResourceLocation lrl = littleMaid.textureData.getGUITexture();
        if (lrl == null) {
            lrl = fguiTex;
        }
        Client.setTexture(lrl);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int lj = guiLeft;
        int lk = guiTop;
        drawTexturedModalRect(lj, lk, 0, 0, xSize, ySize);

        // PotionEffect
        displayDebuffEffects();

        // LP/AP
        if (LittleMaidConfig.oldStatsRendering)
            drawHealthArmor(0, 0);
        else {
            drawArmor();
            drawHealthBar();
        }
    }

    protected void drawHealthBar() {
        float health = littleMaid.getHealth();
        float maxHealth = (float) littleMaid.getEntityAttribute(SharedMonsterAttributes.maxHealth).getAttributeValue();
        // LPゲージ
        drawGradientRect(guiLeft + 97, guiTop + 7, guiLeft + 168, guiTop + 18, 0x80202020, 0x80202020);
        if (health > 0) {
            drawGradientRect(guiLeft + 98, guiTop + 8, guiLeft + 98 + MathHelper.ceiling_float_int(69.0F * health / maxHealth), guiTop + 17, 0xf0008000, 0xf000f000);
        } else {
            drawGradientRect(guiLeft + 98, guiTop + 8, guiLeft + 98, guiTop + 17, 0xf0008000, 0xf000f000);
        }
        mc.fontRenderer.drawString(StatCollector.translateToLocal(
                health + "/" + maxHealth), guiLeft + 99, guiTop + 9, 0x404040);
    }


    protected void drawArmor() {
        Client.setTexture(icons);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        int ldrawx;
        int ldrawy;

        // AP
        int larmor = littleMaid.getTotalArmorValue();
        if (larmor == 0) {
            RenderInfoPart.setEnabled(0, false);
        } else if (larmor > 0) {
            RenderInfoPart.setEnabled(0, true);
            if (RenderInfoPart.getRenderingPart() == 0) {
                ldrawy = guiTop + 64;
                for (int li = 0; li < 10; ++li) {
                    ldrawx = guiLeft + li * 8 + 7;

                    if (li * 2 + 1 < larmor) {
                        this.drawTexturedModalRect(ldrawx, ldrawy, 34, 9, 9, 9);
                    }
                    if (li * 2 + 1 == larmor) {
                        this.drawTexturedModalRect(ldrawx, ldrawy, 25, 9, 9, 9);
                    }
                    if (li * 2 + 1 > larmor) {
                        this.drawTexturedModalRect(ldrawx, ldrawy, 16, 9, 9, 9);
                    }
                }
            }
        }

        ldrawy = guiTop + 64;
        if (littleMaid.isInsideOfMaterial(Material.water)) {
            int var23 = littleMaid.getAir();
            int var35 = MathHelper.ceiling_double_int((double) (var23 - 2) * 10.0D / 300.0D);
            int var25 = MathHelper.ceiling_double_int((double) var23 * 10.0D / 300.0D) - var35;

            for (int var26 = 0; var26 < var35 + var25; ++var26) {
                ldrawx = guiLeft + var26 * 8 + 7;
                if (var26 < var35) {
                    this.drawTexturedModalRect(ldrawx, ldrawy, 16, 9, 9, 9);
                } else {
                    this.drawTexturedModalRect(ldrawx, ldrawy, 25, 9, 9, 9);
                }
            }
        }
    }

    protected void drawHealthArmor(int width, int height) {
        boolean var3 = littleMaid.hurtResistantTime / 3 % 2 == 1;

        if (littleMaid.hurtResistantTime < 10) {
            var3 = false;
        }

        Client.setTexture(icons);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        int lhealth = MathHelper.ceiling_float_int(littleMaid.getHealth());
        int llasthealth = MathHelper.ceiling_float_int(littleMaid.prevHealth);
        this.rand.setSeed(updateCounter * 312871L);
        boolean var6 = false;
//		FoodStats var7 = entitylittlemaid.getFoodStats();
//		int var8 = var7.getFoodLevel();
//		int var9 = var7.getPrevFoodLevel();
        IAttributeInstance var10 = littleMaid.getEntityAttribute(SharedMonsterAttributes.maxHealth);
        int armorWidth = width / 2 - 91;
        int var13 = height - 39;
        float var14 = (float) var10.getAttributeValue();
        float var15 = littleMaid.getAbsorptionAmount();
        int var16 = MathHelper.ceiling_float_int((var14 + var15) / 2.0F / 10.0F);
        int var17 = Math.max(10 - (var16 - 2), 3);
        float var19 = var15;
        int var21 = -1;

        if (littleMaid.isPotionActive(Potion.regeneration)) {
            var21 = updateCounter % MathHelper.ceiling_float_int(var14 + 5.0F);
        }

        int x = 95; // (old value is 86)
        int ldrawx;
        int ldrawy;

        // AP
        int larmor = littleMaid.getTotalArmorValue();
        ldrawy = guiTop + 36;
        for (int li = 0; li < 10; ++li) {
            if (larmor > 0) {
                ldrawx = guiLeft + li * 8 + x;

                if (li * 2 + 1 < larmor) {
                    this.drawTexturedModalRect(ldrawx, ldrawy, 34, 9, 9, 9);
                }
                if (li * 2 + 1 == larmor) {
                    this.drawTexturedModalRect(ldrawx, ldrawy, 25, 9, 9, 9);
                }
                if (li * 2 + 1 > larmor) {
                    this.drawTexturedModalRect(ldrawx, ldrawy, 16, 9, 9, 9);
                }
            }
        }

        // LP
        for (int li = MathHelper.ceiling_float_int((var14 + var15) / 2.0F) - 1; li >= 0; --li) {
            int var23 = 16;
            if (littleMaid.isPotionActive(Potion.poison)) {
                var23 += 36;
            } else if (littleMaid.isPotionActive(Potion.wither)) {
                var23 += 72;
            }

            int var25 = MathHelper.ceiling_float_int((float) (li + 1) / 10.0F);
            ldrawx = guiLeft + li % 10 * 8 + x;
            ldrawy = guiTop + 7 + var25 * var17;

            if (lhealth <= 4) {
                ldrawy += this.rand.nextInt(2);
            }
            if (li == var21) {
                ldrawy -= 2;
            }

            this.drawTexturedModalRect(ldrawx, ldrawy, var3 ? 25 : 16, 0, 9, 9);

            if (var3) {
                if (li * 2 + 1 < llasthealth) {
                    this.drawTexturedModalRect(ldrawx, ldrawy, var23 + 54, 0, 9, 9);
                }
                if (li * 2 + 1 == llasthealth) {
                    this.drawTexturedModalRect(ldrawx, ldrawy, var23 + 63, 0, 9, 9);
                }
            }

            if (var19 > 0.0F) {
                if (var19 == var15 && var15 % 2.0F == 1.0F) {
                    this.drawTexturedModalRect(ldrawx, ldrawy, var23 + 153, 0, 9, 9);
                } else {
                    this.drawTexturedModalRect(ldrawx, ldrawy, var23 + 144, 0, 9, 9);
                }

                var19 -= 2.0F;
            } else {
                if (li * 2 + 1 < lhealth) {
                    this.drawTexturedModalRect(ldrawx, ldrawy, var23 + 36, 0, 9, 9);
                }
                if (li * 2 + 1 == lhealth) {
                    this.drawTexturedModalRect(ldrawx, ldrawy, var23 + 45, 0, 9, 9);
                }
            }
        }

        // Air
        ldrawy = guiTop + 46;
        if (littleMaid.isInsideOfMaterial(Material.water)) {
            int var23 = littleMaid.getAir();
            int var35 = MathHelper.ceiling_double_int((double) (var23 - 2) * 10.0D / 300.0D);
            int var25 = MathHelper.ceiling_double_int((double) var23 * 10.0D / 300.0D) - var35;

            for (int var26 = 0; var26 < var35 + var25; ++var26) {
                ldrawx = guiLeft + var26 * 8 + 86;
                if (var26 < var35) {
                    this.drawTexturedModalRect(ldrawx, ldrawy, 16, 18, 9, 9);
                } else {
                    this.drawTexturedModalRect(ldrawx, ldrawy, 25, 18, 9, 9);
                }
            }
        }

    }

    @Override
    public void drawScreen(int i, int j, float f) {
        if ((littleMaid.ticksExisted - topTicks) % 30 == 0) {
            if (!RenderInfoPart.islocked()) RenderInfoPart.shiftPart();
            RenderInfoPart.lock();
        } else {
            RenderInfoPart.unlock();
        }
        super.drawScreen(i, j, f);
        xSize_lo = i;
        ySize_lo = j;

        int ii = i - guiLeft;
        int jj = j - guiTop;
        if (ii > 25 && ii < 78 && jj > 7 && jj < 60) {
            // ボタンの表示
            txbutton[0].visible = true;
            txbutton[1].visible = true;
            txbutton[2].visible = true;
            txbutton[3].visible = true;
            selectbutton.visible = true;

            // テクスチャ名称の表示
            GL11.glPushMatrix();
            GL11.glTranslatef(i - ii, j - jj, 0.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);

            if (littleMaid.textureData.textureBox[0] != null) {
                String ls1 = littleMaid.textureData.getTextureName(0);
                String ls2 = littleMaid.textureData.getTextureName(1);
                int ltw1 = this.mc.fontRenderer.getStringWidth(ls1);
                int ltw2 = this.mc.fontRenderer.getStringWidth(ls2);
                int ltwmax = Math.max(ltw1, ltw2);
                int lbx = 52 - ltwmax / 2;
                int lby = 68;
                int lcolor;
                lcolor = jj < 20 ? 0xc0882222 : 0xc0000000;
                drawGradientRect(lbx - 3, lby - 4, lbx + ltwmax + 3, lby + 8, lcolor, lcolor);
                drawString(this.mc.fontRenderer, ls1, 52 - ltw1 / 2, lby - 2, -1);
                lcolor = jj > 46 ? 0xc0882222 : 0xc0000000;
                drawGradientRect(lbx - 3, lby + 8, lbx + ltwmax + 3, lby + 16 + 4, lcolor, lcolor);
                drawString(this.mc.fontRenderer, ls2, 52 - ltw2 / 2, lby + 10, -1);
            }

            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            txbutton[0].visible = false;
            txbutton[1].visible = false;
            txbutton[2].visible = false;
            txbutton[3].visible = false;
            selectbutton.visible = false;
        }

    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        updateCounter++;
    }

    @Override
    protected void mouseClicked(int i, int j, int k) {
        super.mouseClicked(i, j, k);
/*
		// 26,8-77,59
		int ii = i - guiLeft;
		int jj = j - guiTop;

		// TODO:メイドアセンブル画面を作る
		if (ii > 25 && ii < 78 && jj > 7 && jj < 60) {
			// 伽羅表示領域
			if (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54)) {
				// Shift+で逆周り
				LMM_Client.setPrevTexturePackege(entitylittlemaid, k);
			} else {
				LMM_Client.setNextTexturePackege(entitylittlemaid, k);
			}
			LMM_Client.setTextureValue(entitylittlemaid);
		}
*/
    }

    @Override
    protected void actionPerformed(GuiButton par1GuiButton) {
        switch (par1GuiButton.id) {
            case 100:
                littleMaid.setNextTexturePackege(0);
                littleMaid.setTextureNames();
                break;
            case 101:
                littleMaid.setPrevTexturePackege(0);
                littleMaid.setTextureNames();
                break;
            case 110:
                littleMaid.setNextTexturePackege(1);
                littleMaid.setTextureNames();
                break;
            case 111:
                littleMaid.setPrevTexturePackege(1);
                littleMaid.setTextureNames();
                break;
            case 200:
                int ldye = 0;
                if (mc.thePlayer.capabilities.isCreativeMode) {
                    ldye = 0xffff;
                } else {
                    for (ItemStack lis : mc.thePlayer.inventory.mainInventory) {
                        if (lis != null && lis.getItem() == Items.dye) {
                            ldye |= (1 << (15 - lis.getItemDamage()));
                        }
                    }
                }
                isChangeTexture = false;
                mc.displayGuiScreen(new GuiTextureSelect(this, littleMaid, ldye, true));
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        // entitylittlemaid.onGuiClosed();
        if (isChangeTexture) {
            littleMaid.sendTextureToServer();
        }
    }

    private void displayDebuffEffects() {
        // ポーションエフェクトの表示
        int lx = guiLeft - 124;
        int ly = guiTop;
        Collection collection = littleMaid.getActivePotionEffects();
        if (collection.isEmpty()) {
            return;
        }
        int lh = 33;
        if (collection.size() > 5) {
            lh = 132 / (collection.size() - 1);
        }
        for (Object o : littleMaid.getActivePotionEffects()) {
            PotionEffect potioneffect = (PotionEffect) o;
            Potion potion = Potion.potionTypes[potioneffect.getPotionID()];
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            Client.setTexture(field_147001_a);
            drawTexturedModalRect(lx, ly, 0, ySizebk, 140, 32);
            if (potion.hasStatusIcon()) {
                int i1 = potion.getStatusIconIndex();
                drawTexturedModalRect(lx + 6, ly + 7, (i1 % 8) * 18,
                        ySizebk + 32 + (i1 / 8) * 18, 18, 18);
            }
            String ls = StatCollector.translateToLocal(potion.getName());
            if (potioneffect.getAmplifier() > 0) {
                ls = ls + " " +
                        StatCollector.translateToLocal("potion.potency." +
                                potioneffect.getAmplifier());
            }
            mc.fontRenderer.drawStringWithShadow(ls, lx + 10 + 18, ly + 6, 0xffffff);
            String s1 = Potion.getDurationString(potioneffect);
            mc.fontRenderer.drawStringWithShadow(s1, lx + 10 + 18, ly + 6 + 10, 0x7f7f7f);
            ly += lh;
        }
    }

}
