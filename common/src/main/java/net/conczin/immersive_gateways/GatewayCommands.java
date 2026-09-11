package net.conczin.immersive_gateways;

import com.mojang.brigadier.CommandDispatcher;
import net.conczin.immersive_gateways.config.Config;
import net.conczin.immersive_gateways.data.PortalDataManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GatewayCommands {
    private static final Map<UUID, PendingStart> STARTS = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gateway")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("automatic_generation")
                        .then(Commands.literal("on").executes(context -> setAutomaticGeneration(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setAutomaticGeneration(context.getSource(), false))))
                .then(Commands.literal("start").executes(context -> start(context.getSource())))
                .then(Commands.literal("finish").executes(context -> finish(context.getSource())))
                .then(Commands.literal("detect").executes(context -> detect(context.getSource()))));
    }

    public static void reset() {
        STARTS.clear();
    }

    private static int setAutomaticGeneration(CommandSourceStack source, boolean enabled) {
        Config config = Config.getInstance();
        config.generatePortalsAutomatically = enabled;
        config.save();
        source.sendSuccess(() -> Component.translatable(
                "immersive_gateways.command.automatic_generation",
                Component.translatable(enabled ? "options.on" : "options.off")
        ), true);
        return 1;
    }

    private static int start(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos pos = getLookedGateway(player);
        if (pos == null) {
            source.sendFailure(Component.translatable("immersive_gateways.command.look_at_gateway"));
            return 0;
        }

        STARTS.put(player.getUUID(), new PendingStart(player.level().dimension(), pos));
        source.sendSuccess(() -> Component.translatable("immersive_gateways.command.start", tp(pos)), false);
        return 1;
    }

    private static int finish(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos finish = getLookedGateway(player);
        if (finish == null) {
            source.sendFailure(Component.translatable("immersive_gateways.command.look_at_gateway"));
            return 0;
        }

        PendingStart start = STARTS.get(player.getUUID());
        if (start == null) {
            source.sendFailure(Component.translatable("immersive_gateways.command.start_required"));
            return 0;
        }
        if (start.dimension() != player.level().dimension()) {
            source.sendFailure(Component.translatable("immersive_gateways.command.dimension_mismatch"));
            return 0;
        }

        PortalDataManager.addManualConnection(
                player.level(),
                start.pos(),
                finish
        );
        STARTS.remove(player.getUUID());

        source.sendSuccess(() -> Component.translatable("immersive_gateways.command.finish"), true);
        return 1;
    }

    private static int detect(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.level();
        BlockPos pos = getLookedGateway(player);
        if (pos == null) {
            source.sendFailure(Component.translatable("immersive_gateways.command.look_at_gateway"));
            return 0;
        }

        PortalDataManager.PortalPair pair = PortalDataManager.search(level, pos, false);
        if (pair == null) {
            source.sendFailure(Component.translatable("immersive_gateways.command.detect_missing", tp(pos)));
            return 0;
        }

        BoundingBox target = pair.getTarget(pos).boundingBox();
        source.sendSuccess(() -> Component.translatable("immersive_gateways.command.detect", tp(pos), tp(target.getCenter())), false);
        return 1;
    }

    private static BlockPos getLookedGateway(ServerPlayer player) {
        HitResult hit = player.pick(20.0, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        return player.level().getBlockState(pos).is(Blocks.GATEWAY) ? pos : null;
    }

    private static Component tp(BlockPos pos) {
        String command = "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        return ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ()))
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.SuggestCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip"))));
    }

    private record PendingStart(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
