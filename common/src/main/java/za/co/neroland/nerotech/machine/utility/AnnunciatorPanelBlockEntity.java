package za.co.neroland.nerotech.machine.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.MachineAlarms;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;

/**
 * Annunciator Panel: an alarm board for the NeroTech machines touching it. It lights one lamp per alarm
 * ({@link MachineAlarms}: overheat, starved, full, polluting, failing) as the OR of its neighbours'
 * {@code alarmMask()}, so a base in trouble shows on the wall without opening a GUI, and drives a
 * comparator per decision 013 (any channel: 3 per lit alarm; an alarm channel: 15 while lit).
 *
 * <p>Budget: the touching machines are cached and re-found only on a neighbour change; their masks are
 * re-read every {@code annunciatorRefreshTicks} ticks. Needs no power. Its {@link #alarmMask()} is the
 * aggregate (machine contract 009) and it never reads another panel, so panels do not chain.
 *
 * <p>Privacy (POPIA/GDPR): it reads machine state only, keeps a channel number, never records an owner.
 */
public class AnnunciatorPanelBlockEntity extends NeroTechMachineBlockEntity {

    public static final ProcessingSpec SPEC = new ProcessingSpec(0, false, 0, 0L, 0L, 0L, 0L);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC).build();

    /** Registered by the integrator; the panel stays silent while it is absent. */
    public static final Identifier ALARM_SOUND = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID,
            "annunciator_panel.alarm");

    private final List<NeroTechMachineBlockEntity> watched = new ArrayList<>(6);
    private boolean neighboursDirty = true;
    private int mask;
    private int freshMask;
    private int channel = AnnunciatorMath.CHANNEL_ANY;
    private int lastSignal = -1;
    private final long[] risenAt = new long[MachineAlarms.COUNT];
    private final int refreshPhase = Math.floorMod(System.identityHashCode(this), 1024);

    private int syncedFresh = -1;
    private int syncedChannel = -1;

    public AnnunciatorPanelBlockEntity(BlockPos pos, BlockState state) {
        super(UtilityContent.ANNUNCIATOR_PANEL_BE.get(), pos, state, 0);
        Arrays.fill(this.risenAt, Long.MIN_VALUE / 2);
        // No capability is exposed (the panel is in no surface list); the preset only satisfies the contract.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .build());
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (this.neighboursDirty) {
            findNeighbours(level, pos);
        }
        int interval = Math.max(1, UtilitySettings.annunciatorRefreshTicks());
        if ((level.getGameTime() + this.refreshPhase) % interval == 0) {
            refresh(level, pos, level.getGameTime());
        }
        setActive(false);
        reportStatus(MachineStatus.IDLE);
    }

    /** Re-find the NeroTech machines on the six faces (neighbour change or load only). */
    private void findNeighbours(Level level, BlockPos pos) {
        this.watched.clear();
        for (Direction direction : Direction.values()) {
            BlockPos at = pos.relative(direction);
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(at);
            if (be instanceof NeroTechMachineBlockEntity machine && !(be instanceof AnnunciatorPanelBlockEntity)) {
                this.watched.add(machine);
            }
        }
        this.neighboursDirty = false;
    }

    private void refresh(Level level, BlockPos pos, long now) {
        int[] masks = new int[this.watched.size()];
        int n = 0;
        for (NeroTechMachineBlockEntity machine : this.watched) {
            if (machine.isRemoved()) {
                this.neighboursDirty = true;
                continue;
            }
            masks[n++] = machine.alarmMask();
        }
        int now5 = AnnunciatorMath.aggregate(Arrays.copyOf(masks, n));
        int risen = AnnunciatorMath.risen(this.mask, now5);
        this.freshMask = AnnunciatorMath.fresh(this.mask, now5, this.risenAt, now);
        this.mask = now5;
        if (risen != 0 && UtilitySettings.annunciatorAlarmSound()) {
            BuiltInRegistries.SOUND_EVENT.getOptional(ALARM_SOUND)
                    .ifPresent(sound -> level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8F, 1.0F));
        }
        updateSignal(level, pos);
    }

    private void updateSignal(Level level, BlockPos pos) {
        int signal = AnnunciatorMath.comparator(this.mask, this.channel);
        if (signal != this.lastSignal) {
            this.lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        }
    }

    /** Lit alarm bits: the aggregate on the server, the last synced value on the client. */
    @Override
    public int alarmMask() {
        if (this.level == null || this.level.isClientSide()) {
            return super.alarmMask();
        }
        return this.mask;
    }

    /** How many machines the panel watches (menu data; "No machine touching" on the screen). */
    public int watchedCount() {
        return this.watched.size();
    }

    /** Lit bits that lit within the last 100 ticks (the BER blinks them). Synced on change. */
    public int freshMask() {
        return this.freshMask;
    }

    /** The comparator channel: 0 = any, 1..5 = one alarm (decision 013). Synced on change. */
    public int channel() {
        return this.channel;
    }

    /** Comparator strength now (server side). */
    public int comparatorSignal() {
        return AnnunciatorMath.comparator(alarmMask(), this.channel);
    }

    /** Step to the next comparator channel (crouch-use with an empty hand) and return it. */
    public int cycleChannel() {
        this.channel = AnnunciatorMath.nextChannel(this.channel);
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            updateSignal(this.level, this.worldPosition);
        }
        return this.channel;
    }

    /** A neighbour changed: re-find the touching machines on the next tick. */
    @Override
    public void invalidateThermalLinks() {
        super.invalidateThermalLinks();
        this.neighboursDirty = true;
    }

    @Override
    protected boolean renderSyncDirty() {
        boolean dirty = this.freshMask != this.syncedFresh || this.channel != this.syncedChannel;
        this.syncedFresh = this.freshMask;
        this.syncedChannel = this.channel;
        return dirty;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("AnFresh", this.freshMask);
        return tag;
    }

    /** The panel never records an owner. */
    @Override
    public void setOwner(@Nullable UUID owner) {
    }

    /** Needs no power and does no work: nothing for a Grid Controller to shed. */
    @Override
    public boolean shedable() {
        return false;
    }

    @Override
    protected int extraDataCount() {
        return ProcessingMachineBlockEntity.EXTRA_COUNT;
    }

    @Override
    protected int extraData(int index) {
        return UtilityMenus.extraData(this, index);
    }

    @Override
    public void setRemoved() {
        // World-inert (26.x): drop in-memory references only.
        this.watched.clear();
        this.neighboursDirty = true;
        super.setRemoved();
    }

    // --- persistence: the channel (world data); the base's owner keys are dropped -----------------

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (String key : UtilitySaveKeys.STRIPPED_OWNER_KEYS) {
            output.discard(key);
        }
        output.putInt("AnChannel", this.channel);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (String key : UtilitySaveKeys.STRIPPED_OWNER_KEYS) {
            tag.remove(key);
        }
        tag.putInt("AnChannel", this.channel);
    }
    *///?}

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
        this.channel = AnnunciatorMath.sanitizeChannel(input.getIntOr("AnChannel", AnnunciatorMath.CHANNEL_ANY));
        this.freshMask = input.getIntOr("AnFresh", 0) & AnnunciatorMath.ALL;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.annunciator_panel");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ProcessingMenu(UtilityContent.ANNUNCIATOR_PANEL_MENU, containerId, playerInventory, this,
                this.data, LAYOUT);
    }

}
