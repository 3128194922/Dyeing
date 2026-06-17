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

public class PaintSavedData extends SavedData {
    private static final String DATA_NAME = DyeingMod.MODID + "_paint_data";

    private final Map<UUID, Map<String, PaintData>> entries = new HashMap<>();

    public static PaintSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PaintSavedData::load, PaintSavedData::new, DATA_NAME);
    }

    public static PaintSavedData load(CompoundTag tag) {
        PaintSavedData data = new PaintSavedData();
        ListTag entityList = tag.getList("Entries", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < entityList.size(); i++) {
            CompoundTag entityTag = entityList.getCompound(i);
            if (entityTag.hasUUID("Entity")) {
                UUID entityUuid = entityTag.getUUID("Entity");
                Map<String, PaintData> paintMap = new HashMap<>();
                ListTag paintList = entityTag.getList("Paints", CompoundTag.TAG_COMPOUND);
                for (int j = 0; j < paintList.size(); j++) {
                    CompoundTag paintTag = paintList.getCompound(j);
                    String id = paintTag.getString("Id");
                    paintMap.put(id, PaintData.load(paintTag.getCompound("Paint")));
                }
                data.entries.put(entityUuid, paintMap);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entityList = new ListTag();
        for (Map.Entry<UUID, Map<String, PaintData>> entityEntry : this.entries.entrySet()) {
            CompoundTag entityTag = new CompoundTag();
            entityTag.putUUID("Entity", entityEntry.getKey());
            ListTag paintList = new ListTag();
            for (Map.Entry<String, PaintData> paintEntry : entityEntry.getValue().entrySet()) {
                CompoundTag paintTag = new CompoundTag();
                paintTag.putString("Id", paintEntry.getKey());
                paintTag.put("Paint", paintEntry.getValue().save());
                paintList.add(paintTag);
            }
            entityTag.put("Paints", paintList);
            entityList.add(entityTag);
        }
        tag.put("Entries", entityList);
        return tag;
    }

    public Map<UUID, Map<String, PaintData>> getEntries() {
        Map<UUID, Map<String, PaintData>> copy = new HashMap<>();
        for (Map.Entry<UUID, Map<String, PaintData>> entry : this.entries.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public Map<String, PaintData> getAll(UUID entityUuid) {
        Map<String, PaintData> paints = this.entries.get(entityUuid);
        if (paints == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(paints));
    }

    public void put(UUID entityUuid, String id, PaintData paintData) {
        this.entries.computeIfAbsent(entityUuid, k -> new HashMap<>()).put(id, paintData);
        this.setDirty();
    }

    public boolean remove(UUID entityUuid, String id) {
        Map<String, PaintData> paints = this.entries.get(entityUuid);
        if (paints == null) {
            return false;
        }
        PaintData removed = paints.remove(id);
        if (removed != null) {
            if (paints.isEmpty()) {
                this.entries.remove(entityUuid);
            }
            this.setDirty();
            return true;
        }
        return false;
    }

    public boolean removeAll(UUID entityUuid) {
        Map<String, PaintData> removed = this.entries.remove(entityUuid);
        if (removed != null && !removed.isEmpty()) {
            this.setDirty();
            return true;
        }
        return false;
    }
}
