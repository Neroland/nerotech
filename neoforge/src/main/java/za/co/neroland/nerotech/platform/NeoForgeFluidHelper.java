package za.co.neroland.nerotech.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;

import za.co.neroland.nerotech.fluid.GasFluid;
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
