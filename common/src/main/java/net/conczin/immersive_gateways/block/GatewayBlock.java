package net.conczin.immersive_gateways.block;

import com.mojang.serialization.MapCodec;
import net.conczin.immersive_gateways.BlockEntityTypes;
import net.conczin.immersive_gateways.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GatewayBlock extends BaseEntityBlock {
    public static final MapCodec<GatewayBlock> CODEC = simpleCodec(GatewayBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 14.0);
    protected static final VoxelShape Y_AXIS_AABB = Block.box(0.0, 2.0, 0.0, 16.0, 14.0, 16.0);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(2.0, 0.0, 0.0, 14.0, 16.0, 16.0);

    public GatewayBlock(BlockBehaviour.Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<GatewayBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GatewayBlockEntity(pos, state);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, BlockEntityTypes.GATEWAY, getTicker(level));
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level) {
        return level.isClientSide ? (l, p, s, b) -> GatewayBlockEntity.clientTick(l, p, s, (GatewayBlockEntity) b) : (l, p, s, b) -> GatewayBlockEntity.serverTick((ServerLevel) l, p, s, (GatewayBlockEntity) b);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X_AXIS_AABB;
            case Y -> Y_AXIS_AABB;
            case Z -> Z_AXIS_AABB;
        };
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel serverLevel && canEntityTeleport(entity)) {
            if (!entity.getRootVehicle().isOnPortalCooldown()) {
                GatewayBlockEntity.teleportEntity(serverLevel, pos, entity);
            }
            entity.getRootVehicle().setPortalCooldown(20);
        }
    }

    public static boolean canEntityTeleport(Entity entity) {
        return EntitySelector.NO_SPECTATORS.test(entity) && (!Config.getInstance().onlyPlayersCanTeleport || entity instanceof Player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; ++i) {
            double x = (double) pos.getX() + random.nextDouble();
            double y = (double) pos.getY() + random.nextDouble();
            double z = (double) pos.getZ() + random.nextDouble();
            double ox = ((double) random.nextFloat() - 0.5) * 0.5;
            double oy = ((double) random.nextFloat() - 0.5) * 0.5;
            double oz = ((double) random.nextFloat() - 0.5) * 0.5;
            int k = random.nextInt(2) * 2 - 1;
            if (level.getBlockState(pos.west()).is(this) || level.getBlockState(pos.east()).is(this)) {
                z = (double) pos.getZ() + 0.5 + 0.25 * (double) k;
                oz = random.nextFloat() * 2.0f * (float) k;
            } else {
                x = (double) pos.getX() + 0.5 + 0.25 * (double) k;
                ox = random.nextFloat() * 2.0f * (float) k;
            }
            level.addParticle(ParticleTypes.PORTAL, x, y, z, ox, oy, oz);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> {
                switch (state.getValue(AXIS)) {
                    case X: {
                        yield state.setValue(AXIS, Direction.Axis.Z);
                    }
                    case Z: {
                        yield state.setValue(AXIS, Direction.Axis.X);
                    }
                }
                yield state;
            }
            default -> state;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
