package moe.plushie.armourers_workshop.init.platform.fabric;

import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import moe.plushie.armourers_workshop.api.network.IClientPacketHandler;
import moe.plushie.armourers_workshop.api.network.IPacketDistributor;
import moe.plushie.armourers_workshop.api.network.IServerPacketHandler;
import moe.plushie.armourers_workshop.init.ModConfig;
import moe.plushie.armourers_workshop.init.environment.EnvironmentExecutor;
import moe.plushie.armourers_workshop.init.environment.EnvironmentType;
import moe.plushie.armourers_workshop.init.platform.EnvironmentManager;
import moe.plushie.armourers_workshop.init.platform.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class NetworkManagerImpl {


    public static NetworkManager.Dispatcher createDispatcher(ResourceLocation registryName, String version) {
        return new Dispatcher(registryName, version);
    }

    public static NetworkManager.Distributors createDistributors() {
        return new Distributors();
    }

    public static class Dispatcher extends NetworkManager.Dispatcher {

        public Dispatcher(ResourceLocation channelName, String channelVersion) {
            super(channelName, channelVersion);
        }

        @Override
        public void register() {
            ServerLoginConnectionEvents.QUERY_START.register(this::startServerHandshake);
            ServerLoginNetworking.registerGlobalReceiver(channelName, this::onServerHandshake);
            ServerPlayNetworking.registerGlobalReceiver(channelName, this::onServerEvent);

            EnvironmentExecutor.runOn(EnvironmentType.CLIENT, () -> () -> {
                ClientLoginNetworking.registerGlobalReceiver(channelName, this::onClientHandshake);
                ClientPlayNetworking.registerGlobalReceiver(channelName, this::onClientEvent);
            });
        }

        public void startServerHandshake(Object handler, MinecraftServer server, PacketSender sender, ServerLoginNetworking.LoginSynchronizer synchronizer) {
            if (ModConfig.Common.enableProtocolCheck) {
                sender.sendPacket(channelName, PacketByteBufs.empty());
            }
        }

        public void onServerHandshake(MinecraftServer server, ServerLoginPacketListenerImpl handler, boolean understood, FriendlyByteBuf buf, ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender responseSender) {
            if (!ModConfig.Common.enableProtocolCheck) {
                return;
            }
            if (understood) {
                String version = buf.readUtf(Short.MAX_VALUE);
                if (version.equals(channelVersion)) {
                    return;
                }
            }
            handler.disconnect(Component.literal("Please install correct Armourers Workshop to play on this server!"));
        }

        @Environment(EnvType.CLIENT)
        public CompletableFuture<@Nullable FriendlyByteBuf> onClientHandshake(Minecraft client, Object handler, FriendlyByteBuf buf, Consumer<GenericFutureListener<? extends Future<? super Void>>> listenerAdder) {
            FriendlyByteBuf responseBuffer = new FriendlyByteBuf(Unpooled.buffer());
            responseBuffer.writeUtf(channelVersion);
            return CompletableFuture.completedFuture(responseBuffer);
        }

        public void onServerEvent(MinecraftServer server, ServerPlayer player, Object handler, FriendlyByteBuf buf, PacketSender responseSender) {
            IServerPacketHandler packetHandler = server::execute;
            didReceivePacket(packetHandler, buf, player);
        }

        @Environment(EnvType.CLIENT)
        public void onClientEvent(Minecraft client, Object handler, FriendlyByteBuf buf, PacketSender responseSender) {
            IClientPacketHandler packetHandler = client::execute;
            didReceivePacket(packetHandler, buf, null);
        }
    }

    public static class Distributor implements IPacketDistributor {

        private final NetworkDirection direction;
        private final Consumer<Packet<?>> target;
        private final Packet<?> packet;

        Distributor(NetworkDirection direction, Consumer<Packet<?>> target, Packet<?> packet) {
            this.direction = direction;
            this.target = target;
            this.packet = packet;
        }

        @Override
        public IPacketDistributor add(ResourceLocation channel, FriendlyByteBuf buf) {
            Packet<?> packet1 = direction.buildPacket(buf, channel);
            return new Distributor(direction, target, packet1);
        }

        @Override
        public void execute() {
            if (packet != null) {
                target.accept(packet);
            }
        }

        @Override
        public boolean isClientbound() {
            return direction == NetworkDirection.PLAY_TO_CLIENT;
        }
    }

    public static class Distributors implements NetworkManager.Distributors {

        @Override
        public IPacketDistributor trackingChunk(Supplier<LevelChunk> supplier) {
            LevelChunk chunk = supplier.get();
            ServerLevel serverLevel = (ServerLevel) chunk.getLevel();
            Collection<ServerPlayer> players = PlayerLookup.tracking(serverLevel, chunk.getPos());
            return new Distributor(NetworkDirection.PLAY_TO_CLIENT, dispatch(players), null);
        }

        @Override
        public IPacketDistributor trackingEntityAndSelf(Supplier<Entity> supplier) {
            Entity entity = supplier.get();
            Collection<ServerPlayer> players = PlayerLookup.tracking(entity);
            if (entity instanceof ServerPlayer) {
                ArrayList<ServerPlayer> trackingAndSelf = new ArrayList<>(players);
                trackingAndSelf.add((ServerPlayer) entity);
                players = trackingAndSelf;
            }
            return new Distributor(NetworkDirection.PLAY_TO_CLIENT, dispatch(players), null);
        }

        @Override
        public IPacketDistributor player(Supplier<ServerPlayer> supplier) {
            ServerPlayer player = supplier.get();
            return new Distributor(NetworkDirection.PLAY_TO_CLIENT, dispatch(Collections.singleton(player)), null);
        }

        public IPacketDistributor allPlayers() {
            return new Distributor(NetworkDirection.PLAY_TO_CLIENT, dispatch(PlayerLookup.all(EnvironmentManager.getServer())), null);
        }

        public IPacketDistributor server() {
            return new Distributor(NetworkDirection.PLAY_TO_SERVER, ClientPlayNetworking.getSender()::sendPacket, null);
        }

        private Consumer<Packet<?>> dispatch(Collection<ServerPlayer> players) {
            return packet -> players.forEach(player -> player.connection.send(packet));
        }
    }

    public enum NetworkDirection {
        PLAY_TO_SERVER(() -> ClientPlayNetworking::createC2SPacket),
        PLAY_TO_CLIENT(() -> ServerPlayNetworking::createS2CPacket);

        final Supplier<BiFunction<ResourceLocation, FriendlyByteBuf, Packet<?>>> builder;

        NetworkDirection(Supplier<BiFunction<ResourceLocation, FriendlyByteBuf, Packet<?>>> builder) {
            this.builder = builder;
        }

        public Packet<?> buildPacket(FriendlyByteBuf buf, ResourceLocation channelName) {
            return builder.get().apply(channelName, buf);
        }
    }
}
