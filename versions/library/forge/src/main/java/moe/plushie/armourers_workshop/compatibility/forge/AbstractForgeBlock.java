package moe.plushie.armourers_workshop.compatibility.forge;

import moe.plushie.armourers_workshop.api.annotation.Available;
import moe.plushie.armourers_workshop.api.common.IBlockHandler;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;

@Available("[1.21, )")
public interface AbstractForgeBlock extends IBlockExtension {

    @Override
    default boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        IBlockHandler handler = ObjectUtils.unsafeCast(this);
        InteractionResult result = handler.attackBlock(level, pos, state, Direction.NORTH, player, InteractionHand.MAIN_HAND);
        if (result == InteractionResult.PASS) {
            return IBlockExtension.super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        }
        if (result == InteractionResult.SUCCESS) {
            // when the result is successful, we need to add the break effects.
            Block block = ObjectUtils.unsafeCast(this);
            block.playerWillDestroy(level, pos, state, player);
        }
        return result.consumesAction();
    }
}
