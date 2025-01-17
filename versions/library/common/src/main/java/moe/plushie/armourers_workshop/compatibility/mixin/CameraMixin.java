package moe.plushie.armourers_workshop.compatibility.mixin;

import moe.plushie.armourers_workshop.api.annotation.Available;
import moe.plushie.armourers_workshop.builder.entity.CameraEntity;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Available("[1.16, )")
@Mixin(Camera.class)
public class CameraMixin {

    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
    private double aw2$getMaxZoom(double zoom) {
        Camera camera = ObjectUtils.unsafeCast(this);
        CameraEntity cameraEntity = ObjectUtils.safeCast(camera.getEntity(), CameraEntity.class);
        if (cameraEntity != null) {
            return cameraEntity.getMaxZoom(zoom);
        }
        return zoom;
    }
}
