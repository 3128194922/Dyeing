package com.example.dyeing.data;

import com.example.dyeing.DyeingMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AreaPaintSavedData extends SavedData {
    private static final String DATA_NAME = DyeingMod.MODID + "_area_paint_data";

    private final Map<UUID, Map<String, AreaPaintData>> entries = new HashMap<>();

    public static AreaPaintSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(AreaPaintSavedData::load, AreaPaintSavedData::new, DATA_NAME);
    }

    public static AreaPaintSavedData load(CompoundTag tag) {
        AreaPaintSavedData data = new AreaPaintSavedData();
        ListTag entityList = tag.getList("Entries", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < entityList.size(); i++) {
            CompoundTag entityTag = entityList.getCompound(i);
            ListTag areaList = entityTag.getList("Areas", CompoundTag.TAG_COMPOUND);
            for (int j = 0; j < areaList.size(); j++) {
                CompoundTag areaTag = areaList.getCompound(j);
                String id = areaTag.getString("Id");
                AreaPaintData areaData = AreaPaintData.load(areaTag.getCompound("Area"));
                data.entries.computeIfAbsent(areaData.entityUuid(), k -> new HashMap<>()).put(id, areaData);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entityList = new ListTag();
        for (Map.Entry<UUID, Map<String, AreaPaintData>> entityEntry : this.entries.entrySet()) {
            CompoundTag entityTag = new CompoundTag();
            entityTag.putUUID("Entity", entityEntry.getKey());
            ListTag areaList = new ListTag();
            for (Map.Entry<String, AreaPaintData> areaEntry : entityEntry.getValue().entrySet()) {
                CompoundTag areaTag = new CompoundTag();
                areaTag.putString("Id", areaEntry.getKey());
                areaTag.put("Area", areaEntry.getValue().save());
                areaList.add(areaTag);
            }
            entityTag.put("Areas", areaList);
            entityList.add(entityTag);
        }
        tag.put("Entries", entityList);
        return tag;
    }

    public Map<UUID, Map<String, AreaPaintData>> getEntries() {
        Map<UUID, Map<String, AreaPaintData>> copy = new HashMap<>();
        for (Map.Entry<UUID, Map<String, AreaPaintData>> entry : this.entries.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public Map<String, AreaPaintData> getAll(UUID entityUuid) {
        Map<String, AreaPaintData> areas = this.entries.get(entityUuid);
        if (areas == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(areas));
    }

    public void put(UUID entityUuid, String id, AreaPaintData areaPaintData) {
        this.entries.computeIfAbsent(entityUuid, k -> new HashMap<>()).put(id, areaPaintData);
        this.setDirty();
    }

    public boolean remove(UUID entityUuid, String id) {
        Map<String, AreaPaintData> areas = this.entries.get(entityUuid);
        if (areas == null) {
            return false;
        }
        AreaPaintData removed = areas.remove(id);
        if (removed != null) {
            if (areas.isEmpty()) {
                this.entries.remove(entityUuid);
            }
            this.setDirty();
            return true;
        }
        return false;
    }

    public boolean removeAll(UUID entityUuid) {
        Map<String, AreaPaintData> removed = this.entries.remove(entityUuid);
        if (removed != null && !removed.isEmpty()) {
            this.setDirty();
            return true;
        }
        return false;
    }
}
