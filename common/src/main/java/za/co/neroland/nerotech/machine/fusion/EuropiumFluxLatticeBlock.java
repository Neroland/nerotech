package za.co.neroland.nerotech.machine.fusion;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.multiblock.ShellBlock;

/**
 * Europium Flux Lattice — a fusion shell block that takes a casing's place. Each one in a formed shell
 * widens the reactor's stable heat band (less heat per burning tick), up to a cap. Fusion only: it is no
 * moderator, and nothing outside the fusion shell reads it. {@link #STABLE} is set by the reactor while
 * the lattice sits in a formed shell; no block entity, no player data.
 */
public class EuropiumFluxLatticeBlock extends ShellBlock {

    public static final MapCodec<EuropiumFluxLatticeBlock> LATTICE_CODEC =
            BlockCodecs.simple(EuropiumFluxLatticeBlock::new);

    /** True while the lattice is part of a formed fusion shell (lights the beads). */
    public static final BooleanProperty STABLE = BooleanProperty.create("stable");

    @SuppressWarnings("this-escape")
    public EuropiumFluxLatticeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STABLE, false));
    }

    // Overrides ShellBlock's own codec() (not Block#codec, which 26.3 dropped).
    @Override
    protected MapCodec<? extends ShellBlock> codec() {
        return LATTICE_CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STABLE);
    }
}
