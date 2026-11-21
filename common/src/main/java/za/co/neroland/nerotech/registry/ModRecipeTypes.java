package za.co.neroland.nerotech.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.recipe.ColliderRecipe;
import za.co.neroland.nerotech.recipe.MachineRecipe;
import za.co.neroland.nerotech.recipe.ProcessRecipe;

/**
 * NeroTech's datapack recipe types + serializers (Stage C, 2026-07-10): one per processing
 * machine family. All but the collider decode to {@link MachineRecipe} (single ingredient → single
 * result); the collider needs two inputs and an energy floor, so it has its own
 * {@link ColliderRecipe} class and codec pair.
 *
 * <ul>
 *   <li>{@code nerotech:ore_processing} — Ore Processor and Advanced Ore Processor (the advanced
 *       tier adds its config-driven yield bonus on top of the same recipes).</li>
 *   <li>{@code nerotech:fabricating} — Fabricator.</li>
 *   <li>{@code nerotech:advanced_fabricating} — Advanced Fabricator (the Starsteel tier: ungated,
 *       paced by its recipes and materials alone).</li>
 *   <li>{@code nerotech:collider} — Particle Accelerator collisions (two particles + a minimum
 *       collision energy → one product).</li>
 *   <li>{@code nerotech:chemical_processing} — Chemical Processor (Stage C oxygen wash: raw ore →
 *       3 dust). The oxygen cost is <b>not</b> a recipe field — it is the single config value
 *       {@code chemicalProcessorGasPerOp}, so every wash costs the same reagent and the recipe JSON
 *       keeps the shared one-in/one-out shape (and therefore the shared JEI page and serializer).</li>
 * </ul>
 */
public final class ModRecipeTypes {

    public static final RegistrationProvider<RecipeType<?>> TYPES =
            RegistrationProvider.get(Registries.RECIPE_TYPE, NeroTechCommon.MOD_ID);
    public static final RegistrationProvider<RecipeSerializer<?>> SERIALIZERS =
            RegistrationProvider.get(Registries.RECIPE_SERIALIZER, NeroTechCommon.MOD_ID);

