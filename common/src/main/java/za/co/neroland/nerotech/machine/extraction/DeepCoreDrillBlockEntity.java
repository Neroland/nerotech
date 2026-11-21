package za.co.neroland.nerotech.machine.extraction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.deposit.DepositService;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.machine.multiblock.DrillShape;
import za.co.neroland.nerotech.machine.multiblock.MultiblockController;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.progression.MilestoneGates;
import za.co.neroland.nerotech.registry.ModItems;

/**
 * Deep Core Drill — the controller of a {@link DrillShape} multiblock. While formed and powered it
 * pulls ore from the virtual deposit under its chunk ({@link DepositService#extract}) once per
 * completed operation, never per tick, and never touches terrain. Each operation wears the fitted
 * drill head ({@link DrillHeadTier}: better heads last longer and yield more); a worn head is used up.
 * Output lands in four output slots and leaves through the normal item surface (side config
 * auto-eject). It makes heat and pollution like any machine, idles in a dimension listed in
 * {@code depositDisabledDimensions}, and syncs {@link #drilling()} and the head tier for the art.
 *
 * <p>Slot 0 holds drill heads; slots 1-4 are outputs. World data only: no owner beyond the base's
 * opt-in owner, no player data.
 */
public class DeepCoreDrillBlockEntity extends NeroTechMachineBlockEntity {

