package za.co.neroland.nerotech.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;

import za.co.neroland.nerotech.fluid.GasFluid;
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
