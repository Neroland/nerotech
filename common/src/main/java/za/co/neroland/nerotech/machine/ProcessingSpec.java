package za.co.neroland.nerotech.machine;

/**
 * The fixed shape of one {@link ProcessingMachineBlockEntity}: how many input slots (0-6), whether it
 * has a catalyst slot (held, never consumed), how many output slots (0-4) and which tanks it carries
 * (capacity in mB, 0 = no tank). Slot order is inputs, then the catalyst, then the outputs; the Core
 * upgrade slots follow those in the container. Pure data, shared by the block entity, its menu layout
 * and the tests.
 */
public record ProcessingSpec(int inputs, boolean catalyst, int outputs, long fluidIn, long fluidOut,
        long gasIn, long gasOut) {

    public ProcessingSpec {
        if (inputs < 0 || inputs > 6) {
            throw new IllegalArgumentException("inputs must be 0..6, was " + inputs);
        }
        if (outputs < 0 || outputs > 4) {
            throw new IllegalArgumentException("outputs must be 0..4, was " + outputs);
        }
        fluidIn = Math.max(0L, fluidIn);
        fluidOut = Math.max(0L, fluidOut);
        gasIn = Math.max(0L, gasIn);
        gasOut = Math.max(0L, gasOut);
    }

    /** Items only: {@code inputs} in, {@code outputs} out, no catalyst, no tanks. */
    public static ProcessingSpec items(int inputs, int outputs) {
        return new ProcessingSpec(inputs, false, outputs, 0L, 0L, 0L, 0L);
    }

    /** Machine slots (inputs + catalyst + outputs), excluding the Core upgrade slots. */
    public int slotCount() {
        return this.inputs + (this.catalyst ? 1 : 0) + this.outputs;
    }

    /** Container index of input {@code i}. */
    public int inputSlot(int i) {
        return i;
    }

    /** Container index of the catalyst slot, or -1 without one. */
    public int catalystSlot() {
        return this.catalyst ? this.inputs : -1;
    }

    /** Container index of the first output slot. */
    public int firstOutput() {
        return this.inputs + (this.catalyst ? 1 : 0);
    }

    /** Container index of output {@code i}. */
    public int outputSlot(int i) {
        return firstOutput() + i;
    }

    /** Whether {@code slot} is an input or the catalyst (everything a recipe reads). */
    public boolean isRecipeSlot(int slot) {
        return slot >= 0 && slot < firstOutput();
    }

    /** Whether {@code slot} is an output slot. */
    public boolean isOutput(int slot) {
        return slot >= firstOutput() && slot < slotCount();
    }

    /** Whether the machine carries any fluid tank. */
    public boolean hasFluid() {
        return this.fluidIn > 0 || this.fluidOut > 0;
    }

    /** Whether the machine carries any gas tank. */
    public boolean hasGas() {
        return this.gasIn > 0 || this.gasOut > 0;
    }
}
