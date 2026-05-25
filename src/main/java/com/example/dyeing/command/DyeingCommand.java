package com.example.dyeing.command;

import com.example.dyeing.DyeingMod;
import com.example.dyeing.data.AreaPaintData;
import com.example.dyeing.data.AreaPaintSavedData;
import com.example.dyeing.data.PaintData;
import com.example.dyeing.data.PaintSavedData;
import com.example.dyeing.util.ColorParser;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.UUID;

public final class DyeingCommand {
    private static final SimpleCommandExceptionType ENTITY_NOT_FOUND = new SimpleCommandExceptionType(
            Component.literal("找不到对应的已加载实体")
    );
    private static final SimpleCommandExceptionType NO_PAINT_DATA = new SimpleCommandExceptionType(
            Component.literal("该 UUID 没有已保存的染色数据")
    );
    private static final SimpleCommandExceptionType NO_AREA_PAINT_DATA = new SimpleCommandExceptionType(
            Component.literal("该 UUID 没有已保存的区域油漆数据")
    );

    private DyeingCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(createRoot("dyeing"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRoot(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(createPaintBranch())
                .then(createAreaBranch())
                .then(Commands.literal("remove")
                        .then(Commands.literal("all")
                                .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                                        .executes(DyeingCommand::removeAll))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createPaintBranch() {
        return Commands.literal("paint")
                .then(Commands.literal("add")
                        .then(createScaleAddBranch())
                        .then(createColorAddBranch())
                        .then(createComboAddBranch())
                        .then(createStaticAddBranch()))
                .then(Commands.literal("remove")
                        .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                                .executes(DyeingCommand::removePaint)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaBranch() {
        return Commands.literal("area")
                .then(Commands.literal("add")
                        .then(createAreaStaticAddBranch())
                        .then(createAreaScaleAddBranch())
                        .then(createAreaColorAddBranch())
                        .then(createAreaComboAddBranch()))
                .then(Commands.literal("remove")
                        .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                                .executes(DyeingCommand::removeArea)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createStaticAddBranch() {
        return Commands.literal("static")
                .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                        .then(Commands.argument("hex_color", StringArgumentType.word())
                                .executes(context -> addStatic(context, 1.0F))
                                .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F))
                                        .executes(context -> addStatic(context, FloatArgumentType.getFloat(context, "scale")))
                                        .then(createOffsetArguments(ctx -> addStatic(ctx, FloatArgumentType.getFloat(ctx, "scale")))))
                                .then(createOffsetArguments(ctx -> addStatic(ctx, 1.0F)))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createScaleAddBranch() {
        return Commands.literal("scale")
                .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                        .then(Commands.argument("hex_color", StringArgumentType.word())
                                .then(Commands.argument("scale_from", FloatArgumentType.floatArg(0.01F))
                                        .then(Commands.argument("scale_to", FloatArgumentType.floatArg(0.01F))
                                                .then(Commands.argument("alpha_from", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                        .then(Commands.argument("alpha_to", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                .then(Commands.argument("period", IntegerArgumentType.integer(1))
                                                                        .executes(DyeingCommand::addScaleAnimation)
                                                                        .then(createOffsetArguments(DyeingCommand::addScaleAnimation)))))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createColorAddBranch() {
        return Commands.literal("color")
                .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                        .then(Commands.argument("color_from", StringArgumentType.word())
                                .then(Commands.argument("color_to", StringArgumentType.word())
                                        .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F))
                                                .then(Commands.argument("period", IntegerArgumentType.integer(1))
                                                        .executes(DyeingCommand::addColorAnimation)
                                                        .then(createOffsetArguments(DyeingCommand::addColorAnimation)))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createComboAddBranch() {
        return Commands.literal("combo")
                .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                        .then(Commands.argument("color_from", StringArgumentType.word())
                                .then(Commands.argument("color_to", StringArgumentType.word())
                                        .then(Commands.argument("scale_from", FloatArgumentType.floatArg(0.01F))
                                                .then(Commands.argument("scale_to", FloatArgumentType.floatArg(0.01F))
                                                        .then(Commands.argument("alpha_from", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                .then(Commands.argument("alpha_to", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                        .then(Commands.argument("scale_period", IntegerArgumentType.integer(1))
                                                                                .then(Commands.argument("color_period", IntegerArgumentType.integer(1))
                                                                                        .executes(DyeingCommand::addCombinedAnimation)
                                                                                        .then(createOffsetArguments(DyeingCommand::addCombinedAnimation)))))))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createOffsetArguments(Command<CommandSourceStack> command) {
        return Commands.argument("offset_x", FloatArgumentType.floatArg())
                .then(Commands.argument("offset_y", FloatArgumentType.floatArg())
                        .then(Commands.argument("offset_z", FloatArgumentType.floatArg())
                                .executes(command)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaStaticAddBranch() {
        return Commands.literal("static")
                .then(createAreaBoundsArguments(
                        Commands.argument("hex_color", StringArgumentType.word())
                                .executes(context -> addAreaStatic(context, 1.0F))
                                .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F))
                                        .executes(context -> addAreaStatic(context, FloatArgumentType.getFloat(context, "scale"))))
                ));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaScaleAddBranch() {
        return Commands.literal("scale")
                .then(createAreaBoundsArguments(
                        Commands.argument("hex_color", StringArgumentType.word())
                                .then(Commands.argument("scale_from", FloatArgumentType.floatArg(0.01F))
                                        .then(Commands.argument("scale_to", FloatArgumentType.floatArg(0.01F))
                                                .then(Commands.argument("alpha_from", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                        .then(Commands.argument("alpha_to", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                .then(Commands.argument("period", IntegerArgumentType.integer(1))
                                                                        .executes(DyeingCommand::addAreaScaleAnimation)))))))
                );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaColorAddBranch() {
        return Commands.literal("color")
                .then(createAreaBoundsArguments(
                        Commands.argument("color_from", StringArgumentType.word())
                                .then(Commands.argument("color_to", StringArgumentType.word())
                                        .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F))
                                                .then(Commands.argument("period", IntegerArgumentType.integer(1))
                                                        .executes(DyeingCommand::addAreaColorAnimation)))))
                );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaComboAddBranch() {
        return Commands.literal("combo")
                .then(createAreaBoundsArguments(
                        Commands.argument("color_from", StringArgumentType.word())
                                .then(Commands.argument("color_to", StringArgumentType.word())
                                        .then(Commands.argument("scale_from", FloatArgumentType.floatArg(0.01F))
                                                .then(Commands.argument("scale_to", FloatArgumentType.floatArg(0.01F))
                                                        .then(Commands.argument("alpha_from", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                .then(Commands.argument("alpha_to", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                        .then(Commands.argument("scale_period", IntegerArgumentType.integer(1))
                                                                                .then(Commands.argument("color_period", IntegerArgumentType.integer(1))
                                                                                        .executes(DyeingCommand::addAreaCombinedAnimation)))))))))
                );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaBoundsArguments(ArgumentBuilder<CommandSourceStack, ?> next) {
        return Commands.argument("entity_uuid", UuidArgument.uuid())
                .then(Commands.argument("from_x", FloatArgumentType.floatArg())
                        .then(Commands.argument("from_y", FloatArgumentType.floatArg())
                                .then(Commands.argument("from_z", FloatArgumentType.floatArg())
                                        .then(Commands.argument("to_x", FloatArgumentType.floatArg())
                                                .then(Commands.argument("to_y", FloatArgumentType.floatArg())
                                                        .then(Commands.argument("to_z", FloatArgumentType.floatArg())
                                                                .then(next)))))));
    }

    private static int addStatic(CommandContext<CommandSourceStack> context, float scale) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        LivingEntity entity = findLoadedLivingEntity(context.getSource(), entityUuid);
        String colorInput = StringArgumentType.getString(context, "hex_color");
        int argb = ColorParser.parse(colorInput);

        PaintData paintData = PaintData.staticPaint(argb, scale, getOffsetX(context), getOffsetY(context), getOffsetZ(context));
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        savedData.put(entityUuid, paintData);
        DyeingMod.broadcastUpdate(entityUuid, paintData);

        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 添加油漆层，颜色=" + formatArgb(argb) + "，缩放=" + scale),
                true
        );
        return 1;
    }

    private static int addScaleAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        LivingEntity entity = findLoadedLivingEntity(context.getSource(), entityUuid);
        int argb = ColorParser.parse(StringArgumentType.getString(context, "hex_color"));
        float scaleFrom = FloatArgumentType.getFloat(context, "scale_from");
        float scaleTo = FloatArgumentType.getFloat(context, "scale_to");
        float alphaFrom = FloatArgumentType.getFloat(context, "alpha_from");
        float alphaTo = FloatArgumentType.getFloat(context, "alpha_to");
        int period = IntegerArgumentType.getInteger(context, "period");

        PaintData paintData = PaintData.withScaleAnimation(
                argb,
                scaleFrom,
                scaleTo,
                alphaFrom,
                alphaTo,
                period,
                getOffsetX(context),
                getOffsetY(context),
                getOffsetZ(context)
        );
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        savedData.put(entityUuid, paintData);
        DyeingMod.broadcastUpdate(entityUuid, paintData);

        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 添加缩放动画油漆层"),
                true
        );
        return 1;
    }

    private static int addColorAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        LivingEntity entity = findLoadedLivingEntity(context.getSource(), entityUuid);
        int colorFrom = ColorParser.parse(StringArgumentType.getString(context, "color_from"));
        int colorTo = ColorParser.parse(StringArgumentType.getString(context, "color_to"));
        float scale = FloatArgumentType.getFloat(context, "scale");
        int period = IntegerArgumentType.getInteger(context, "period");

        PaintData paintData = PaintData.withColorAnimation(
                colorFrom,
                colorTo,
                scale,
                period,
                getOffsetX(context),
                getOffsetY(context),
                getOffsetZ(context)
        );
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        savedData.put(entityUuid, paintData);
        DyeingMod.broadcastUpdate(entityUuid, paintData);

        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 添加颜色渐变油漆层"),
                true
        );
        return 1;
    }

    private static int addCombinedAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        LivingEntity entity = findLoadedLivingEntity(context.getSource(), entityUuid);
        int colorFrom = ColorParser.parse(StringArgumentType.getString(context, "color_from"));
        int colorTo = ColorParser.parse(StringArgumentType.getString(context, "color_to"));
        float scaleFrom = FloatArgumentType.getFloat(context, "scale_from");
        float scaleTo = FloatArgumentType.getFloat(context, "scale_to");
        float alphaFrom = FloatArgumentType.getFloat(context, "alpha_from");
        float alphaTo = FloatArgumentType.getFloat(context, "alpha_to");
        int scalePeriod = IntegerArgumentType.getInteger(context, "scale_period");
        int colorPeriod = IntegerArgumentType.getInteger(context, "color_period");

        PaintData paintData = PaintData.withCombinedAnimation(
                colorFrom,
                colorTo,
                scaleFrom,
                scaleTo,
                alphaFrom,
                alphaTo,
                scalePeriod,
                colorPeriod,
                getOffsetX(context),
                getOffsetY(context),
                getOffsetZ(context)
        );
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        savedData.put(entityUuid, paintData);
        DyeingMod.broadcastUpdate(entityUuid, paintData);

        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 添加组合动画油漆层"),
                true
        );
        return 1;
    }

    private static int removePaint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        if (!savedData.remove(entityUuid)) {
            throw NO_PAINT_DATA.create();
        }

        DyeingMod.broadcastRemove(entityUuid);
        context.getSource().sendSuccess(() -> Component.literal("已移除实体 " + entityUuid + " 的油漆层"), true);
        return 1;
    }

    private static int addAreaStatic(CommandContext<CommandSourceStack> context, float scale) throws CommandSyntaxException {
        Entity entity = findLoadedEntity(context.getSource(), UuidArgument.getUuid(context, "entity_uuid"));
        int argb = ColorParser.parse(StringArgumentType.getString(context, "hex_color"));
        PaintData paintData = PaintData.staticPaint(argb, scale);
        AreaPaintData areaPaintData = createAreaPaintData(context, paintData);
        AreaPaintSavedData savedData = DyeingMod.getAreaPaintData(context.getSource().getServer());
        savedData.put(entity.getUUID(), areaPaintData);
        DyeingMod.broadcastAreaUpdate(entity.getUUID(), areaPaintData);
        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 绑定静态区域油漆"),
                true
        );
        return 1;
    }

    private static int addAreaScaleAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = findLoadedEntity(context.getSource(), UuidArgument.getUuid(context, "entity_uuid"));
        int argb = ColorParser.parse(StringArgumentType.getString(context, "hex_color"));
        PaintData paintData = PaintData.withScaleAnimation(
                argb,
                FloatArgumentType.getFloat(context, "scale_from"),
                FloatArgumentType.getFloat(context, "scale_to"),
                FloatArgumentType.getFloat(context, "alpha_from"),
                FloatArgumentType.getFloat(context, "alpha_to"),
                IntegerArgumentType.getInteger(context, "period"),
                0.0F,
                0.0F,
                0.0F
        );
        AreaPaintData areaPaintData = createAreaPaintData(context, paintData);
        AreaPaintSavedData savedData = DyeingMod.getAreaPaintData(context.getSource().getServer());
        savedData.put(entity.getUUID(), areaPaintData);
        DyeingMod.broadcastAreaUpdate(entity.getUUID(), areaPaintData);
        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 绑定缩放动画区域油漆"),
                true
        );
        return 1;
    }

    private static int addAreaColorAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = findLoadedEntity(context.getSource(), UuidArgument.getUuid(context, "entity_uuid"));
        PaintData paintData = PaintData.withColorAnimation(
                ColorParser.parse(StringArgumentType.getString(context, "color_from")),
                ColorParser.parse(StringArgumentType.getString(context, "color_to")),
                FloatArgumentType.getFloat(context, "scale"),
                IntegerArgumentType.getInteger(context, "period"),
                0.0F,
                0.0F,
                0.0F
        );
        AreaPaintData areaPaintData = createAreaPaintData(context, paintData);
        AreaPaintSavedData savedData = DyeingMod.getAreaPaintData(context.getSource().getServer());
        savedData.put(entity.getUUID(), areaPaintData);
        DyeingMod.broadcastAreaUpdate(entity.getUUID(), areaPaintData);
        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 绑定颜色渐变区域油漆"),
                true
        );
        return 1;
    }

    private static int addAreaCombinedAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = findLoadedEntity(context.getSource(), UuidArgument.getUuid(context, "entity_uuid"));
        PaintData paintData = PaintData.withCombinedAnimation(
                ColorParser.parse(StringArgumentType.getString(context, "color_from")),
                ColorParser.parse(StringArgumentType.getString(context, "color_to")),
                FloatArgumentType.getFloat(context, "scale_from"),
                FloatArgumentType.getFloat(context, "scale_to"),
                FloatArgumentType.getFloat(context, "alpha_from"),
                FloatArgumentType.getFloat(context, "alpha_to"),
                IntegerArgumentType.getInteger(context, "scale_period"),
                IntegerArgumentType.getInteger(context, "color_period"),
                0.0F,
                0.0F,
                0.0F
        );
        AreaPaintData areaPaintData = createAreaPaintData(context, paintData);
        AreaPaintSavedData savedData = DyeingMod.getAreaPaintData(context.getSource().getServer());
        savedData.put(entity.getUUID(), areaPaintData);
        DyeingMod.broadcastAreaUpdate(entity.getUUID(), areaPaintData);
        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 绑定组合动画区域油漆"),
                true
        );
        return 1;
    }

    private static int removeArea(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        AreaPaintSavedData savedData = DyeingMod.getAreaPaintData(context.getSource().getServer());
        if (!savedData.remove(entityUuid)) {
            throw NO_AREA_PAINT_DATA.create();
        }
        DyeingMod.broadcastAreaRemove(entityUuid);
        context.getSource().sendSuccess(() -> Component.literal("已移除实体 " + entityUuid + " 的区域油漆"), true);
        return 1;
    }

    private static int removeAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        PaintSavedData paintSavedData = DyeingMod.getPaintData(context.getSource().getServer());
        AreaPaintSavedData areaPaintSavedData = DyeingMod.getAreaPaintData(context.getSource().getServer());

        boolean removedPaint = paintSavedData.remove(entityUuid);
        boolean removedArea = areaPaintSavedData.remove(entityUuid);
        if (!removedPaint && !removedArea) {
            throw new SimpleCommandExceptionType(Component.literal("该 UUID 没有已保存的油漆层或区域油漆数据")).create();
        }

        if (removedPaint) {
            DyeingMod.broadcastRemove(entityUuid);
        }
        if (removedArea) {
            DyeingMod.broadcastAreaRemove(entityUuid);
        }

        context.getSource().sendSuccess(() -> Component.literal("已移除实体 " + entityUuid + " 的全部油漆数据"), true);
        return 1;
    }

    private static LivingEntity findLoadedLivingEntity(CommandSourceStack source, UUID entityUuid) throws CommandSyntaxException {
        Entity entity = findLoadedEntity(source, entityUuid);
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        throw ENTITY_NOT_FOUND.create();
    }

    private static Entity findLoadedEntity(CommandSourceStack source, UUID entityUuid) throws CommandSyntaxException {
        for (ServerLevel level : source.getServer().getAllLevels()) {
            Entity entity = level.getEntity(entityUuid);
            if (entity != null) {
                return entity;
            }
        }
        throw ENTITY_NOT_FOUND.create();
    }

    private static String formatArgb(int argb) {
        return "0x" + String.format(Locale.ROOT, "%08X", argb);
    }

    private static float getOffsetX(CommandContext<CommandSourceStack> context) {
        return getOptionalFloat(context, "offset_x");
    }

    private static float getOffsetY(CommandContext<CommandSourceStack> context) {
        return getOptionalFloat(context, "offset_y");
    }

    private static float getOffsetZ(CommandContext<CommandSourceStack> context) {
        return getOptionalFloat(context, "offset_z");
    }

    private static float getOptionalFloat(CommandContext<CommandSourceStack> context, String name) {
        try {
            return FloatArgumentType.getFloat(context, name);
        } catch (IllegalArgumentException exception) {
            return 0.0F;
        }
    }

    private static AreaPaintData createAreaPaintData(CommandContext<CommandSourceStack> context, PaintData paintData) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        return new AreaPaintData(
                entityUuid,
                FloatArgumentType.getFloat(context, "from_x"),
                FloatArgumentType.getFloat(context, "from_y"),
                FloatArgumentType.getFloat(context, "from_z"),
                FloatArgumentType.getFloat(context, "to_x"),
                FloatArgumentType.getFloat(context, "to_y"),
                FloatArgumentType.getFloat(context, "to_z"),
                paintData
        );
    }
}
