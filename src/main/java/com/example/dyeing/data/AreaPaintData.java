package com.example.dyeing.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record AreaPaintData(
        UUID entityUuid,
        float fromX,
        float fromY,
        float fromZ,
        float toX,
        float toY,
        float toZ,
        PaintData paintData
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Entity", this.entityUuid);
        tag.putFloat("FromX", this.fromX);
        tag.putFloat("FromY", this.fromY);
        tag.putFloat("FromZ", this.fromZ);
        tag.putFloat("ToX", this.toX);
        tag.putFloat("ToY", this.toY);
        tag.putFloat("ToZ", this.toZ);
        tag.put("Paint", this.paintData.save());
        return tag;
    }

    public static AreaPaintData load(CompoundTag tag) {
        return new AreaPaintData(
                tag.getUUID("Entity"),
                tag.getFloat("FromX"),
                tag.getFloat("FromY"),
                tag.getFloat("FromZ"),
                tag.getFloat("ToX"),
                tag.getFloat("ToY"),
                tag.getFloat("ToZ"),
                PaintData.load(tag.getCompound("Paint"))
        );
    }
}
