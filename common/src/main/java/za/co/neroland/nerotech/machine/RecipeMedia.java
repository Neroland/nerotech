package za.co.neroland.nerotech.machine;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.fluid.MachineFluidTank;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.recipe.ProcessMedium;
import za.co.neroland.nerotech.recipe.ProcessRecipe;

/**
 * Which fluids and gases a recipe machine's recipes take in, so an adjacent pull only draws a medium the
 * machine can use: pulling anything a neighbour offers would fill an input tank with a medium no recipe
 * reads and jam it. Built once per recipe type per recipe manager (a datapack reload makes a new
 * manager) and read on the server thread only. Recipe definitions only; no player data.
 */
final class RecipeMedia {

    /** One recipe's input media and the atmosphere (or cascade stage) it runs in, empty for any. */
    private record Inputs(Optional<ProcessMedium> fluid, Optional<ProcessMedium> gas, String atmosphere) {
    }

    private record Entry(RecipeManager manager, List<Inputs> inputs) {
    }

    private static final Map<RecipeType<?>, Entry> CACHE = new IdentityHashMap<>();

    private RecipeMedia() {
    }

    private static List<Inputs> inputs(ServerLevel level, RecipeType<ProcessRecipe> type) {
        RecipeManager manager = level.getServer().getRecipeManager();
        Entry entry = CACHE.get(type);
        if (entry == null || entry.manager() != manager) {
            List<Inputs> list = new ArrayList<>();
            for (RecipeHolder<?> holder : manager.getRecipes()) {
                if (holder.value() instanceof ProcessRecipe recipe && recipe.getType() == type) {
                    list.add(new Inputs(recipe.fluidIn(), recipe.gasIn(), recipe.atmosphere().orElse("")));
                }
            }
            entry = new Entry(manager, List.copyOf(list));
            CACHE.put(type, entry);
        }
        return entry.inputs();
    }

    private static boolean atmosphereFits(String required, String present) {
        return required.isEmpty() || required.equalsIgnoreCase(present == null ? "" : present);
    }

    /** Whether some recipe of {@code type} that can run in {@code atmosphere} takes {@code fluid} in. */
    static boolean usesFluid(ServerLevel level, RecipeType<ProcessRecipe> type, String atmosphere,
            @Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        Fluid source = MachineFluidTank.normalize(fluid);
        String id = BuiltInRegistries.FLUID.getKey(source).toString();
        for (Inputs in : inputs(level, type)) {
            if (in.fluid().isPresent() && atmosphereFits(in.atmosphere(), atmosphere)
                    && in.fluid().get().satisfiedBy(id, Long.MAX_VALUE / 4, 1L, tag -> {
                        Identifier tagId = Identifier.tryParse(tag);
                        return tagId != null && source.is(TagKey.create(Registries.FLUID, tagId));
                    })) {
                return true;
            }
        }
        return false;
    }

    /** Whether some recipe of {@code type} that can run in {@code atmosphere} takes {@code gas} in. */
    static boolean usesGas(ServerLevel level, RecipeType<ProcessRecipe> type, String atmosphere,
            @Nullable Identifier gas) {
        Identifier canonical = NeroTechGases.canonical(gas);
        if (canonical == null) {
            return false;
        }
        String id = canonical.toString();
        for (Inputs in : inputs(level, type)) {
            if (in.gas().isPresent() && atmosphereFits(in.atmosphere(), atmosphere)
                    && in.gas().get().satisfiedBy(id, Long.MAX_VALUE / 4, NeroTechGases.UNIT_MB, tag -> false)) {
                return true;
            }
        }
        return false;
    }
}
