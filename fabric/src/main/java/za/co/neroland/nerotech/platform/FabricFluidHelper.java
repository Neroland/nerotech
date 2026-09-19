package za.co.neroland.nerotech.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;

import za.co.neroland.nerotech.fluid.GasFluid;

/**
 * Fabric {@link IFluidHelper}: Fabric has no {@code FluidType}, so the common
 * {@link GasFluid} is used unchanged. Registered via {@code META-INF/services}.
 */
public final class FabricFluidHelper implements IFluidHelper {

    @Override
    public Fluid createGasFluid(Identifier gas) {
        return new GasFluid(gas);
    }
}
