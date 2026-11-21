package za.co.neroland.nerotech.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;

import za.co.neroland.nerotech.fluid.GasFluid;
import za.co.neroland.nerotech.fluid.NeroTechLiquid;
import za.co.neroland.nerotech.neoforge.NeoForgeFluidTypes;

/**
 * NeoForge {@link IFluidHelper}: NeoForge requires every fluid to name a {@link FluidType}, so the
 * common {@link GasFluid} is subclassed here to point at the type registered in
 * {@link NeoForgeFluidTypes}. Registered via {@code META-INF/services}.
 */
public final class NeoForgeFluidHelper implements IFluidHelper {

    @Override
    public Fluid createGasFluid(Identifier gas) {
        return new NeoForgeGasFluid(gas);
    }

    /** One property set per liquid, built on first use and shared only by that liquid's two forms. */
    private static final java.util.Map<NeroTechLiquid, net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties> LIQUID_PROPERTIES =
            new java.util.EnumMap<>(NeroTechLiquid.class);

    private static synchronized net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties properties(NeroTechLiquid liquid) {
        return LIQUID_PROPERTIES.computeIfAbsent(liquid, l -> new net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties(
                () -> NeoForgeFluidTypes.forLiquid(l), l::source, l::flowing)
                .bucket(l::bucket)
                .block(l::block)
                .slopeFindDistance(l.slopeFindDistance())
                .levelDecreasePerBlock(l.levelDecreasePerBlock())
                .tickRate(l.tickDelay())
                .explosionResistance(100.0F));
    }

    @Override
    public Fluid createLiquid(NeroTechLiquid liquid, boolean source) {
        return source
                ? new net.neoforged.neoforge.fluids.BaseFlowingFluid.Source(properties(liquid))
                : new net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing(properties(liquid));
    }

    private static final class NeoForgeGasFluid extends GasFluid {

        private NeoForgeGasFluid(Identifier gas) {
            super(gas);
        }

        @Override
        public FluidType getFluidType() {
            return NeoForgeFluidTypes.forGas(gas());
        }
    }
}
