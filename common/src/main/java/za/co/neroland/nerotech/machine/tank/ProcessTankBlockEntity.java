package za.co.neroland.nerotech.machine.tank;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.fluid.GatedFluidView;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.gas.NeroGases;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;

import za.co.neroland.nerotech.fluid.MachineFluidTank;
import za.co.neroland.nerotech.fluid.NeroTechFluids;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.machine.MediumExchange;
import za.co.neroland.nerotech.machine.MediumTransfer;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModDataComponents;

/**
 * Process Tank — holds one medium at a time, a fluid <i>or</i> a gas, and stacks into a vertical
 * column of up to {@value TankColumnMath#MAX_COLUMN} tanks that shares one level.
 *
 * <p>Each tank keeps its own <b>share</b> ({@link #medium} + {@link #amount}, at most
 * {@link #CAPACITY_MB}); the column is resolved from the stack on demand, cached, and dropped again on
 * any neighbour change. The <b>bottom</b> tank is the column's reference: every face of every tank
 * reads and writes the whole column (fill bottom-first, drain top-first) through views that start at
 * the bottom. Resolving a column also settles it, so merged columns pour down and stay consistent.
 * The math lives in {@link TankColumnMath} (no Minecraft types, unit-tested).
 *
 * <p>Breaking a tank first moves its share into the rest of its column; whatever does not fit leaves
 * on the dropped tank item ({@link TankContents}), so nothing is lost and nothing is duplicated.
 *
 * <p>Exposed on Core's fluid and gas surfaces and on each loader's standard fluid capability (a gas
 * wears its transport fluid there). Zero NE buffer: the tank neither stores nor moves power.
 */
public class ProcessTankBlockEntity extends NeroTechMachineBlockEntity {

    /** Millibuckets one tank holds (fluid or gas alike). */
    public static final long CAPACITY_MB = 16_000L;

    /** How far down/up a stack is walked when resolving columns. */
    private static final int STACK_LIMIT = 256;

    private TankMedium medium = TankMedium.EMPTY;
    private long amount;

    /** The resolved column (bottom-up, includes this tank); null when it must be re-resolved. */
    @Nullable
    private List<ProcessTankBlockEntity> column;

    /** Transient: removal already dropped this tank's item, so the block's loot withholds its own. */
    private boolean carrierDropped;

    /** Fill steps the BER sync resolves (a packet only when a share or the column crosses one). */
    private static final int FILL_SYNC_STEPS = 64;
    /** Render sync (machine contract C1): last pushed share step, medium and column-fill step. */
    private int syncedShareStep = -1;
    private TankMedium syncedMedium = TankMedium.EMPTY;
    private int syncedColumnStep = -1;
    /** Server: the column's contents changed since the last sync check. */
    private boolean columnFillDirty = true;
    /** Client: the column fill (0..1) from the last update tag. */
    private float clientColumnFill;

