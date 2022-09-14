package moe.plushie.armourers_workshop.compatibility.forge;

import com.mojang.authlib.GameProfile;
import moe.plushie.armourers_workshop.api.permission.IPermissionContext;
import moe.plushie.armourers_workshop.api.permission.IPermissionNode;
import moe.plushie.armourers_workshop.core.permission.BlockPermissionContext;
import moe.plushie.armourers_workshop.core.permission.PlayerPermissionContext;
import moe.plushie.armourers_workshop.core.permission.TargetPermissionContext;
import moe.plushie.armourers_workshop.init.platform.forge.builder.PermissionNodeBuilderImpl;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.context.BlockPosContext;
import net.minecraftforge.server.permission.context.IContext;
import net.minecraftforge.server.permission.context.PlayerContext;
import net.minecraftforge.server.permission.context.TargetContext;

public abstract class AbstractForgePermissionManager {

    public static IPermissionNode makeNode(ResourceLocation registryName, int level) {
        PermissionNodeBuilderImpl.NodeImpl nodeImpl = new PermissionNodeBuilderImpl.NodeImpl(registryName) {

            @Override
            public boolean resolve(GameProfile profile, IPermissionContext context) {
                return PermissionAPI.hasPermission(profile, getKey(), of(context));
            }
        };
        PermissionAPI.registerNode(nodeImpl.getKey(), of(level), nodeImpl.getName().getContents());
        return nodeImpl;
    }

    private static DefaultPermissionLevel of(int level) {
        switch (level) {
            case 0:
                return DefaultPermissionLevel.ALL;
            case 3:
                return DefaultPermissionLevel.OP;
            default:
                return DefaultPermissionLevel.NONE;
        }
    }

    private static IContext of(IPermissionContext context) {
        if (context == null) {
            return null;
        }
        BlockPermissionContext block = ObjectUtils.safeCast(context, BlockPermissionContext.class);
        if (block != null) {
            return new BlockPosContext(block.player, block.blockPos, block.blockState, block.facing);
        }
        TargetPermissionContext target = ObjectUtils.safeCast(context, TargetPermissionContext.class);
        if (target != null) {
            return new TargetContext(target.player, target.target);
        }
        PlayerPermissionContext player = ObjectUtils.safeCast(context, PlayerPermissionContext.class);
        if (player != null) {
            return new PlayerContext(player.player);
        }
        return null;
    }
}
