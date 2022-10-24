package moe.plushie.armourers_workshop.init.platform.fabric;

import com.apple.library.coregraphics.CGRect;
import com.mojang.blaze3d.vertex.PoseStack;
import moe.plushie.armourers_workshop.ArmourersWorkshop;
import moe.plushie.armourers_workshop.api.common.IBlockTintColorProvider;
import moe.plushie.armourers_workshop.api.common.IEntityHandler;
import moe.plushie.armourers_workshop.api.common.IItemPropertiesProvider;
import moe.plushie.armourers_workshop.api.common.IItemTintColorProvider;
import moe.plushie.armourers_workshop.core.client.bake.SkinBakery;
import moe.plushie.armourers_workshop.core.client.render.HighlightPlacementRenderer;
import moe.plushie.armourers_workshop.core.data.slot.SkinSlotType;
import moe.plushie.armourers_workshop.core.registry.Registry;
import moe.plushie.armourers_workshop.core.skin.SkinLoader;
import moe.plushie.armourers_workshop.core.skin.part.SkinPartTypes;
import moe.plushie.armourers_workshop.compatibility.fabric.AbstractFabricClientRegistries;
import moe.plushie.armourers_workshop.init.ModConfig;
import moe.plushie.armourers_workshop.init.ModConfigSpec;
import moe.plushie.armourers_workshop.init.ModContext;
import moe.plushie.armourers_workshop.init.ModItems;
import moe.plushie.armourers_workshop.init.client.ClientWardrobeHandler;
import moe.plushie.armourers_workshop.init.platform.fabric.config.FabricConfig;
import moe.plushie.armourers_workshop.init.platform.fabric.config.FabricConfigTracker;
import moe.plushie.armourers_workshop.init.environment.EnvironmentExecutor;
import moe.plushie.armourers_workshop.init.environment.EnvironmentType;
import moe.plushie.armourers_workshop.init.platform.ItemTooltipManager;
import moe.plushie.armourers_workshop.init.platform.fabric.builder.KeyBindingBuilderImpl;
import moe.plushie.armourers_workshop.init.platform.fabric.event.RenderSpecificArmEvents;
import moe.plushie.armourers_workshop.init.platform.fabric.event.RenderTooltipCallback;
import moe.plushie.armourers_workshop.library.data.SkinLibraryManager;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import moe.plushie.armourers_workshop.utils.RenderSystem;
import moe.plushie.armourers_workshop.utils.TickUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ClientEventDispatcherImpl implements ClientModInitializer {

    boolean isPaused = false;

    @Override
    public void onInitializeClient() {
        EnvironmentExecutor.willInit(EnvironmentType.CLIENT);

        ClientTickEvents.START_CLIENT_TICK.register(this::onRenderTick);
        ClientSpriteRegistryCallback.event(InventoryMenu.BLOCK_ATLAS).register(this::onTextureStitch);
        ModelLoadingRegistry.INSTANCE.registerModelProvider(this::onModelRegistry);
        ItemTooltipCallback.EVENT.register(this::onItemTooltipEvent);
        RenderTooltipCallback.EVENT.register(this::onRenderTooltip);

        ClientPlayConnectionEvents.INIT.register(this::onPlayerLogin);
        ClientPlayConnectionEvents.DISCONNECT.register(this::onPlayerLogout);

        WorldRenderEvents.BLOCK_OUTLINE.register(this::onDrawBlockHighlightEvent);

        ClientTickEvents.END_CLIENT_TICK.register(this::onKeyInputEvent);
        ClientPickBlockGatherCallback.EVENT.register(this::onPickItem);

        RenderSpecificArmEvents.MAIN_HAND.register(this::onRenderSpecificFirstPersonHand);
        RenderSpecificArmEvents.OFF_HAND.register(this::onRenderSpecificFirstPersonHand);

        registerItemColors();
        registerBlockColors();

        registerItemModels();

        EnvironmentExecutor.didInit(EnvironmentType.CLIENT);

        // load all configs
        FabricConfigTracker.INSTANCE.loadConfigs(FabricConfig.Type.CLIENT, FabricLoader.getInstance().getConfigDir());

        RenderSystem.recordRenderCall(() -> EnvironmentExecutor.didSetup(EnvironmentType.CLIENT));
    }

    public void registerBlockColors() {
        ColorProviderRegistry<Block, BlockColor> blockColors = ColorProviderRegistry.BLOCK;
        Registry.BLOCK.getEntries().forEach(object -> {
            Block block = object.get();
            if (block instanceof IBlockTintColorProvider) {
                blockColors.register(((IBlockTintColorProvider) block)::getTintColor, block);
            }
        });
    }

    public void registerItemColors() {
        ColorProviderRegistry<ItemLike, ItemColor> itemColors = ColorProviderRegistry.ITEM;
        Registry.ITEM.getEntries().forEach(object -> {
            Item item = object.get();
            if (item instanceof IItemTintColorProvider) {
                itemColors.register(((IItemTintColorProvider) item)::getTintColor, item);
            }
        });
    }

    public void registerItemModels() {
        Registry.ITEM.getEntries().forEach(object -> {
            Item item = object.get();
            IItemPropertiesProvider provider = ObjectUtils.safeCast(item, IItemPropertiesProvider.class);
            if (provider != null) {
                provider.createModelProperties((key, property) -> AbstractFabricClientRegistries.registerItemProperty(item, key, property));
            }
        });
    }

    public void onModelRegistry(ResourceManager resourceManager, Consumer<ResourceLocation> out) {
        SkinPartTypes.registeredTypes().forEach(partType -> {
            ResourceLocation rl = ArmourersWorkshop.getCustomModel(partType.getRegistryName());
            if (resourceManager.hasResource(new ResourceLocation(rl.getNamespace(), "models/item/" + rl.getPath() + ".json"))) {
                out.accept(rl);
            }
        });
    }

    public void onTextureStitch(TextureAtlas atlasTexture, ClientSpriteRegistryCallback.Registry registry) {
        if (atlasTexture.location().equals(InventoryMenu.BLOCK_ATLAS)) {
            for (SkinSlotType slotType : SkinSlotType.values()) {
                registry.register(slotType.getIconSprite());
            }
        }
    }

    public void onKeyInputEvent(Minecraft client) {
        KeyBindingBuilderImpl.tick();
    }

    public void onItemTooltipEvent(ItemStack stack, TooltipFlag context, List<Component> lines) {
        ItemTooltipManager.appendHoverText(stack, null, lines, context);
    }

    public void onRenderTooltip(PoseStack poseStack, ItemStack itemStack, int x, int y, int width, int height, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (itemStack.isEmpty()) {
            return;
        }
        CGRect frame = new CGRect(x, y, width, height);
        ItemTooltipManager.renderHoverText(itemStack, frame, mouseX, mouseY, screenWidth, screenHeight, poseStack);
    }

    public void onPlayerLogin(ClientPacketListener handler, Minecraft client) {
//        if (event.getPlayer() != null && event.getPlayer().equals(Minecraft.getInstance().player)) {
        SkinBakery.start();
//        }
    }

    public void onPlayerLogout(ClientPacketListener handler, Minecraft client) {
//        Player player = client.player;
//        if (player != null && player.equals(Minecraft.getInstance().player)) {
        SkinBakery.stop();
        SkinLoader.getInstance().clear();
        SkinLibraryManager.getClient().getPublicSkinLibrary().reset();
        SkinLibraryManager.getClient().getPrivateSkinLibrary().reset();
        ModContext.reset();
        ModConfigSpec.COMMON.apply(null);
//        }
    }

    public ItemStack onPickItem(Player player, HitResult result) {
        EntityHitResult result1 = ObjectUtils.safeCast(result, EntityHitResult.class);
        if (result1 != null) {
            IEntityHandler handler = ObjectUtils.safeCast(result1.getEntity(), IEntityHandler.class);
            if (handler != null) {
                return handler.getCustomPickResult(result);
            }
        }
        return ItemStack.EMPTY;
    }

    public void onRenderTick(Minecraft minecraft) {
        boolean isPaused = minecraft.isPaused();
        if (this.isPaused != isPaused) {
            this.isPaused = isPaused;
            if (isPaused) {
                TickUtils.pause();
            } else {
                TickUtils.resume();
            }
        }
    }

    public boolean onDrawBlockHighlightEvent(WorldRenderContext context, WorldRenderContext.BlockOutlineContext blockOutlineContext) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockHitResult target = ObjectUtils.safeCast(minecraft.hitResult, BlockHitResult.class);
        Player player = minecraft.player;
        if (player == null || target == null) {
            return true;
        }
        // hidden hit box at inside
        // if (event.getTarget().isInside()) {
        //     BlockState state = player.level.getBlockState(event.getTarget().getBlockPos());
        //     if (state.is(ModBlocks.BOUNDING_BOX)) {
        //         event.setCanceled(true);
        //         return;
        //     }
        // }
        ItemStack itemStack = player.getMainHandItem();
        Item item = itemStack.getItem();
        if (ModConfig.Client.enableEntityPlacementHighlight && item == ModItems.MANNEQUIN.get()) {
            HighlightPlacementRenderer.renderEntity(player, target, context.camera(), context.matrixStack(), context.consumers());
        }
        if (ModConfig.Client.enableBlockPlacementHighlight && item == ModItems.SKIN.get()) {
            HighlightPlacementRenderer.renderBlock(itemStack, player, target, context.camera(), context.matrixStack(), context.consumers());
        }
        if (ModConfig.Client.enablePaintToolPlacementHighlight && item == ModItems.BLENDING_TOOL.get()) {
            HighlightPlacementRenderer.renderPaintTool(itemStack, player, target, context.camera(), context.matrixStack(), context.consumers());
        }
        return true;
    }
