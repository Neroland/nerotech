package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import za.co.neroland.nerolandcore.fluid.GatedFluidView;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideGating;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.fluid.MachineFluidTank;
import za.co.neroland.nerotech.fluid.NeroTechFluids;
import za.co.neroland.nerotech.gas.MachineGasTank;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.menu.ElectrolyzerMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Electrolyzer — the head of NeroTech's gas chain (Stage C). It burns NE to split stored water into
 * <b>hydrogen</b> and <b>oxygen</b> at the electrolytic 2:1 ratio, holding each product in its own
 * internal tank on Core's {@link NeroGasStorage} contract, and hands them off once a second to any
 * adjacent gas-accepting block through Core's {@code GasLookup} seam (Core's Gas Tank, the Gas
 * Turbine, the Chemical Processor, or any third-party block on the same capability).
 *
 * <p><b>Water in</b>: right-click with a water bucket (1000 mB per bucket), or pipe it in — the tank
 * is exposed both on Core's {@code FluidLookup} and on each loader's <i>standard</i> fluid capability
 * (NeoForge {@code Capabilities.Fluid.BLOCK}, Forge {@code FLUID_HANDLER}, Fabric
 * {@code FluidStorage.SIDED}), so any mod's fluid pipe fills it with no NeroTech dependency. The
 * FLUID side-config channel gates which faces accept it, and its auto-input pulls from an adjacent
 * tank on its own.
 *
 * <p>No item slots at all: this machine's whole I/O is fluid in, gas out. Emits no pollution — the
 * gas chain is NeroTech's <i>clean</i> branch, and its cost is the electricity bill.
 */
public class ElectrolyzerBlockEntity extends NeroTechMachineBlockEntity {

    /** Millibuckets in one vanilla bucket. */
    public static final int BUCKET_MB = 1_000;

    /** Per-side gas handoff budget each second (4 units). */
    private static final long PUSH_BUDGET_MB = NeroTechGases.UNIT_MB * 4L;

    private final MachineFluidTank water = MachineFluidTank.of(Fluids.WATER,
            NeroTechConfig.machineFluidCapacity(), this::setChanged);
    private final MachineGasTank hydrogen = MachineGasTank.of(NeroTechGases.HYDROGEN,
            NeroTechConfig.machineGasCapacity(), this::setChanged);
    private final MachineGasTank oxygen = MachineGasTank.of(NeroTechGases.OXYGEN,
            NeroTechConfig.machineGasCapacity(), this::setChanged);

