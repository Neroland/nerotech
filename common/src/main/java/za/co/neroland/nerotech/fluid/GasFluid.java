package za.co.neroland.nerotech.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import za.co.neroland.nerotech.gas.NeroTechGases;

/**
 * A NeroTech gas seen as a Minecraft {@link Fluid} — the transport identity of
 * {@code nerotech:hydrogen} / {@code nerospace:oxygen} (fluid ids {@code nerotech:hydrogen} /
 * {@code nerotech:oxygen}), so any mod's fluid pipe can carry them.
 *
 * <p>It is deliberately <b>not placeable</b>: there is no fluid block, no flowing variant, no
 * bucket, no world physics. A gas exists only inside tanks and pipes, which is exactly what a
 * transport identity needs — the gameplay contract stays Core's
 * {@link za.co.neroland.nerolandcore.gas.NeroGasStorage}, and this class is only how that contract
 * shows up on the loaders' standard fluid capability.
 *
 * <p>Loaders that demand a {@code FluidType} (NeoForge, Forge) subclass this in their own module
 * and attach one; Fabric uses it as-is. See {@link NeroTechFluids}.
 */
public class GasFluid extends Fluid {

    private final Identifier gas;

    public GasFluid(Identifier gas) {
        this.gas = NeroTechGases.canonical(gas);
    }

    /** The Core gas id this fluid is the transport identity of. */
    public Identifier gas() {
        return this.gas;
    }

    @Override
    public Item getBucket() {
        return Items.AIR; // a gas is never bottled into a bucket
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid,
            Direction direction) {
        return true;
    }

    @Override
    protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
        return Vec3.ZERO;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
        return 0.0F;
    }

    @Override
    public float getOwnHeight(FluidState state) {
        return 0.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState state) {
        return true;
    }

    @Override
    public int getAmount(FluidState state) {
        return 0;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
