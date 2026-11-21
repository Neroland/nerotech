package za.co.neroland.nerotech.machine.foundry;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.multiblock.ShellBlock;

/**
 * Foundry Brick: the Blast Foundry's shell ({@code #nerotech:foundry_shell}). No block entity. The
 * {@link #LIT} property is set shell-wide by the controller, only when its formed-and-running state
 * changes (never per tick), and swaps the brick to its warm-mortar model.
 */
public class FoundryBrickBlock extends ShellBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public static final MapCodec<FoundryBrickBlock> CODEC = BlockCodecs.simple(FoundryBrickBlock::new);

    @SuppressWarnings("this-escape")
    public FoundryBrickBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected MapCodec<FoundryBrickBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    /** Light from a lit brick (warm mortar), none from a cold one. */
    public static int lightLevel(BlockState state) {
        return state.hasProperty(LIT) && state.getValue(LIT) ? 7 : 0;
    }
}