    /** Spreads the once-per-second gas handoff across ticks (the pollutionPhase recipe). */
    private final int pushPhase = Math.floorMod(System.identityHashCode(this) * 23, 20);

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYZER.get(), pos, state, 0);
        // No item channel: power and water in, gas out. PROCESSOR gives ENERGY input on every face;
        // the per-channel presets keep water accepted on every face (ALL_INPUT) and both gases
        // extractable on every face (STORAGE = I/O), which is the handoff this machine always had.
        // FLUID auto-input is on by default so an adjacent tank or pipe end feeds it unattended.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .channel(Channel.FLUID)
                .channel(Channel.GAS)
                .defaultPreset(SidePreset.PROCESSOR)
                .preset(Channel.FLUID, SidePreset.ALL_INPUT)
                .preset(Channel.GAS, SidePreset.STORAGE)
                .autoInput(Channel.FLUID, true)
                .build());
        sideConfig().withFluid(this::waterTank);
    }

    /**
     * Empty one water bucket into the tank. Server-side only; returns false when the tank cannot take
     * a full bucket (partial fills would silently eat the rest of the bucket).
     */
    public boolean fillFromBucket() {
        if (!this.water.hasRoomFor(BUCKET_MB)) {
            return false;
        }
        // Verify what actually landed: a tank holding something else takes none of it, and a bucket
        // that vanished into a full or mismatched tank is a player's lost item.
        return this.water.forceFill(Fluids.WATER, BUCKET_MB) == BUCKET_MB;
    }

    /** The internal water tank (read surface for tooling; the GUI reads the synced gauge instead). */
    public NeroFluidStorage waterTank() {
        return this.water;
    }

    /**
     * Drop any non-water left in the tank by an older build. The tank only accepts water now, but a
     * world saved before the filter existed can still hold something else, which would read STARVED
     * for good with no way to empty it.
     */
    private void purgeNonWater() {
        if (this.water.getAmount() > 0 && this.water.getFluid() != Fluids.WATER) {
            NeroTechCommon.LOGGER.info(
                    "Electrolyzer at {} held {} mB of a non-water fluid from an older build; emptying it "
                            + "so the machine can run again.",
                    getBlockPos(), this.water.getAmount());
            this.water.drain(this.water.getAmount(), false);
        }
    }

    // --- Core fluid/gas surfaces ---------------------------------------------

    /**
     * Gas view per face. A single lookup can expose only one store, so the products are split by
     * face: <b>DOWN</b> hands out oxygen (the heavier gas), every other face hydrogen. The automatic
     * push in {@link #pushGas} is unaffected — it queries the neighbours, not this view.
     *
     * <p>The products are outputs only: a face set to I/O hands gas out but never takes it back, and
     * an INPUT face takes nothing. Otherwise a pipe that both pulls and pushes (a Universal Pipe on
     * AUTO) hands every millibucket straight back, the tanks never empty and the machine sits
     * BLOCKED on full product tanks.
     */
    @Nullable
    @Override
    public NeroGasStorage gasStorage(@Nullable Direction side) {
        MachineGasTank tank = side == Direction.DOWN ? this.oxygen : this.hydrogen;
        SideConfigComponent config = sideConfig();
        if (side == null || config == null || !config.config().has(Channel.GAS)) {
            return tank; // unsided/internal access is ungated, as everywhere else in Core
        }
        SideMode mode = config.config().modeAbsolute(Channel.GAS, config.facing(), side);
        if (mode == SideMode.DISABLED) {
            return null;
        }
        return SideGating.gas(tank,
                () -> outputOnly(config.config().modeAbsolute(Channel.GAS, config.facing(), side)));
    }

    /** A face's GAS mode with insertion removed: I/O becomes OUTPUT, INPUT becomes DISABLED. */
    private static SideMode outputOnly(SideMode mode) {
        return switch (mode) {
            case IO -> SideMode.OUTPUT;
            case INPUT -> SideMode.DISABLED;
            default -> mode;
        };
    }

    /**
     * Tank 0 is water (in), tank 1 the gas this face serves (out) — a fixed order, so an index a pipe
     * remembers keeps meaning the same thing even when a channel is closed on that face.
     */
    @Override
    public List<GatedFluidView> standardFluidViews(@Nullable Direction side) {
        MachineGasTank product = side == Direction.DOWN ? this.oxygen : this.hydrogen;
        return List.of(
                gatedView(this.water, Channel.FLUID, side),
                gatedView(NeroTechFluids.asFluid(product), Channel.GAS, side));
    }

    @Nullable
    @Override
    public NeroFluidStorage fluidStorage(@Nullable Direction side) {
        SideConfigComponent config = sideConfig();
        return config == null ? this.water : config.fluidView(side);
    }

    // --- GUI gauges (indices 7..9 after the seven shared ones) ---------------

    @Override
    protected int extraDataCount() {
        return 3;
    }

    @Override
    protected int extraData(int index) {
        return switch (index) {
            case 0 -> permille(this.water.getAmount(), this.water.getCapacity());
            case 1 -> permille(this.hydrogen.getAmount(), this.hydrogen.getCapacity());
            case 2 -> permille(this.oxygen.getAmount(), this.oxygen.getCapacity());
            default -> 0;
        };
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        int waterPerOp = NeroTechConfig.electrolyzerWaterPerOp();
        int hydrogenPerOp = NeroTechConfig.electrolyzerHydrogenPerOp();
        int oxygenPerOp = NeroTechConfig.electrolyzerOxygenPerOp();

        boolean hasWater = this.water.getAmount() >= waterPerOp;
        boolean roomForGas = this.hydrogen.hasRoomFor(hydrogenPerOp) && this.oxygen.hasRoomFor(oxygenPerOp);

        if (!hasWater || !roomForGas) {
            // Analytics: a dry tank reads STARVED; full product tanks BLOCKED.
            reportStatus(hasWater ? MachineStatus.BLOCKED : MachineStatus.STARVED);
            setActive(false);
            if (this.maxProgress != 0 || this.progress != 0) {
                this.progress = 0;
                this.maxProgress = 0;
                setChanged();
            }
            pushGas(level, pos);
            return;
        }

        UpgradeModifiers mods = modifiers();
        int effectiveTicks = Math.max(1, (int) Math.round(NeroTechConfig.electrolyzerOperationTicks()
                / Math.max(0.01D, mods.speedMultiplier() * presetSpeedFactor())));
        int cost = (int) Math.max(0, Math.round(NeroTechConfig.electrolyzerNePerTick()
                * mods.energyMultiplier() * presetEnergyFactor()));
        this.maxProgress = effectiveTicks;

        // Heat throttle: an electrolyzer run too hard stalls until it sheds heat.
        if (overheated()) {
            reportStatus(MachineStatus.THROTTLED);
            setActive(false);
            pushGas(level, pos);
            return;
        }

        setActive(energyBuffer().has(cost));

        if (energyBuffer().has(cost)) {
            energyBuffer().consume(cost);
            this.progress++;
            addHeat(NeroTechConfig.heatPerOperation());
            if (this.progress >= effectiveTicks) {
                this.water.drain(waterPerOp, false);
                this.hydrogen.produce(NeroTechGases.HYDROGEN, hydrogenPerOp);
                this.oxygen.produce(NeroTechGases.OXYGEN, oxygenPerOp);
                this.progress = 0;
            }
            setChanged();
        } else {
            reportStatus(MachineStatus.NO_ENERGY);
        }

        pushGas(level, pos);
    }

    /** Hand both products to adjacent gas sinks, once a second on this machine's phase. */
    private void pushGas(Level level, BlockPos pos) {
        if ((level.getGameTime() + this.pushPhase) % 20 != 0) {
            return;
        }
        MachineGas.pushToNeighbours(level, pos, this.hydrogen, PUSH_BUDGET_MB, sideConfig());
        MachineGas.pushToNeighbours(level, pos, this.oxygen, PUSH_BUDGET_MB, sideConfig());
    }

    // --- persistence ---------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.water.save(output, "Water");
        this.hydrogen.save(output, "Hydrogen");
        this.oxygen.save(output, "Oxygen");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Capacity is config-driven: re-apply it on load so a config change takes effect on reload.
        this.water.resize(NeroTechConfig.machineFluidCapacity());
        this.hydrogen.resize(NeroTechConfig.machineGasCapacity());
        this.oxygen.resize(NeroTechConfig.machineGasCapacity());
        this.water.load(input, "Water");
        purgeNonWater();
        this.hydrogen.load(input, "Hydrogen");
        this.oxygen.load(input, "Oxygen");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.electrolyzer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectrolyzerMenu(containerId, playerInventory, this, this.data);
    }
}