    public static final RegistrationProvider.RegistryEntry<RecipeType<MachineRecipe>> ORE_PROCESSING =
            type("ore_processing");
    public static final RegistrationProvider.RegistryEntry<RecipeType<MachineRecipe>> FABRICATING =
            type("fabricating");
    public static final RegistrationProvider.RegistryEntry<RecipeType<MachineRecipe>> ADVANCED_FABRICATING =
            type("advanced_fabricating");
    public static final RegistrationProvider.RegistryEntry<RecipeType<ColliderRecipe>> COLLIDER =
            colliderType("collider");
    public static final RegistrationProvider.RegistryEntry<RecipeType<MachineRecipe>> CHEMICAL_PROCESSING =
            type("chemical_processing");

    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<MachineRecipe>> ORE_PROCESSING_SERIALIZER =
            serializer("ore_processing", ORE_PROCESSING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<MachineRecipe>> FABRICATING_SERIALIZER =
            serializer("fabricating", FABRICATING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<MachineRecipe>> ADVANCED_FABRICATING_SERIALIZER =
            serializer("advanced_fabricating", ADVANCED_FABRICATING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ColliderRecipe>> COLLIDER_SERIALIZER =
            colliderSerializer("collider", COLLIDER);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<MachineRecipe>> CHEMICAL_PROCESSING_SERIALIZER =
            serializer("chemical_processing", CHEMICAL_PROCESSING);

    // --- 0.8.0 process recipe types: one ProcessRecipe class, one type per machine ---
    /** Plasma Crucible. */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> ALLOYING = processType("alloying");
    /** Plate Press. */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> PRESSING = processType("pressing");
    /** Filament Drawer. */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> DRAWING = processType("drawing");
    /** Blast Foundry. */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> FOUNDRY_CASTING =
            processType("foundry_casting");
    /** Centrifuge. */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> CENTRIFUGING =
            processType("centrifuging");
    /** Cryo Distiller. */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> CRYO_DISTILLING =
            processType("cryo_distilling");
    /** Atmospheric Condenser (one recipe per atmosphere). */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> CONDENSING = processType("condensing");
    /** Reaction Vat. */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> REACTING = processType("reacting");
    /** Component Forge. */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> COMPONENT_FORGING =
            processType("component_forging");
    /** Laser Etcher (the focus lens is the catalyst). */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> ETCHING = processType("etching");
    /** Crystal Grower. */
    public static final RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> CRYSTAL_GROWING =
            processType("crystal_growing");

    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> ALLOYING_SERIALIZER =
            processSerializer("alloying", ALLOYING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> PRESSING_SERIALIZER =
            processSerializer("pressing", PRESSING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> DRAWING_SERIALIZER =
            processSerializer("drawing", DRAWING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> FOUNDRY_CASTING_SERIALIZER =
            processSerializer("foundry_casting", FOUNDRY_CASTING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> CENTRIFUGING_SERIALIZER =
            processSerializer("centrifuging", CENTRIFUGING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> CRYO_DISTILLING_SERIALIZER =
            processSerializer("cryo_distilling", CRYO_DISTILLING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> CONDENSING_SERIALIZER =
            processSerializer("condensing", CONDENSING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> REACTING_SERIALIZER =
            processSerializer("reacting", REACTING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> COMPONENT_FORGING_SERIALIZER =
            processSerializer("component_forging", COMPONENT_FORGING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> ETCHING_SERIALIZER =
            processSerializer("etching", ETCHING);
    public static final RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> CRYSTAL_GROWING_SERIALIZER =
            processSerializer("crystal_growing", CRYSTAL_GROWING);

    /** The eleven process types, in display order. */
    public static java.util.List<RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>>> processTypes() {
        return java.util.List.of(ALLOYING, PRESSING, DRAWING, FOUNDRY_CASTING, CENTRIFUGING, CRYO_DISTILLING,
                CONDENSING, REACTING, COMPONENT_FORGING, ETCHING, CRYSTAL_GROWING);
    }

    /** Every NeroTech recipe type the server syncs to clients for recipe viewers (all sixteen). */
    public static java.util.List<RecipeType<?>> syncedTypes() {
        java.util.List<RecipeType<?>> out = new java.util.ArrayList<>();
        out.add(ORE_PROCESSING.get());
        out.add(FABRICATING.get());
        out.add(ADVANCED_FABRICATING.get());
        out.add(COLLIDER.get());
        out.add(CHEMICAL_PROCESSING.get());
        processTypes().forEach(entry -> out.add(entry.get()));
        return java.util.List.copyOf(out);
    }

    /** Every NeroTech recipe serializer (all sixteen), for loaders that sync by serializer (Fabric). */
    public static java.util.List<RecipeSerializer<?>> syncedSerializers() {
        return java.util.List.of(
                ORE_PROCESSING_SERIALIZER.get(), FABRICATING_SERIALIZER.get(), ADVANCED_FABRICATING_SERIALIZER.get(),
                COLLIDER_SERIALIZER.get(), CHEMICAL_PROCESSING_SERIALIZER.get(),
                ALLOYING_SERIALIZER.get(), PRESSING_SERIALIZER.get(), DRAWING_SERIALIZER.get(),
                FOUNDRY_CASTING_SERIALIZER.get(), CENTRIFUGING_SERIALIZER.get(), CRYO_DISTILLING_SERIALIZER.get(),
                CONDENSING_SERIALIZER.get(), REACTING_SERIALIZER.get(), COMPONENT_FORGING_SERIALIZER.get(),
                ETCHING_SERIALIZER.get(), CRYSTAL_GROWING_SERIALIZER.get());
    }

    private ModRecipeTypes() {
    }

    private static RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> processType(String name) {
        return TYPES.register(name, key -> new RecipeType<ProcessRecipe>() {
            @Override
            public String toString() {
                return key.identifier().toString();
            }
        });
    }

    /** A process serializer from {@link ProcessRecipe}'s codec pair, bound to its own type. */
    private static RegistrationProvider.RegistryEntry<RecipeSerializer<ProcessRecipe>> processSerializer(
            String name, RegistrationProvider.RegistryEntry<RecipeType<ProcessRecipe>> type) {
        java.util.concurrent.atomic.AtomicReference<RecipeSerializer<ProcessRecipe>> self =
                new java.util.concurrent.atomic.AtomicReference<>();
        return SERIALIZERS.register(name, key -> {
            //? if >=26.1 {
            RecipeSerializer<ProcessRecipe> serializer = new RecipeSerializer<>(
                    ProcessRecipe.mapCodec(type::get, self::get),
                    ProcessRecipe.streamCodec(type::get, self::get));
            //?} else {
            /*com.mojang.serialization.MapCodec<ProcessRecipe> mapCodec = ProcessRecipe.mapCodec(type::get, self::get);
            net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ProcessRecipe> streamCodec =
                    ProcessRecipe.streamCodec(type::get, self::get);
            RecipeSerializer<ProcessRecipe> serializer = new RecipeSerializer<>() {
                @Override
                public com.mojang.serialization.MapCodec<ProcessRecipe> codec() {
                    return mapCodec;
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ProcessRecipe> streamCodec() {
                    return streamCodec;
                }
            };
            *///?}
            self.set(serializer);
            return serializer;
        });
    }

    private static RegistrationProvider.RegistryEntry<RecipeType<MachineRecipe>> type(String name) {
        // Inline anonymous type: RecipeType.simple(...) is a Forge patch, not vanilla/NeoForm API,
        // so common/ can't use it.
        return TYPES.register(name, key -> new RecipeType<MachineRecipe>() {
            @Override
            public String toString() {
                return key.identifier().toString();
            }
        });
    }

    /**
     * Registers a serializer whose codec factory binds decoded {@link MachineRecipe}s to the given
     * type and to itself (suppliers, so registration order doesn't matter).
     */
    private static RegistrationProvider.RegistryEntry<RecipeSerializer<MachineRecipe>> serializer(String name,
            RegistrationProvider.RegistryEntry<RecipeType<MachineRecipe>> type) {
        java.util.concurrent.atomic.AtomicReference<RecipeSerializer<MachineRecipe>> self =
                new java.util.concurrent.atomic.AtomicReference<>();
        RegistrationProvider.RegistryEntry<RecipeSerializer<MachineRecipe>> entry =
                SERIALIZERS.register(name, key -> {
                    var factory = MachineRecipe.factory(type::get, self::get);
                    //? if >=26.1 {
                    RecipeSerializer<MachineRecipe> serializer = new RecipeSerializer<>(
                            net.minecraft.world.item.crafting.SingleItemRecipe.simpleMapCodec(factory),
                            net.minecraft.world.item.crafting.SingleItemRecipe.simpleStreamCodec(factory));
                    //?} else {
                    /*// Before 26.1 the serializer is vanilla's single-item serializer (the stonecutter's).
                    RecipeSerializer<MachineRecipe> serializer =
                            new net.minecraft.world.item.crafting.SingleItemRecipe.Serializer<MachineRecipe>(factory) {
                            };
                    *///?}
                    self.set(serializer);
                    return serializer;
                });
        return entry;
    }

    /** The collider's own type — same anonymous-instance trick, bound to {@link ColliderRecipe}. */
    private static RegistrationProvider.RegistryEntry<RecipeType<ColliderRecipe>> colliderType(String name) {
        return TYPES.register(name, key -> new RecipeType<ColliderRecipe>() {
            @Override
            public String toString() {
                return key.identifier().toString();
            }
        });
    }

    /** The collider's own serializer, built from {@link ColliderRecipe}'s hand-written codec pair. */
    private static RegistrationProvider.RegistryEntry<RecipeSerializer<ColliderRecipe>> colliderSerializer(
            String name, RegistrationProvider.RegistryEntry<RecipeType<ColliderRecipe>> type) {
        java.util.concurrent.atomic.AtomicReference<RecipeSerializer<ColliderRecipe>> self =
                new java.util.concurrent.atomic.AtomicReference<>();
        return SERIALIZERS.register(name, key -> {
            //? if >=26.1 {
            RecipeSerializer<ColliderRecipe> serializer = new RecipeSerializer<>(
                    ColliderRecipe.mapCodec(type::get, self::get),
                    ColliderRecipe.streamCodec(type::get, self::get));
            //?} else {
            /*// Before 26.1 RecipeSerializer is an interface rather than a record of the two codecs.
            com.mojang.serialization.MapCodec<ColliderRecipe> mapCodec = ColliderRecipe.mapCodec(type::get, self::get);
            net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ColliderRecipe> streamCodec =
                    ColliderRecipe.streamCodec(type::get, self::get);
            RecipeSerializer<ColliderRecipe> serializer = new RecipeSerializer<>() {
                @Override
                public com.mojang.serialization.MapCodec<ColliderRecipe> codec() {
                    return mapCodec;
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ColliderRecipe> streamCodec() {
                    return streamCodec;
                }
            };
            *///?}
            self.set(serializer);
            return serializer;
        });
    }

    /** Force classloading so the static registrations run during mod init. */
    public static void init() {
    }
}
