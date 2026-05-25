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

    private final Map<UUID, AreaPaintData> entries = new HashMap<>();

    public static AreaPaintSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(AreaPaintSavedData::load, AreaPaintSavedData::new, DATA_NAME);
    }

    public static AreaPaintSavedData load(CompoundTag tag) {
        AreaPaintSavedData data = new AreaPaintSavedData();
        ListTag list = tag.getList("Entries", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            AreaPaintData entry = AreaPaintData.load(list.getCompound(i));
            data.entries.put(entry.entityUuid(), entry);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (AreaPaintData entry : this.entries.values()) {
            list.add(entry.save());
        }
        tag.put("Entries", list);
        return tag;
    }

    public Map<UUID, AreaPaintData> getEntries() {
        return Collections.unmodifiableMap(new HashMap<>(this.entries));
    }

    public void put(UUID entityUuid, AreaPaintData areaPaintData) {
        this.entries.put(entityUuid, areaPaintData);
        this.setDirty();
    }

    public boolean remove(UUID entityUuid) {
        AreaPaintData removed = this.entries.remove(entityUuid);
        if (removed != null) {
            this.setDirty();
            return true;
        }
        return false;
    }
}
