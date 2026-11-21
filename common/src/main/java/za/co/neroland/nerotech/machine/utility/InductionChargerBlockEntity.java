package za.co.neroland.nerotech.machine.utility;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.energy.EnergyConversions;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;

import za.co.neroland.nerotech.api.WirelessCharging;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;

/**
 * Induction Charger: a floor pad that tops up energy items held or worn by players standing within its
 * bounded reach, and answers {@link WirelessCharging#offer} for entities (NeroCompanion's companions).
 *
 * <p>Budget: players are looked for once every {@value ChargeMath#PASS_TICKS} ticks inside a fixed box
 * around the pad; the entity API reads a per-level registry, never the world. Output is capped per tick
 * ({@code inductionChargerTransferPerTick}, scaled by Speed modules and the preset) through one shared
 * {@link ChargeBudget}.
 *
 * <p>Privacy (POPIA/GDPR): proximity only. Players found in a pass are used in the moment and forgotten;
 * the charger persists no player data, never records an owner, and logs nothing about who it charged.
 */
public class InductionChargerBlockEntity extends NeroTechMachineBlockEntity {

    /** No slots: the generic processing screen shows energy, status, upgrades and preset. */
    public static final ProcessingSpec SPEC = new ProcessingSpec(0, false, 0, 0L, 0L, 0L, 0L);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC).build();

    private static final EquipmentSlot[] CHARGED_SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final ChargeBudget budget = new ChargeBudget();
    private final int passPhase = Math.floorMod(System.identityHashCode(this), ChargeMath.PASS_TICKS);
    private boolean registered;
    private long lastDelivery = Long.MIN_VALUE / 2;
    private boolean charging;
    private boolean syncedCharging;

    public InductionChargerBlockEntity(BlockPos pos, BlockState state) {
        super(UtilityContent.INDUCTION_CHARGER_BE.get(), pos, state, 0);
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .build());
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (!this.registered) {
            InductionChargers.register(level, pos, this);
            this.registered = true;
        }
        long now = level.getGameTime();
        if ((now + this.passPhase) % ChargeMath.PASS_TICKS == 0 && UtilitySettings.inductionChargePlayers()
                && WirelessCharging.hasItemChargers()) {
            chargePlayers(level, pos, now);
        }
        this.charging = now - this.lastDelivery <= ChargeMath.PASS_TICKS;
        setActive(this.charging);
        if (getEnergy().getAmount() <= 0) {
            reportStatus(MachineStatus.NO_ENERGY);
        } else if (this.charging) {
            reportStatus(MachineStatus.RUNNING);
        } else {
            int code = idleCode();
            reportStatus(MachineStatus.IDLE, code == CODE_PLAYERS_OFF ? DETAIL_PLAYERS_OFF
                    : code == CODE_NO_ITEM_CHARGERS ? DETAIL_NO_ITEM_CHARGERS : null);
        }
    }

    /** Idle causes on the shared detail slot: player charging switched off, or nothing can charge items. */
    public static final int CODE_PLAYERS_OFF = 40;
    public static final int CODE_NO_ITEM_CHARGERS = 41;
    public static final String DETAIL_PLAYERS_OFF = "nerotech.machine.detail.induction_charger.players_off";
    public static final String DETAIL_NO_ITEM_CHARGERS = "nerotech.machine.detail.induction_charger.no_item_chargers";

    /** Why an idle pad charges no player items ({@code DETAIL_NONE} when it would). */
    int idleCode() {
        if (!UtilitySettings.inductionChargePlayers()) {
            return CODE_PLAYERS_OFF;
        }
        if (!WirelessCharging.hasItemChargers()) {
            return CODE_NO_ITEM_CHARGERS;
        }
        return za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity.DETAIL_NONE;
    }

    /** One bounded pass over the players standing in reach. Nothing found here is kept. */
    private void chargePlayers(Level level, BlockPos pos, long now) {
        if (availableNe(now) <= 0) {
            return;
        }
        int r = UtilitySettings.inductionChargerRadius();
        AABB box = new AABB(pos.getX() - r, pos.getY() - r, pos.getZ() - r,
                pos.getX() + 1 + r, pos.getY() + 1 + r, pos.getZ() + 1 + r);
        List<Player> players = level.getEntitiesOfClass(Player.class, box, p -> !p.isSpectator() && p.isAlive());
        for (Player player : players) {
            for (EquipmentSlot slot : CHARGED_SLOTS) {
                long ne = availableNe(now);
                if (ne <= 0) {
                    return;
                }
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                long fe = EnergyConversions.neroToForge(ne);
                long accepted = WirelessCharging.chargeItem(stack, fe, false);
                if (accepted > 0) {
                    spendNe(feToNeCost(Math.min(accepted, fe)), now);
                }
            }
        }
    }

    /** NE this charger can hand out right now: its stored energy, limited by the per-tick budget. */
    long availableNe(long now) {
        long perTick = ChargeMath.scaledPerTick(UtilitySettings.inductionChargerTransferPerTick(),
                modifiers().speedMultiplier() * presetSpeedFactor());
        long allowance = this.budget.available(now, perTick, perTick * ChargeMath.PASS_TICKS);
        return Math.min(allowance, Math.max(0L, getEnergy().getAmount()));
    }

    /** Take {@code ne} from the buffer and the budget, and mark the pad as charging. */
    void spendNe(long ne, long now) {
        if (ne <= 0) {
            return;
        }
        long taken = energyBuffer().extract(ne, false);
        this.budget.spend(taken);
        this.lastDelivery = now;
        setChanged();
    }

    /** NE that paying {@code fe} costs (at least 1 for any non-zero delivery, so nothing is free). */
    static long feToNeCost(long fe) {
        if (fe <= 0) {
            return 0L;
        }
        return Math.max(1L, EnergyConversions.forgeToNero(fe));
    }

    /** Reach in blocks on every axis (menu data; the screen's reach line). */
    public int reach() {
        return UtilitySettings.inductionChargerRadius();
    }

    /** The per-tick NE cap after upgrades and preset (menu data; the screen's rate line). */
    public long ratePerTick() {
        return ChargeMath.scaledPerTick(UtilitySettings.inductionChargerTransferPerTick(),
                modifiers().speedMultiplier() * presetSpeedFactor());
    }

    /** Whether the pad delivered energy in the last pass (synced on change; the BER lights its rings). */
    public boolean charging() {
        return this.charging;
    }

    /** {@link ChargeMath.Source} view for the entity API, in FE. */
    public ChargeMath.Source feSource(long now) {
        InductionChargerBlockEntity self = this;
        return new ChargeMath.Source() {
            @Override
            public long available() {
                return EnergyConversions.neroToForge(self.availableNe(now));
            }

            @Override
            public void take(long amount) {
                self.spendNe(feToNeCost(amount), now);
            }
        };
    }

    @Override
    protected boolean renderSyncDirty() {
        boolean dirty = this.charging != this.syncedCharging;
        this.syncedCharging = this.charging;
        return dirty;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("IcCharging", this.charging);
        return tag;
    }

    /** A charger never records an owner: it charges by proximity and keeps no player data. */
    @Override
    public void setOwner(@Nullable UUID owner) {
    }

    /** A pad that only hands energy on has no work rate for a Grid Controller to throttle. */
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
        // World-inert (26.x): a map edit only, no level reads or writes.
        if (this.level != null) {
            InductionChargers.unregister(this.level, this.worldPosition, this);
        }
        this.registered = false;
        super.setRemoved();
    }

    // --- persistence: nothing of its own; the base's owner keys are dropped ------------------------

    //? if >=1.21.6 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (String key : UtilitySaveKeys.STRIPPED_OWNER_KEYS) {
            output.discard(key);
        }
    }
    //?} else {
    /*@Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (String key : UtilitySaveKeys.STRIPPED_OWNER_KEYS) {
            tag.remove(key);
        }
    }
    *///?}

    //? if >=1.21.6 {
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.charging = input.getBooleanOr("IcCharging", false);
    }
    //?} else {
    /*@Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
        this.charging = input.getBooleanOr("IcCharging", false);
    }
    *///?}

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.induction_charger");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ProcessingMenu(UtilityContent.INDUCTION_CHARGER_MENU, containerId, playerInventory, this,
                this.data, LAYOUT);
    }

}
