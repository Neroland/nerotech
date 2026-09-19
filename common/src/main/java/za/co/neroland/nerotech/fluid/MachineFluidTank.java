package za.co.neroland.nerotech.fluid;

import java.util.function.Predicate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerolandcore.fluid.FluidBuffer;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;

/**
 * A machine's fluid tank: Core's {@link FluidBuffer} plus an accept filter, so a tank only ever
 * takes the fluid its machine actually handles (the Electrolyzer's tank refuses everything but
 * water). This is the fluid twin of {@link za.co.neroland.nerotech.gas.MachineGasTank}, and the
 * filter matters for the same reason: a bare {@link FluidBuffer} latches onto whatever arrives
 * first, so one millibucket of lava from a mod's pipe would jam the machine for good — and now that
 * the tanks are exposed on the loaders' standard fluid capability, anything can arrive.
 *
 * <p>The machine's own filling bypasses the filter through {@link #forceFill} — a machine owns what
 * it puts in itself. NBT save/load rides Core's raw accessors.
 */
public final class MachineFluidTank implements NeroFluidStorage {

    private final FluidBuffer buffer;
    private final Predicate<Fluid> accepts;

    public MachineFluidTank(long capacity, Predicate<Fluid> accepts, Runnable onChanged) {
        this.buffer = new FluidBuffer(capacity, onChanged);
        this.accepts = accepts;
    }

    /** A tank that accepts exactly one fluid. */
    public static MachineFluidTank of(Fluid fluid, long capacity, Runnable onChanged) {
        return new MachineFluidTank(capacity, fluid::equals, onChanged);
    }

    @Override
    public Fluid getFluid() {
        return this.buffer.getFluid();
    }

    @Override
    public long getAmount() {
        return this.buffer.getAmount();
    }

    @Override
    public long getCapacity() {
        return this.buffer.getCapacity();
    }

    /**
     * Accepts the fluid if it passes the filter, normalising a flowing fluid to its source first —
     * a pipe may well offer {@code minecraft:flowing_water}, and storing that as a distinct fluid
     * would make the tank refuse the still water offered next.
     */
    @Override
    public long fill(Fluid fluid, long amount, boolean simulate) {
        Fluid source = normalize(fluid);
        return this.accepts.test(source) ? this.buffer.fill(source, amount, simulate) : 0L;
    }

    /** The source form of a flowing fluid ({@code flowing_water} to {@code water}); others as-is. */
    public static Fluid normalize(Fluid fluid) {
        return fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
    }

    @Override
    public long drain(long amount, boolean simulate) {
        return this.buffer.drain(amount, simulate);
    }

    /** Machine-internal filling — bypasses the accept filter. @return mB actually stored. */
    public long forceFill(Fluid fluid, long amount) {
        return this.buffer.fill(normalize(fluid), amount, false);
    }

    /** Whether the tank could take {@code amount} more mB. */
    public boolean hasRoomFor(long amount) {
        return this.buffer.getAmount() + amount <= this.buffer.getCapacity();
    }

    /** Re-apply the configured capacity (called on load; config capacity changes need a reload). */
    public void resize(long capacity) {
        this.buffer.resize(capacity);
    }

    /** Writes {@code <key>Fluid} / {@code <key>Amount} — the Electrolyzer's historical NBT layout. */
    public void save(ValueOutput output, String key) {
        output.putString(key + "Fluid", BuiltInRegistries.FLUID.getKey(this.buffer.getRawFluid()).toString());
        output.putInt(key + "Amount", this.buffer.getRawAmount());
    }

    public void load(ValueInput input, String key) {
        Fluid fluid = BuiltInRegistries.FLUID.getValue(
                Identifier.parse(input.getStringOr(key + "Fluid", "minecraft:empty")));
        this.buffer.setRaw(fluid == null ? Fluids.EMPTY : normalize(fluid), input.getIntOr(key + "Amount", 0));
    }
}
