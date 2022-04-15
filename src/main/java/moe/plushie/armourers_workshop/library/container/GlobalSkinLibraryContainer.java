package moe.plushie.armourers_workshop.library.container;

import moe.plushie.armourers_workshop.core.skin.SkinDescriptor;
import moe.plushie.armourers_workshop.init.common.ModBlocks;
import moe.plushie.armourers_workshop.init.common.ModContainerTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IWorldPosCallable;

@SuppressWarnings("NullableProblems")
public class GlobalSkinLibraryContainer extends Container {

    private boolean isVisible = false;

    public int inventoryWidth = 162;
    public int inventoryHeight = 76;

    private final IInventory inventory = new Inventory(2);
    private final PlayerInventory playerInventory;
    private final IWorldPosCallable access;

    public GlobalSkinLibraryContainer(int containerId, PlayerInventory playerInventory, IWorldPosCallable access) {
        super(ModContainerTypes.SKIN_LIBRARY_GLOBAL, containerId);
        this.access = access;
        this.playerInventory = playerInventory;
        this.reload(0, 0, 240, 240);
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public ItemStack getInputStack() {
        return inventory.getItem(0);
    }

    public void reload(int x, int y, int width, int height) {
        this.slots.clear();
        int inventoryX = x + 5;
        int inventoryY = y + height;
        this.addPlayerSlots(playerInventory, x + width - inventoryWidth - 4, y + height - inventoryHeight - 5);
        this.addInputSlot(inventory, 0, inventoryX + 1, inventoryY - 27);
        this.addOutputSlot(inventory, 1, inventoryX + 129, inventoryY - 27);
    }

    @Override
    public void removed(PlayerEntity player) {
        super.removed(player);
        this.access.execute((world, pos) -> this.clearContainer(player, world, inventory));
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return stillValid(this.access, player, ModBlocks.SKIN_LIBRARY_GLOBAL);
    }


    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack itemStack = slot.getItem();
        if (index >= 36) {
            if (!(moveItemStackTo(itemStack, 9, 36, false) || moveItemStackTo(itemStack, 0, 9, false))) {
                return ItemStack.EMPTY;
            }
            slot.set(ItemStack.EMPTY);
            return itemStack.copy();
        }
        if (!moveItemStackTo(itemStack, 36, slots.size() - 1, false)) {
            return ItemStack.EMPTY;
        }
        slot.setChanged();
        return ItemStack.EMPTY;
    }

    public void crafting() {
        this.access.execute((world, pos) -> this.clearContainer(playerInventory.player, world, inventory));
    }

    protected void addInputSlot(IInventory inventory, int slot, int x, int y) {
        addSlot(new Slot(inventory, slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return !SkinDescriptor.of(itemStack).isEmpty();
            }

            @Override
            public boolean isActive() {
                return isVisible;
            }
        });
    }

    protected void addOutputSlot(IInventory inventory, int slot, int x, int y) {
        addSlot(new Slot(inventory, slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return isVisible;
            }
        });
    }

    protected void addPlayerSlot(IInventory inventory, int slot, int x, int y) {
        addSlot(new Slot(inventory, slot, x, y) {
            @Override
            public boolean isActive() {
                return isVisible;
            }
        });
    }

    protected void addPlayerSlots(IInventory inventory, int slotsX, int slotsY) {
        for (int col = 0; col < 9; ++col) {
            this.addPlayerSlot(inventory, col, slotsX + col * 18, slotsY + 58);
        }
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addPlayerSlot(inventory, col + row * 9 + 9, slotsX + col * 18, slotsY + row * 18);
            }
        }
    }
}