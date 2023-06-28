package moe.plushie.armourers_workshop.compatibility.client.model;

import moe.plushie.armourers_workshop.api.annotation.Available;
import moe.plushie.armourers_workshop.compatibility.client.AbstractEntityRendererProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.LivingEntity;

@Available("[1.18, )")
@Environment(EnvType.CLIENT)
public abstract class AbstractPlayerModel<T extends LivingEntity> extends PlayerModel<T> {

    public AbstractPlayerModel(AbstractEntityRendererProvider.Context context, float scale, boolean slim) {
        super(context.bakeLayer(ModelLayers.PLAYER), slim);
    }
}
