package za.co.neroland.nerotech.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.network.MachineStatsPayload;
import za.co.neroland.nerotech.network.NeroTechNetwork;

/**
 * Shared base for NeroTech machine menus. Lays out the upgrade-module slots and the player inventory,
 * exposes the synced {@link ContainerData} (energy + work progress as permille for short-safe sync),
 * and implements a standard shift-click transfer between the machine block and the player inventory.
 * Subclasses add their machine-specific I/O slots in index order before calling {@link #addUpgradeAndPlayerSlots}.
 *
 * <p>Stage G analytics ride the menu: on the server ctor path (live block-entity container + a
 * {@link ServerPlayer} inventory) {@link #broadcastChanges()} pushes the machine's analytics
 * payload to that one viewer every {@value #STATS_SYNC_TICKS} ticks while the menu is open —
 * analytics never broadcast to non-viewers, and nothing here carries player data (POPIA/GDPR).
 */
public abstract class MachineMenu extends AbstractContainerMenu {

    protected final Container container;
    protected final ContainerData data;
    protected final int machineSlots;
    protected final int totalNonPlayer;

    /**
     * The machine's world position. Set server-side when the backing container is the live block-entity;
     * the client receives it via {@code network.MachineMenuPosPayload} right after the menu opens (the
     * screen keeps a looked-at-block fallback), so the Side Config tab can target this machine. World/block
     * data only — never a player identity (POPIA/GDPR).
     */
    @Nullable
    private BlockPos machinePos;

    /** Interval (ticks) between analytics pushes while the menu is open (matches the 1 Hz sampling). */
    protected static final int STATS_SYNC_TICKS = 20;

    /** The one viewer to stream analytics to — set on the server ctor path only, never synced. */
    @Nullable
    private ServerPlayer statsViewer;
    /** The live machine behind this menu (server ctor path only). */
    @Nullable
    private NeroTechMachineBlockEntity statsSource;
    /** Ticks until the next analytics push; starts at 0 so the first broadcast populates the GUI. */
    private int statsCountdown;

    protected MachineMenu(MenuType<?> type, int id, Container container, ContainerData data, int machineSlots) {
        super(type, id);
        this.container = container;
        this.data = data;
        this.machineSlots = machineSlots;
        this.totalNonPlayer = container.getContainerSize();
        if (container instanceof BlockEntity be) {
            this.machinePos = be.getBlockPos();
        }
        // Vanilla data-slot sync: without this the client's SimpleContainerData stays zeroed and
        // every gauge reads empty (the known 26.x "missing addDataSlots = dead gauges" gotcha).
        this.addDataSlots(data);
    }

    /** The machine's position, or {@code null} if not yet known (client before resolution). */
    @Nullable
    public BlockPos machinePos() {
        return this.machinePos;
    }

    /** Client-side: record the resolved machine position so the Side Config tab can target it. */
    public void setMachinePos(BlockPos pos) {
        this.machinePos = pos;
    }

    /**
     * Capture the analytics push target: on the server ctor path the backing container is the live
     * machine block-entity and the inventory belongs to a {@link ServerPlayer}. Called by every
     * subclass ctor (both paths) — the client path simply matches neither test.
     */
    protected void captureStatsTarget(Inventory playerInventory) {
        if (playerInventory.player instanceof ServerPlayer serverPlayer
                && this.container instanceof NeroTechMachineBlockEntity machine) {
            this.statsViewer = serverPlayer;
            this.statsSource = machine;
        }
    }

    /**
     * Vanilla calls this once per server tick while the menu is open — the seam for the Stage G
     * analytics stream: every {@value #STATS_SYNC_TICKS} ticks, push this machine's snapshot to
     * the one viewing player.
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (this.statsViewer == null || this.statsSource == null) {
            return; // client instance, or a menu without a live machine behind it
        }
        if (--this.statsCountdown > 0) {
            return;
        }
        this.statsCountdown = STATS_SYNC_TICKS;
        NeroTechNetwork.sendToPlayer(this.statsViewer, analyticsPayload(this.statsSource));
    }

    /**
     * The analytics payload this menu streams while open. Default: the per-machine stats snapshot;
     * the Analytics Terminal menu overrides this with its dashboard payload.
     */
    protected CustomPacketPayload analyticsPayload(NeroTechMachineBlockEntity machine) {
        return MachineStatsPayload.of(this.containerId, machine);
    }

    /** Add the upgrade-module slots (right column) then the player inventory + hotbar. */
    protected void addUpgradeAndPlayerSlots(Inventory playerInventory) {
        captureStatsTarget(playerInventory);
        int upgrades = this.totalNonPlayer - this.machineSlots;
        // Upgrade modules: a tidy 2×2 block in the top-right of the machine area (clear of the gauges).
        for (int i = 0; i < upgrades; i++) {
            int col = i % 2;
            int row = i / 2;
            this.addSlot(new PredicateSlot(this.container, this.machineSlots + i, 138 + col * 18, 18 + row * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    // --- synced data (permille; see NeroTechMachineBlockEntity) --------------

    public int energyPermille() {
        return this.data.get(0);
    }

    public float energyFraction() {
        return this.data.get(1) <= 0 ? 0f : (float) this.data.get(0) / this.data.get(1);
    }

    public float workFraction() {
        return this.data.get(3) <= 0 ? 0f : this.data.get(2) / 1000f;
    }

    public boolean working() {
        return this.data.get(3) > 0;
    }

    public float heatFraction() {
        return this.data.get(5) <= 0 ? 0f : this.data.get(4) / (float) this.data.get(5);
    }

    /** The synced Stage H overclock preset ordinal (index 6; see {@code MachinePreset.byOrdinal}). */
    public int presetOrdinal() {
        return this.data.get(6);
    }

    /**
     * The live machine behind this menu — non-null only on the server ctor path (the same target
     * the analytics stream uses). The preset intent handler resolves its machine through this, so
     * a client can only ever re-preset the machine of the menu it actually has open.
     */
    @Nullable
    public NeroTechMachineBlockEntity serverMachine() {
        return this.statsSource;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int playerStart = this.totalNonPlayer;
            int playerEnd = playerStart + 36;
            if (index < playerStart) {
                if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, playerStart, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    /** A slot that defers placement validity to the backing container ({@code canPlaceItem}). */
    protected static class PredicateSlot extends Slot {
        public PredicateSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.getContainerSlot(), stack);
        }
    }

    /** An output slot: never accepts manual placement. */
    protected static class OutputSlot extends Slot {
        public OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    /**
     * A display/sync-only slot: never picked up or placed into. Used to sync server-written stacks
     * (recipe previews, lock templates) to the client through vanilla's slot sync — position it
     * off-panel (negative x) when it should not be visible.
     */
    protected static class GhostSlot extends Slot {
        public GhostSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