//
//        @SubscribeEvent
//        public void onRenderLivingPre(RenderLivingEvent.Pre<LivingEntity, EntityModel<LivingEntity>> event) {
//            LivingEntity entity = event.getEntity();
//            SkinRenderData renderData = SkinRenderData.of(entity);
//            if (renderData == null) {
//                return;
//            }
//            EntityModel<?> entityModel = event.getRenderer().getModel();
//            SkinRenderer<LivingEntity, EntityModel<?>> renderer = SkinRendererManager.getInstance().getRenderer(entity, entityModel, event.getRenderer());
//            if (renderer != null) {
//                renderer.willRender(entity, entityModel, renderData, event.getLight(), event.getPartialRenderTick(), event.getPoseStack(), event.getBuffers());
//            }
//        }
//
//        @SubscribeEvent
//        public void onRenderLivingPost(RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>> event) {
//            LivingEntity entity = event.getEntity();
//            SkinRenderData renderData = SkinRenderData.of(entity);
//            if (renderData == null) {
//                return;
//            }
//            EntityModel<?> entityModel = event.getRenderer().getModel();
//            SkinRenderer<LivingEntity, EntityModel<?>> renderer = SkinRendererManager.getInstance().getRenderer(entity, entityModel, event.getRenderer());
//            if (renderer != null) {
//                renderer.didRender(entity, entityModel, renderData, event.getLight(), event.getPartialRenderTick(), event.getPoseStack(), event.getBuffers());
//            }
//        }

    public boolean onRenderSpecificFirstPersonHand(PoseStack poseStack, MultiBufferSource buffers, int light, Player player, InteractionHand hand) {
        if (!ModConfig.enableFirstPersonSkinRenderer()) {
            return true;
        }
        ItemTransforms.TransformType transformType = ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND;
        if (hand == InteractionHand.MAIN_HAND) {
            transformType = ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
        }
        boolean[] flags = {false};
        ClientWardrobeHandler.onRenderSpecificHand(player, 0, light, 0, transformType, poseStack, buffers, () -> {
            flags[0] = true;
        });
        return !flags[0];
    }

//    public static ItemStack unwrap(FormattedCharSequence line) {
//        // A slow path, only to check frist string.
//        ResourceLocation[] fonts = {Style.DEFAULT_FONT};
//        line.accept((i, style, j) -> {
//            fonts[0] = style.getFont();
//            return false;
//        });
//        if (fonts[0] instanceof OpenResourceLocation) {
//            return (ItemStack) ((OpenResourceLocation<?>) fonts[0]).extra;
//        }
//        return null;
//    }
//
//    public static Component wrap(Component component, ItemStack itemStack) {
//        TextComponent text = new TextComponent("");
//        text.setStyle(Style.EMPTY.withFont(new OpenResourceLocation<>(Style.DEFAULT_FONT, itemStack)));
//        text.append(component);
//        return text;
//    }
}