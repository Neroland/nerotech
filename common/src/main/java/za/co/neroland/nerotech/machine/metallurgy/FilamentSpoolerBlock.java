package za.co.neroland.nerotech.machine.metallurgy;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.BlockCodecs;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;

/** Filament Spooler block: directional, ticks its {@link FilamentSpoolerBlockEntity}. */
public class FilamentSpoolerBlock extends NeroTechMachineBlock {

    public static final MapCodec<FilamentSpoolerBlock> CODEC = BlockCodecs.simple(FilamentSpoolerBlock::new);

    public FilamentSpoolerBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<FilamentSpoolerBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilamentSpoolerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return MetallurgyContent.FILAMENT_SPOOLER_BE.get();
    }
}
