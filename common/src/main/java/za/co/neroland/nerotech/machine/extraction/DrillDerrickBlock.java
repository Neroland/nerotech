package za.co.neroland.nerotech.machine.extraction;

import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.multiblock.ShellBlock;

/**
 * Drill Derrick — the open lattice column under a Deep Core Drill ({@code DrillShape}: three of them
 * stacked under the controller). No block entity. {@code part} is {@code lower} for the foot (no derrick
 * below) and {@code upper} above it; it changes only when the block below changes, never per tick.
 * Placing, removing or changing next to one asks the drill above to revalidate ({@link ShellBlock}).
 */
public class DrillDerrickBlock extends ShellBlock {

    public static final MapCodec<DrillDerrickBlock> CODEC = BlockCodecs.simple(DrillDerrickBlock::new);

    /** Foot or mast segment. */
    public enum Part implements StringRepresentable {
        LOWER,
        UPPER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    @SuppressWarnings("this-escape")
    public DrillDerrickBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.LOWER));
    }

    // No @Override: 26.3 dropped Block#codec (same as ShellBlock).
    protected MapCodec<? extends ShellBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(PART,
                partFor(context.getLevel().getBlockState(context.getClickedPos().below())));
    }

    /** {@code upper} when another derrick sits below, else {@code lower}. */
    public Part partFor(BlockState below) {
        return below.getBlock() instanceof DrillDerrickBlock ? Part.UPPER : Part.LOWER;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        refreshPart(state, level, pos);
    }

    @Override
    //? if >=1.21.2 {
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            @Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    //?} else {
    /*protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
            BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
    *///?}
        refreshPart(state, level, pos);
    }

    /** Set {@code part} from the block below; a no-op when it already matches (so no update loop). */
    private void refreshPart(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide() || !state.is(this)) {
            return;
        }
        Part part = partFor(level.getBlockState(pos.below()));
        if (state.getValue(PART) != part) {
            level.setBlock(pos, state.setValue(PART, part), Block.UPDATE_CLIENTS);
        }
    }
}
