package za.co.neroland.nerotech.forge;

import java.util.function.Consumer;

import net.minecraft.resources.Identifier;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.fluid.NeroTechLiquid;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.registry.RegistrationProvider;

/**
 * The {@link FluidType}s behind NeroTech's gas fluids on Forge — the Forge twin of
 * {@code NeoForgeFluidTypes}. A gas is never placed in the world; the type exists because Forge
 * requires one per fluid.
 */
public final class ForgeFluidTypes {

    private static final RegistrationProvider<FluidType> TYPES =
            RegistrationProvider.get(ForgeRegistries.Keys.FLUID_TYPES, NeroTechCommon.MOD_ID);

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

    private ForgeFluidTypes() {
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

    private static FluidType liquidType(NeroTechLiquid liquid) {
        return new TexturedFluidType(FluidType.Properties.create()
                .density(liquid.density())
                .viscosity(liquid.viscosity())
                .temperature(liquid.temperature())
                .canConvertToSource(false), liquid.id());
    }

    private static FluidType gasType(String gas) {
        return new TexturedFluidType(FluidType.Properties.create()
                .density(-10)
                .viscosity(100)
                .canDrown(false)
                .canSwim(false)
                .canPushEntity(false)
                .canExtinguish(false), gas);
    }

    /**
     * Forge has no client-extension event: a {@link FluidType} hands its render properties over from
     * its own constructor, dist-guarded, so the textures live here rather than in the client setup.
     * The consumer must be given an anonymous implementation — Forge rejects {@code this}.
     */
    private static final class TexturedFluidType extends FluidType {

        private final Identifier still;
        private final Identifier flowing;

        private TexturedFluidType(Properties properties, String gas) {
            super(properties);
            this.still = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "block/" + gas + "_still");
            this.flowing = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "block/" + gas + "_flow");
        }

        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override
                public Identifier getStillTexture() {
                    return TexturedFluidType.this.still;
                }

                @Override
                public Identifier getFlowingTexture() {
                    return TexturedFluidType.this.flowing;
                }
            });
        }
    }
}
