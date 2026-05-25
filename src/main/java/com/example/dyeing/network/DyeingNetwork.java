package com.example.dyeing.network;

import com.example.dyeing.DyeingMod;
import com.example.dyeing.client.ClientPaintManager;
import com.example.dyeing.data.AreaPaintData;
import com.example.dyeing.data.AnimationEndAction;
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
                id++,
                PaintUpdateS2CPacket.class,
                PaintUpdateS2CPacket::encode,
                PaintUpdateS2CPacket::decode,
                PaintUpdateS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                AreaPaintSyncS2CPacket.class,
                AreaPaintSyncS2CPacket::encode,
                AreaPaintSyncS2CPacket::decode,
                AreaPaintSyncS2CPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id,
                AreaPaintUpdateS2CPacket.class,
                AreaPaintUpdateS2CPacket::encode,
                AreaPaintUpdateS2CPacket::decode,
                AreaPaintUpdateS2CPacket::handle,
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

    public static void sendFullAreaSync(ServerPlayer player, Map<UUID, AreaPaintData> entries) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AreaPaintSyncS2CPacket(entries));
    }

    public static void broadcastAreaUpdate(UUID entityUuid, AreaPaintData areaPaintData) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new AreaPaintUpdateS2CPacket(entityUuid, true, areaPaintData));
    }

    public static void broadcastAreaRemove(UUID entityUuid) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new AreaPaintUpdateS2CPacket(entityUuid, false, null));
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

    private record AreaPaintSyncS2CPacket(Map<UUID, AreaPaintData> entries) {
        private static void encode(AreaPaintSyncS2CPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.entries.size());
            for (Map.Entry<UUID, AreaPaintData> entry : packet.entries.entrySet()) {
                buffer.writeUUID(entry.getKey());
                writeAreaPaintData(buffer, entry.getValue());
            }
        }

        private static AreaPaintSyncS2CPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            Map<UUID, AreaPaintData> entries = new HashMap<>();
            for (int i = 0; i < size; i++) {
                UUID entityUuid = buffer.readUUID();
                entries.put(entityUuid, readAreaPaintData(buffer));
            }
            return new AreaPaintSyncS2CPacket(entries);
        }

        private static void handle(AreaPaintSyncS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> ClientPaintManager.replaceAllAreas(packet.entries));
            context.setPacketHandled(true);
        }
    }

    private record AreaPaintUpdateS2CPacket(UUID entityUuid, boolean present, AreaPaintData areaPaintData) {
        private static void encode(AreaPaintUpdateS2CPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.entityUuid);
            buffer.writeBoolean(packet.present);
            if (packet.present) {
                writeAreaPaintData(buffer, packet.areaPaintData);
            }
        }

        private static AreaPaintUpdateS2CPacket decode(FriendlyByteBuf buffer) {
            UUID entityUuid = buffer.readUUID();
            boolean present = buffer.readBoolean();
            if (!present) {
                return new AreaPaintUpdateS2CPacket(entityUuid, false, null);
            }
            return new AreaPaintUpdateS2CPacket(entityUuid, true, readAreaPaintData(buffer));
        }

        private static void handle(AreaPaintUpdateS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                if (packet.present) {
                    ClientPaintManager.putArea(packet.entityUuid, packet.areaPaintData);
                } else {
                    ClientPaintManager.removeArea(packet.entityUuid);
                }
            });
            context.setPacketHandled(true);
        }
    }

    private static void writePaintData(FriendlyByteBuf buffer, PaintData paintData) {
        buffer.writeInt(paintData.argb());
        buffer.writeFloat(paintData.scale());
        buffer.writeFloat(paintData.offsetX());
        buffer.writeFloat(paintData.offsetY());
        buffer.writeFloat(paintData.offsetZ());
        buffer.writeLong(paintData.startGameTime());
        buffer.writeBoolean(paintData.hasScaleAnimation());
        buffer.writeFloat(paintData.scaleFrom());
        buffer.writeFloat(paintData.scaleTo());
        buffer.writeFloat(paintData.scaleAlphaFrom());
        buffer.writeFloat(paintData.scaleAlphaTo());
        buffer.writeVarInt(paintData.scalePeriod());
        buffer.writeVarInt(paintData.scalePlayCount() + 1);
        buffer.writeEnum(paintData.scaleEndAction());
        buffer.writeBoolean(paintData.hasColorAnimation());
        buffer.writeInt(paintData.colorFromArgb());
        buffer.writeInt(paintData.colorToArgb());
        buffer.writeVarInt(paintData.colorPeriod());
        buffer.writeVarInt(paintData.colorPlayCount() + 1);
        buffer.writeEnum(paintData.colorEndAction());
    }

    private static PaintData readPaintData(FriendlyByteBuf buffer) {
        int argb = buffer.readInt();
        float scale = buffer.readFloat();
        float offsetX = buffer.readFloat();
        float offsetY = buffer.readFloat();
        float offsetZ = buffer.readFloat();
        long startGameTime = buffer.readLong();
        boolean hasScaleAnimation = buffer.readBoolean();
        float scaleFrom = buffer.readFloat();
        float scaleTo = buffer.readFloat();
        float scaleAlphaFrom = buffer.readFloat();
        float scaleAlphaTo = buffer.readFloat();
        int scalePeriod = buffer.readVarInt();
        int scalePlayCount = buffer.readVarInt() - 1;
        AnimationEndAction scaleEndAction = buffer.readEnum(AnimationEndAction.class);
        boolean hasColorAnimation = buffer.readBoolean();
        int colorFromArgb = buffer.readInt();
        int colorToArgb = buffer.readInt();
        int colorPeriod = buffer.readVarInt();
        int colorPlayCount = buffer.readVarInt() - 1;
        AnimationEndAction colorEndAction = buffer.readEnum(AnimationEndAction.class);
        return new PaintData(
                argb,
                scale,
                offsetX,
                offsetY,
                offsetZ,
                startGameTime,
                hasScaleAnimation,
                scaleFrom,
                scaleTo,
                scaleAlphaFrom,
                scaleAlphaTo,
                scalePeriod,
                scalePlayCount,
                scaleEndAction,
                hasColorAnimation,
                colorFromArgb,
                colorToArgb,
                colorPeriod,
                colorPlayCount,
                colorEndAction
        );
    }

    private static void writeAreaPaintData(FriendlyByteBuf buffer, AreaPaintData areaPaintData) {
        buffer.writeUUID(areaPaintData.entityUuid());
        buffer.writeFloat(areaPaintData.fromX());
        buffer.writeFloat(areaPaintData.fromY());
        buffer.writeFloat(areaPaintData.fromZ());
        buffer.writeFloat(areaPaintData.toX());
        buffer.writeFloat(areaPaintData.toY());
        buffer.writeFloat(areaPaintData.toZ());
        writePaintData(buffer, areaPaintData.paintData());
    }

    private static AreaPaintData readAreaPaintData(FriendlyByteBuf buffer) {
        UUID entityUuid = buffer.readUUID();
        float fromX = buffer.readFloat();
        float fromY = buffer.readFloat();
        float fromZ = buffer.readFloat();
        float toX = buffer.readFloat();
        float toY = buffer.readFloat();
        float toZ = buffer.readFloat();
        PaintData paintData = readPaintData(buffer);
        return new AreaPaintData(entityUuid, fromX, fromY, fromZ, toX, toY, toZ, paintData);
    }
}
