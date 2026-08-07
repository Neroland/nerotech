package za.co.neroland.nerotech.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.recipe.ColliderRecipe;
import za.co.neroland.nerotech.recipe.MachineRecipe;

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

    private ModRecipeTypes() {
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
                    RecipeSerializer<MachineRecipe> serializer = new RecipeSerializer<>(
                            net.minecraft.world.item.crafting.SingleItemRecipe.simpleMapCodec(factory),
                            net.minecraft.world.item.crafting.SingleItemRecipe.simpleStreamCodec(factory));
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
            RecipeSerializer<ColliderRecipe> serializer = new RecipeSerializer<>(
                    ColliderRecipe.mapCodec(type::get, self::get),
                    ColliderRecipe.streamCodec(type::get, self::get));
            self.set(serializer);
            return serializer;
        });
    }

    /** Force classloading so the static registrations run during mod init. */
    public static void init() {
    }
}
