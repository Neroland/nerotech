package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.RemediatorMenu;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Remediator — the Stage F <b>cleanup</b> machine: no items, no filters — it burns a heavy NE
 * cost per operation to strip pollution out of its own region only
 * ({@link PollutionManager#scrub} with {@code adjacentPermille = 0}). The energy bill is the
 * balance lever ({@code remediatorNePerOp}), and each op runs a full operation's worth of heat,
 * so sustained remediation needs real power and real cooling. Emits no pollution.
 *
 * <p>Slotless like the Solar Array ({@code machineSlots = 0}); ops batch on the
 * pollution-contribution interval with a per-machine phase (the {@code emitPollution} pattern),
 * so the region map is never touched per-tick.
 */
public class RemediatorBlockEntity extends NeroTechMachineBlockEntity {

    /** Spreads remediation ops across ticks so remediators don't all hit the map on the same tick. */
    private final int remediatePhase = Math.floorMod(System.identityHashCode(this), 40);

    public RemediatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REMEDIATOR.get(), pos, state, 0);
        // Pure NE sink: PROCESSOR preset gives ENERGY input on every face (no item channel).
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .build());
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        UpgradeModifiers mods = modifiers();
        int cost = (int) Math.max(0, Math.round(NeroTechConfig.remediatorNePerOp() * mods.energyMultiplier()));

        // Analytics: the energy precondition is cheap, so name it every tick (an op-tick-only
        // report would be clobbered by the RUNNING/IDLE default in between). "No pollution" stays
        // the default IDLE — the region map is only consulted on op ticks (map discipline).
        if (!energyBuffer().has(cost)) {
            reportStatus(MachineStatus.NO_ENERGY);
        }

        // Batch on the pollution-contribution interval with a per-machine phase (emitPollution's
        // recipe) — between op ticks the active flag and gauges simply hold their last state.
        int interval = NeroTechConfig.pollutionContributionIntervalTicks();
        if ((serverLevel.getGameTime() + this.remediatePhase) % interval != 0) {
            return;
        }

        int rate = (int) Math.max(1, Math.round(NeroTechConfig.remediatorPollutionPerOp() * mods.speedMultiplier()));

        boolean remediating = false;
        if (PollutionManager.regionPollution(serverLevel, pos) > 0 && energyBuffer().has(cost)) {
            energyBuffer().consume(cost);
            // Own region only: cleanup is local; the Scrubber owns the cross-region reach.
            PollutionManager.scrub(serverLevel, pos, rate, 0);
            // Remediation runs hot — a full operation's heat per op (the cooling half of the lever).
            addHeat(NeroTechConfig.heatPerOperation());
            remediating = true;
            setChanged();
        }

        // BER surface: booms sweep + mist drifts exactly while the region is being cleaned.
        setActive(remediating);
        // GUI surface: show "working" while remediating (the Solar Array display hook).
        this.progress = remediating ? 1 : 0;
        this.maxProgress = remediating ? 1 : 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.remediator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RemediatorMenu(containerId, playerInventory, this, this.data);
    }
}
