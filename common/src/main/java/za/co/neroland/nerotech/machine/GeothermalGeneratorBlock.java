package za.co.neroland.nerotech.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Geothermal Generator block (Stage D) — directional, ticks its
 * {@link GeothermalGeneratorBlockEntity}. Neighbour changes reach the block entity through the base
 * class's thermal-link invalidation, which the generator also uses to drop its heat-source cache.
 */
public class GeothermalGeneratorBlock extends NeroTechMachineBlock {

    public static final MapCodec<GeothermalGeneratorBlock> CODEC = simpleCodec(GeothermalGeneratorBlock::new);

    public GeothermalGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GeothermalGeneratorBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeothermalGeneratorBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.GEOTHERMAL_GENERATOR.get();
    }
}
