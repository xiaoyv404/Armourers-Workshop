package moe.plushie.armourers_workshop;

import moe.plushie.armourers_workshop.api.skin.ISkinType;
import moe.plushie.armourers_workshop.core.crafting.recipe.SkinningRecipes;
import moe.plushie.armourers_workshop.core.skin.SkinTypes;
import moe.plushie.armourers_workshop.init.*;
import moe.plushie.armourers_workshop.init.client.ClientWardrobeHandler;
import moe.plushie.armourers_workshop.init.environment.EnvironmentExecutor;
import moe.plushie.armourers_workshop.init.environment.EnvironmentType;
import moe.plushie.armourers_workshop.init.platform.NetworkManager;
import moe.plushie.armourers_workshop.init.platform.RendererManager;
import moe.plushie.armourers_workshop.library.data.SkinLibraryManager;
import moe.plushie.armourers_workshop.utils.RenderSystem;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public class ArmourersWorkshop {

    public static void init() {
        ModItemGroups.init();
        ModItems.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModEntities.init();
        ModCapabilities.init();
        ModMenus.init();
        ModSounds.init();
        ModConfig.init();
        // setup common objects.
        EnvironmentExecutor.didInit(EnvironmentType.COMMON, () -> () -> {
            // setup network manager.
            NetworkManager.init("play", ModConstants.MOD_NET_ID);
            ModPackets.init();

            ModEntityProfiles.init();
            ModHolidays.init();
            ModPermissions.init();
            SkinningRecipes.init();

            EnvironmentExecutor.run(() -> SkinLibraryManager::startClient, () -> SkinLibraryManager::startServer);
        });
        // setup client in setup.
        EnvironmentExecutor.didInit(EnvironmentType.CLIENT, () -> () -> {
            // setup client objects.
//            SkinResourceManager.init();
            ClientWardrobeHandler.init();
            ModKeyBindings.init();
            ModDebugger.init();
        });
        // setup client renderer in finish.
        EnvironmentExecutor.didSetup(EnvironmentType.CLIENT, () -> () -> {
            // setup skin manager.
            RenderSystem.init();
            RendererManager.init();
        });
    }

    public static ResourceLocation getItemIcon(ISkinType skinType) {
        if (skinType == SkinTypes.UNKNOWN || skinType.getRegistryName() == null) {
            return null;
        }
        return ModConstants.key("textures/item/template/" + skinType.getRegistryName().getPath() + ".png");
    }

    public static ModelResourceLocation getCustomModel(ResourceLocation resourceLocation) {
        String name = resourceLocation.getPath().toLowerCase();
        name = name.replaceAll("\\.base", "");
        name = name.replaceAll("\\.", "_");
        return new ModelResourceLocation(ModConstants.key("skin/" + name), "inventory");
    }

}