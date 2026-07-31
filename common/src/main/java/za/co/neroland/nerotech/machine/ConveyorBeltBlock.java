package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Conveyor Belt — the cheapest automation in NeroTech and the one block here with <b>no block
 * entity at all</b>: a flat, 4-pixel-high directional plate that nudges item entities riding it
 * along its {@link #FACING} direction. Chains of belts form lines and corners naturally, because
 * each belt only ever pushes along its own facing — there is no belt "network", no path-finding
 * and no per-tick world scan.
 *
 * <p>Deliberately dumb: it applies motion and nothing else. No item merging, no pickup, no
 * inventory insertion — feed the end of a line into a hopper, a machine face or a
 * {@link RoboticArmBlockEntity Robotic Arm} for that. Only {@link ItemEntity} is moved; mobs and
 * players walk over a belt normally.
 *
 * <p>It is also the mod's main <b>Iron Dust sink</b>: six belts per craft.
 */
public class ConveyorBeltBlock extends Block {

    public static final MapCodec<ConveyorBeltBlock> CODEC = simpleCodec(ConveyorBeltBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Belt speed, blocks per tick — brisk enough to read, slow enough not to fling items off corners. */
    private static final double SPEED = 0.15D;

    /** Flat plate: the bottom 4 pixels of the block (a half-slab is 8 — a belt sits lower). */
    private static final VoxelShape SHAPE = Block.column(16.0D, 0.0D, 4.0D);

    @SuppressWarnings("this-escape")
    public ConveyorBeltBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<ConveyorBeltBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Placed facing AWAY from the player, so a belt run builds in the direction you are walking. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * The whole belt, in one hook: add {@link #SPEED} along the facing axis to any item entity
     * inside this block's cell, clamped so a chain of belts never accelerates an item past the
     * belt speed. Server-side only — motion is authoritative there and rides the normal entity
     * position sync, so no packet of our own is needed.
     *
     * <p>Nothing else happens here: no neighbour lookups, no block-above checks, no item merging.
     * A belt under a solid block simply carries items that are already on it.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide() || !(entity instanceof ItemEntity)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        Vec3 motion = entity.getDeltaMovement();
        double x = facing.getStepX() == 0 ? motion.x : clamp(motion.x + facing.getStepX() * SPEED);
        double z = facing.getStepZ() == 0 ? motion.z : clamp(motion.z + facing.getStepZ() * SPEED);
        if (x != motion.x || z != motion.z) {
            entity.setDeltaMovement(x, motion.y, z);
        }
    }

    /** Cap a belt-driven axis at the belt speed in either direction. */
    private static double clamp(double value) {
        return Math.max(-SPEED, Math.min(SPEED, value));
    }
}
