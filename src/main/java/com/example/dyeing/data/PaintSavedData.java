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

    private final Map<UUID, PaintData> entries = new HashMap<>();

    public static PaintSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PaintSavedData::load, PaintSavedData::new, DATA_NAME);
    }

    public static PaintSavedData load(CompoundTag tag) {
        PaintSavedData data = new PaintSavedData();
        ListTag list = tag.getList("Entries", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            if (entryTag.hasUUID("Entity")) {
                data.entries.put(entryTag.getUUID("Entity"), PaintData.load(entryTag.getCompound("Paint")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PaintData> entry : this.entries.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("Entity", entry.getKey());
            entryTag.put("Paint", entry.getValue().save());
            list.add(entryTag);
        }

        tag.put("Entries", list);
        return tag;
    }

    public Map<UUID, PaintData> getEntries() {
        return Collections.unmodifiableMap(new HashMap<>(this.entries));
    }

    public void put(UUID entityUuid, PaintData paintData) {
        this.entries.put(entityUuid, paintData);
        this.setDirty();
    }

    public boolean remove(UUID entityUuid) {
        PaintData removed = this.entries.remove(entityUuid);
        if (removed != null) {
            this.setDirty();
            return true;
        }

        return false;
    }
}
