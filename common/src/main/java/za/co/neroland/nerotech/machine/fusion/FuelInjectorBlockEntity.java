package za.co.neroland.nerotech.machine.fusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.gas.NeroGases;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;

import za.co.neroland.nerotech.gas.MachineGasTank;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;

/**
 * Fuel Injector — holds deuterium and helium-3 for the fusion reactor it is built into. Two tanks, one
 * per gas, filled through the gas surface (an adjacent Core gas tank pushes in, or the injector pulls on
 * its auto-input faces). Every other gas is refused, and the injector has no item slot, so fuel cells,
 * fission fuel and rocket fuel never enter it. The reactor draws a whole charge at a time
 * ({@link #draw}); the injector itself burns nothing and needs no power.
 */
public class FuelInjectorBlockEntity extends NeroTechMachineBlockEntity {

    /** Ticks the couplings stay lit after an injection (one gas charge is 20 ticks by default). */
    private static final int PULSE_HOLD_TICKS = 30;

    private final MachineGasTank deuterium = MachineGasTank.of(NeroTechGases.DEUTERIUM,
            FusionSettings.injectorGasCapacity(), this::setChanged);
    private final MachineGasTank helium3 = MachineGasTank.of(NeroTechGases.HELIUM_3,
            FusionSettings.injectorGasCapacity(), this::setChanged);
    private final NeroGasStorage gasView = new FuelGas(this.deuterium, this.helium3);

    /** Ticks left of the "injecting" look; transient. */
    private int pulseHold;
    /** Render sync (C1 extra): which tanks were non-empty at the last push (bit 0 deuterium, bit 1 helium-3). */
    private int syncedFuelMask = -1;

