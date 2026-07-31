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
 * Solar Array block — directional, ticks its {@link SolarArrayBlockEntity}.
 *
 * <p>Deliberately distinct from Nerospace's tiered Solar Panels: this is NeroTech's cheap,
 * single-block Earth-tier panel (low output, upgrade modules, side config, no pooling). Nerospace's
 * panels are the scalable solar line (three tiers, array pooling, airless 2×); both feed the same
 * Core energy network, so they coexist rather than duplicate. The niche tooltip lives on the
 * block-item (registered in {@code ModItems}), since 26.x blocks expose no hover-text hook.
 */
public class SolarArrayBlock extends NeroTechMachineBlock {

    public static final MapCodec<SolarArrayBlock> CODEC = simpleCodec(SolarArrayBlock::new);

    public SolarArrayBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SolarArrayBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarArrayBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractMachineBlockEntity> machineType() {
        return ModBlockEntities.SOLAR_ARRAY.get();
    }
}
