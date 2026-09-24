package za.co.neroland.nerotech.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.platform.Services;
import za.co.neroland.nerotech.registry.RegistrationProvider;

/**
 * NeroTech's two gases registered as Minecraft fluids — the <b>transport identity</b> of
 * {@code nerotech:hydrogen} and the shared {@code nerospace:oxygen}.
 *
 * <p>The oxygen fluid keeps its registry id {@code nerotech:oxygen} (renaming a registered fluid
 * would orphan it in other mods' pipes and tanks); it simply stands for the canonical gas id
 * {@link NeroTechGases#OXYGEN}, and {@link #fluidFor} maps the legacy gas id to it as well.
 *
 * <p>Core's gas layer identifies a gas by {@link Identifier}, which is perfect inside the Nero
 * ecosystem and invisible outside it: no other mod's pipe knows what a "Nero gas" is, so oxygen
 * could never leave the block it was made in except by touching its consumer (issue #9). Giving
 * each gas a real {@link Fluid} gives it a name every mod already speaks, so a fluid pipe can carry
 * it, and {@link #asFluid(NeroGasStorage)} presents a gas tank on that surface without changing the
 * gameplay contract — the tank is still a {@link NeroGasStorage}, still measured in mB, and Nero
 * blocks still talk to it through Core's gas capability.
 *
 * <p>These fluids are not placeable and have no bucket: see {@link GasFluid}.
 */
public final class NeroTechFluids {

    private static final RegistrationProvider<Fluid> FLUIDS =
            RegistrationProvider.get(Registries.FLUID, NeroTechCommon.MOD_ID);

    /** Transport identity of {@link NeroTechGases#HYDROGEN}. */
    public static final RegistrationProvider.RegistryEntry<Fluid> HYDROGEN =
            FLUIDS.register("hydrogen", key -> Services.FLUIDS.createGasFluid(NeroTechGases.HYDROGEN));

    /** Transport identity of {@link NeroTechGases#OXYGEN}. */
    public static final RegistrationProvider.RegistryEntry<Fluid> OXYGEN =
            FLUIDS.register("oxygen", key -> Services.FLUIDS.createGasFluid(NeroTechGases.OXYGEN));

    private NeroTechFluids() {
    }

    /** Force class-load so the entries register (called from {@code ModRegistries.init()}). */
    public static void init() {
    }

    /** The fluid a gas travels as, or {@code Fluids.EMPTY} for a gas NeroTech does not declare. */
    public static Fluid fluidFor(@Nullable Identifier gas) {
        Identifier canonical = NeroTechGases.canonical(gas);
        if (NeroTechGases.HYDROGEN.equals(canonical)) {
            return HYDROGEN.get();
        }
        if (NeroTechGases.OXYGEN.equals(canonical)) {
            return OXYGEN.get();
        }
        return Fluids.EMPTY;
    }

    /** The gas a fluid stands for, or {@code null} if it is an ordinary fluid. */
    @Nullable
    public static Identifier gasFor(@Nullable Fluid fluid) {
        return fluid instanceof GasFluid gasFluid ? NeroTechGases.canonical(gasFluid.gas()) : null;
    }

    /**
     * A gas tank seen as a Nero fluid tank, so the loader adapters Core ships can put it on the
     * standard fluid capability. Amounts pass through unchanged — a gas and a fluid are both mB
     * here — and a fluid that is not one of NeroTech's gases is refused.
     */
    public static NeroFluidStorage asFluid(NeroGasStorage tank) {
        return new GasFluidView(tank);
    }

    private record GasFluidView(NeroGasStorage tank) implements NeroFluidStorage {

        @Override
        public Fluid getFluid() {
            return this.tank.getAmount() <= 0 ? Fluids.EMPTY : fluidFor(this.tank.getGas());
        }

        @Override
        public long getAmount() {
            return this.tank.getAmount();
        }

        @Override
        public long getCapacity() {
            return this.tank.getCapacity();
        }

        @Override
        public long fill(Fluid fluid, long amount, boolean simulate) {
            Identifier gas = gasFor(fluid);
            return gas == null ? 0L : this.tank.fill(gas, amount, simulate);
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return this.tank.drain(amount, simulate);
        }
    }
}
