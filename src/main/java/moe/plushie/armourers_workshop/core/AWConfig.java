package moe.plushie.armourers_workshop.core;

import moe.plushie.armourers_workshop.core.api.common.skin.ISkinPart;

public class AWConfig {

    // Performance
    public static int renderDistanceSkin;
    public static int renderDistanceBlockSkin;
    public static int renderDistanceMannequinEquipment;
    public static int modelBakingThreadCount;
    public static double lodDistance = 32F;
    public static boolean multipassSkinRendering = true;
    public static int maxLodLevels = 4;
    public static boolean useClassicBlockModels;

    // Misc
    public static int skinLoadAnimationTime;

    // Cache
    public static int skinCacheExpireTime;
    public static int skinCacheMaxSize;
    public static int modelPartCacheExpireTime;
    public static int modelPartCacheMaxSize;
    public static int textureCacheExpireTime;
    public static int textureCacheMaxSize;
    public static int maxSkinRequests;
    public static int fastCacheSize;
    public static int maxSkinSlots = 10;

    // Skin preview
    public static boolean skinPreEnabled = true;
    public static boolean skinPreDrawBackground = true;
    public static int skinPreSize = 96;
    public static double skinPreLocHorizontal = 0.0;
    public static double skinPreLocVertical = 0.5;
    public static boolean skinPreLocFollowMouse = true;

    // Tool-tip
    public static boolean tooltipHasSkin;
    public static boolean tooltipSkinName;
    public static boolean tooltipSkinAuthor;
    public static boolean tooltipSkinType;
    public static boolean tooltipFlavour;
    public static boolean tooltipOpenWardrobe;
    public static boolean debugTooltip = false;

    // Debug
    public static int texturePaintingType;
    public static boolean showF3DebugInfo;
    public static float ploOffset = -0.01f;

    public static boolean enableEntityPlacementHighlight = true;
    public static boolean enableBlockPlacementHighlight = true;

    public static boolean enableModelOverridden = true;
    public static boolean enableWireframeRender = false;
    public static boolean enableMagicWhenContributor = false;

    // Wardrobe
//    public static boolean wardrobeAllowOpening = true;
    public static boolean showWardrobeSkins = true;
    public static boolean showWardrobeOutfits = true;
    public static boolean showWardrobeDisplaySettings = true;
    public static boolean showWardrobeColourSettings = true;
    public static boolean showWardrobeDyeSetting = true;
    public static boolean showWardrobeContributorSetting = true;

    public static int prefersWardrobeSlots = 3;
    public static int prefersWardrobeMobSlots = 3;
    public static int prefersWardrobeDropOnDeath = 0;

    // Debug tool
    public static boolean showArmourerDebugRender;
    public static boolean showLodLevels;
    public static boolean showSkinBlockBounds;
    public static boolean showSkinRenderBounds;
    public static boolean showSortOrderToolTip;


    public static boolean debugSkinnableBlock = false;
    public static boolean debugHologramProjectorBlock = false;

    public static boolean debugSkinBounds = false;
    public static boolean debugSkinPartBounds = false;

    public static boolean debugTargetOrigin = false;
    public static boolean debugTargetBounds = false;

    public static boolean showDebugTextureBounds = false;
    public static boolean showDebugSpin = false;

    public static int getNumberOfRenderLayers() {
        if (multipassSkinRendering) {
            return 4;
        } else {
            return 2;
        }
    }
//
//    public static boolean isSkinnableEntity(@Nullable Entity entity) {
//        if (entity instanceof PlayerEntity) {
//            return true;
//        }
//        if (entity instanceof AbstractArrowEntity) {
//            return true;
//        }
//        if (entity instanceof MannequinEntity) {
//            return true;
//        }
//        return false;
//    }

    public static boolean shouldRenderPart(ISkinPart skinPart) {
        return true;
    }
///give @p chest{BlockEntityTag:{Items:[
//        {id:"armourers_workshop:dye-bottle",Count:1,Slot:0, tag:{Color:0x1ffffff}},
//    {id:"armourers_workshop:dye-bottle",Count:1,Slot:0, tag:{Color:0x2ffffff},
//        {id:"armourers_workshop:dye-bottle",Count:1,Slot:0, tag:{Color:0x3ffffff}
//        }}]}} 1

    public static TexturePaintType getTexturePaintType() {
        if (AWConfig.texturePaintingType < 0) {
            return TexturePaintType.DISABLED;
        }
        if (AWConfig.texturePaintingType == 0) {
//            if (ModLoader.isModLoaded("tlauncher_custom_cape_skin")) {
//                return TexturePaintType.MODEL_REPLACE_AW;
//            }
            return TexturePaintType.TEXTURE_REPLACE;
        }
        return TexturePaintType.values()[AWConfig.texturePaintingType];
    }

    public enum TexturePaintType {
        DISABLED, TEXTURE_REPLACE, MODEL_REPLACE_MC, MODEL_REPLACE_AW
    }

}