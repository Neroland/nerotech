package za.co.neroland.nerotech.machine.multiblock;

import net.minecraft.world.level.block.state.BlockState;

/**
 * The Blast Foundry: a 5³ or 3³ hollow cube of {@code #nerotech:foundry_shell}, with
 * {@code #nerotech:foundry_ports} blocks allowed anywhere in the shell, and the controller at the centre of
 * one vertical wall facing outward. The interior is air or a {@code #nerotech:foundry_interior} block (the
 * molten-batch display).
 */
public final class FoundryShape implements StructureShape {

    public static final FoundryShape INSTANCE = new FoundryShape();

    private static final int[] SIZES = {5, 3};

    private FoundryShape() {
    }

    @Override
    public int[] sizes() {
        return SIZES.clone();
    }

    @Override
    public boolean isShell(BlockState state) {
        return state.is(MultiblockTags.FOUNDRY_SHELL);
    }

    @Override
    public boolean isInterior(BlockState state) {
        return state.isAir() || state.is(MultiblockTags.FOUNDRY_INTERIOR);
    }

    @Override
    public boolean isPort(BlockState state) {
        return state.is(MultiblockTags.FOUNDRY_PORTS);
    }
}
