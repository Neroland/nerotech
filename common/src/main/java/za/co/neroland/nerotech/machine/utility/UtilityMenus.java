package za.co.neroland.nerotech.machine.utility;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;

/**
 * The utility blocks reuse the generic processing menu and screen with no slots and no tanks. This fills
 * the processing extras so that screen reads a status line and "no tank" for every tank.
 */
final class UtilityMenus {

    /**
     * Utility-only extras, in the gas-out tank's amount fields (the utility layouts have no tanks, and
     * that tank's capacity stays 0, so the shared screen never reads them as a tank): the charger's
     * reach (blocks) and per-tick cap (NE, clamped to a short); the panel's watched-machine count and
     * its lit alarm bits.
     */
    static final int X_UTILITY_A = ProcessingMachineBlockEntity.X_TANKS + 3 * ProcessingMachineBlockEntity.TANK_STRIDE + 1;
    static final int X_UTILITY_B = X_UTILITY_A + 1;

    private UtilityMenus() {
    }

    static int extraData(NeroTechMachineBlockEntity machine, int index) {
        if (index == X_UTILITY_A || index == X_UTILITY_B) {
            if (machine instanceof InductionChargerBlockEntity charger) {
                long value = index == X_UTILITY_A ? charger.reach() : charger.ratePerTick();
                return (int) Math.max(0L, Math.min(Short.MAX_VALUE, value));
            }
            if (machine instanceof AnnunciatorPanelBlockEntity panel) {
                return index == X_UTILITY_A ? panel.watchedCount() : panel.alarmMask();
            }
            return 0;
        }
        if (index == ProcessingMachineBlockEntity.X_STATUS) {
            return machine.stats().status().ordinal();
        }
        if (index == ProcessingMachineBlockEntity.X_DETAIL) {
            return machine instanceof InductionChargerBlockEntity charger
                    ? charger.idleCode() : ProcessingMachineBlockEntity.DETAIL_NONE;
        }
        int t = index - ProcessingMachineBlockEntity.X_TANKS;
        if (t >= 0 && t % ProcessingMachineBlockEntity.TANK_STRIDE == 0) {
            return -1; // tank medium: empty
        }
        return 0;
    }
}
