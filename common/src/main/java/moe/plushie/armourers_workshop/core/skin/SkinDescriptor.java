package moe.plushie.armourers_workshop.core.skin;

import moe.plushie.armourers_workshop.api.skin.ISkinDescriptor;
import moe.plushie.armourers_workshop.api.skin.ISkinToolType;
import moe.plushie.armourers_workshop.api.skin.ISkinType;
import moe.plushie.armourers_workshop.core.data.ItemStackStorage;
import moe.plushie.armourers_workshop.core.data.color.ColorScheme;
import moe.plushie.armourers_workshop.init.ModItems;
import moe.plushie.armourers_workshop.utils.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class SkinDescriptor implements ISkinDescriptor {

    public static final SkinDescriptor EMPTY = new SkinDescriptor("");

    private final String identifier;
    private final ISkinType type;
    private final SkinOptions options;
    private final ColorScheme colorScheme;

    // not a required property, but it can help we reduce memory usage and improve performance.
    private ItemStack skinItemStack;

    public SkinDescriptor(String identifier) {
        this(identifier, SkinTypes.UNKNOWN, SkinOptions.DEFAULT, ColorScheme.EMPTY);
    }

    public SkinDescriptor(String identifier, ISkinType type) {
        this(identifier, type, SkinOptions.DEFAULT, ColorScheme.EMPTY);
    }

    public SkinDescriptor(String identifier, ISkinType type, ColorScheme colorScheme) {
        this(identifier, type, SkinOptions.DEFAULT, colorScheme);
    }

    public SkinDescriptor(String identifier, ISkinType type, SkinOptions options, ColorScheme colorScheme) {
        this.identifier = identifier;
        this.type = type;
        this.options = options;
        this.colorScheme = colorScheme;
    }

    public SkinDescriptor(SkinDescriptor descriptor, ColorScheme colorScheme) {
        this(descriptor.getIdentifier(), descriptor.getType(), descriptor.getOptions(), colorScheme);
    }

    public SkinDescriptor(CompoundTag tag) {
        this.identifier = tag.getString(Constants.Key.SKIN_IDENTIFIER);
        this.type = SkinTypes.byName(tag.getString(Constants.Key.SKIN_TYPE));
        this.options = tag.getOptionalSkinOptions(Constants.Key.SKIN_OPTIONS, SkinOptions.DEFAULT);
        this.colorScheme = tag.getOptionalColorScheme(Constants.Key.SKIN_DYE, ColorScheme.EMPTY);
    }

    public static SkinDescriptor of(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return EMPTY;
        }
        ItemStackStorage storage = ItemStackStorage.of(itemStack);
        SkinDescriptor descriptor = storage.skinDescriptor;
        if (descriptor != null) {
            return descriptor;
        }
        CompoundTag tag = itemStack.getTag();
        if (tag == null || !tag.contains(Constants.Key.SKIN)) {
            storage.skinDescriptor = EMPTY;
            return EMPTY;
        }
        descriptor = tag.getOptionalSkinDescriptor(Constants.Key.SKIN);
        storage.skinDescriptor = descriptor;
        return descriptor;
    }

    public static void setDescriptor(ItemStack itemStack, SkinDescriptor descriptor) {
        if (itemStack.isEmpty()) {
            return;
        }
        if (descriptor.isEmpty()) {
            itemStack.removeTagKey(Constants.Key.SKIN);
            return;
        }
        itemStack.addTagElement(Constants.Key.SKIN, descriptor.serializeNBT());
    }

    public boolean accept(ItemStack itemStack) {
        if (itemStack.isEmpty() || isEmpty()) {
            return false;
        }
        ISkinType skinType = getType();
        if (skinType == SkinTypes.ITEM) {
            return true;
        }
        if (skinType instanceof ISkinToolType) {
            return ((ISkinToolType) skinType).contains(itemStack);
        }
        return false;
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (isEmpty()) {
            return nbt;
        }
        nbt.putString(Constants.Key.SKIN_TYPE, type.getRegistryName().toString());
        nbt.putString(Constants.Key.SKIN_IDENTIFIER, identifier);
        nbt.putOptionalSkinOptions(Constants.Key.SKIN_OPTIONS, options, SkinOptions.DEFAULT);
        nbt.putOptionalColorScheme(Constants.Key.SKIN_DYE, colorScheme, ColorScheme.EMPTY);
        return nbt;
    }

    public ItemStack sharedItemStack() {
        if (isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (skinItemStack != null) {
            return skinItemStack;
        }
        ItemStack itemStack = new ItemStack(ModItems.SKIN.get());
        setDescriptor(itemStack, this);
        skinItemStack = itemStack;
        return itemStack;
    }

    public ItemStack asItemStack() {
        return sharedItemStack().copy();
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public ColorScheme getColorScheme() {
        return colorScheme;
    }

    public ISkinType getType() {
        return type;
    }

    public SkinOptions getOptions() {
        return options;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return String.format("%s@%s[%s]", identifier, type.getRegistryName(), type.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkinDescriptor that = (SkinDescriptor) o;
        return identifier.equals(that.identifier) && colorScheme.equals(that.colorScheme);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
}
