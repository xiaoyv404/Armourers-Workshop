package moe.plushie.armourers_workshop.api.registry;

import moe.plushie.armourers_workshop.api.common.ILootFunction;
import moe.plushie.armourers_workshop.api.common.ILootFunctionType;

@SuppressWarnings("unused")
public interface ILootFunctionBuilder<T extends ILootFunction> extends IRegistryBuilder<ILootFunctionType<T>> {
}
