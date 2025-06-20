package littleMaidMobX.tweaker;

import com.google.common.collect.ImmutableSet;
import cpw.mods.fml.common.ModContainer;
import littleMaidMobX.LittleMaidMobX;
import littleMaidMobX.client.audio.LittleMaidSoundManager;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class LittleMaidMobXResourcePack implements IResourcePack {

    private static final Logger LOGGER = LogManager.getLogger();
    public final ModContainer ownerContainer;

    public LittleMaidMobXResourcePack(ModContainer container) {
        this.ownerContainer = container;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        InputStream input = getResourceStream(location, true);

        if (input != null) {
            return input;
        } else {
            throw new FileNotFoundException(location.getResourcePath());
        }
    }

    private InputStream getResourceStream(ResourceLocation location, boolean existCheck) {
        if(location.getResourceDomain().equalsIgnoreCase(LittleMaidMobX.MOD_ID)) {
            String path = location.getResourcePath();
            InputStream resource = LittleMaidMobXResourcePack.class.getResourceAsStream("/assets/" + LittleMaidMobX.MOD_ID + "/" + path);
            if (resource == null) {
                resource = LittleMaidSoundManager.getResourceStream(location);
            }
            return resource;
        }
        return null;
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        InputStream stream = getResourceStream(location, false);

        return stream != null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set getResourceDomains() {
        return ImmutableSet.of(LittleMaidMobX.MOD_ID);
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer par1MetadataSerializer, String par2Str)
    { //throws IOException {
        return null;
    }

    // 未使用
    @Override
    public BufferedImage getPackImage() {
        try {
            InputStream pack = DefaultResourcePack.class.getResourceAsStream("/" + new ResourceLocation("pack.png").getResourcePath());
            if (pack != null) {
                return ImageIO.read(pack);
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot found pack.png for LittleMaid old MultiModel compatibility (Maybe you can ignore the message, it doesn't cause for error)", e);
        }
        return null;
    }

    @Override
    public String getPackName() {
        return "Default";
    }
}
