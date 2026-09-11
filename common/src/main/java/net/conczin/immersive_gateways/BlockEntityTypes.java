package net.conczin.immersive_gateways;

import net.conczin.immersive_gateways.block.GatewayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityTypes {
    public static BlockEntityType<GatewayBlockEntity> GATEWAY;

    public interface TriFunction<E extends BlockEntity> {
        BlockEntityType<E> apply(Identifier name, BlockEntitySupplier<E> constructor, Block block);
    }

    public interface BlockEntitySupplier<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register(TriFunction register) {
        GATEWAY = register.apply(
                Common.locate("gateway"),
                GatewayBlockEntity::new,
                Blocks.GATEWAY
        );
    }
}
