package za.co.neroland.nerotech.machine.foundry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.multiblock.ShellBlock;

/**
 * Foundry Heat Port: a shell block ({@code #nerotech:foundry_ports}) that takes heat from whatever is
 * next to it (Heat Pipes, Heat Exchangers) into a small buffer the Blast Foundry controller drains.
 * The only way heat enters a foundry. Passive: its block entity never ticks.
 */
public class FoundryHeatPortBlock extends ShellBlock implements EntityBlock {

    public static final MapCodec<FoundryHeatPortBlock> CODEC = BlockCodecs.simple(FoundryHeatPortBlock::new);

    public FoundryHeatPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FoundryHeatPortBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundryHeatPortBlockEntity(pos, state);
    }
}
