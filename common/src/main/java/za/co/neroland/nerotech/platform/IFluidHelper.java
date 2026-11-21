package za.co.neroland.nerotech.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;

import za.co.neroland.nerotech.fluid.GasFluid;
import za.co.neroland.nerotech.fluid.NeroTechLiquid;

/**
 * Loader seam for NeroTech's gas fluids. The {@link Fluid} objects themselves are plain vanilla,
 * but NeoForge and Forge additionally require every fluid to name a {@code FluidType} — a
 * loader-only class — so each loader builds its own flavour of {@link GasFluid} here. Fabric
 * returns the common class unchanged.
 */
public interface IFluidHelper {

    /** The loader's flavour of the transport fluid for {@code gas}. */
    Fluid createGasFluid(Identifier gas);

    /**
     * The loader's flavour of a placeable liquid: its source ({@code source == true}) or flowing form.
     * NeoForge/Forge back it with a {@code FluidType} and one flowing-fluid property set per liquid
     * (built lazily, never shared between liquids); Fabric uses a plain {@code FlowingFluid}.
     */
    Fluid createLiquid(NeroTechLiquid liquid, boolean source);
}
