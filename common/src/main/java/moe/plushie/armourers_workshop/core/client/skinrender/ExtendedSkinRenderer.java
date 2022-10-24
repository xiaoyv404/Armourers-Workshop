package moe.plushie.armourers_workshop.core.client.skinrender;

import com.mojang.blaze3d.vertex.PoseStack;
import moe.plushie.armourers_workshop.api.client.model.IHumanoidModelHolder;
import moe.plushie.armourers_workshop.core.client.bake.BakedSkinPart;
import moe.plushie.armourers_workshop.core.client.other.SkinOverriddenManager;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderContext;
import moe.plushie.armourers_workshop.core.client.other.SkinRenderData;
import moe.plushie.armourers_workshop.core.entity.EntityProfile;
import moe.plushie.armourers_workshop.core.skin.part.SkinPartTypes;
import moe.plushie.armourers_workshop.core.skin.property.SkinProperty;
import moe.plushie.armourers_workshop.utils.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@Environment(value = EnvType.CLIENT)
public abstract class ExtendedSkinRenderer<T extends LivingEntity, V extends EntityModel<T>, M extends IHumanoidModelHolder<V>> extends LivingSkinRenderer<T, V, M> {

    public ExtendedSkinRenderer(EntityProfile profile) {
        super(profile);
    }

    @Override
    public void initTransformers() {
        transformer.registerArmor(SkinPartTypes.BIPED_HAT, this::setHatPart);
        transformer.registerArmor(SkinPartTypes.BIPED_HEAD, this::setHeadPart);
        transformer.registerArmor(SkinPartTypes.BIPED_CHEST, this::setBodyPart);
        transformer.registerArmor(SkinPartTypes.BIPED_LEFT_ARM, this::setLeftArmPart);
        transformer.registerArmor(SkinPartTypes.BIPED_RIGHT_ARM, this::setRightArmPart);
        transformer.registerArmor(SkinPartTypes.BIPED_LEFT_FOOT, this::setLeftFootPart);
        transformer.registerArmor(SkinPartTypes.BIPED_RIGHT_FOOT, this::setRightFootPart);
        transformer.registerArmor(SkinPartTypes.BIPED_LEFT_LEG, this::setLeftLegPart);
        transformer.registerArmor(SkinPartTypes.BIPED_RIGHT_LEG, this::setRightLegPart);
        transformer.registerArmor(SkinPartTypes.BIPED_SKIRT, this::setSkirtPart);
        transformer.registerArmor(SkinPartTypes.BIPED_RIGHT_WING, this::setWings);
        transformer.registerArmor(SkinPartTypes.BIPED_LEFT_WING, this::setWings);

        transformer.registerItem(ItemTransforms.TransformType.NONE, Transformer::withModel);
        transformer.registerItem(ItemTransforms.TransformType.GUI, Transformer::withModel);
        transformer.registerItem(ItemTransforms.TransformType.FIXED, Transformer::withModel);
        transformer.registerItem(ItemTransforms.TransformType.GROUND, Transformer::withModel);
        transformer.registerItem(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, Transformer::withModel);
        transformer.registerItem(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, Transformer::withModel);
        transformer.registerItem(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, Transformer::withModel);
        transformer.registerItem(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, Transformer::withModel);
    }

    @Override
    public void willRender(T entity, M model, SkinRenderData renderData, SkinRenderContext context) {
        super.willRender(entity, model, renderData, context);
        renderData.getOverriddenManager().willRender(entity);

        // Limit the players limbs if they have a skirt equipped.
        // A proper lady should not swing her legs around!
        if (renderData.isLimitLimbs()) {
            if (entity.animationSpeed > 0.25F) {
                entity.animationSpeed = 0.25F;
                entity.animationSpeedOld = 0.25F;
            }
        }
    }

    @Override
    public void didRender(T entity, M model, SkinRenderData renderData, SkinRenderContext renderContext) {
        super.didRender(entity, model, renderData, renderContext);
        renderData.getOverriddenManager().didRender(entity);
    }

