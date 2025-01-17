package moe.plushie.armourers_workshop.api.common;

import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface IResourceManager {

    boolean hasResource(ResourceLocation resourceLocation);

    IResource readResource(ResourceLocation resourceLocation) throws IOException;

    void readResources(ResourceLocation target, Predicate<String> validator, BiConsumer<ResourceLocation, IResource> consumer);
}
