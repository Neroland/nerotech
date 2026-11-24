package za.co.neroland.nerotech.neoforge;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
//? if <26.1 {
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
*///?}

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.fluid.NeroTechLiquid;
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
            TYPES.register("hydrogen", key -> gasType("hydrogen"));

    public static final RegistrationProvider.RegistryEntry<FluidType> OXYGEN =
            TYPES.register("oxygen", key -> gasType("oxygen"));

    /** 0.8.0 gases, keyed by canonical gas id (hydrogen and oxygen keep their own fields above). */
    private static final java.util.Map<Identifier, RegistrationProvider.RegistryEntry<FluidType>> GAS_TYPES =
            new java.util.HashMap<>();
    /** 0.8.0 liquids. */
    private static final java.util.Map<NeroTechLiquid, RegistrationProvider.RegistryEntry<FluidType>> LIQUID_TYPES =
            new java.util.EnumMap<>(NeroTechLiquid.class);

    static {
        GAS_TYPES.put(NeroTechGases.HYDROGEN, HYDROGEN);
        GAS_TYPES.put(NeroTechGases.OXYGEN, OXYGEN);
        for (Identifier gas : NeroTechGases.ALL) {
            if (!GAS_TYPES.containsKey(gas)) {
                GAS_TYPES.put(gas, TYPES.register(gas.getPath(), key -> gasType(gas.getPath())));
            }
        }
        for (NeroTechLiquid liquid : NeroTechLiquid.values()) {
            LIQUID_TYPES.put(liquid, TYPES.register(liquid.id(), key -> liquidType(liquid)));
        }
    }

    private NeoForgeFluidTypes() {
    }

    /** Force class-load so the types register. */
    public static void init() {
    }

    /** The type for one of NeroTech's gases; hydrogen is the fallback for anything unknown. */
    public static FluidType forGas(Identifier gas) {
        RegistrationProvider.RegistryEntry<FluidType> entry = GAS_TYPES.get(NeroTechGases.canonical(gas));
        return entry == null ? HYDROGEN.get() : entry.get();
    }

    /** The type of one of NeroTech's liquids. */
    public static FluidType forLiquid(NeroTechLiquid liquid) {
        return LIQUID_TYPES.get(liquid).get();
    }

    /** Every gas type entry (for the client's pre-26.1 render extensions). */
    public static java.util.Map<Identifier, RegistrationProvider.RegistryEntry<FluidType>> gasTypes() {
        return java.util.Collections.unmodifiableMap(GAS_TYPES);
    }

    private static FluidType liquidType(NeroTechLiquid liquid) {
        return fluidType(liquid.id(), FluidType.Properties.create()
                .density(liquid.density())
                .viscosity(liquid.viscosity())
                .temperature(liquid.temperature())
                .canConvertToSource(false));
    }

    private static FluidType gasType(String name) {
        return fluidType(name, FluidType.Properties.create()
                .density(-10)
                .viscosity(100)
                .canDrown(false)
                .canSwim(false)
                .canPushEntity(false)
                .canExtinguish(false));
    }

    private static FluidType fluidType(String name, FluidType.Properties properties) {
        //? if >=1.21 {
        return new FluidType(properties);
        //?} else {
        /*return new FluidType(properties) {
            @Override
            public void initializeClient(java.util.function.Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(textureExtensions(name));
            }
        };
        *///?}
    }

    //? if <26.1 {
    /*static IClientFluidTypeExtensions textureExtensions(String name) {
        Identifier still = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "block/" + name + "_still");
        Identifier flowing = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "block/" + name + "_flow");
        return new IClientFluidTypeExtensions() {
            @Override
            public Identifier getStillTexture() {
                return still;
            }

            @Override
            public Identifier getFlowingTexture() {
                return flowing;
            }
        };
    }
    *///?}
}
