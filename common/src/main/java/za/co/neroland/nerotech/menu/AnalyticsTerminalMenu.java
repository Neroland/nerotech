package za.co.neroland.nerotech.menu;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;

import za.co.neroland.nerotech.machine.AnalyticsTerminalBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.network.AnalyticsTerminalPayload;
import za.co.neroland.nerotech.network.MachineStatsPayload;
import za.co.neroland.nerotech.registry.ModMenuTypes;

/**
 * Analytics Terminal menu: no machine slots — the top of the (taller, 222px) GUI is the dashboard
 * drawn by {@code client.AnalyticsTerminalScreen}, with the upgrade column and player inventory
 * pushed to the bottom. Overrides {@link #analyticsPayload} so the menu-open stream carries the
 * terminal's dashboard snapshot instead of the per-machine stats window.
 */
public class AnalyticsTerminalMenu extends MachineMenu {

    private static final int MACHINE_SLOTS = 0;

    /** Player inventory top edge in the taller GUI (hotbar at +58, the vanilla stride). */
    private static final int INVENTORY_Y = 140;

    public AnalyticsTerminalMenu(int id, Inventory playerInventory) {
        this(id, playerInventory,
                new SimpleContainer(MACHINE_SLOTS + NeroTechMachineBlockEntity.UPGRADE_SLOTS),
                new SimpleContainerData(7));
    }

    public AnalyticsTerminalMenu(int id, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.ANALYTICS_TERMINAL.get(), id, container, data, MACHINE_SLOTS);
        addTerminalSlots(playerInventory);
    }

    /**
     * The taller-GUI slot layout (the base {@code addUpgradeAndPlayerSlots} hardcodes the 166px
     * offsets): the upgrade modules as one horizontal row just above the inventory, then the
     * player inventory + hotbar at the 222px offsets.
     */
    private void addTerminalSlots(Inventory playerInventory) {
        captureStatsTarget(playerInventory);
        int upgrades = this.totalNonPlayer - this.machineSlots;
        for (int i = 0; i < upgrades; i++) {
            this.addSlot(new PredicateSlot(this.container, this.machineSlots + i, 102 + i * 18, 112));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18,
                        INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    /** Stream the dashboard snapshot instead of the per-machine stats window. */
    @Override
    protected CustomPacketPayload analyticsPayload(NeroTechMachineBlockEntity machine) {
        if (machine instanceof AnalyticsTerminalBlockEntity terminal) {
            return AnalyticsTerminalPayload.of(this.containerId, terminal);
        }
        return MachineStatsPayload.of(this.containerId, machine); // unreachable in practice
    }
}
