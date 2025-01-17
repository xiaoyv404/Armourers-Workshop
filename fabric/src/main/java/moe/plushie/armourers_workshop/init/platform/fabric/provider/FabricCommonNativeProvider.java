package moe.plushie.armourers_workshop.init.platform.fabric.provider;

import com.mojang.brigadier.CommandDispatcher;
import moe.plushie.armourers_workshop.api.common.IBlockSnapshot;
import moe.plushie.armourers_workshop.init.ModConstants;
import moe.plushie.armourers_workshop.init.platform.fabric.event.EntityLifecycleEvents;
import moe.plushie.armourers_workshop.init.platform.fabric.event.PlayerBlockPlaceEvents;
import moe.plushie.armourers_workshop.init.provider.CommonNativeProvider;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface FabricCommonNativeProvider extends CommonNativeProvider {

    @Override
    default void willRegisterEntityAttributes(Consumer<EntityAttributesRegistry> consumer) {
        // noinspection all
        consumer.accept(((entity, builder) -> FabricDefaultAttributeRegistry.register(entity, builder)));
    }

    @Override
    default void willRegisterCustomDataPack(Supplier<PreparableReloadListener> consumer) {
        PreparableReloadListener listener = consumer.get();
        ResourceLocation registryName = ModConstants.key("custom-data-pack");
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return registryName;
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2) {
                return listener.reload(preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2);
            }
        });
    }

    @Override
    default void willServerTick(Consumer<ServerLevel> consumer) {
        ServerTickEvents.START_WORLD_TICK.register(consumer::accept);
    }

    @Override
    default void willServerStart(Consumer<MinecraftServer> consumer) {
        ServerLifecycleEvents.SERVER_STARTING.register(consumer::accept);
    }

    @Override
    default void didServerStart(Consumer<MinecraftServer> consumer) {
        ServerLifecycleEvents.SERVER_STARTED.register(consumer::accept);
    }

    @Override
    default void willServerStop(Consumer<MinecraftServer> consumer) {
        ServerLifecycleEvents.SERVER_STOPPING.register(consumer::accept);
    }

    @Override
    default void didServerStop(Consumer<MinecraftServer> consumer) {
        ServerLifecycleEvents.SERVER_STOPPED.register(consumer::accept);
    }

    @Override
    default void willPlayerLogin(Consumer<Player> consumer) {
        ServerPlayConnectionEvents.JOIN.register(((handler, sender, server) -> consumer.accept(handler.player)));
    }

    @Override
    default void willPlayerLogout(Consumer<Player> consumer) {
        ServerPlayConnectionEvents.DISCONNECT.register(((handler, server) -> consumer.accept(handler.player)));
    }

    @Override
    default void willPlayerClone(BiConsumer<Player, Player> consumer) {
        ServerPlayerEvents.COPY_FROM.register(((oldPlayer, newPlayer, alive) -> consumer.accept(oldPlayer, newPlayer)));
    }

    @Override
    default void didEntityTacking(BiConsumer<Entity, Player> consumer) {
        EntityLifecycleEvents.DID_START_TRACKING.register(consumer::accept);
    }

    @Override
    default void didEntityJoin(Consumer<Entity> consumer) {
        ServerEntityEvents.ENTITY_LOAD.register(((entity, level) -> consumer.accept(entity)));
    }

    @Override
    default void willBlockPlace(BlockSnapshot consumer) {
        PlayerBlockPlaceEvents.BEFORE.register((context, blockState) -> {
            Player player = context.getPlayer();
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();
            consumer.snapshot(player, level, blockPos, blockState, new IBlockSnapshot() {
                @Override
                public BlockState getState() {
                    return level.getBlockState(blockPos);
                }

                @Override
                public CompoundTag getTag() {
                    BlockEntity oldBlockEntity = level.getBlockEntity(blockPos);
                    if (oldBlockEntity != null) {
                        return oldBlockEntity.saveWithFullMetadata();
                    }
                    return null;
                }
            });
            return true;
        });
    }

    @Override
    default void willBlockBreak(BlockSnapshot consumer) {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            consumer.snapshot(player, level, pos, null, new IBlockSnapshot() {

                @Override
                public BlockState getState() {
                    return state;
                }

                @Override
                public CompoundTag getTag() {
                    if (blockEntity != null) {
                        return blockEntity.saveWithFullMetadata();
                    }
                    return null;
                }
            });
            return true;
        });
    }

    @Override
    default void willPlayerDeath(Consumer<Player> consumer) {
        Registry.willEntityDeathFA(entity -> {
            if (entity instanceof Player) {
                consumer.accept((Player) entity);
            }
        });
    }

    @Override
    default void willRegisterCommand(Consumer<CommandDispatcher<CommandSourceStack>> consumer) {
        Registry.willRegisterCommandFA(consumer);
    }

    @Override
    default void willRegisterArgument(Consumer<ArgumentRegistry> consumer) {
        Registry.willRegisterArgumentFA(consumer);
    }
}