    public ProcessTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROCESS_TANK.get(), pos, state, 0, 0, 0);
        // Fluid and gas in and out on every face by default. Core's builder defaults to ALL_DISABLED,
        // which would hide the tank from every pipe, hence the explicit preset.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.FLUID)
                .channel(Channel.GAS)
                .defaultPreset(SidePreset.STORAGE)
                .build());
    }

    @Override
    public boolean shedable() {
        return false;
    }

    /**
     * Passive by default: machines push into the tank and pull from it. The tank pushes on its own only
     * out of a face the player set to PUSH in Side Config, every {@link MediumTransfer#INTERVAL_TICKS}
     * ticks, and never into a tank of its own column.
     */
    @Override
    protected void exchangeMedia(Level level, BlockPos pos) {
        if (!MediumTransfer.due(level.getGameTime(), exchangePhase())) {
            return;
        }
        TankMedium medium = columnMedium();
        if (medium.isEmpty()) {
            return;
        }
        List<ProcessTankBlockEntity> members = column();
        java.util.function.Predicate<BlockEntity> skip =
                be -> be instanceof ProcessTankBlockEntity other && members.contains(other);
        if (medium.kind() == TankMedium.Kind.GAS) {
            MediumExchange.pushGas(level, pos, sideConfig(), new ColumnGasView(this, () -> true, () -> true),
                    (gas, mb) -> fillColumn(TankMedium.gas(NeroTechGases.canonical(gas).toString()), mb, false),
                    MediumTransfer::tankPushes, skip);
        } else {
            MediumExchange.pushFluid(level, pos, sideConfig(), new ColumnFluidView(this, () -> true, () -> true),
                    (fluid, mb) -> fillColumn(mediumOf(fluid), mb, false), MediumTransfer::tankPushes, skip);
        }
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        // Passive storage. A neighbour change drops the cached column; resolve it again here so the
        // `column` blockstate follows the stack (a null check per tick, the walk only on change).
        if (this.column == null) {
            column();
        }
    }

    /**
     * The column's fill, 0..1 (machine contract C1). Server: live; client: the last synced value,
     * pushed only when it crosses one of {@value #FILL_SYNC_STEPS} steps.
     */
    public float columnFill() {
        if (this.level != null && this.level.isClientSide()) {
            return this.clientColumnFill;
        }
        long capacity = columnCapacity();
        return capacity <= 0 ? 0.0F : Math.min(1.0F, columnAmount() / (float) capacity);
    }

    @Override
    protected boolean renderSyncDirty() {
        int shareStep = (int) (this.amount * FILL_SYNC_STEPS / CAPACITY_MB);
        boolean dirty = shareStep != this.syncedShareStep || !this.medium.equals(this.syncedMedium);
        if (this.columnFillDirty) {
            this.columnFillDirty = false;
            int columnStep = (int) (columnFill() * FILL_SYNC_STEPS);
            if (columnStep != this.syncedColumnStep) {
                this.syncedColumnStep = columnStep;
                dirty = true;
            }
        }
        if (dirty) {
            this.syncedShareStep = shareStep;
            this.syncedMedium = this.medium;
        }
        return dirty;
    }

    @Override
    protected void writeRenderExtras(CompoundTag tag) {
        tag.putInt("ColFill", Math.round(columnFill() * 10_000.0F)); // ten-thousandths
    }

    @Override
    protected void readRenderExtras(ValueInput input) {
        this.clientColumnFill = input.getIntOr("ColFill", 0) / 10_000.0F;
    }

    /** Set this tank's {@code column} blockstate (server side, only on a real change; no neighbour updates). */
    private void applyColumnPart(ProcessTankBlock.Column part) {
        Level level = this.level;
        if (level == null || level.isClientSide() || this.isRemoved()) {
            return;
        }
        BlockState state = level.getBlockState(this.worldPosition);
        if (state.getBlock() instanceof ProcessTankBlock && state.hasProperty(ProcessTankBlock.COLUMN)
                && state.getValue(ProcessTankBlock.COLUMN) != part) {
            level.setBlock(this.worldPosition, state.setValue(ProcessTankBlock.COLUMN, part),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        this.columnFillDirty = true;
    }

    // --- share accessors -------------------------------------------------------------------------

    /** This tank's own share (not the column's). */
    public long ownAmount() {
        return this.amount;
    }

    public TankMedium ownMedium() {
        return this.medium;
    }

    /** Whether removal already dropped this tank's item (the block's loot then withholds its own). */
    public boolean carrierDropped() {
        return this.carrierDropped;
    }

    private void setShare(TankMedium newMedium, long newAmount) {
        TankMedium m = newAmount <= 0 ? TankMedium.EMPTY : newMedium;
        long a = Math.max(0L, newAmount);
        if (!m.equals(this.medium) || a != this.amount) {
            this.medium = m;
            this.amount = a;
            setChanged();
        }
    }

    // --- column resolution -----------------------------------------------------------------------

    /** The column this tank belongs to, bottom-up (always contains this tank). */
    public List<ProcessTankBlockEntity> column() {
        List<ProcessTankBlockEntity> cached = this.column;
        if (cached != null && cached.stream().noneMatch(BlockEntity::isRemoved)) {
            return cached;
        }
        resolveStack();
        return this.column == null ? List.of(this) : this.column;
    }

    /** Drop the cached column of every tank in this contiguous stack (called on neighbour change). */
    public void invalidateStack() {
        this.column = null;
        if (this.level == null) {
            return;
        }
        for (Direction dir : new Direction[] {Direction.DOWN, Direction.UP}) {
            BlockPos cursor = this.worldPosition.relative(dir);
            for (int i = 0; i < STACK_LIMIT; i++) {
                if (!(this.level.getBlockEntity(cursor) instanceof ProcessTankBlockEntity tank)) {
                    break;
                }
                tank.column = null;
                cursor = cursor.relative(dir);
            }
        }
    }

    /** Walk the contiguous stack, partition it into columns, cache them and settle each one. */
    private void resolveStack() {
        if (this.level == null) {
            this.column = List.of(this);
            return;
        }
        BlockPos base = this.worldPosition;
        for (int i = 0; i < STACK_LIMIT; i++) {
            if (!(this.level.getBlockEntity(base.below()) instanceof ProcessTankBlockEntity)) {
                break;
            }
            base = base.below();
        }
        List<ProcessTankBlockEntity> stack = new ArrayList<>();
        BlockPos cursor = base;
        for (int i = 0; i < STACK_LIMIT; i++) {
            if (!(this.level.getBlockEntity(cursor) instanceof ProcessTankBlockEntity tank) || tank.isRemoved()) {
                break;
            }
            stack.add(tank);
            cursor = cursor.above();
        }
        if (!stack.contains(this)) {
            this.column = List.of(this);
            return;
        }
        List<TankMedium> media = new ArrayList<>(stack.size());
        for (ProcessTankBlockEntity tank : stack) {
            media.add(tank.medium);
        }
        boolean server = !this.level.isClientSide();
        for (int[] range : TankColumnMath.partition(media, TankColumnMath.MAX_COLUMN)) {
            List<ProcessTankBlockEntity> members = List.copyOf(stack.subList(range[0], range[1]));
            for (ProcessTankBlockEntity member : members) {
                member.column = members;
            }
            if (server) {
                settle(members);
                for (int i = 0; i < members.size(); i++) {
                    members.get(i).applyColumnPart(ProcessTankBlock.Column.of(i, members.size()));
                }
            }
        }
    }

    /** Pour a column's contents bottom-first and give every member the column's medium. */
    private static void settle(List<ProcessTankBlockEntity> members) {
        TankMedium medium = mediumOf(members);
        long[] settled = TankColumnMath.settle(shares(members), CAPACITY_MB);
        for (int i = 0; i < members.size(); i++) {
            members.get(i).setShare(medium, settled[i]);
        }
    }

    private static TankMedium mediumOf(List<ProcessTankBlockEntity> members) {
        TankMedium medium = TankMedium.EMPTY;
        for (ProcessTankBlockEntity member : members) {
            if (member.amount > 0) {
                medium = medium.or(member.medium);
            }
        }
        return medium;
    }

    private static long[] shares(List<ProcessTankBlockEntity> members) {
        long[] out = new long[members.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = members.get(i).amount;
        }
        return out;
    }

    // --- column operations -----------------------------------------------------------------------

    public TankMedium columnMedium() {
        return mediumOf(column());
    }

    public long columnAmount() {
        return TankColumnMath.total(shares(column()));
    }

    public long columnCapacity() {
        return CAPACITY_MB * column().size();
    }

    /** Insert {@code amount} of {@code incoming} into the column; @return mB accepted. */
    public long fillColumn(TankMedium incoming, long amount, boolean simulate) {
        if (incoming == null || incoming.isEmpty() || amount <= 0) {
            return 0L;
        }
        List<ProcessTankBlockEntity> members = column();
        if (!mediumOf(members).compatible(incoming)) {
            return 0L;
        }
        long[] shares = shares(members);
        long accepted = TankColumnMath.fill(shares, CAPACITY_MB, amount, simulate);
        if (!simulate && accepted > 0) {
            for (int i = 0; i < members.size(); i++) {
                members.get(i).setShare(incoming, shares[i]);
            }
            signalColumn(members);
        }
        return accepted;
    }

    /** Remove up to {@code amount} from the column (top-first); @return mB removed. */
    public long drainColumn(long amount, boolean simulate) {
        List<ProcessTankBlockEntity> members = column();
        TankMedium medium = mediumOf(members);
        if (medium.isEmpty() || amount <= 0) {
            return 0L;
        }
        long[] shares = shares(members);
        long drained = TankColumnMath.drain(shares, amount, simulate);
        if (!simulate && drained > 0) {
            for (int i = 0; i < members.size(); i++) {
                members.get(i).setShare(medium, shares[i]);
            }
            signalColumn(members);
        }
        return drained;
    }

    /** Comparators on any tank of the column read the column's fill. */
    private static void signalColumn(List<ProcessTankBlockEntity> members) {
        for (ProcessTankBlockEntity member : members) {
            member.columnFillDirty = true;
            Level level = member.level;
            if (level != null && !level.isClientSide()) {
                level.updateNeighbourForOutputSignal(member.worldPosition, member.getBlockState().getBlock());
            }
        }
    }

    /** Comparator strength 0–15 for the whole column's fill. */
    public int comparatorSignal() {
        return TankColumnMath.comparatorSignal(columnAmount(), columnCapacity());
    }

    // --- medium <-> Minecraft ids ----------------------------------------------------------------

    @Nullable
    static Fluid fluidOf(TankMedium medium) {
        if (medium.kind() == TankMedium.Kind.GAS) {
            Fluid transport = NeroTechFluids.fluidFor(Identifier.tryParse(medium.id()));
            return transport == Fluids.EMPTY ? null : transport;
        }
        if (medium.kind() != TankMedium.Kind.FLUID) {
            return null;
        }
        Identifier id = Identifier.tryParse(medium.id());
        return id == null ? null : BuiltInRegistries.FLUID.getValue(id);
    }

    /** A fluid offered by a pipe, as a medium: a gas transport fluid becomes its gas. */
    static TankMedium mediumOf(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return TankMedium.EMPTY;
        }
        Identifier gas = NeroTechFluids.gasFor(fluid);
        if (gas != null) {
            return TankMedium.gas(gas.toString());
        }
        Fluid source = MachineFluidTank.normalize(fluid);
        return TankMedium.fluid(BuiltInRegistries.FLUID.getKey(source).toString());
    }

    /** The medium's display name (the liquid block's or the gas's translation key). */
    public static Component displayName(TankMedium medium) {
        Identifier id = Identifier.tryParse(medium.id());
        if (id == null) {
            return Component.translatable("block.nerotech.process_tank.empty");
        }
        String prefix = medium.kind() == TankMedium.Kind.GAS ? "gas." : "block.";
        return Component.translatable(prefix + id.getNamespace() + "." + id.getPath());
    }

    // --- surfaces --------------------------------------------------------------------------------

    @Nullable
    @Override
    public NeroGasStorage gasStorage(@Nullable Direction side) {
        BooleanSupplier[] gate = gate(Channel.GAS, side);
        return gate == null ? null : new ColumnGasView(this, gate[0], gate[1]);
    }

    @Nullable
    @Override
    public NeroFluidStorage fluidStorage(@Nullable Direction side) {
        BooleanSupplier[] gate = gate(Channel.FLUID, side);
        return gate == null ? null : new ColumnFluidView(this, gate[0], gate[1]);
    }

    /**
     * One tank per face on the standard fluid capability: the whole column, gated by the FLUID
     * channel for a liquid (or empty) column and by the GAS channel for a gas column.
     */
    @Override
    public List<GatedFluidView> standardFluidViews(@Nullable Direction side) {
        Channel channel = columnMedium().kind() == TankMedium.Kind.GAS ? Channel.GAS : Channel.FLUID;
        return List.of(gatedView(new ColumnFluidView(this, () -> true, () -> true), channel, side));
    }

    /** {insert, extract} permission suppliers for a face, or null when the face is closed. */
    @Nullable
    private BooleanSupplier[] gate(Channel channel, @Nullable Direction side) {
        SideConfigComponent config = sideConfig();
        if (side == null || config == null || !config.config().has(channel)) {
            return new BooleanSupplier[] {() -> true, () -> true};
        }
        if (config.config().modeAbsolute(channel, config.facing(), side) == SideMode.DISABLED) {
            return null;
        }
        return new BooleanSupplier[] {
            () -> config.config().modeAbsolute(channel, config.facing(), side).canInsert(),
            () -> config.config().modeAbsolute(channel, config.facing(), side).canExtract()};
    }

    /** The column on Core's fluid surface (and, wrapped, the standard one). */
    private record ColumnFluidView(ProcessTankBlockEntity tank, BooleanSupplier canInsert,
            BooleanSupplier canExtract) implements NeroFluidStorage {

        @Override
        public Fluid getFluid() {
            Fluid fluid = fluidOf(this.tank.columnMedium());
            return fluid == null ? Fluids.EMPTY : fluid;
        }

        @Override
        public long getAmount() {
            return this.tank.columnAmount();
        }

        @Override
        public long getCapacity() {
            return this.tank.columnCapacity();
        }

        @Override
        public long fill(Fluid fluid, long amount, boolean simulate) {
            return this.canInsert.getAsBoolean() ? this.tank.fillColumn(mediumOf(fluid), amount, simulate) : 0L;
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return this.canExtract.getAsBoolean() ? this.tank.drainColumn(amount, simulate) : 0L;
        }
    }

    /** The column on Core's gas surface; a liquid column reads as empty and refuses gas. */
    private record ColumnGasView(ProcessTankBlockEntity tank, BooleanSupplier canInsert,
            BooleanSupplier canExtract) implements NeroGasStorage {

        private boolean isGas() {
            return this.tank.columnMedium().kind() == TankMedium.Kind.GAS;
        }

        @Override
        public Identifier getGas() {
            TankMedium medium = this.tank.columnMedium();
            Identifier id = medium.kind() == TankMedium.Kind.GAS ? Identifier.tryParse(medium.id()) : null;
            return id == null ? NeroGases.EMPTY : id;
        }

        @Override
        public long getAmount() {
            return isGas() ? this.tank.columnAmount() : 0L;
        }

        @Override
        public long getCapacity() {
            return this.tank.columnCapacity();
        }

        @Override
        public long fill(Identifier gas, long amount, boolean simulate) {
            Identifier canonical = NeroTechGases.canonical(gas);
            if (!this.canInsert.getAsBoolean() || canonical == null || NeroGases.EMPTY.equals(canonical)) {
                return 0L;
            }
            return this.tank.fillColumn(TankMedium.gas(canonical.toString()), amount, simulate);
        }

        @Override
        public long drain(long amount, boolean simulate) {
            return this.canExtract.getAsBoolean() && isGas() ? this.tank.drainColumn(amount, simulate) : 0L;
        }
    }

    // --- removal and the carried share -----------------------------------------------------------

    /**
     * Breaking a tank: its share moves into the rest of its column (below first, then above); what
     * does not fit leaves on a tank item carrying {@link TankContents}, dropped here, and the block's
     * loot withholds its plain tank so exactly one drops. The share is zeroed as it leaves.
     */
    //? if >=1.21.5 {
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
    //?} else {
    /*// Before 1.21.5 there is no block-entity removal hook; the block's onRemove calls this.
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    *///?}
        if (!(this.level instanceof ServerLevel serverLevel) || this.amount <= 0 || this.medium.isEmpty()) {
            return;
        }
        TankMedium leaving = this.medium;
        List<ProcessTankBlockEntity> members = column();
        int index = Math.max(0, members.indexOf(this));
        List<ProcessTankBlockEntity> below = members.subList(0, Math.min(index, members.size()));
        List<ProcessTankBlockEntity> above = index + 1 <= members.size()
                ? members.subList(index + 1, members.size()) : List.of();
        long[] belowShares = shares(below);
        long[] aboveShares = shares(above);
        TankColumnMath.Removal removal = TankColumnMath.remove(this.amount, belowShares, aboveShares, CAPACITY_MB);
        for (int i = 0; i < below.size(); i++) {
            below.get(i).setShare(leaving, belowShares[i]);
        }
        for (int i = 0; i < above.size(); i++) {
            above.get(i).setShare(leaving, aboveShares[i]);
        }
        ItemStack carrier = new ItemStack(state.getBlock());
        if (removal.carried() > 0 && !carrier.isEmpty()) {
            carrier.set(ModDataComponents.TANK_CONTENTS.get(),
                    new TankContents(leaving.kind().serialized(), leaving.id(), removal.carried()));
        }
        this.medium = TankMedium.EMPTY;
        this.amount = 0L;
        this.carrierDropped = true;
        if (!carrier.isEmpty()) {
            Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, carrier);
        }
        for (ProcessTankBlockEntity member : members) {
            member.column = null;
        }
        signalColumn(members);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        TankContents contents = components.get(ModDataComponents.TANK_CONTENTS.get());
        if (contents != null && contents.amount() > 0) {
            TankMedium carried = contents.asMedium();
            if (!carried.isEmpty()) {
                this.medium = carried;
                this.amount = contents.amount();
            }
        }
    }

    // --- persistence ---------------------------------------------------------------------------

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
        output.putString("TankKind", this.medium.kind().serialized());
        output.putString("TankMedium", this.medium.id());
        output.putLong("TankAmount", this.amount);
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
        TankMedium loaded = new TankMedium(TankMedium.Kind.parse(input.getStringOr("TankKind", "none")),
                input.getStringOr("TankMedium", ""));
        long loadedAmount = Math.max(0L, input.getLongOr("TankAmount", 0L));
        this.medium = loadedAmount <= 0 ? TankMedium.EMPTY : loaded;
        this.amount = loaded.isEmpty() ? 0L : loadedAmount;
        this.column = null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nerotech.process_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }
}
