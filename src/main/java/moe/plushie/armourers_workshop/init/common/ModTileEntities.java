/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package moe.plushie.armourers_workshop.init.common;

import moe.plushie.armourers_workshop.builder.tileentity.ColourMixerTileEntity;
import moe.plushie.armourers_workshop.core.tileentity.DyeTableTileEntity;
import moe.plushie.armourers_workshop.core.tileentity.HologramProjectorTileEntity;
import moe.plushie.armourers_workshop.core.tileentity.SkinnableTileEntity;
import moe.plushie.armourers_workshop.library.block.SkinLibraryBlock;
import moe.plushie.armourers_workshop.library.tileentity.GlobalSkinLibraryTileEntity;
import moe.plushie.armourers_workshop.library.tileentity.SkinLibraryTileEntity;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.function.Consumer;

public final class ModTileEntities {

    private static final HashMap<ResourceLocation, TileEntityType<?>> REGISTERED_ENTITY_TYPES = new HashMap<>();

    public static final TileEntityType<HologramProjectorTileEntity> HOLOGRAM_PROJECTOR = register("hologram-projector", TileEntityType.Builder.of(HologramProjectorTileEntity::new, ModBlocks.HOLOGRAM_PROJECTOR));
    public static final TileEntityType<SkinnableTileEntity> SKINNABLE = register("skinnable", TileEntityType.Builder.of(SkinnableTileEntity::new, ModBlocks.SKINNABLE));
    public static final TileEntityType<ColourMixerTileEntity> COLOUR_MIXER = register("colour-mixer", TileEntityType.Builder.of(ColourMixerTileEntity::new, ModBlocks.COLOUR_MIXER));
    public static final TileEntityType<DyeTableTileEntity> DYE_TABLE = register("dye-table", TileEntityType.Builder.of(DyeTableTileEntity::new, ModBlocks.DYE_TABLE));

    public static final TileEntityType<SkinLibraryTileEntity> SKIN_LIBRARY_CREATIVE = register("skin-library-creative", TileEntityType.Builder.of(SkinLibraryTileEntity::new, ModBlocks.SKIN_LIBRARY_CREATIVE));
    public static final TileEntityType<SkinLibraryTileEntity> SKIN_LIBRARY = register("skin-library", TileEntityType.Builder.of(SkinLibraryTileEntity::new, ModBlocks.SKIN_LIBRARY));
    public static final TileEntityType<GlobalSkinLibraryTileEntity> SKIN_LIBRARY_GLOBAL = register("skin-library-global", TileEntityType.Builder.of(GlobalSkinLibraryTileEntity::new, ModBlocks.SKIN_LIBRARY_GLOBAL));

    public static void forEach(Consumer<TileEntityType<?>> action) {
        REGISTERED_ENTITY_TYPES.values().forEach(action);
    }

    private static <T extends TileEntity> TileEntityType<T> register(String name, TileEntityType.Builder<T> builder) {
        ResourceLocation registryName = AWCore.resource(name);
        TileEntityType<T> entityType = builder.build(null);
        entityType.setRegistryName(registryName);
        REGISTERED_ENTITY_TYPES.put(registryName, entityType);
        return entityType;
    }

}
