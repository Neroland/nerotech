package za.co.neroland.nerotech.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import za.co.neroland.nerotech.machine.ColliderStatus;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/**
 * Accelerator Controller menu: injection slot + collision slot + output, the upgrade column and the
 * player inventory. On top of the seven shared gauges it syncs five accelerator readouts — see
 * {@code ColliderCoreBlockEntity.extraData} for the scaling (ContainerData rides shorts, so speed is
 * in tenths and collision energy in tens of joules).
 */
public class ColliderMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 3;
    /** Seven shared gauges + five accelerator ones. */
    private static final int DATA_SLOTS = 12;

    public ColliderMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(DATA_SLOTS));
    }

    public ColliderMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.COLLIDER.get(), id, container, data, MACHINE_SLOTS);
        this.addSlot(new PredicateSlot(container, 0, 44, 33));   // injected particle
        this.addSlot(new PredicateSlot(container, 1, 66, 33));   // collision target
        this.addSlot(new OutputSlot(container, 2, 116, 33));
        addUpgradeAndPlayerSlots(playerInventory);
    }

    /** Current beam speed in "metres per second" (synced in tenths). */
    public double beamSpeed() {
        return extraValue(0) / 10.0D;
    }

    /** Current collision energy in joules (synced in tens). */
    public int collisionEnergy() {
        return extraValue(1) * 10;
    }

    /** Guides on the traced beam line. */
    public int guideCount() {
        return extraValue(2);
    }

    /** Whether the traced beam line comes back into the controller. */
    public boolean loopClosed() {
        return extraValue(3) > 0;
    }

    /** The beam status line. */
    public ColliderStatus beamStatus() {
        return ColliderStatus.byOrdinal(extraValue(4));
    }
}
