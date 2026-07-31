package za.co.neroland.nerotech.gas;

import java.util.function.Predicate;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerolandcore.gas.GasBuffer;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.gas.NeroGases;

/**
 * A machine's gas tank: Core's {@link GasBuffer} plus an accept filter, so a tank only ever takes
 * the gas its machine actually handles (the Chemical Processor's oxygen tank refuses hydrogen; the
 * Gas Turbine's tank refuses anything that is not a configured fuel). Without the filter a
 * {@link GasBuffer} would latch onto whatever arrived first and then jam.
 *
 * <p>The machine's own production bypasses the filter through {@link #produce} — a machine always
 * owns what it makes. NBT save/load rides Core's raw accessors.
 */
public final class MachineGasTank implements NeroGasStorage {

    private final GasBuffer buffer;
    private final Predicate<Identifier> accepts;

    public MachineGasTank(long capacity, Predicate<Identifier> accepts, Runnable onChanged) {
        this.buffer = new GasBuffer(capacity, onChanged);
        this.accepts = accepts;
    }

    /** A tank that accepts exactly one gas. */
    public static MachineGasTank of(Identifier gas, long capacity, Runnable onChanged) {
        return new MachineGasTank(capacity, gas::equals, onChanged);
    }

    @Override
    public Identifier getGas() {
        return this.buffer.getGas();
    }

    @Override
    public long getAmount() {
        return this.buffer.getAmount();
    }

    @Override
    public long getCapacity() {
        return this.buffer.getCapacity();
    }

    @Override
    public long fill(Identifier gas, long amount, boolean simulate) {
        return this.accepts.test(gas) ? this.buffer.fill(gas, amount, simulate) : 0L;
    }

    @Override
    public long drain(long amount, boolean simulate) {
        return this.buffer.drain(amount, simulate);
    }

    /** Machine-internal production — bypasses the accept filter. @return mB actually stored. */
    public long produce(Identifier gas, long amount) {
        return this.buffer.fill(gas, amount, false);
    }

    /** Whether the tank could take {@code amount} more mB of its current (or any) gas. */
    public boolean hasRoomFor(long amount) {
        return this.buffer.getAmount() + amount <= this.buffer.getCapacity();
    }

    /** Re-apply the configured capacity (called on load; config capacity changes need a reload). */
    public void resize(long capacity) {
        this.buffer.resize(capacity);
    }

    public void save(ValueOutput output, String key) {
        output.putString(key + "Gas", this.buffer.getRawGas().toString());
        output.putInt(key + "Amount", this.buffer.getRawAmount());
    }

    public void load(ValueInput input, String key) {
        Identifier gas = Identifier.parse(input.getStringOr(key + "Gas", NeroGases.EMPTY.toString()));
        this.buffer.setRaw(gas, input.getIntOr(key + "Amount", 0));
    }
}