    @Override
    protected void apply(T entity, M model, SkinOverriddenManager overriddenManager, SkinRenderData renderData) {
        super.apply(entity, model, overriddenManager, renderData);
        // model
        if (overriddenManager.overrideModel(SkinPartTypes.BIPED_HEAD)) {
            addModelOverride(model.getHeadPart());
        }
        if (overriddenManager.overrideModel(SkinPartTypes.BIPED_CHEST)) {
            addModelOverride(model.getBodyPart());
        }
        if (overriddenManager.overrideModel(SkinPartTypes.BIPED_LEFT_ARM)) {
            addModelOverride(model.getLeftArmPart());
        }
        if (overriddenManager.overrideModel(SkinPartTypes.BIPED_RIGHT_ARM)) {
            addModelOverride(model.getRightArmPart());
        }
        if (overriddenManager.overrideModel(SkinPartTypes.BIPED_LEFT_LEG)) {
            addModelOverride(model.getLeftLegPart());
        }
        if (overriddenManager.overrideModel(SkinPartTypes.BIPED_RIGHT_LEG)) {
            addModelOverride(model.getRightLegPart());
        }
        if (overriddenManager.overrideModel(SkinPartTypes.BIPED_LEFT_FOOT)) {
            addModelOverride(model.getLeftLegPart());
        }
        if (overriddenManager.overrideModel(SkinPartTypes.BIPED_RIGHT_FOOT)) {
            addModelOverride(model.getRightLegPart());
        }
        // overlay
        if (overriddenManager.overrideOverlay(SkinPartTypes.BIPED_HEAD)) {
            addModelOverride(model.getHatPart());
        }
    }

    protected void setHatPart(PoseStack matrixStack, M model) {
        transformer.apply(matrixStack, model.getHatPart());
    }

    protected void setHeadPart(PoseStack matrixStack, M model) {
        transformer.apply(matrixStack, model.getHeadPart());
    }

    protected void setBodyPart(PoseStack matrixStack, M model) {
        transformer.apply(matrixStack, model.getBodyPart());
    }

    protected void setLeftArmPart(PoseStack matrixStack, M model) {
        transformer.apply(matrixStack, model.getLeftArmPart());
    }

    protected void setRightArmPart(PoseStack matrixStack, M model) {
        transformer.apply(matrixStack, model.getRightArmPart());
    }

    protected void setLeftLegPart(PoseStack matrixStack, M model) {
        transformer.apply(matrixStack, model.getLeftLegPart());
    }

    protected void setRightLegPart(PoseStack matrixStack, M model) {
        transformer.apply(matrixStack, model.getRightLegPart());
    }

    protected void setLeftFootPart(PoseStack matrixStack, M model) {
        transformer.apply(matrixStack, model.getLeftLegPart());
    }

    protected void setRightFootPart(PoseStack matrixStack, M model) {
        transformer.apply(matrixStack, model.getRightLegPart());
    }

    protected void setSkirtPart(PoseStack matrixStack, M model) {
        ModelPart body = model.getBodyPart();
        ModelPart leg = model.getRightLegPart();
        matrixStack.translate(body.x, leg.y, leg.z);
        if (body.yRot != 0) {
            matrixStack.mulPose(Vector3f.YP.rotation(body.yRot));
        }
        // skirt does not wobble during normal walking.
        if (!model.isRiding()) {
            return;
        }
        if (leg.xRot != 0) {
            matrixStack.mulPose(Vector3f.XP.rotation(leg.xRot));
        }
    }

    protected void setWings(PoseStack matrixStack, T entity, M model, ItemStack itemStack, ItemTransforms.TransformType transformType, BakedSkinPart bakedPart) {
        if (bakedPart.getProperties().get(SkinProperty.WINGS_MATCHING_POSE)) {
            transformer.apply(matrixStack, model.getBodyPart());
        }
        matrixStack.translate(0, 0, 2);
    }
}