    public static final ProcessingSpec SPEC = new ProcessingSpec(0, true, 4, 0L, 0L, 0L, 0L);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC)
            .catalystHint(() -> new ItemStack(ModItems.IRON_DRILL_HEAD.get()), "nerotech.gui.slot.drill_head")
            .build();

    public static final int HEAD_SLOT = SPEC.catalystSlot();

    // Detail codes beyond ProcessingMachineBlockEntity.DETAIL_KEYS (the drill screen maps them).
    public static final int DETAIL_UNFORMED = 100;
    public static final int DETAIL_DIMENSION_DISABLED = 101;
    public static final int DETAIL_NO_HEAD = 102;
    public static final int DETAIL_DEPLETED = 103;
    public static final int DETAIL_NO_DEPOSIT = 104;

    /** Lang keys for the drill detail codes, indexed by {@code code - DETAIL_UNFORMED}. */
    public static final String[] DRILL_DETAIL_KEYS = {
            "nerotech.machine.detail.drill.unformed",
            "nerotech.machine.detail.drill.dimension_disabled",
            "nerotech.machine.detail.drill.no_head",
            "nerotech.machine.detail.drill.depleted",
            "nerotech.machine.detail.drill.no_deposit"};

    /** How often the dimension switch is re-read (config can change at runtime). */
    private static final int DIMENSION_CHECK_TICKS = 100;

    private final MultiblockController controller = new MultiblockController(DrillShape.INSTANCE);
    private boolean formed;
    /**
     * Wear from an old save, where the machine held it: moved onto the fitted head on the first tick and
     * then forgotten. Head wear now lives on the head item ({@link DrillHeadItem#wear}).
     */
    private int legacyHeadUsed;
    @Nullable
    private DrillHeadTier legacyWearTier;

    // Transient caches, re-derived after a load.
    private boolean dimensionDisabled;
    private long dimensionCheckAt = Long.MIN_VALUE;
    @Nullable
    private Item surveyedOutput;
    private int surveyedRichness;
    private long retryAt = Long.MIN_VALUE;
    private int retryDetail = ProcessingMachineBlockEntity.DETAIL_NONE;
    private int detail = ProcessingMachineBlockEntity.DETAIL_NONE;
    private int energyRate;

    // Render sync (client reads these through the update tag).
    private boolean drilling;
    private int headTierOrdinal = -1;
    private boolean syncedDrilling;
    private int syncedHeadTier = -1;

    public DeepCoreDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ExtractionContent.DEEP_CORE_DRILL_BE.get(), pos, state, SPEC.slotCount());
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", new int[] {HEAD_SLOT}),
                        SlotGroup.of("output", new int[] {1, 2, 3, 4}))
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                // PROCESSOR would put item output on the bottom, which is always a derrick: I/O on every
                // face lets a pipe on the back or a side take the ore and feed heads.
                .preset(Channel.ITEM, SidePreset.STORAGE)
                .build());
    }

    // --- read surface ----------------------------------------------------------------------------

    /** Whether the drill is turning this moment (formed, head fitted, powered). Synced on change. */
    public boolean drilling() {
        return this.drilling;
    }

    /**
     * Whether the drill is stalled because there is nothing to cut — the deposit is used up or there is
     * none under the chunk — as opposed to any other stall (output full, unpowered). Read from the
     * synced readout detail, so it is live on the client too; the BER creeps the string without chips.
     */
    public boolean nothingToCut() {
        String detail = readoutDetail();
        return DRILL_DETAIL_KEYS[DETAIL_DEPLETED - DETAIL_UNFORMED].equals(detail)
                || DRILL_DETAIL_KEYS[DETAIL_NO_DEPOSIT - DETAIL_UNFORMED].equals(detail);
    }

    /** Whether the stall cause is a used-up deposit specifically (synced readout detail). */
    public boolean depleted() {
        return DRILL_DETAIL_KEYS[DETAIL_DEPLETED - DETAIL_UNFORMED].equals(readoutDetail());
    }

    /** The fitted head's tier, or {@code null} without one. Synced on change (head colour in the BER). */
    @Nullable
    public DrillHeadTier headTier() {
        return DrillHeadTier.byOrdinal(this.headTierOrdinal);
    }

    public boolean formed() {
        return this.formed;
    }

    /** Operations left on the fitted head (0 without one). */
    public int headOperationsLeft() {
        ItemStack head = this.items.get(HEAD_SLOT);
        DrillHeadTier tier = tierOf(head);
        return tier == null ? 0 : DrillRules.operationsLeft(DrillHeadItem.wear(head), tier);
    }

    @Override
    protected boolean syncsProgress() {
        return true;
    }

    @Override
    public int pollutionPerMinute() {
        return emissionPerMinute();
    }

    @Nullable
    public static DrillHeadTier tierOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == ModItems.IRON_DRILL_HEAD.get()) {
            return DrillHeadTier.IRON;
        }
        if (item == ModItems.CINDERSTEEL_DRILL_HEAD.get()) {
            return DrillHeadTier.CINDERSTEEL;
        }
        if (item == ModItems.GLACITE_DRILL_HEAD.get()) {
            return DrillHeadTier.GLACITE;
        }
        return null;
    }

    // --- tick ------------------------------------------------------------------------------------

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = level.getGameTime();
        Direction facing = state.hasProperty(NeroTechMachineBlock.FACING)
                ? state.getValue(NeroTechMachineBlock.FACING) : Direction.NORTH;
        if (this.controller.due(level, pos, facing, this.formed)) {
            boolean nowFormed = this.controller.validate(level, pos, facing).isPresent();
            if (nowFormed != this.formed) {
                this.formed = nowFormed;
                setChanged();
            }
        }
        if (now >= this.dimensionCheckAt) {
            this.dimensionDisabled = DepositService.disabled(PollutionManager.dimensionId(serverLevel),
                    NeroTechConfig.depositDisabledDimensions());
            this.dimensionCheckAt = now + DIMENSION_CHECK_TICKS;
        }

        ItemStack head = this.items.get(HEAD_SLOT);
        DrillHeadTier tier = tierOf(head);
        trackHead(tier, head);
        boolean waiting = now < this.retryAt;
        if (!waiting && this.surveyedOutput == null && this.formed && !this.dimensionDisabled && tier != null) {
            refreshSurvey(serverLevel, pos, now);
            waiting = now < this.retryAt;
        }
        int expected = tier == null || this.surveyedOutput == null ? 0
                : DrillRules.headYield(DepositService.yieldFor(tier.unitsPerOperation(), this.surveyedRichness,
                        NeroTechConfig.depositYieldPermille()), tier);
        boolean room = this.surveyedOutput == null || expected <= 0
                || fits(new ItemStack(this.surveyedOutput, expected));

        switch (DrillRules.stall(this.formed, this.dimensionDisabled, tier != null, waiting, room)) {
            case UNFORMED -> {
                stall(MachineStatus.UNFORMED, DETAIL_UNFORMED);
                return;
            }
            case DIMENSION_DISABLED -> {
                stall(MachineStatus.STARVED, DETAIL_DIMENSION_DISABLED); // never runs here: a lit cause, not rest
                return;
            }
            case NO_HEAD -> {
                stall(MachineStatus.STARVED, DETAIL_NO_HEAD);
                return;
            }
            case DEPLETED -> {
                stall(MachineStatus.STARVED, this.retryDetail);
                return;
            }
            case OUTPUT_FULL -> {
                stall(MachineStatus.BLOCKED, ProcessingMachineBlockEntity.DETAIL_OUTPUT_FULL);
                return;
            }
            default -> {
            }
        }

        UpgradeModifiers mods = modifiers();
        int ticks = DrillRules.operationTicks(ExtractionSettings.drillOperationTicks(), mods.speedMultiplier(),
                presetSpeedFactor());
        int cost = (int) Math.max(0, Math.round(ExtractionSettings.drillEnergyPerTick()
                * mods.energyMultiplier() * presetEnergyFactor()));
        this.maxProgress = ticks;
        this.energyRate = cost;
        if (overheated()) {
            setRunning(false, ProcessingMachineBlockEntity.DETAIL_OVERHEATED);
            reportStatus(MachineStatus.THROTTLED,
                    ProcessingMachineBlockEntity.DETAIL_KEYS[ProcessingMachineBlockEntity.DETAIL_OVERHEATED]);
            return;
        }
        if (!energyBuffer().has(cost)) {
            setRunning(false, ProcessingMachineBlockEntity.DETAIL_NO_ENERGY);
            reportStatus(MachineStatus.NO_ENERGY,
                    ProcessingMachineBlockEntity.DETAIL_KEYS[ProcessingMachineBlockEntity.DETAIL_NO_ENERGY]);
            return;
        }
        setRunning(true, ProcessingMachineBlockEntity.DETAIL_NONE);
        energyBuffer().consume(cost);
        addHeat(NeroTechConfig.heatPerOperation());
        emitPollution(level, pos);
        int before = this.progress;
        this.progress = DrillRules.advance(this.progress, ticks);
        if (DrillRules.completed(before, this.progress)) {
            operate(serverLevel, pos, now, tier, head);
        }
        setChanged();
    }

    /** One completed operation: the single deposit read, the output, and head wear. */
    private void operate(ServerLevel level, BlockPos pos, long now, DrillHeadTier tier, ItemStack head) {
        DepositService.Extraction taken = DepositService.extract(level, pos.getX() >> 4, pos.getZ() >> 4,
                tier.unitsPerOperation(), isGalleryExhibit());
        if (taken == null) {
            this.surveyedOutput = null;
            this.retryAt = now + ExtractionSettings.drillDepletedRetryTicks();
            this.retryDetail = DETAIL_DEPLETED;
            return;
        }
        this.surveyedOutput = taken.output();
        int count = DrillRules.headYield(taken.yield(), tier);
        if (count > 0) {
            insertOutput(new ItemStack(taken.output(), count));
            markCompleted();
            // Milestone record: credit the operator in the moment (online only, never stored; idempotent).
            MilestoneGates.credit(level, lastOperator().orElse(null), MilestoneGates.FIRST_DEPOSIT_TAPPED);
        }
        int used = DrillHeadItem.wear(head);
        if (DrillRules.wornOut(used, tier)) {
            head.shrink(1);
            DrillHeadItem.setWear(head, 0); // an old oversize stack: the next head starts fresh
        } else {
            DrillHeadItem.setWear(head, DrillRules.wear(used, tier));
        }
        pulseClient();
    }

    /** Read the deposit once (formation, load, after a retry wait); never per tick. */
    private void refreshSurvey(ServerLevel level, BlockPos pos, long now) {
        DepositService.Survey survey = DepositService.survey(level, pos.getX() >> 4, pos.getZ() >> 4,
                isGalleryExhibit());
        if (survey == null) {
            this.retryAt = now + ExtractionSettings.drillDepletedRetryTicks();
            this.retryDetail = DETAIL_NO_DEPOSIT;
            return;
        }
        if (survey.remaining() <= 0) {
            this.retryAt = now + ExtractionSettings.drillDepletedRetryTicks();
            this.retryDetail = DETAIL_DEPLETED;
            return;
        }
        this.surveyedOutput = survey.output();
        this.surveyedRichness = survey.richness();
    }

    /** Track the fitted head's tier for the BER, and move an old save's machine-held wear onto the head. */
    private void trackHead(@Nullable DrillHeadTier tier, ItemStack head) {
        if (this.legacyHeadUsed > 0 && tier != null) {
            if (DrillHeadItem.wear(head) == 0) {
                DrillHeadTier from = this.legacyWearTier == null ? tier : this.legacyWearTier;
                DrillHeadItem.setWear(head, Math.min(tier.operations() - 1,
                        DrillRules.carryWear(this.legacyHeadUsed, from, tier)));
            }
            this.legacyHeadUsed = 0;
            this.legacyWearTier = null;
            setChanged();
        }
        this.headTierOrdinal = tier == null ? -1 : tier.ordinal();
    }

    private void stall(MachineStatus status, int detailCode) {
        setRunning(false, detailCode);
        String key = detailKey(detailCode);
        reportStatus(status, key);
        this.energyRate = 0;
        if (this.progress != 0 || this.maxProgress != 0) {
            this.progress = 0;
            this.maxProgress = 0;
            setChanged();
        }
    }

    private void setRunning(boolean running, int detailCode) {
        this.detail = detailCode;
        this.drilling = running;
        setActive(running);
    }

    @Nullable
    private static String detailKey(int code) {
        if (code >= DETAIL_UNFORMED && code < DETAIL_UNFORMED + DRILL_DETAIL_KEYS.length) {
            return DRILL_DETAIL_KEYS[code - DETAIL_UNFORMED];
        }
        if (code >= 0 && code < ProcessingMachineBlockEntity.DETAIL_KEYS.length) {
            return ProcessingMachineBlockEntity.DETAIL_KEYS[code];
        }
        return null;
    }

    // --- output slots ----------------------------------------------------------------------------

    private boolean fits(ItemStack stack) {
        int left = stack.getCount();
        for (int i = 0; i < SPEC.outputs() && left > 0; i++) {
            ItemStack slot = this.items.get(SPEC.outputSlot(i));
            if (slot.isEmpty()) {
                left -= stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                left -= slot.getMaxStackSize() - slot.getCount();
            }
        }
        return left <= 0;
    }

    private void insertOutput(ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = 0; i < SPEC.outputs() && !stack.isEmpty(); i++) {
                int slotIndex = SPEC.outputSlot(i);
                ItemStack slot = this.items.get(slotIndex);
                if (pass == 0 && !slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                    int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                    if (move > 0) {
                        slot.grow(move);
                        stack.shrink(move);
                    }
                } else if (pass == 1 && slot.isEmpty()) {
                    this.items.set(slotIndex, stack.copy());
                    stack.setCount(0);
                }
            }
        }
    }

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return slot == HEAD_SLOT && tierOf(stack) != null;
    }

    @Override
    public boolean canTakeMachineItem(int slot) {
        return SPEC.isOutput(slot);
    }

    // --- menu --------------------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.deep_core_drill");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ProcessingMenu(ExtractionContent.DEEP_CORE_DRILL_MENU, id, inventory, this, this.data, LAYOUT);
    }

    @Override
    protected int extraDataCount() {
        return ProcessingMachineBlockEntity.EXTRA_COUNT;
    }

    @Override
    protected int extraData(int index) {
        switch (index) {
            case ProcessingMachineBlockEntity.X_STATUS:
                return stats().status().ordinal();
            case ProcessingMachineBlockEntity.X_DETAIL:
                return this.detail;
            case ProcessingMachineBlockEntity.X_DETAIL_PARAM:
                return Math.min(Short.MAX_VALUE, Math.max(0, headOperationsLeft()));
            case ProcessingMachineBlockEntity.X_HEAT:
                return Math.min(Short.MAX_VALUE, Math.max(0, this.heat));
            case ProcessingMachineBlockEntity.X_ENERGY_RATE:
                return Math.min(Short.MAX_VALUE, this.energyRate);
            default:
                break;
        }
        int t = index - ProcessingMachineBlockEntity.X_TANKS;
        // No tanks: medium -1, amounts 0.
        return t >= 0 && t % ProcessingMachineBlockEntity.TANK_STRIDE == 0 ? -1 : 0;
    }

    // --- render sync -------------------------------------------------------------------------------

    @Override
    protected boolean renderSyncDirty() {
        boolean dirty = this.drilling != this.syncedDrilling || this.headTierOrdinal != this.syncedHeadTier;
        this.syncedDrilling = this.drilling;
        this.syncedHeadTier = this.headTierOrdinal;
        return dirty;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // Update tag only (never written to disk): the BER's string and head colour.
        tag.putBoolean("Drilling", this.drilling);
        tag.putInt("HeadTier", this.headTierOrdinal);
        return tag;
    }

    @Override
    public void setRemoved() {
        if (this.level != null) {
            this.controller.release(this.level, this.worldPosition);
        }
        super.setRemoved();
    }

    // Head wear rides on the head item, so a broken drill simply drops its head with the wear it has.

    // --- persistence -------------------------------------------------------------------------------

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
    *///?}
        output.putBoolean("Formed", this.formed);
        if (this.legacyHeadUsed > 0) {
            // Not yet moved onto a head (no head fitted since the old save loaded): keep it until it is.
            output.putInt("HeadUsed", this.legacyHeadUsed);
            output.putInt("WearTier", this.legacyWearTier == null ? -1 : this.legacyWearTier.ordinal());
        }
    }

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
    *///?}
        this.formed = input.getBooleanOr("Formed", false);
        this.legacyHeadUsed = Math.max(0, input.getIntOr("HeadUsed", 0));
        this.legacyWearTier = DrillHeadTier.byOrdinal(input.getIntOr("WearTier", -1));
        // Update-tag extras (absent in a disk save, which keeps the defaults).
        this.drilling = input.getBooleanOr("Drilling", false);
        this.headTierOrdinal = input.getIntOr("HeadTier", -1);
        this.surveyedOutput = null;
        this.retryAt = Long.MIN_VALUE;
    }
}
