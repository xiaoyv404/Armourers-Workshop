package moe.plushie.armourers_workshop.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import moe.plushie.armourers_workshop.common.ArmourersConfig;
import moe.plushie.armourers_workshop.core.entity.MannequinEntity;
import moe.plushie.armourers_workshop.core.render.bake.BakedSkin;
import moe.plushie.armourers_workshop.core.render.buffer.SkinRenderBuffer;
import moe.plushie.armourers_workshop.core.render.renderer.SkinRenderer;
import moe.plushie.armourers_workshop.core.render.renderer.SkinRendererManager;
import moe.plushie.armourers_workshop.core.skin.SkinDescriptor;
import moe.plushie.armourers_workshop.core.utils.RenderUtils;
import moe.plushie.armourers_workshop.core.capability.Wardrobe;
import moe.plushie.armourers_workshop.core.capability.WardrobeState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.function.Function;


@OnlyIn(Dist.CLIENT)
public class ClientWardrobeHandler {

    public final static float SCALE = 1 / 16f;

    public static void init() {
    }

    public static void onRenderArrow(AbstractArrowEntity entity, Model model, float p_225623_2_, float partialTicks, int light, MatrixStack matrixStack, IRenderTypeBuffer renderType, CallbackInfo callback) {
        Wardrobe wardrobe = Wardrobe.of(entity);
        if (wardrobe == null) {
            return;
        }
        matrixStack.pushPose();

        matrixStack.mulPose(Vector3f.YP.rotationDegrees(MathHelper.lerp(partialTicks, entity.yRotO, entity.yRot) - 90.0F));
        matrixStack.mulPose(Vector3f.ZP.rotationDegrees(MathHelper.lerp(partialTicks, entity.xRotO, entity.xRot)));

        float f9 = (float) entity.shakeTime - partialTicks;
        if (f9 > 0.0F) {
            float f10 = -MathHelper.sin(f9 * 3.0F) * f9;
            matrixStack.mulPose(Vector3f.ZP.rotationDegrees(f10));
        }

        matrixStack.mulPose(Vector3f.YP.rotationDegrees(-90));
        matrixStack.scale(-SCALE, -SCALE, SCALE);
        matrixStack.translate(0, 0, -1);

        int count = render(wardrobe, entity, model, light, matrixStack, ItemCameraTransforms.TransformType.NONE, WardrobeState::getItemSkins);
        if (count != 0) {
            callback.cancel();
        }

        matrixStack.popPose();
    }

    public static void onRenderArmor(Entity entity, Model model, int light, MatrixStack matrixStack, IRenderTypeBuffer renderType) {
        Wardrobe wardrobe = Wardrobe.of(entity);
        if (wardrobe == null) {
            return;
        }
        matrixStack.pushPose();
        matrixStack.scale(SCALE, SCALE, SCALE);

        render(wardrobe, entity, model, light, matrixStack, null, WardrobeState::getArmorSkins);

        matrixStack.popPose();
    }

    public static void onRenderItem(Entity entity, ItemStack itemStack, ItemCameraTransforms.TransformType transformType, int light, MatrixStack matrixStack, IRenderTypeBuffer renderType, CallbackInfo callback) {
        Wardrobe wardrobe = Wardrobe.of(entity);
        if (wardrobe == null) {
            return;
        }
        matrixStack.pushPose();
        matrixStack.scale(-SCALE, -SCALE, SCALE);

        int count = renderItemSkins(wardrobe, entity, null, light, matrixStack, transformType, itemStack);
        if (count != 0) {
            callback.cancel();
        }

        matrixStack.popPose();
    }

    public static void onRenderEntityInInventoryPre(LivingEntity entity, int x, int y, int scale, float mouseX, float mouseY) {
        if (!ArmourersConfig.enableEntityInInventoryClip) {
            return;
        }
        int left, top, width, height;
        switch (scale) {
            case 20: // in creative container screen
                width = 32;
                height = 43;
                left = x - width / 2 + 1;
                top = y - height + 4;
                break;

            case 30: // in survival container screen
                width = 49;
                height = 70;
                left = x - width / 2 - 1;
                top = y - height + 3;
                break;

            default:
                return;
        }
        RenderUtils.enableScissor(left, top, width, height);
    }

    public static void onRenderEntityInInventoryPost(LivingEntity entity) {
        if (!ArmourersConfig.enableEntityInInventoryClip) {
            return;
        }
        RenderUtils.disableScissor();
    }

    public static void onRenderEquipment(LivingEntity entity, EquipmentSlotType slotType, MatrixStack matrixStack, IRenderTypeBuffer renderType, CallbackInfo callback) {
        ItemStack itemStack = entity.getItemBySlot(slotType);
        if (itemStack.isEmpty()) {
            return;
        }
        Wardrobe wardrobe = Wardrobe.of(entity);
        if (wardrobe != null && !wardrobe.shouldRenderEquipment(slotType)) {
            callback.cancel();
        }
    }

    private static int renderItemSkins(Wardrobe wardrobe, Entity entity, Model model, int light, MatrixStack matrixStack, ItemCameraTransforms.TransformType transformType, ItemStack itemStack) {
        return render(wardrobe, entity, model, light, matrixStack, transformType, snapshot -> {
            if (entity instanceof MannequinEntity) {
                SkinDescriptor target = SkinDescriptor.of(itemStack);
                for (BakedSkin bakedSkin : snapshot.getItemSkins()) {
                    if (bakedSkin.accept(target)) {
                        return Collections.singletonList(bakedSkin);
                    }
                }
                return Collections.emptyList();
            }
            for (BakedSkin bakedSkin : snapshot.getItemSkins()) {
                if (bakedSkin.accept(itemStack)) {
                    return Collections.singletonList(bakedSkin);
                }
            }
            return Collections.emptyList();
        });
    }

    private static int render(Wardrobe wardrobe, Entity entity, Model model, int light, MatrixStack matrixStack, ItemCameraTransforms.TransformType transformType, Function<WardrobeState, Iterable<BakedSkin>> provider) {
        int r = 0;
        float partialTicks = System.currentTimeMillis() % 100000000;
        WardrobeState snapshot = wardrobe.snapshot();
        SkinRenderBuffer buffer = SkinRenderBuffer.getInstance();
        SkinRenderer<Entity, Model> renderer = SkinRendererManager.getInstance().getRenderer(entity);
        if (renderer == null) {
            return 0;
        }
        for (BakedSkin bakedSkin : provider.apply(snapshot)) {
            renderer.render(entity, model, bakedSkin, snapshot.getColorScheme(), transformType, light, partialTicks, matrixStack, buffer);
            r += 1;
        }
        buffer.endBatch();
        return r;
    }
}