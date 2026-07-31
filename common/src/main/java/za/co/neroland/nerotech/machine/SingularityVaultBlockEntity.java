package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Singularity Vault — bulk storage for <b>one</b> item type, up to
 * {@code singularityVaultCapacity} (default 1,000,000) of it. The bulk itself is a virtual store
 * (an item prototype plus a {@code long} count), so a full vault costs two NBT fields, not
 * fifteen thousand stacks.
 *
 * <p>Automation talks to it through a two-slot facade: <b>slot 0</b> is the input, drained into
 * the store each tick while the type matches (or while the store is empty, which sets the type),
 * and <b>slot 1</b> is the output, kept topped up with a full stack of the stored item so a
 * hopper, pipe or {@link RoboticArmBlockEntity Robotic Arm} can pull from it continuously.
 *
 * <p>By hand: right-click with a stack to deposit it, crouch-right-click to take a stack back.
 * A comparator reads fill as a fraction of capacity.
 *
 * <p><b>Not</b> an energy machine — it is built on the shared machine base purely for the
 * container/persistence plumbing, and declares a zero NE buffer (it is registered on the item
 * surface only, never the energy one). It has no GUI: the block interactions and the comparator
 * are the whole interface.
 */
public class SingularityVaultBlockEntity extends NeroTechMachineBlockEntity {

    /** Automation input — absorbed into the virtual store each tick. */
    public static final int INPUT_SLOT = 0;
    /** Automation output — kept stocked with a full stack of the stored item. */
    public static final int OUTPUT_SLOT = 1;

    /** The stored item type as a single-count prototype, or EMPTY when the vault is unassigned. */
    private ItemStack storedType = ItemStack.EMPTY;

    /** How many of {@link #storedType} sit in the virtual store (excludes the two facade slots). */
    private long storedCount;

    public SingularityVaultBlockEntity(BlockPos pos, BlockState state) {
        // Zero NE buffer, zero transfer: the vault neither stores nor moves power.
        super(ModBlockEntities.SINGULARITY_VAULT.get(), pos, state, 2, 0, 0);
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        boolean changed = absorbInput();
        changed |= restockOutput();
        changed |= clearWhenEmpty();
        if (changed) {
            setChanged();
        }
    }

    // --- the virtual store ----------------------------------------------------------------------

    /** The vault's capacity (config; re-read live so a reload takes effect immediately). */
    public static long capacity() {
        return NeroTechConfig.singularityVaultCapacity();
    }

    /** The stored item prototype (count 1), or EMPTY when the vault is unassigned. */
    public ItemStack storedType() {
        return this.storedType;
    }

    /** Everything the vault holds: the virtual store plus whatever sits in the two facade slots. */
    public long totalStored() {
        return this.storedCount + this.items.get(INPUT_SLOT).getCount()
                + this.items.get(OUTPUT_SLOT).getCount();
    }

    /**
     * Deposit up to a whole stack by hand. Sets the vault's type when it is unassigned; refuses a
     * mismatched item outright rather than silently voiding it.
     *
     * @return how many items were accepted (0 = wrong type, or full)
     */
    public int deposit(ItemStack held) {
        if (held.isEmpty()) {
            return 0;
        }
        long room = capacity() - this.storedCount;
        if (room <= 0) {
            return 0;
        }
        if (this.storedType.isEmpty() && totalStored() == 0) {
            this.storedType = held.copyWithCount(1);
        }
        if (!ItemStack.isSameItemSameComponents(this.storedType, held)) {
            return 0;
        }
        int accepted = (int) Math.min(held.getCount(), room);
        if (accepted <= 0) {
            return 0;
        }
        this.storedCount += accepted;
        held.shrink(accepted);
        setChanged();
        return accepted;
    }

    /** Take one stack back by hand — from the virtual store first, then the output slot. */
    public ItemStack extractStack() {
        if (this.storedType.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int take = (int) Math.min(this.storedType.getMaxStackSize(), this.storedCount);
        if (take > 0) {
            this.storedCount -= take;
            ItemStack out = this.storedType.copyWithCount(take);
            clearWhenEmpty();
            setChanged();
            return out;
        }
        ItemStack buffered = this.items.get(OUTPUT_SLOT);
        if (!buffered.isEmpty()) {
            this.items.set(OUTPUT_SLOT, ItemStack.EMPTY);
            clearWhenEmpty();
            setChanged();
            return buffered;
        }
        return ItemStack.EMPTY;
    }

    /** Drain the input slot into the store while the type matches (or the vault is unassigned). */
    private boolean absorbInput() {
        ItemStack in = this.items.get(INPUT_SLOT);
        if (in.isEmpty()) {
            return false;
        }
        long room = capacity() - this.storedCount;
        if (room <= 0) {
            return false;
        }
        if (this.storedType.isEmpty()) {
            this.storedType = in.copyWithCount(1);
        } else if (!ItemStack.isSameItemSameComponents(this.storedType, in)) {
            return false;
        }
        int taken = (int) Math.min(in.getCount(), room);
        if (taken <= 0) {
            return false;
        }
        this.storedCount += taken;
        in.shrink(taken);
        if (in.isEmpty()) {
            this.items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
        return true;
    }

    /** Keep the output slot topped up with a full stack so extraction never stalls. */
    private boolean restockOutput() {
        if (this.storedType.isEmpty() || this.storedCount <= 0) {
            return false;
        }
        ItemStack out = this.items.get(OUTPUT_SLOT);
        int max = this.storedType.getMaxStackSize();
        if (out.isEmpty()) {
            int give = (int) Math.min(max, this.storedCount);
            this.items.set(OUTPUT_SLOT, this.storedType.copyWithCount(give));
            this.storedCount -= give;
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(out, this.storedType) || out.getCount() >= max) {
            return false;
        }
        int give = (int) Math.min(max - out.getCount(), this.storedCount);
        if (give <= 0) {
            return false;
        }
        out.grow(give);
        this.storedCount -= give;
        return true;
    }

    /** A fully drained vault forgets its type, so it can be re-assigned without being broken. */
    private boolean clearWhenEmpty() {
        if (this.storedType.isEmpty() || totalStored() > 0) {
            return false;
        }
        this.storedType = ItemStack.EMPTY;
        return true;
    }

    /** Comparator output: fill as a fraction of capacity, with a floor of 1 while anything is held. */
    public int comparatorSignal() {
        long total = totalStored();
        if (total <= 0) {
            return 0;
        }
        long cap = Math.max(1L, capacity());
        return (int) Math.max(1L, Math.min(15L, total * 15L / cap));
    }

    // --- container surface ----------------------------------------------------------------------

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        if (slot != INPUT_SLOT) {
            return false;
        }
        return this.storedType.isEmpty() || ItemStack.isSameItemSameComponents(this.storedType, stack);
    }

    @Override
    public boolean canTakeMachineItem(int slot) {
        return slot == OUTPUT_SLOT;
    }

    /** Nothing to shed: the vault draws no power, so a Grid Controller leaves it alone. */
    @Override
    public boolean shedable() {
        return false;
    }

    // --- persistence -----------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("StoredType", ItemStack.OPTIONAL_CODEC, this.storedType);
        output.putLong("StoredCount", this.storedCount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.storedType = input.read("StoredType", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.storedCount = input.getLongOr("StoredCount", 0L);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nerotech.singularity_vault");
    }

    /**
     * No GUI: the block interactions and the comparator are the whole interface. Returning null
     * makes {@code ServerPlayer#openMenu} a no-op (the Coolant Pump's recipe).
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }
}
