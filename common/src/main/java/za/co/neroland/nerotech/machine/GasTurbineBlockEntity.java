package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.gas.MachineGasTank;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.gas.TurbineFuels;
import za.co.neroland.nerotech.menu.GasTurbineMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Gas Turbine — the clean end of NeroTech's gas chain (Stage C). It accepts gas pushed into its
 * internal tank through Core's gas capability (from an Electrolyzer, a Core Gas Tank, or anything
 * else on that surface), burns it a unit at a time, and pushes the resulting NE to its neighbours
 * exactly the way {@link NeroGeneratorBlockEntity} does — through Core's energy seam only, so
 * generation stays thin (the NeroPower split discipline).
 *
 * <p>Which gases burn, and how well, is config-driven ({@code turbineGasBurn}, default
 * {@code nerotech:hydrogen=2}); the tank refuses anything not on that list, so a stray gas can
 * never jam the turbine. One unit yields {@code gasTurbineNePerUnit x multiplier} NE spread evenly
 * over {@code gasTurbineTicksPerUnit} ticks.
 *
 * <p><b>Mild heat, zero pollution</b> — that is the design point: hydrogen power costs you the
 * electrolysis bill up front and gives you a generator that never dirties its region.
 */
public class GasTurbineBlockEntity extends NeroTechMachineBlockEntity {

    /** Burning gas runs cooler than burning coal: half a normal machine's heat per working tick. */
    private static final int HEAT_DIVISOR = 2;

    private final MachineGasTank fuel = new MachineGasTank(NeroTechConfig.machineGasCapacity(),
            TurbineFuels::burns, this::setChanged);

    /** NE/tick for the burn charge currently in progress (persisted alongside it). */
    private int burnRate;

    public GasTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_TURBINE.get(), pos, state, 0);
        // GENERATOR preset: ENERGY OUTPUT on every face. No item channel — the fuel is a gas.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.GENERATOR)
                .autoEject(Channel.ENERGY, true)
                .build());
    }

    @Nullable
    @Override
    public NeroGasStorage gasStorage(@Nullable Direction side) {
        return this.fuel;
    }

    @Override
    protected int extraDataCount() {
        return 1;
    }

    @Override
    protected int extraData(int index) {
        return index == 0 ? permille(this.fuel.getAmount(), this.fuel.getCapacity()) : 0;
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        UpgradeModifiers mods = modifiers();
        boolean roomToStore = getEnergy().getAmount() < getEnergy().getCapacity();

        if (this.progress > 0) {
            this.progress--;
            addHeat(Math.max(1, NeroTechConfig.heatPerOperation() / HEAT_DIVISOR));
            if (roomToStore) {
                // Stage H preset: output NE scales with the speed factor (heat scales at the base).
                energyBuffer().generate((int) Math.max(0,
                        Math.round(this.burnRate * mods.speedMultiplier() * presetSpeedFactor())));
            }
        } else {
            startBurn(roomToStore);
        }

        // BER surface: the rotor spins while a gas charge is burning.
        setActive(this.progress > 0);

        MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(),
                sideConfig());
    }

    /** Draw one unit of fuel and set up the burn charge, or report why we cannot. */
    private void startBurn(boolean roomToStore) {
        int multiplier = TurbineFuels.burnMultiplier(this.fuel.getGas());
        long unit = NeroTechGases.UNIT_MB;
        boolean fuelled = multiplier > 0 && this.fuel.getAmount() >= unit;
        if (fuelled && roomToStore) {
            int ticks = Math.max(1, NeroTechConfig.gasTurbineTicksPerUnit());
            int total = NeroTechConfig.gasTurbineNePerUnit() * multiplier;
            this.fuel.drain(unit, false);
            this.progress = ticks;
            this.maxProgress = ticks;
            this.burnRate = Math.max(1, total / ticks);
            setChanged();
        } else {
            // Analytics: waiting on gas reads STARVED; gas ready but the buffer full, BLOCKED.
            reportStatus(fuelled ? MachineStatus.BLOCKED : MachineStatus.STARVED);
            if (this.maxProgress != 0) {
                this.maxProgress = 0;
            }
        }
    }

    // --- persistence ---------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.fuel.save(output, "Fuel");
        output.putInt("BurnRate", this.burnRate);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.fuel.resize(NeroTechConfig.machineGasCapacity());
        this.fuel.load(input, "Fuel");
        this.burnRate = input.getIntOr("BurnRate", 0);
    }

    /** A generator is never load-shed by a Grid Controller (Stage D). */
    @Override
    public boolean shedable() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.gas_turbine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GasTurbineMenu(containerId, playerInventory, this, this.data);
    }
}
