package com.example.dyeing.command;

import com.example.dyeing.DyeingMod;
import com.example.dyeing.data.PaintData;
import com.example.dyeing.data.PaintSavedData;
import com.example.dyeing.util.ColorParser;
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
            Component.literal("找不到对应的已加载生物实体")
    );
    private static final SimpleCommandExceptionType NO_PAINT_DATA = new SimpleCommandExceptionType(
            Component.literal("该 UUID 没有已保存的染色数据")
    );

    private DyeingCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(createRoot("dyeing"));
        dispatcher.register(createRoot("Dyeing"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRoot(String name) {
        LiteralArgumentBuilder<CommandSourceStack> addNode = Commands.literal("add")
                .then(createScaleAddBranch())
                .then(createColorAddBranch())
                .then(createComboAddBranch())
                .then(createStaticAddBranch());

        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(addNode)
                .then(Commands.literal("remove")
                        .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                                .executes(DyeingCommand::remove)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createStaticAddBranch() {
        return Commands.argument("entity_uuid", UuidArgument.uuid())
                .then(Commands.argument("hex_color", StringArgumentType.word())
                        .executes(context -> addStatic(context, 1.0F))
                        .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F))
                                .executes(context -> addStatic(context, FloatArgumentType.getFloat(context, "scale")))));
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
                                                                        .executes(DyeingCommand::addScaleAnimation))))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> createColorAddBranch() {
        return Commands.literal("color")
                .then(Commands.argument("entity_uuid", UuidArgument.uuid())
                        .then(Commands.argument("color_from", StringArgumentType.word())
                                .then(Commands.argument("color_to", StringArgumentType.word())
                                        .then(Commands.argument("scale", FloatArgumentType.floatArg(0.01F))
                                                .then(Commands.argument("period", IntegerArgumentType.integer(1))
                                                        .executes(DyeingCommand::addColorAnimation))))));
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
                                                                                        .executes(DyeingCommand::addCombinedAnimation))))))))));
    }

    private static int addStatic(CommandContext<CommandSourceStack> context, float scale) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        LivingEntity entity = findLoadedLivingEntity(context.getSource(), entityUuid);
        String colorInput = StringArgumentType.getString(context, "hex_color");
        int argb = ColorParser.parse(colorInput);

        PaintData paintData = PaintData.staticPaint(argb, scale);
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

        PaintData paintData = PaintData.withScaleAnimation(argb, scaleFrom, scaleTo, alphaFrom, alphaTo, period);
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

        PaintData paintData = PaintData.withColorAnimation(colorFrom, colorTo, scale, period);
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
                colorPeriod
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

    private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID entityUuid = UuidArgument.getUuid(context, "entity_uuid");
        PaintSavedData savedData = DyeingMod.getPaintData(context.getSource().getServer());
        if (!savedData.remove(entityUuid)) {
            throw NO_PAINT_DATA.create();
        }

        DyeingMod.broadcastRemove(entityUuid);
        context.getSource().sendSuccess(() -> Component.literal("已移除实体 " + entityUuid + " 的油漆层"), true);
        return 1;
    }

    private static LivingEntity findLoadedLivingEntity(CommandSourceStack source, UUID entityUuid) throws CommandSyntaxException {
        for (ServerLevel level : source.getServer().getAllLevels()) {
            Entity entity = level.getEntity(entityUuid);
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
            if (entity != null) {
                break;
            }
        }

        throw ENTITY_NOT_FOUND.create();
    }

    private static String formatArgb(int argb) {
        return "0x" + String.format(Locale.ROOT, "%08X", argb);
    }
}
