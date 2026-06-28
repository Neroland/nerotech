package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.upgrade.UpgradeModuleItem;

/**
 * Shared base for NeroTech's Tier-1 machine block-entities. Extends Core's
 * {@link AbstractMachineBlockEntity} (which supplies the {@code EnergyBuffer}, the
 * {@code UpgradeContainer} and their persistence) and adds the parts a GUI machine needs: a small
 * fixed bank of machine I/O slots, a combined {@link Container} view (machine slots followed by the
 * Core upgrade slots) for the menu, GUI {@link ContainerData} sync, and save/load of the extra state.
 *
 * <p>Energy and upgrades are NOT re-implemented — they come from Core. Subclasses override
 * {@link #serverTick} and {@link #createMenu}.
 */
public abstract class NeroTechMachineBlockEntity extends AbstractMachineBlockEntity
        implements Container, MenuProvider {

    public static final int UPGRADE_SLOTS = 4;

    protected final int machineSlots;
    protected final NonNullList<ItemStack> items;

    /** Work progress (ticks) — burn time for generators, processing time for processors. */
    protected int progress;
    protected int maxProgress;

    /** Synced to the menu: [0]=energy permille, [1]=1000, [2]=work permille, [3]=1000 when working. */
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> permille(getEnergy().getAmount(), getEnergy().getCapacity());
                case 1 -> 1000;
                case 2 -> permille(progress, maxProgress);
                case 3 -> maxProgress > 0 ? 1000 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Client-authoritative sync target is the menu's own SimpleContainerData; nothing to store here.
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    protected NeroTechMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            int machineSlots) {
        super(type, pos, state,
                NeroTechConfig.machineEnergyCapacity(), NeroTechConfig.machineMaxTransfer(),
                UPGRADE_SLOTS, UpgradeModuleItem.CLASSIFIER);
        this.machineSlots = machineSlots;
        this.items = NonNullList.withSize(machineSlots, ItemStack.EMPTY);
    }

    private static int permille(long amount, long max) {
        return max <= 0 ? 0 : (int) Math.max(0, Math.min(1000, amount * 1000L / max));
    }

    public ContainerData containerData() {
        return this.data;
    }

    /** Whether a machine I/O slot (0..machineSlots-1) accepts {@code stack}. Override per machine. */
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return true;
    }

    // --- persistence (Core's super handles energy + upgrades) ----------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", this.progress);
        output.putInt("MaxProgress", this.maxProgress);
        for (int i = 0; i < this.items.size(); i++) {
            output.store("Item" + i, ItemStack.OPTIONAL_CODEC, this.items.get(i));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("Progress", 0);
        this.maxProgress = input.getIntOr("MaxProgress", 0);
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, input.read("Item" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
    }

    // --- Container (machine slots then Core upgrade slots) -------------------

    @Override
    public int getContainerSize() {
        return this.machineSlots + upgrades().slots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < this.machineSlots) {
            return this.items.get(slot);
        }
        return upgrades().getStack(slot - this.machineSlots);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack current = getItem(slot);
        if (current.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = current.split(amount);
        if (!taken.isEmpty()) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < this.machineSlots) {
            this.items.set(slot, stack);
            setChanged();
        } else {
            upgrades().setStack(slot - this.machineSlots, stack);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < this.machineSlots) {
            return canPlaceMachineItem(slot, stack);
        }
        return upgrades().isModule(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        for (int i = 0; i < upgrades().slots(); i++) {
            upgrades().setStack(i, ItemStack.EMPTY);
        }
    }
}
