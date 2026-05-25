package com.example.dyeing.client;

import com.example.dyeing.data.PaintData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPaintManager {
    private static final Map<UUID, PaintData> ENTRIES = new ConcurrentHashMap<>();

    private ClientPaintManager() {
    }

    public static PaintData get(UUID entityUuid) {
        return ENTRIES.get(entityUuid);
    }

    public static void put(UUID entityUuid, PaintData paintData) {
        ENTRIES.put(entityUuid, paintData);
    }

    public static void remove(UUID entityUuid) {
        ENTRIES.remove(entityUuid);
    }

    public static void replaceAll(Map<UUID, PaintData> entries) {
        ENTRIES.clear();
        ENTRIES.putAll(new HashMap<>(entries));
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
