package za.co.neroland.nerotech.menu;

import java.util.function.Supplier;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;

/**
 * The generic menu for every {@link ProcessingMachineBlockEntity}: slots placed by a
 * {@link ProcessingLayout}, the shared {@code ContainerData} (registered by {@link MachineMenu}'s
 * constructor through {@code addDataSlots}) plus the processing extras, read back through the typed
 * accessors below. A family registers one {@code MenuType} per machine:
 *
 * <pre>{@code
 * MENUS.register("plate_press", key -> new MenuType<>(
 *         (id, inv) -> new ProcessingMenu(PLATE_PRESS_MENU, id, inv, PlatePressBlockEntity.LAYOUT),
 *         FeatureFlags.VANILLA_SET));
 * }</pre>
 *
 * and the block entity's {@code createMenu} returns
 * {@code new ProcessingMenu(PLATE_PRESS_MENU, id, inv, this, this.data, LAYOUT)}. Subclass for a
 * special screen; keep the slot order (inputs, catalyst, outputs, upgrades, player).
 */
public class ProcessingMenu extends MachineMenu {

    private final ProcessingLayout layout;

    /** Client constructor (the {@code MenuType} factory). */
    public ProcessingMenu(Supplier<? extends MenuType<?>> type, int id, Inventory playerInventory,
            ProcessingLayout layout) {
        this(type, id, playerInventory,
                new SimpleContainer(layout.spec().slotCount() + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(ProcessingMachineBlockEntity.DATA_COUNT), layout);
    }

    /** Server constructor: the live block entity and its data. */
    public ProcessingMenu(Supplier<? extends MenuType<?>> type, int id, Inventory playerInventory,
            Container container, ContainerData data, ProcessingLayout layout) {
        super(type.get(), id, container, data, layout.spec().slotCount());
        this.layout = layout;
        ProcessingSpec spec = layout.spec();
        for (int i = 0; i < spec.inputs(); i++) {
            int[] at = layout.input(i);
            this.addSlot(new PredicateSlot(container, spec.inputSlot(i), at[0], at[1]));
        }
        if (spec.catalyst()) {
            int[] at = layout.catalyst();
            this.addSlot(new PredicateSlot(container, spec.catalystSlot(), at[0], at[1]));
        }
        for (int i = 0; i < spec.outputs(); i++) {
            int[] at = layout.output(i);
            this.addSlot(new OutputSlot(container, spec.outputSlot(i), at[0], at[1]));
        }
        addUpgradeAndPlayerSlots(playerInventory, layout.upgradeX(), layout.upgradeY(), layout.upgradeColumns(),
                layout.playerY());
    }

    public ProcessingLayout layout() {
        return this.layout;
    }

    private int extra(int index) {
        return extraValue(index);
    }

    /** The live analytics status (not debounced: the GUI shows the cause the moment it appears). */
    public MachineStatus status() {
        return MachineStatus.byOrdinal(extra(ProcessingMachineBlockEntity.X_STATUS));
    }

    /** The detail code ({@link ProcessingMachineBlockEntity#DETAIL_NONE} when none). */
    public int detail() {
        // extraValue returns 0 for a short data array; -1 rides the short sync fine.
        return extra(ProcessingMachineBlockEntity.X_DETAIL);
    }

    public int detailParam() {
        return extra(ProcessingMachineBlockEntity.X_DETAIL_PARAM);
    }

    public int heatAvailable() {
        return extra(ProcessingMachineBlockEntity.X_HEAT);
    }

    /** NE per tick the current recipe draws (0 when idle). */
    public int energyRate() {
        return extra(ProcessingMachineBlockEntity.X_ENERGY_RATE);
    }

    private int tankField(int tank, int field) {
        return extra(ProcessingMachineBlockEntity.X_TANKS + tank * ProcessingMachineBlockEntity.TANK_STRIDE + field);
    }

    /** Medium id of a tank: a fluid raw id or a gas index into {@code NeroTechGases.ALL}; -1 when empty. */
    public int tankMedium(int tank) {
        return tankField(tank, 0);
    }

    /** Tank amount in mB. */
    public long tankAmount(int tank) {
        return ProcessingMachineBlockEntity.join(tankField(tank, 1), tankField(tank, 2));
    }

    /** Tank capacity in mB (0 = the machine has no such tank). */
    public long tankCapacity(int tank) {
        return ProcessingMachineBlockEntity.join(tankField(tank, 3), tankField(tank, 4));
    }
}
