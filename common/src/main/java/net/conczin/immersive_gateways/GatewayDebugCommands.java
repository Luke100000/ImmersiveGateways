package net.conczin.immersive_gateways;

import com.mojang.brigadier.CommandDispatcher;
import net.conczin.immersive_gateways.data.PortalDataManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

public class GatewayDebugCommands {
    private static final Map<UUID, PendingStart> STARTS = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gateway")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start").executes(context -> start(context.getSource())))
                .then(Commands.literal("finish").executes(context -> finish(context.getSource())))
                .then(Commands.literal("detect").executes(context -> detect(context.getSource()))));
    }

    public static void reset() {
        STARTS.clear();
    }

    private static int start(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos pos = getLookedGateway(player);
        if (pos == null) {
            source.sendFailure(Component.literal("Look at a gateway block first."));
            return 0;
        }

        STARTS.put(player.getUUID(), new PendingStart(player.level().dimension(), pos));
        source.sendSuccess(() -> Component.literal("Gateway start set to " + format(pos) + "."), false);
        return 1;
    }

    private static int finish(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BlockPos finish = getLookedGateway(player);
        if (finish == null) {
            source.sendFailure(Component.literal("Look at a gateway block first."));
            return 0;
        }

        PendingStart start = STARTS.get(player.getUUID());
        if (start == null) {
            source.sendFailure(Component.literal("Run /gateway start first."));
            return 0;
        }
        if (start.dimension() != player.level().dimension()) {
            source.sendFailure(Component.literal("Gateway start is in another dimension. Run /gateway start here first."));
            return 0;
        }

        PortalDataManager.addManualConnection(
                player.serverLevel(),
                start.pos(),
                finish
        );
        STARTS.remove(player.getUUID());

        source.sendSuccess(() -> Component.literal("Added gateway connection."), true);
        return 1;
    }

    private static int detect(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos pos = getLookedGateway(player);
        if (pos == null) {
            source.sendFailure(Component.literal("Look at a gateway block first."));
            return 0;
        }

        PortalDataManager.PortalPair pair = PortalDataManager.search(level, pos, false);
        if (pair == null) {
            source.sendFailure(Component.literal("No saved gateway connection for " + format(pos) + "."));
            return 0;
        }

        BoundingBox target = pair.getTarget(pos).boundingBox();
        source.sendSuccess(() -> Component.literal("Gateway at " + format(pos) + " points to " + format(target.getCenter()) + "."), false);
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

    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private record PendingStart(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
