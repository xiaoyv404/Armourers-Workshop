package moe.plushie.armourers_workshop.core.skin.painting;

import moe.plushie.armourers_workshop.api.common.ITextureKey;
import moe.plushie.armourers_workshop.api.skin.ISkinPaintType;
import moe.plushie.armourers_workshop.core.skin.SkinDyeType;
import moe.plushie.armourers_workshop.utils.texture.TextureKey;
import net.minecraft.resources.ResourceLocation;

public class SkinPaintType implements ISkinPaintType {

    private static final TextureKey DEFAULT_TEXTURE = new TextureKey(0, 0, 1, 1, 256, 256);

    private final int id;
    private final int index;

    private SkinDyeType dyeType;
    private ResourceLocation registryName;
    private TextureKey texture = DEFAULT_TEXTURE;

    public SkinPaintType(int index, int id) {
        this.id = id;
        this.index = index;
    }

    @Override
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    public void setRegistryName(ResourceLocation registryName) {
        this.registryName = registryName;
    }

    public SkinPaintType setTexture(float u, float v) {
        this.texture = new TextureKey(u, v, 1, 1, 256, 256);
        return this;
    }

    @Override
    public ITextureKey getTexture() {
        return this.texture;
    }

    @Override
    public SkinDyeType getDyeType() {
        return dyeType;
    }

    public SkinPaintType setDyeType(SkinDyeType dyeType) {
        this.dyeType = dyeType;
        return this;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SkinPaintType other = (SkinPaintType) obj;
        return id == other.id;
    }

    @Override
    public String toString() {
        return registryName.toString();
    }
}
