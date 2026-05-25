package com.example.dyeing.network;

import com.example.dyeing.DyeingMod;
import com.example.dyeing.client.ClientPaintManager;
import com.example.dyeing.data.PaintData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class DyeingNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(DyeingMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static boolean registered;

    private DyeingNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        int id = 0;
        CHANNEL.registerMessage(
                id++,
                PaintSyncS2CPacket.class,
                PaintSyncS2CPacket::encode,
                PaintSyncS2CPacket::decode,
                PaintSyncS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id,
                PaintUpdateS2CPacket.class,
                PaintUpdateS2CPacket::encode,
                PaintUpdateS2CPacket::decode,
                PaintUpdateS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        registered = true;
    }

    public static void sendFullSync(ServerPlayer player, Map<UUID, PaintData> entries) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PaintSyncS2CPacket(entries));
    }

    public static void broadcastUpdate(UUID entityUuid, PaintData paintData) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new PaintUpdateS2CPacket(entityUuid, false, paintData));
    }

    public static void broadcastRemove(UUID entityUuid) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new PaintUpdateS2CPacket(entityUuid, true, null));
    }

    private record PaintSyncS2CPacket(Map<UUID, PaintData> entries) {
        private static void encode(PaintSyncS2CPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.entries.size());
            for (Map.Entry<UUID, PaintData> entry : packet.entries.entrySet()) {
                buffer.writeUUID(entry.getKey());
                writePaintData(buffer, entry.getValue());
            }
        }

        private static PaintSyncS2CPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            Map<UUID, PaintData> entries = new HashMap<>();
            for (int i = 0; i < size; i++) {
                UUID entityUuid = buffer.readUUID();
                entries.put(entityUuid, readPaintData(buffer));
            }
            return new PaintSyncS2CPacket(entries);
        }

        private static void handle(PaintSyncS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> ClientPaintManager.replaceAll(packet.entries));
            context.setPacketHandled(true);
        }
    }

    private record PaintUpdateS2CPacket(UUID entityUuid, boolean remove, PaintData paintData) {
        private static void encode(PaintUpdateS2CPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.entityUuid);
            buffer.writeBoolean(packet.remove);
            if (!packet.remove) {
                writePaintData(buffer, packet.paintData);
            }
        }

        private static PaintUpdateS2CPacket decode(FriendlyByteBuf buffer) {
            UUID entityUuid = buffer.readUUID();
            boolean remove = buffer.readBoolean();
            if (remove) {
                return new PaintUpdateS2CPacket(entityUuid, true, null);
            }

            PaintData paintData = readPaintData(buffer);
            return new PaintUpdateS2CPacket(entityUuid, false, paintData);
        }

        private static void handle(PaintUpdateS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (packet.remove) {
                    ClientPaintManager.remove(packet.entityUuid);
                } else {
                    ClientPaintManager.put(packet.entityUuid, packet.paintData);
                }
            });
            context.setPacketHandled(true);
        }
    }

    private static void writePaintData(FriendlyByteBuf buffer, PaintData paintData) {
        buffer.writeInt(paintData.argb());
        buffer.writeFloat(paintData.scale());
        buffer.writeBoolean(paintData.hasScaleAnimation());
        buffer.writeFloat(paintData.scaleFrom());
        buffer.writeFloat(paintData.scaleTo());
        buffer.writeFloat(paintData.scaleAlphaFrom());
        buffer.writeFloat(paintData.scaleAlphaTo());
        buffer.writeVarInt(paintData.scalePeriod());
        buffer.writeBoolean(paintData.hasColorAnimation());
        buffer.writeInt(paintData.colorFromArgb());
        buffer.writeInt(paintData.colorToArgb());
        buffer.writeVarInt(paintData.colorPeriod());
    }

    private static PaintData readPaintData(FriendlyByteBuf buffer) {
        int argb = buffer.readInt();
        float scale = buffer.readFloat();
        boolean hasScaleAnimation = buffer.readBoolean();
        float scaleFrom = buffer.readFloat();
        float scaleTo = buffer.readFloat();
        float scaleAlphaFrom = buffer.readFloat();
        float scaleAlphaTo = buffer.readFloat();
        int scalePeriod = buffer.readVarInt();
        boolean hasColorAnimation = buffer.readBoolean();
        int colorFromArgb = buffer.readInt();
        int colorToArgb = buffer.readInt();
        int colorPeriod = buffer.readVarInt();
        return new PaintData(
                argb,
                scale,
                hasScaleAnimation,
                scaleFrom,
                scaleTo,
                scaleAlphaFrom,
                scaleAlphaTo,
                scalePeriod,
                hasColorAnimation,
                colorFromArgb,
                colorToArgb,
                colorPeriod
        );
    }
}
