package com.example.dyeing.command;

import com.example.dyeing.DyeingMod;
import com.example.dyeing.data.AreaPaintData;
import com.example.dyeing.data.AreaPaintSavedData;
import com.example.dyeing.data.AnimationEndAction;
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
import net.minecraft.commands.SharedSuggestionProvider;
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
            Component.literal("该 UUID/ID 没有已保存的染色数据")
    );
    private static final SimpleCommandExceptionType NO_AREA_PAINT_DATA = new SimpleCommandExceptionType(
            Component.literal("该 UUID/ID 没有已保存的区域油漆数据")
    );
    private static final SimpleCommandExceptionType INVALID_PLAY_COUNT = new SimpleCommandExceptionType(
            Component.literal("播放次数只能是 -1 或大于等于 1 的整数")
    );
    private static final SimpleCommandExceptionType INVALID_END_ACTION = new SimpleCommandExceptionType(
            Component.literal("结束行为只能是 start、end、remove")
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
                .then(createAreaBranch());
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createPaintBranch() {
        return Commands.literal("paint")
                .then(Commands.literal("add")
                        .then(createStaticAddBranch())
                        .then(createScaleAddBranch())
                        .then(createColorAddBranch())
                        .then(createComboAddBranch()))
                .then(Commands.literal("remove")
                        .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(DyeingCommand::removePaint))));
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
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(DyeingCommand::removeArea))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createStaticAddBranch() {
        return Commands.literal("static")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                                .then(Commands.argument("hex_color", StringArgumentType.word())
                                        .executes(context -> addStatic(context, 1.0F))
                                        .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F))
                                                .executes(context -> addStatic(context, FloatArgumentType.getFloat(context, "scale")))
                                                .then(createOffsetArguments(ctx -> addStatic(ctx, FloatArgumentType.getFloat(ctx, "scale")))))
                                        .then(createOffsetArguments(ctx -> addStatic(ctx, 1.0F))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createScaleAddBranch() {
        var idArg = Commands.argument("id", StringArgumentType.word());
        var entityArg = Commands.argument("entity_uuid", UuidArgument.uuid());
        var colorArg = Commands.argument("hex_color", StringArgumentType.word());
        var scaleFromArg = Commands.argument("scale_from", FloatArgumentType.floatArg(0.01F));
        var scaleToArg = Commands.argument("scale_to", FloatArgumentType.floatArg(0.01F));
        var alphaFromArg = Commands.argument("alpha_from", FloatArgumentType.floatArg(0.0F, 1.0F));
        var alphaToArg = Commands.argument("alpha_to", FloatArgumentType.floatArg(0.0F, 1.0F));
        var periodArg = Commands.argument("period", IntegerArgumentType.integer(1));
        periodArg.executes(DyeingCommand::addScaleAnimation);
        periodArg.then(createOffsetThenSingleAnimationOptions("play_count", "end_action", DyeingCommand::addScaleAnimation));
        periodArg.then(createSingleAnimationOptions("play_count", "end_action", DyeingCommand::addScaleAnimation));
        alphaToArg.then(periodArg);
        alphaFromArg.then(alphaToArg);
        scaleToArg.then(alphaFromArg);
        scaleFromArg.then(scaleToArg);
        colorArg.then(scaleFromArg);
        entityArg.then(colorArg);
        idArg.then(entityArg);
        return Commands.literal("scale").then(idArg);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createColorAddBranch() {
        return Commands.literal("color")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                                .then(Commands.argument("color_from", StringArgumentType.word())
                                        .then(Commands.argument("color_to", StringArgumentType.word())
                                                .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F))
                                                        .then(Commands.argument("period", IntegerArgumentType.integer(1))
                                                                .executes(DyeingCommand::addColorAnimation)
                                                                .then(createOffsetThenSingleAnimationOptions("play_count", "end_action", DyeingCommand::addColorAnimation))
                                                                .then(createSingleAnimationOptions("play_count", "end_action", DyeingCommand::addColorAnimation))
                                                ))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createComboAddBranch() {
        var idArg = Commands.argument("id", StringArgumentType.word());
        var entityArg = Commands.argument("entity_uuid", UuidArgument.uuid());
        var colorFromArg = Commands.argument("color_from", StringArgumentType.word());
        var colorToArg = Commands.argument("color_to", StringArgumentType.word());
        var scaleFromArg = Commands.argument("scale_from", FloatArgumentType.floatArg(0.01F));
        var scaleToArg = Commands.argument("scale_to", FloatArgumentType.floatArg(0.01F));
        var alphaFromArg = Commands.argument("alpha_from", FloatArgumentType.floatArg(0.0F, 1.0F));
        var alphaToArg = Commands.argument("alpha_to", FloatArgumentType.floatArg(0.0F, 1.0F));
        var scalePeriodArg = Commands.argument("scale_period", IntegerArgumentType.integer(1));
        var colorPeriodArg = Commands.argument("color_period", IntegerArgumentType.integer(1));
        colorPeriodArg.executes(DyeingCommand::addCombinedAnimation);
        colorPeriodArg.then(createOffsetThenComboAnimationOptions(DyeingCommand::addCombinedAnimation));
        colorPeriodArg.then(createComboAnimationOptions(DyeingCommand::addCombinedAnimation));
        scalePeriodArg.then(colorPeriodArg);
        alphaToArg.then(scalePeriodArg);
        alphaFromArg.then(alphaToArg);
        scaleToArg.then(alphaFromArg);
        scaleFromArg.then(scaleToArg);
        colorToArg.then(scaleFromArg);
        colorFromArg.then(colorToArg);
        entityArg.then(colorFromArg);
        idArg.then(entityArg);
        return Commands.literal("combo").then(idArg);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createOffsetArguments(Command<CommandSourceStack> command) {
        return Commands.argument("offset_x", FloatArgumentType.floatArg())
                .then(Commands.argument("offset_y", FloatArgumentType.floatArg())
                        .then(Commands.argument("offset_z", FloatArgumentType.floatArg())
                                .executes(command)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createSingleAnimationOptions(
            String playCountName,
            String endActionName,
            Command<CommandSourceStack> command
    ) {
        return Commands.argument(playCountName, IntegerArgumentType.integer(-1))
                .executes(command)
                .then(Commands.argument(endActionName, StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"start", "end", "remove"}, builder))
                        .executes(command));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createOffsetThenSingleAnimationOptions(
            String playCountName,
            String endActionName,
            Command<CommandSourceStack> command
    ) {
        return Commands.argument("offset_x", FloatArgumentType.floatArg())
                .then(Commands.argument("offset_y", FloatArgumentType.floatArg())
                        .then(Commands.argument("offset_z", FloatArgumentType.floatArg())
                                .executes(command)
                                .then(createSingleAnimationOptions(playCountName, endActionName, command))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createComboAnimationOptions(Command<CommandSourceStack> command) {
        return Commands.argument("scale_play_count", IntegerArgumentType.integer(-1))
                .executes(command)
                .then(Commands.argument("scale_end_action", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"start", "end", "remove"}, builder))
                        .executes(command)
                        .then(Commands.argument("color_play_count", IntegerArgumentType.integer(-1))
                                .executes(command)
                                .then(Commands.argument("color_end_action", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"start", "end", "remove"}, builder))
                                        .executes(command))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createOffsetThenComboAnimationOptions(Command<CommandSourceStack> command) {
        return Commands.argument("offset_x", FloatArgumentType.floatArg())
                .then(Commands.argument("offset_y", FloatArgumentType.floatArg())
                        .then(Commands.argument("offset_z", FloatArgumentType.floatArg())
                                .executes(command)
                                .then(createComboAnimationOptions(command))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaStaticAddBranch() {
        return Commands.literal("static")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(createAreaBoundsArguments(
                                Commands.argument("hex_color", StringArgumentType.word())
                                        .executes(context -> addAreaStatic(context, 1.0F))
                                        .then(createAreaRotationOptions(context -> addAreaStatic(context, 1.0F)))
                                        .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F))
                                                .executes(context -> addAreaStatic(context, FloatArgumentType.getFloat(context, "scale")))
                                                .then(createAreaRotationOptions(context -> addAreaStatic(context, FloatArgumentType.getFloat(context, "scale")))))
                        )));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaScaleAddBranch() {
        return Commands.literal("scale")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(createAreaBoundsArguments(
                                Commands.argument("hex_color", StringArgumentType.word())
                                        .then(Commands.argument("scale_from", FloatArgumentType.floatArg(0.01F))
                                                .then(Commands.argument("scale_to", FloatArgumentType.floatArg(0.01F))
                                                        .then(Commands.argument("alpha_from", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                .then(Commands.argument("alpha_to", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                        .then(Commands.argument("period", IntegerArgumentType.integer(1))
                                                                                .executes(DyeingCommand::addAreaScaleAnimation)
                                                                                .then(createAreaRotationOptions(DyeingCommand::addAreaScaleAnimation))
                                                                                .then(createAreaSingleAnimationOptions("play_count", "end_action", DyeingCommand::addAreaScaleAnimation))))))))
                        ));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaColorAddBranch() {
        return Commands.literal("color")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(createAreaBoundsArguments(
                                Commands.argument("color_from", StringArgumentType.word())
                                        .then(Commands.argument("color_to", StringArgumentType.word())
                                                .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F))
                                                        .then(Commands.argument("period", IntegerArgumentType.integer(1))
                                                                .executes(DyeingCommand::addAreaColorAnimation)
                                                                .then(createAreaRotationOptions(DyeingCommand::addAreaColorAnimation))
                                                                .then(createAreaSingleAnimationOptions("play_count", "end_action", DyeingCommand::addAreaColorAnimation))))))
                        ));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaComboAddBranch() {
        return Commands.literal("combo")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(createAreaBoundsArguments(
                                Commands.argument("color_from", StringArgumentType.word())
                                        .then(Commands.argument("color_to", StringArgumentType.word())
                                                .then(Commands.argument("scale_from", FloatArgumentType.floatArg(0.01F))
                                                        .then(Commands.argument("scale_to", FloatArgumentType.floatArg(0.01F))
                                                                .then(Commands.argument("alpha_from", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                        .then(Commands.argument("alpha_to", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                                                .then(Commands.argument("scale_period", IntegerArgumentType.integer(1))
                                                                                        .then(Commands.argument("color_period", IntegerArgumentType.integer(1))
                                                                                                .executes(DyeingCommand::addAreaCombinedAnimation)
                                                                                                .then(createAreaRotationOptions(DyeingCommand::addAreaCombinedAnimation))
                                                                                                .then(createAreaComboAnimationOptions(DyeingCommand::addAreaCombinedAnimation))))))))))
                        ));
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
        String id = StringArgumentType.getString(context, "id");
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        LivingEntity entity = findLoadedLivingEntity(context.getSource(), entityUuid);
        String colorInput = StringArgumentType.getString(context, "hex_color");
        int argb = ColorParser.parse(colorInput);

        PaintData paintData = PaintData.staticPaint(argb, scale, getOffsetX(context), getOffsetY(context), getOffsetZ(context), getCurrentGameTime(context));
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        savedData.put(entityUuid, id, paintData);
        DyeingMod.broadcastUpdate(entityUuid, id, paintData);

        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 添加油漆层 id=" + id + "，颜色=" + formatArgb(argb) + "，缩放=" + scale),
                true
        );
        return 1;
    }

    private static int addScaleAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "id");
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
                getOffsetZ(context),
                getCurrentGameTime(context),
                getOptionalPlayCount(context, "play_count"),
                getOptionalEndAction(context, "end_action")
        );
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        savedData.put(entityUuid, id, paintData);
        DyeingMod.broadcastUpdate(entityUuid, id, paintData);

        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 添加缩放动画油漆层 id=" + id),
                true
        );
        return 1;
    }

    private static int addColorAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "id");
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
                getOffsetZ(context),
                getCurrentGameTime(context),
                getOptionalPlayCount(context, "play_count"),
                getOptionalEndAction(context, "end_action")
        );
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        savedData.put(entityUuid, id, paintData);
        DyeingMod.broadcastUpdate(entityUuid, id, paintData);

        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 添加颜色渐变油漆层 id=" + id),
                true
        );
        return 1;
    }

    private static int addCombinedAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "id");
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
                getOffsetZ(context),
                getCurrentGameTime(context),
                getOptionalPlayCount(context, "scale_play_count"),
                getOptionalEndAction(context, "scale_end_action"),
                getOptionalPlayCount(context, "color_play_count"),
                getOptionalEndAction(context, "color_end_action")
        );
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        savedData.put(entityUuid, id, paintData);
        DyeingMod.broadcastUpdate(entityUuid, id, paintData);

        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 添加组合动画油漆层 id=" + id),
                true
        );
        return 1;
    }

    private static int removePaint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        String id = StringArgumentType.getString(context, "id");
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        if (!savedData.remove(entityUuid, id)) {
            throw NO_PAINT_DATA.create();
        }

        DyeingMod.broadcastRemove(entityUuid, id);
        context.getSource().sendSuccess(() -> Component.literal("已移除实体 " + entityUuid + " 的油漆层 id=" + id), true);
        return 1;
    }

    private static int addAreaStatic(CommandContext<CommandSourceStack> context, float scale) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "id");
        Entity entity = findLoadedEntity(context.getSource(), UuidArgument.getUuid(context, "entity_uuid"));
        int argb = ColorParser.parse(StringArgumentType.getString(context, "hex_color"));
        PaintData paintData = PaintData.staticPaint(argb, scale, 0.0F, 0.0F, 0.0F, getCurrentGameTime(context));
        AreaPaintData areaPaintData = createAreaPaintData(context, paintData);
        AreaPaintSavedData savedData = DyeingMod.getAreaPaintData(context.getSource().getServer());
        savedData.put(entity.getUUID(), id, areaPaintData);
        DyeingMod.broadcastAreaUpdate(entity.getUUID(), id, areaPaintData);
        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 绑定静态区域油漆 id=" + id),
                true
        );
        return 1;
    }

    private static int addAreaScaleAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "id");
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
                0.0F,
                getCurrentGameTime(context),
                getOptionalPlayCount(context, "play_count"),
                getOptionalEndAction(context, "end_action")
        );
        AreaPaintData areaPaintData = createAreaPaintData(context, paintData);
        AreaPaintSavedData savedData = DyeingMod.getAreaPaintData(context.getSource().getServer());
        savedData.put(entity.getUUID(), id, areaPaintData);
        DyeingMod.broadcastAreaUpdate(entity.getUUID(), id, areaPaintData);
        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 绑定缩放动画区域油漆 id=" + id),
                true
        );
        return 1;
    }

    private static int addAreaColorAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "id");
        Entity entity = findLoadedEntity(context.getSource(), UuidArgument.getUuid(context, "entity_uuid"));
        PaintData paintData = PaintData.withColorAnimation(
                ColorParser.parse(StringArgumentType.getString(context, "color_from")),
                ColorParser.parse(StringArgumentType.getString(context, "color_to")),
                FloatArgumentType.getFloat(context, "scale"),
                IntegerArgumentType.getInteger(context, "period"),
                0.0F,
                0.0F,
                0.0F,
                getCurrentGameTime(context),
                getOptionalPlayCount(context, "play_count"),
                getOptionalEndAction(context, "end_action")
        );
        AreaPaintData areaPaintData = createAreaPaintData(context, paintData);
        AreaPaintSavedData savedData = DyeingMod.getAreaPaintData(context.getSource().getServer());
        savedData.put(entity.getUUID(), id, areaPaintData);
        DyeingMod.broadcastAreaUpdate(entity.getUUID(), id, areaPaintData);
        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 绑定颜色渐变区域油漆 id=" + id),
                true
        );
        return 1;
    }

    private static int addAreaCombinedAnimation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "id");
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
                0.0F,
                getCurrentGameTime(context),
                getOptionalPlayCount(context, "scale_play_count"),
                getOptionalEndAction(context, "scale_end_action"),
                getOptionalPlayCount(context, "color_play_count"),
                getOptionalEndAction(context, "color_end_action")
        );
        AreaPaintData areaPaintData = createAreaPaintData(context, paintData);
        AreaPaintSavedData savedData = DyeingMod.getAreaPaintData(context.getSource().getServer());
        savedData.put(entity.getUUID(), id, areaPaintData);
        DyeingMod.broadcastAreaUpdate(entity.getUUID(), id, areaPaintData);
        context.getSource().sendSuccess(
                () -> Component.literal("已为实体 " + entity.getName().getString() + " 绑定组合动画区域油漆 id=" + id),
                true
        );
        return 1;
    }

    private static int removeArea(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        String id = StringArgumentType.getString(context, "id");
        AreaPaintSavedData savedData = DyeingMod.getAreaPaintData(context.getSource().getServer());
        if (!savedData.remove(entityUuid, id)) {
            throw NO_AREA_PAINT_DATA.create();
        }
        DyeingMod.broadcastAreaRemove(entityUuid, id);
        context.getSource().sendSuccess(() -> Component.literal("已移除实体 " + entityUuid + " 的区域油漆 id=" + id), true);
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

    private static int getOptionalPlayCount(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        int value;
        try {
            value = IntegerArgumentType.getInteger(context, name);
        } catch (IllegalArgumentException exception) {
            return -1;
        }

        if (value == 0 || value < -1) {
            throw INVALID_PLAY_COUNT.create();
        }
        return value;
    }

    private static AnimationEndAction getOptionalEndAction(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String value;
        try {
            value = StringArgumentType.getString(context, name);
        } catch (IllegalArgumentException exception) {
            return AnimationEndAction.END;
        }

        try {
            return AnimationEndAction.fromCommandName(value);
        } catch (IllegalArgumentException exception) {
            throw INVALID_END_ACTION.create();
        }
    }

    private static long getCurrentGameTime(CommandContext<CommandSourceStack> context) {
        return context.getSource().getServer().overworld().getGameTime();
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
                paintData,
                getOptionalRotationPeriod(context),
                getOptionalRotationMode(context)
        );
    }

    private static int getOptionalRotationPeriod(CommandContext<CommandSourceStack> context) {
        try {
            return IntegerArgumentType.getInteger(context, "rotation_period");
        } catch (IllegalArgumentException exception) {
            return 0;
        }
    }

    private static int getOptionalRotationMode(CommandContext<CommandSourceStack> context) {
        try {
            int value = IntegerArgumentType.getInteger(context, "rotation_mode");
            if (value == 0) {
                return -1;
            }
            return value;
        } catch (IllegalArgumentException exception) {
            return -1;
        }
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaRotationOptions(Command<CommandSourceStack> command) {
        return Commands.argument("rotation_period", IntegerArgumentType.integer())
                .executes(command)
                .then(Commands.argument("rotation_mode", IntegerArgumentType.integer(-1))
                        .executes(command));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaSingleAnimationOptions(
            String playCountName,
            String endActionName,
            Command<CommandSourceStack> command
    ) {
        return Commands.argument(playCountName, IntegerArgumentType.integer(-1))
                .executes(command)
                .then(createAreaRotationOptions(command))
                .then(Commands.argument(endActionName, StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"start", "end", "remove"}, builder))
                        .executes(command)
                        .then(createAreaRotationOptions(command)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createAreaComboAnimationOptions(Command<CommandSourceStack> command) {
        return Commands.argument("scale_play_count", IntegerArgumentType.integer(-1))
                .executes(command)
                .then(createAreaRotationOptions(command))
                .then(Commands.argument("scale_end_action", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"start", "end", "remove"}, builder))
                        .executes(command)
                        .then(createAreaRotationOptions(command))
                        .then(Commands.argument("color_play_count", IntegerArgumentType.integer(-1))
                                .executes(command)
                                .then(createAreaRotationOptions(command))
                                .then(Commands.argument("color_end_action", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(new String[]{"start", "end", "remove"}, builder))
                                        .executes(command)
                                        .then(createAreaRotationOptions(command)))));
    }
}
