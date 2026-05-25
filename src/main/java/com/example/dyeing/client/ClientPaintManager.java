package com.example.dyeing.client;

import com.example.dyeing.data.AreaPaintData;
import com.example.dyeing.data.PaintData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPaintManager {
    private static final Map<UUID, PaintData> ENTRIES = new ConcurrentHashMap<>();
    private static final Map<UUID, AreaPaintData> AREA_ENTRIES = new ConcurrentHashMap<>();

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

    public static Map<UUID, AreaPaintData> getAreaEntries() {
        return Collections.unmodifiableMap(AREA_ENTRIES);
    }

    public static void putArea(UUID entityUuid, AreaPaintData areaPaintData) {
        AREA_ENTRIES.put(entityUuid, areaPaintData);
    }

    public static void removeArea(UUID entityUuid) {
        AREA_ENTRIES.remove(entityUuid);
    }

    public static void replaceAllAreas(Map<UUID, AreaPaintData> entries) {
        AREA_ENTRIES.clear();
        AREA_ENTRIES.putAll(new HashMap<>(entries));
    }

    public static void clear() {
        ENTRIES.clear();
        AREA_ENTRIES.clear();
    }
}
