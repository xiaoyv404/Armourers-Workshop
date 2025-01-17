package moe.plushie.armourers_workshop.library.menu;

import moe.plushie.armourers_workshop.api.common.IContainerLevelAccess;
import moe.plushie.armourers_workshop.core.menu.AbstractBlockEntityMenu;
import moe.plushie.armourers_workshop.core.skin.SkinDescriptor;
import moe.plushie.armourers_workshop.library.blockentity.GlobalSkinLibraryBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class GlobalSkinLibraryMenu extends AbstractBlockEntityMenu<GlobalSkinLibraryBlockEntity> {

    private final Container inventory = new SimpleContainer(2);
    private final Inventory playerInventory;

    public int inventoryWidth = 162;
    public int inventoryHeight = 76;

    private boolean isVisible = false;

    public GlobalSkinLibraryMenu(MenuType<?> menuType, Block block, int containerId, Inventory playerInventory, IContainerLevelAccess access) {
        super(menuType, block, containerId, access);
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
        this.addPlayerSlots(playerInventory, x + width - inventoryWidth - 4, y + height - inventoryHeight - 5, visibleSlotBuilder(() -> isVisible));
        this.addInputSlot(inventory, 0, inventoryX + 1, inventoryY - 27);
        this.addOutputSlot(inventory, 1, inventoryX + 129, inventoryY - 27);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, inventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveStack(player, index, slots.size() - 1);
    }

    public void crafting() {
        this.clearContainer(playerInventory.player, inventory);
    }

    protected void addInputSlot(Container inventory, int slot, int x, int y) {
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

    protected void addOutputSlot(Container inventory, int slot, int x, int y) {
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
}
