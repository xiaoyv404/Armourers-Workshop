package moe.plushie.armourers_workshop.core.gui.wardrobe;

import moe.plushie.armourers_workshop.core.gui.widget.AWCheckBox;
import moe.plushie.armourers_workshop.core.gui.widget.AWTabPanel;
import moe.plushie.armourers_workshop.core.network.NetworkHandler;
import moe.plushie.armourers_workshop.core.network.packet.UpdateWardrobePacket;
import moe.plushie.armourers_workshop.core.capability.Wardrobe;
import moe.plushie.armourers_workshop.core.container.WardrobeContainer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@SuppressWarnings("NullableProblems")
@OnlyIn(Dist.CLIENT)
public class WardrobeDisplaySetting extends AWTabPanel {

    private final Wardrobe wardrobe;

    public WardrobeDisplaySetting(WardrobeContainer container) {
        super("inventory.armourers_workshop.wardrobe.display_settings");
        this.wardrobe = container.getWardrobe();
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
        addOption(leftPos + 83, topPos + 27, UpdateWardrobePacket.Field.ARMOUR_HEAD, "renderHeadArmour");
        addOption(leftPos + 83, topPos + 47, UpdateWardrobePacket.Field.ARMOUR_CHEST, "renderChestArmour");
        addOption(leftPos + 83, topPos + 67, UpdateWardrobePacket.Field.ARMOUR_LEGS, "renderLegArmour");
        addOption(leftPos + 83, topPos + 87, UpdateWardrobePacket.Field.ARMOUR_FEET, "renderFootArmour");
    }

    private void addOption(int x, int y, UpdateWardrobePacket.Field option, String key) {
        addButton(new AWCheckBox(x, y, 9, 9, getDisplayText(key), option.get(wardrobe, false), button -> {
            if (button instanceof AWCheckBox) {
                boolean newValue = ((AWCheckBox) button).isSelected();
                NetworkHandler.getInstance().sendToServer(UpdateWardrobePacket.field(wardrobe, option, newValue));
            }
        }));
    }
}