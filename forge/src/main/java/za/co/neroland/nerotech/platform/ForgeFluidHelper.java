package za.co.neroland.nerotech.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;

import za.co.neroland.nerotech.fluid.GasFluid;
import za.co.neroland.nerotech.fluid.NeroTechLiquid;
import za.co.neroland.nerotech.forge.ForgeFluidTypes;

/**
 * Forge {@link IFluidHelper}: Forge requires every fluid to name a {@link FluidType}, so the common
 * {@link GasFluid} is subclassed here to point at the type registered in {@link ForgeFluidTypes}.
 * Registered via {@code META-INF/services}.
 */
public final class ForgeFluidHelper implements IFluidHelper {

    @Override
    public Fluid createGasFluid(Identifier gas) {
        return new ForgeGasFluid(gas);
    }

    /** One property set per liquid, built on first use and shared only by that liquid's two forms. */
    private static final java.util.Map<NeroTechLiquid, net.minecraftforge.fluids.ForgeFlowingFluid.Properties> LIQUID_PROPERTIES =
            new java.util.EnumMap<>(NeroTechLiquid.class);

    private static synchronized net.minecraftforge.fluids.ForgeFlowingFluid.Properties properties(NeroTechLiquid liquid) {
        return LIQUID_PROPERTIES.computeIfAbsent(liquid, l -> new net.minecraftforge.fluids.ForgeFlowingFluid.Properties(
                () -> ForgeFluidTypes.forLiquid(l), l::source, l::flowing)
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
                ? new net.minecraftforge.fluids.ForgeFlowingFluid.Source(properties(liquid))
                : new net.minecraftforge.fluids.ForgeFlowingFluid.Flowing(properties(liquid));
    }

    private static final class ForgeGasFluid extends GasFluid {

        private ForgeGasFluid(Identifier gas) {
            super(gas);
        }

        @Override
        public FluidType getFluidType() {
            return ForgeFluidTypes.forGas(gas());
        }
    }
}
