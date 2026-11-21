package za.co.neroland.nerotech.machine.multiblock;

import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerotech.machine.fusion.FusionContent;
import za.co.neroland.nerotech.registry.ModBlocks;

/**
 * The fusion reactor's shell: a hollow cube of 7, 5 or 3 blocks built of {@code fusion_casing} or
 * {@code fusion_containment_glass}, air inside, the controller at the centre of one vertical wall facing
 * outward. The predicates are exactly those the reactor used before the shared base existed.
 *
 * <p>Two blocks may also stand in for a casing as ports (0.8.0): the Fuel Injector and the Europium Flux
 * Lattice. A shell with neither validates exactly as before.
 */
public final class FusionShape implements StructureShape {

    public static final FusionShape INSTANCE = new FusionShape();

    private static final int[] SIZES = {7, 5, 3};

    private FusionShape() {
    }

    @Override
    public int[] sizes() {
        return SIZES.clone();
    }

    @Override
    public boolean isShell(BlockState state) {
        return state.is(ModBlocks.FUSION_CASING.get()) || state.is(ModBlocks.FUSION_CONTAINMENT_GLASS.get());
    }

    @Override
    public boolean isInterior(BlockState state) {
        return state.isAir();
    }

    /** Fuel Injectors and Europium Flux Lattices take a casing's place and are collected as ports. */
    @Override
    public boolean isPort(BlockState state) {
        return state.is(FusionContent.FUEL_INJECTOR.get()) || state.is(FusionContent.EUROPIUM_FLUX_LATTICE.get());
    }
}
