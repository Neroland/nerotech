package za.co.neroland.nerotech.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import za.co.neroland.nerotech.fluid.NeroTechLiquid;

/**
 * Fabric implementation of a {@link NeroTechLiquid} as a vanilla {@link FlowingFluid} (NeoForge and
 * Forge use their flowing-fluid classes with a {@code FluidType} instead). Overrides mirror vanilla
 * water; the source/flowing/bucket/block resolve lazily through the liquid, so registration order
 * does not matter. Never converts to a source.
 */
public abstract class FabricLiquidFluid extends FlowingFluid {

    private final NeroTechLiquid liquid;

    protected FabricLiquidFluid(NeroTechLiquid liquid) {
        this.liquid = liquid;
    }

    @Override
    public Fluid getFlowing() {
        return this.liquid.flowing();
    }

    @Override
    public Fluid getSource() {
        return this.liquid.source();
    }

    @Override
    public Item getBucket() {
        return this.liquid.bucket();
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return this.liquid.is(fluid);
    }

    //? if >=1.21.2 {
    @Override
    protected boolean canConvertToSource(ServerLevel level) {
    //?} else {
    /*@Override
    protected boolean canConvertToSource(net.minecraft.world.level.Level level) {
    *///?}
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return this.liquid.slopeFindDistance();
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return this.liquid.levelDecreasePerBlock();
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return this.liquid.tickDelay();
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !isSame(fluid);
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return this.liquid.block().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    /** The still form. */
    public static final class Source extends FabricLiquidFluid {
        public Source(NeroTechLiquid liquid) {
            super(liquid);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    /** The flowing form. */
    public static final class Flowing extends FabricLiquidFluid {
        public Flowing(NeroTechLiquid liquid) {
            super(liquid);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