    public FuelInjectorBlockEntity(BlockPos pos, BlockState state) {
        super(FusionContent.FUEL_INJECTOR_BE.get(), pos, state, 0);
        // STORAGE: gas in and out on every face (out only so a player can empty a wrongly filled tank
        // with a pump; the reactor's draw is internal). Auto-input pulls from an adjacent tank.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.GAS)
                .defaultPreset(SidePreset.STORAGE)
                .autoInput(Channel.GAS, true)
                .build());
        sideConfig().withGas(() -> this.gasView);
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (this.pulseHold > 0) {
            this.pulseHold--;
        }
        boolean fuelled = FusionMath.canIgniteGas(this.deuterium.getAmount(), this.helium3.getAmount(),
                FusionSettings.gasChargeTicks(), FusionSettings.deuteriumPerTick(), FusionSettings.helium3PerTick());
        if (!fuelled && this.pulseHold == 0) {
            // Analytics: this injector alone holds less than one charge of one gas or the other.
            reportStatus(MachineStatus.STARVED, "nerotech.status.fuel_injector.ratio");
        }
        setActive(this.pulseHold > 0);
    }

    // --- reactor surface -------------------------------------------------------------------------

    /** Whether any deuterium is held (C1 extra: the left coupling ring; synced on change). */
    public boolean hasDeuterium() {
        return this.deuterium.getAmount() > 0;
    }

    /** Whether any helium-3 is held (C1 extra: the right coupling ring; synced on change). */
    public boolean hasHelium3() {
        return this.helium3.getAmount() > 0;
    }

    @Override
    protected boolean renderSyncDirty() {
        int mask = (hasDeuterium() ? 1 : 0) | (hasHelium3() ? 2 : 0);
        if (mask != this.syncedFuelMask) {
            this.syncedFuelMask = mask;
            return true;
        }
        return false;
    }

    /** mB of deuterium held. */
    public long deuteriumAmount() {
        return this.deuterium.getAmount();
    }

    /** mB of helium-3 held. */
    public long helium3Amount() {
        return this.helium3.getAmount();
    }

    /** Take the reactor's share of a charge from this injector (amounts already planned to fit). */
    public void draw(long deuteriumMb, long helium3Mb) {
        if (deuteriumMb > 0) {
            this.deuterium.drain(deuteriumMb, false);
        }
        if (helium3Mb > 0) {
            this.helium3.drain(helium3Mb, false);
        }
        this.pulseHold = PULSE_HOLD_TICKS;
        pulseClient();
        setChanged();
    }

    // --- gas surface ------------------------------------------------------------------------------

    @Nullable
    @Override
    public NeroGasStorage gasStorage(@Nullable Direction side) {
        SideConfigComponent config = sideConfig();
        return config == null ? this.gasView : config.gasView(side);
    }

    /** Extras: 0/1 permille D/He, 2-5 D and He mB (hi, lo), 6 one charge held (1/0), 7-8 capacity (hi, lo). */
    public static final int EXTRA_COUNT = 9;

    @Override
    protected int extraDataCount() {
        return EXTRA_COUNT;
    }

    @Override
    protected int extraData(int index) {
        return switch (index) {
            case 0 -> permille(this.deuterium.getAmount(), this.deuterium.getCapacity());
            case 1 -> permille(this.helium3.getAmount(), this.helium3.getCapacity());
            case 2 -> ProcessingMachineBlockEntity.hi(this.deuterium.getAmount());
            case 3 -> ProcessingMachineBlockEntity.lo(this.deuterium.getAmount());
            case 4 -> ProcessingMachineBlockEntity.hi(this.helium3.getAmount());
            case 5 -> ProcessingMachineBlockEntity.lo(this.helium3.getAmount());
            case 6 -> FusionMath.canIgniteGas(this.deuterium.getAmount(), this.helium3.getAmount(),
                    FusionSettings.gasChargeTicks(), FusionSettings.deuteriumPerTick(),
                    FusionSettings.helium3PerTick()) ? 1 : 0;
            case 7 -> ProcessingMachineBlockEntity.hi(this.deuterium.getCapacity());
            case 8 -> ProcessingMachineBlockEntity.lo(this.deuterium.getCapacity());
            default -> 0;
        };
    }

    // --- persistence (new block: both keys optional, default empty) -------------------------------

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    //?} else {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
    *///?}
        this.deuterium.save(output, "Deuterium");
        this.helium3.save(output, "Helium3");
    }

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    //?} else {
    /*@Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
    *///?}
        this.deuterium.resize(FusionSettings.injectorGasCapacity());
        this.helium3.resize(FusionSettings.injectorGasCapacity());
        this.deuterium.load(input, "Deuterium");
        this.helium3.load(input, "Helium3");
    }

    /** A passive port: nothing to shed. */
    @Override
    public boolean shedable() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.fuel_injector");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FuelInjectorMenu(containerId, playerInventory, this, this.data);
    }

    /**
     * The injector's one gas surface over its two tanks: a fill goes to the tank of that gas (anything
     * else is refused); a drain and the reported gas follow whichever tank holds more.
     */
    private record FuelGas(MachineGasTank deuterium, MachineGasTank helium3) implements NeroGasStorage {

        private MachineGasTank fuller() {
            return this.helium3.getAmount() > this.deuterium.getAmount() ? this.helium3 : this.deuterium;
        }

        @Override
        public Identifier getGas() {
            MachineGasTank tank = fuller();
            return tank.getAmount() > 0 ? tank.getGas() : NeroGases.EMPTY;
        }

        @Override
        public long getAmount() {
            return fuller().getAmount();
        }

        @Override
        public long getCapacity() {
            return fuller().getCapacity();
        }

        @Override
        public long fill(Identifier gas, long amount, boolean simulate) {
            Identifier canonical = NeroTechGases.canonical(gas);
            if (NeroTechGases.DEUTERIUM.equals(canonical)) {
                return this.deuterium.fill(canonical, amount, simulate);
            }
            if (NeroTechGases.HELIUM_3.equals(canonical)) {
                return this.helium3.fill(canonical, amount, simulate);
            }
            return 0L;
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return fuller().drain(amount, simulate);
        }
    }
}
