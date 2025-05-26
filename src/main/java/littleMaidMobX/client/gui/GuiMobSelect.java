package littleMaidMobX.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

public abstract class GuiMobSelect extends GuiScreen {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final Map<Class<? extends Entity>, String> ENTITY_CLASS_MAP = new HashMap<>();
    public static final List<String> RENDER_EXCLUDES = new ArrayList<>();

    protected String screenTitle;
    protected GuiSlot selectPanel;
    public final Map<String, Entity> entityMap;

    public GuiMobSelect(World world){
        this(world, new TreeMap<>(), true);
    }

    public GuiMobSelect(World world, Map<String, Entity> entityMap){
        this(world, entityMap, false);
    }

    public GuiMobSelect(World world, Map<String, Entity> entityMap, boolean force){
        this.entityMap = entityMap;
        this.initEntities(world, force);
    }

    public void initEntities(World world, boolean force){
        if (ENTITY_CLASS_MAP.isEmpty()){
            // noinspection unchecked //
            ENTITY_CLASS_MAP.putAll(EntityList.classToStringMapping);
        }
        if (!force && !this.entityMap.isEmpty())return;
        for (Map.Entry<Class<? extends Entity>, String> entry : ENTITY_CLASS_MAP.entrySet()){
            if (Modifier.isAbstract(entry.getKey().getModifiers()))continue;
            int index = 0;
            Entity entity;
            try {
                do {
                    entity = entry.getKey().getConstructor(World.class).newInstance(world);
                }while (checkEntity(entry.getValue(), entity, index++));
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException e) {
                LOGGER.warn("Caught Exception on Initialize entity, id: {}", entry.getValue());
            }
        }
    }

    /**
     * 渡されたEntityのチェック及び加工。
     * trueを返すと同じクラスのエンティティを再度渡してくる、そのときpIndexはカウントアップされる
     */
    protected boolean checkEntity(String entityName, Entity entity, int index) {
        this.entityMap.put(entityName, entity);
        return false;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.selectPanel = new Mobs();
        this.selectPanel.registerScrollButtons(3, 4);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        float healthScale = BossStatus.healthScale;
        int statusBarTime = BossStatus.statusBarTime;
        String bossName = BossStatus.bossName;
        boolean hasColorModifier = BossStatus.hasColorModifier;

        drawDefaultBackground();
        selectPanel.drawScreen(mouseX, mouseY, partialTick);
        drawCenteredString(this.mc.fontRenderer, StatCollector.translateToLocal(screenTitle), width / 2, 20, 0xffffff);
        super.drawScreen(mouseX, mouseY, partialTick);

        // GUIで表示した分のボスのステータスを表示しない
        BossStatus.healthScale = healthScale;
        BossStatus.statusBarTime = statusBarTime;
        BossStatus.bossName = bossName;
        BossStatus.hasColorModifier = hasColorModifier;
    }

    public abstract void clickSlot(int index, boolean doubleClick, String entityName, EntityLivingBase entity);

    public abstract void drawSlot(int slotIndex, int x, int y, int slotHeight, Tessellator tessellator, String entityName, Entity entity);

    class Mobs extends GuiSlot {

        protected int selected;

        public Mobs() {
            super(Minecraft.getMinecraft(), GuiMobSelect.this.width, GuiMobSelect.this.height, 32, GuiMobSelect.this.height - 52, 36);
            this.selected = -1;
        }

        @Override
        protected int getSize() {
            return GuiMobSelect.this.entityMap.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {
            String entityName = GuiMobSelect.this.entityMap.keySet().toArray()[index].toString();
            EntityLivingBase entity = (EntityLivingBase) GuiMobSelect.this.entityMap.get(entityName);
            GuiMobSelect.this.clickSlot(index, doubleClick, entityName, entity);
            this.selected = index;
        }

        @Override
        protected boolean isSelected(int index) {
            return index == this.selected;
        }

        @Override
        protected void drawBackground() {
            GuiMobSelect.this.drawDefaultBackground();
        }

        @Override
        protected void drawSlot(int slotIndex, int x, int y, int slotHeight, Tessellator tessellator, int mouseX, int mouseY) {
            // 基本スロットの描画、細かい所はオーナー側で
            // Entityの確保
            String entityName = GuiMobSelect.this.entityMap.keySet().toArray()[slotIndex].toString();
            boolean renderExcluded = GuiMobSelect.RENDER_EXCLUDES.contains(entityName);
            EntityLivingBase entity = renderExcluded ? null : (EntityLivingBase) GuiMobSelect.this.entityMap.get(entityName);
            // 独自描画
            GuiMobSelect.this.drawSlot(slotIndex, x, y, slotHeight, tessellator, entityName, entity);
            // 除外判定
            if (renderExcluded) {
                GuiMobSelect.this.drawString(GuiMobSelect.this.mc.fontRenderer, "NoImage", x + 15, y + 12, 0xffffff);
                return;
            }
            entity.setWorld(mc.theWorld);
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glPushMatrix();
            float scale = 15f;
            if (entity.height > 2f) {
                scale = scale * 3f / entity.height;
            }
            float renderX =
                    ((slotIndex & 1) == 0) ?
                            (float) x + 30f :
                    (float) (GuiMobSelect.this.width - x) - 30f;
            GL11.glTranslatef(renderX, y + 30f, 50f + scale);
            GL11.glScalef(-scale, scale, scale);
            GL11.glRotatef(180f, 0.0f, 0.0f, 1.0f);
            float rotationYaw = renderX - this.mouseX;
            float rotationPitch = (float) ((y + 30) - 10) - this.mouseY;
            GL11.glRotatef(135f, 0.0f, 1.0f, 0.0f);
            RenderHelper.enableStandardItemLighting();
            GL11.glRotatef(-135f, 0.0f, 1.0f, 0.0f);
            GL11.glRotatef(-(float) Math.atan(rotationPitch / 40f) * 20f, 1.0f, 0.0f, 0.0f);
            entity.renderYawOffset = (float) Math.atan(rotationYaw / 40f) * 20f;
            entity.rotationYaw = (float) Math.atan(rotationYaw / 40f) * 40f;
            entity.rotationPitch = -(float) Math.atan(rotationPitch / 40f) * 20f;
            entity.prevRotationYawHead = entity.rotationYawHead;
            entity.rotationYawHead = entity.rotationYaw;
            GL11.glTranslatef(0.0f, entity.yOffset, 0.0f);
            RenderManager.instance.playerViewY = 180f;
            try {
                RenderManager.instance.renderEntityWithPosYaw(entity, 0.0, 0.0, 0.0, 0.0f, 1.0f);
            } catch (Exception e) {
                LOGGER.warn("Caught Exception in Mob slot rendering, added to exclude render list", e);
                GuiMobSelect.RENDER_EXCLUDES.add(entityName);
            }
            GL11.glPopMatrix();
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
    }
}
