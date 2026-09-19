package za.co.neroland.nerotech.neoforge;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.registry.RegistrationProvider;

/**
 * The {@link FluidType}s behind NeroTech's gas fluids on NeoForge. A gas is never placed in the
 * world, so the properties only have to describe something light and unswimmable — the type exists
 * because NeoForge requires one per fluid, and because it carries the display name and the client
 * render extensions.
 *
 * <p>Created from the NeoForge entry point before the DeferredRegisters are flushed to the mod bus.
 */
public final class NeoForgeFluidTypes {

    private static final RegistrationProvider<FluidType> TYPES =
            RegistrationProvider.get(NeoForgeRegistries.Keys.FLUID_TYPES, NeroTechCommon.MOD_ID);

    public static final RegistrationProvider.RegistryEntry<FluidType> HYDROGEN =
            TYPES.register("hydrogen", key -> gasType());

    public static final RegistrationProvider.RegistryEntry<FluidType> OXYGEN =
            TYPES.register("oxygen", key -> gasType());

    private NeoForgeFluidTypes() {
    }

    /** Force class-load so the types register. */
    public static void init() {
    }

    /** The type for one of NeroTech's gases; hydrogen is the fallback for anything unknown. */
    public static FluidType forGas(Identifier gas) {
        return NeroTechGases.OXYGEN.equals(gas) ? OXYGEN.get() : HYDROGEN.get();
    }

    private static FluidType gasType() {
        return new FluidType(FluidType.Properties.create()
                .density(-10)
                .viscosity(100)
                .canDrown(false)
                .canSwim(false)
                .canPushEntity(false)
                .canExtinguish(false));
    }
}
