package moe.plushie.armourers_workshop.builder.blockentity;

import moe.plushie.armourers_workshop.core.blockentity.UpdatableContainerBlockEntity;
import moe.plushie.armourers_workshop.utils.BlockUtils;
import moe.plushie.armourers_workshop.utils.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.util.Strings;

public class OutfitMakerBlockEntity extends UpdatableContainerBlockEntity {

    private String itemName = "";
    private String itemFlavour = "";

    private final NonNullList<ItemStack> items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);

    public OutfitMakerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public void readFromNBT(CompoundTag nbt) {
        ContainerHelper.loadAllItems(nbt, items);
        this.itemName = nbt.getString(Constants.Key.BLOCK_ENTITY_NAME);
        this.itemFlavour = nbt.getString(Constants.Key.BLOCK_ENTITY_FLAVOUR);
    }

    public void writeToNBT(CompoundTag nbt) {
        ContainerHelper.saveAllItems(nbt, items);
        if (Strings.isNotEmpty(itemName)) {
            nbt.putString(Constants.Key.BLOCK_ENTITY_NAME, itemName);
        }
        if (Strings.isNotEmpty(itemFlavour)) {
            nbt.putString(Constants.Key.BLOCK_ENTITY_FLAVOUR, itemFlavour);
        }
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String name) {
        this.itemName = name;
        BlockUtils.combine(this, this::sendBlockUpdates);
    }

    public String getItemFlavour() {
        return itemFlavour;
    }

    public void setItemFlavour(String flavour) {
        this.itemFlavour = flavour;
        BlockUtils.combine(this, this::sendBlockUpdates);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public int getContainerSize() {
        return 21;
    }
}


