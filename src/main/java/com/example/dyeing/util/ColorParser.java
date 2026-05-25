package com.example.dyeing.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.network.chat.Component;

public final class ColorParser {
    private static final DynamicCommandExceptionType INVALID_COLOR = new DynamicCommandExceptionType(
            value -> Component.literal("无效颜色: " + value + "，请使用 RRGGBB、AARRGGBB、#RRGGBB 或 0xAARRGGBB")
    );

    private ColorParser() {
    }

    public static int parse(String rawValue) throws CommandSyntaxException {
        String normalized = rawValue.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }

        if (normalized.length() == 6) {
            normalized = "FF" + normalized;
        }

        if (normalized.length() != 8) {
            throw INVALID_COLOR.create(rawValue);
        }

        try {
            return (int) Long.parseLong(normalized, 16);
        } catch (NumberFormatException exception) {
            throw INVALID_COLOR.create(rawValue);
        }
    }
}
