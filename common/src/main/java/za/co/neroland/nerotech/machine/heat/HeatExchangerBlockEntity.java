package za.co.neroland.nerotech.machine.heat;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.machine.tank.ProcessTankBlockEntity;
import za.co.neroland.nerotech.machine.tank.TankMedium;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;

/**
 * Heat Exchanger: moves heat between the machine on its front and the medium in the Process Tank on its
 * back (or the other way round, if the tank is on the front), either direction by {@link ExchangerModel}:
 * coolant absorbs machine heat and is used up, lava heats the machine and is used up, and any other medium
 * stores heat in the tank so a reactor's waste heat can reach a Foundry through the same tank column.
 * Never heat to energy. Passive: it draws no energy; Speed modules and the overclock preset scale its rate.
 *
 * <p>Ports are cached and rescanned only after a neighbour change. Holds no player data.
 */
public class HeatExchangerBlockEntity extends NeroTechMachineBlockEntity {

    public static final ProcessingSpec SPEC = ProcessingSpec.items(0, 0);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC).build();

    public static final String DETAIL_NO_TANK = "nerotech.machine.detail.heat_exchanger.no_tank";
    public static final String DETAIL_NO_MEDIUM = "nerotech.machine.detail.heat_exchanger.no_medium";
    public static final String DETAIL_NO_MACHINE = "nerotech.machine.detail.heat_exchanger.no_machine";
    public static final String DETAIL_BLOCKED = "nerotech.machine.detail.heat_exchanger.blocked";

    /** Detail codes on the shared {@code X_DETAIL} slot (outside the processing range; see HeatExchangerScreen). */
    public static final int CODE_NO_TANK = 30;
    public static final int CODE_NO_MACHINE = 31;
    public static final int CODE_NO_MEDIUM = 32;
    public static final int CODE_BLOCKED = 33;
    /** Lang key per code, indexed by {@code code - CODE_NO_TANK}. */
    public static final String[] CODE_KEYS = {DETAIL_NO_TANK, DETAIL_NO_MACHINE, DETAIL_NO_MEDIUM, DETAIL_BLOCKED};

    /** The last exchange's outcome, re-reported every tick so a stall commits (READOUT_SETTLE_TICKS). */
    private MachineStatus lastStatus = MachineStatus.IDLE;
    private int lastCode = ProcessingMachineBlockEntity.DETAIL_NONE;

    private final int phase = Math.floorMod(System.identityHashCode(this) * 37, 1024);

    /** Cached tank face; null with {@link #portsKnown} means "no tank". */
    @Nullable
    private Direction tankSide;
    private boolean portsKnown;

    /** Direction of the last exchange: {@link ExchangerModel#TO_TANK}, {@code TO_MACHINE} or {@code NONE}. */
    private int flow;
    private int syncedFlow;
    /** Render sync (C1 extra): which face holds the tank — 0 none, 1 back, 2 front; client copy too. */
    private int syncedTankFace = -1;
    private int clientTankFace;

    private String parsedCoolants;
    private String parsedHot;
    private Map<String, Integer> coolants = Map.of();
    private Map<String, Integer> hot = Map.of();

    public HeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(HeatContent.HEAT_EXCHANGER_BE.get(), pos, state, 0);
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .build());
    }

    /** Drop the cached ports; called by the block on neighbour changes. */
    public void invalidatePorts() {
        this.portsKnown = false;
    }

    /** Synced flow direction for the art (chevrons): +1 machine to tank, -1 tank to machine, 0 none. */
    public int flow() {
        return this.flow;
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        int interval = Math.max(1, za.co.neroland.nerotech.config.NeroTechConfig.thermalExchangeIntervalTicks());
        if ((level.getGameTime() + this.phase) % interval != 0) {
            setActive(this.flow != ExchangerModel.NONE);
            reportLast();
            return;
        }
        Direction facing = state.hasProperty(NeroTechMachineBlock.FACING)
                ? state.getValue(NeroTechMachineBlock.FACING) : Direction.NORTH;
        if (!this.portsKnown) {
            this.tankSide = scanTank(level, pos, facing);
            this.portsKnown = true;
        }
        int result = exchange(level, pos);
        this.flow = result;
        setActive(result != ExchangerModel.NONE);
        reportLast();
    }

    /** Remember an exchange outcome; {@link #reportLast} repeats it on the ticks in between. */
    private void remember(MachineStatus status, int code) {
        this.lastStatus = status;
        this.lastCode = code;
    }

    private void reportLast() {
        int code = this.lastCode;
        if (code >= CODE_NO_TANK && code < CODE_NO_TANK + CODE_KEYS.length) {
            reportStatus(this.lastStatus, CODE_KEYS[code - CODE_NO_TANK]);
        } else {
            reportStatus(this.lastStatus);
        }
    }

    private int exchange(Level level, BlockPos pos) {
        Direction tankFace = this.tankSide;
        if (tankFace == null || !(level.getBlockEntity(pos.relative(tankFace)) instanceof ProcessTankBlockEntity tank)) {
            this.portsKnown = false;
            remember(MachineStatus.STARVED, CODE_NO_TANK);
            return ExchangerModel.NONE;
        }
        Direction machineFace = tankFace.getOpposite();
        HeatRunMath.Node machine = HeatNodes.node(level, pos.relative(machineFace), machineFace.getOpposite());
        if (machine == null) {
            remember(MachineStatus.STARVED, CODE_NO_MACHINE);
            return ExchangerModel.NONE;
        }
        TankMedium medium = tank.columnMedium();
        long amount = tank.columnAmount();
        if (medium.isEmpty() || amount <= 0) {
            remember(MachineStatus.STARVED, CODE_NO_MEDIUM);
            return ExchangerModel.NONE;
        }
        refreshTables();
        ExchangerModel.Medium role = ExchangerModel.classify(medium.id(), this.coolants, this.hot);
        UpgradeModifiers mods = modifiers();
        int perOp = (int) Math.max(1, Math.round(HeatSettings.heatExchangerMaxPerOp()
                * mods.speedMultiplier() * presetSpeedFactor()));
        HeatRunMath.Node store = HeatNodes.machineNode(tank);
        ExchangerModel.Plan plan = ExchangerModel.plan(role, amount, machine.heat(), store.heat(),
                ambient(level, pos), HeatSettings.heatExchangerHotTemperature(), perOp,
                HeatSettings.heatExchangerConductivityPermille());
        if (plan.direction() == ExchangerModel.NONE || plan.heat() <= 0) {
            remember(MachineStatus.IDLE, ProcessingMachineBlockEntity.DETAIL_NONE);
            return ExchangerModel.NONE;
        }
        int moved;
        switch (role.role()) {
            case COOLANT -> {
                moved = machine.take(plan.heat());
                long mb = ExchangerModel.mbFor(moved, role.heatPerMb());
                if (mb > 0) {
                    tank.drainColumn(mb, false);
                }
            }
            case HOT -> {
                moved = machine.give(plan.heat());
                long mb = ExchangerModel.mbFor(moved, role.heatPerMb());
                if (mb > 0) {
                    tank.drainColumn(mb, false);
                }
            }
            default -> moved = plan.direction() == ExchangerModel.TO_TANK
                    ? ExchangerModel.transfer(machine, store, plan.heat())
                    : ExchangerModel.transfer(store, machine, plan.heat());
        }
        if (moved <= 0) {
            remember(MachineStatus.BLOCKED, CODE_BLOCKED);
            return ExchangerModel.NONE;
        }
        remember(MachineStatus.RUNNING, ProcessingMachineBlockEntity.DETAIL_NONE);
        setChanged();
        return plan.direction();
    }

    /** Back face first, then front: the first Process Tank found is the tank port. */
    @Nullable
    private static Direction scanTank(Level level, BlockPos pos, Direction facing) {
        Direction back = facing.getOpposite();
        if (level.getBlockEntity(pos.relative(back)) instanceof ProcessTankBlockEntity) {
            return back;
        }
        if (level.getBlockEntity(pos.relative(facing)) instanceof ProcessTankBlockEntity) {
            return facing;
        }
        return null;
    }

    private void refreshTables() {
        String rawCool = HeatSettings.heatExchangerCoolants();
        if (!rawCool.equals(this.parsedCoolants)) {
            this.coolants = ExchangerModel.parseTable(rawCool);
            this.parsedCoolants = rawCool;
        }
        String rawHot = HeatSettings.heatExchangerHotMedia();
        if (!rawHot.equals(this.parsedHot)) {
            this.hot = ExchangerModel.parseTable(rawHot);
            this.parsedHot = rawHot;
        }
    }

    /** 0 no tank found, 1 the tank is on the back face, 2 on the front face. */
    private int tankFaceCode() {
        Direction tank = this.tankSide;
        if (tank == null || !this.portsKnown) {
            return 0;
        }
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(NeroTechMachineBlock.FACING)
                ? state.getValue(NeroTechMachineBlock.FACING) : Direction.NORTH;
        return tank == facing ? 2 : 1;
    }

    /**
     * Whether the tank port is on the front face rather than the back (C1 extra, synced on change): the
     * chevrons point from the machine side to the tank side, so they flip with it.
     */
    public boolean tankAtFront() {
        if (this.level != null && this.level.isClientSide()) {
            return this.clientTankFace == 2;
        }
        return tankFaceCode() == 2;
    }

    @Override
    protected boolean renderSyncDirty() {
        int face = tankFaceCode();
        if (this.flow != this.syncedFlow || face != this.syncedTankFace) {
            this.syncedFlow = this.flow;
            this.syncedTankFace = face;
            return true;
        }
        return false;
    }

    @Override
    protected void writeRenderExtras(CompoundTag tag) {
        tag.putInt("HxTankFace", tankFaceCode());
    }

    @Override
    protected void readRenderExtras(ValueInput input) {
        this.clientTankFace = input.getIntOr("HxTankFace", 0);
    }

    // --- ProcessingMenu data: the shared layout, with no slots and no tanks of its own -------------

    @Override
    protected int extraDataCount() {
        return ProcessingMachineBlockEntity.EXTRA_COUNT;
    }

    @Override
    protected int extraData(int index) {
        if (index == ProcessingMachineBlockEntity.X_STATUS) {
            return stats().status().ordinal();
        }
        if (index == ProcessingMachineBlockEntity.X_DETAIL) {
            return this.lastCode;
        }
        if (index == ProcessingMachineBlockEntity.X_HEAT) {
            return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, heat()));
        }
        int t = index - ProcessingMachineBlockEntity.X_TANKS;
        if (t >= 0 && t % ProcessingMachineBlockEntity.TANK_STRIDE == 0) {
            return -1;
        }
        return 0;
    }

    // --- persistence: only the flow direction (rides the update tag for the art) -------------------

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
        output.putInt("HxFlow", this.flow);
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
        this.flow = Math.max(-1, Math.min(1, input.getIntOr("HxFlow", 0)));
        this.portsKnown = false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.heat_exchanger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ProcessingMenu(HeatContent.HEAT_EXCHANGER_MENU, id, inventory, this, this.data, LAYOUT);
    }
}
