package com.example.dyeing.client;

import com.example.dyeing.data.AreaPaintData;
import com.example.dyeing.data.PaintData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPaintManager {
    private static final Map<UUID, Map<String, PaintData>> ENTRIES = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, AreaPaintData>> AREA_ENTRIES = new ConcurrentHashMap<>();

    private ClientPaintManager() {
    }

    public static Map<String, PaintData> getAllPaints(UUID entityUuid) {
        Map<String, PaintData> paints = ENTRIES.get(entityUuid);
        if (paints == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(paints);
    }

    public static void put(UUID entityUuid, String id, PaintData paintData) {
        ENTRIES.computeIfAbsent(entityUuid, k -> new ConcurrentHashMap<>()).put(id, paintData);
    }

    public static void remove(UUID entityUuid, String id) {
        Map<String, PaintData> paints = ENTRIES.get(entityUuid);
        if (paints != null) {
            paints.remove(id);
            if (paints.isEmpty()) {
                ENTRIES.remove(entityUuid);
            }
        }
    }

    public static void replaceAll(Map<UUID, Map<String, PaintData>> entries) {
        ENTRIES.clear();
        for (Map.Entry<UUID, Map<String, PaintData>> entityEntry : entries.entrySet()) {
            ENTRIES.put(entityEntry.getKey(), new ConcurrentHashMap<>(entityEntry.getValue()));
        }
    }

    public static Map<UUID, Map<String, AreaPaintData>> getAreaEntries() {
        Map<UUID, Map<String, AreaPaintData>> copy = new HashMap<>();
        for (Map.Entry<UUID, Map<String, AreaPaintData>> entry : AREA_ENTRIES.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public static Map<String, AreaPaintData> getAllAreas(UUID entityUuid) {
        Map<String, AreaPaintData> areas = AREA_ENTRIES.get(entityUuid);
        if (areas == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(areas);
    }

    public static void putArea(UUID entityUuid, String id, AreaPaintData areaPaintData) {
        AREA_ENTRIES.computeIfAbsent(entityUuid, k -> new ConcurrentHashMap<>()).put(id, areaPaintData);
    }

    public static void removeArea(UUID entityUuid, String id) {
        Map<String, AreaPaintData> areas = AREA_ENTRIES.get(entityUuid);
        if (areas != null) {
            areas.remove(id);
            if (areas.isEmpty()) {
                AREA_ENTRIES.remove(entityUuid);
            }
        }
    }

    public static void replaceAllAreas(Map<UUID, Map<String, AreaPaintData>> entries) {
        AREA_ENTRIES.clear();
        for (Map.Entry<UUID, Map<String, AreaPaintData>> entityEntry : entries.entrySet()) {
            AREA_ENTRIES.put(entityEntry.getKey(), new ConcurrentHashMap<>(entityEntry.getValue()));
        }
    }

    public static void clear() {
        ENTRIES.clear();
        AREA_ENTRIES.clear();
    }
}
