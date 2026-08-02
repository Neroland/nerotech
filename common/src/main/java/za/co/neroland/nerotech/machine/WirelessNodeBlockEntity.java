package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
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

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.WirelessNodeMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Wireless Power Node (Stage D) — a paired point-to-point NE link, for when running a cable is
 * merely tedious rather than interesting. Pair two nodes with the Configurator (crouch-use node A,
 * then crouch-use node B) and the sending node hands its buffer to its partner every
 * {@value #TRANSFER_INTERVAL} ticks, up to {@code wirelessNodeTransferPerTick} NE a pass.
 *
 * <p><b>Deliberately unglamorous:</b> same dimension only, {@code wirelessNodeRange} blocks apart at
 * most, and the transfer is <b>lossless</b> — a node buys convenience, never throughput. It never
 * force-loads its partner's chunk: an unloaded partner simply means the pass is skipped, and the
 * node's own buffer fills and then feeds its neighbours normally.
 *
 * <p><b>Link hygiene:</b> the link is validated on every pass and dropped when the partner block is
 * gone, is no longer a node, or no longer points back here — so breaking either end unlinks the
 * survivor by itself, with no block-removal hook and no risk of a chunk unload being mistaken for a
 * break. Persisted state is a block position and a dimension id — world data, never player data
 * (POPIA/GDPR).
 */
public class WirelessNodeBlockEntity extends NeroTechMachineBlockEntity {

    /** Transfer cadence (ticks) — a pass every quarter-second, never a per-tick lookup. */
    private static final int TRANSFER_INTERVAL = 5;

    /** The paired node's position, or null when this node is unlinked. */
    @Nullable
    private BlockPos partnerPos;

    /** The paired node's dimension id — a defensive check; pairing is same-dimension only. */
    private String partnerDimension = "";

    /** Spreads transfer passes across ticks so a wall of nodes never fires on the same tick. */
    private final int transferPhase = Math.floorMod(System.identityHashCode(this), TRANSFER_INTERVAL);

    public WirelessNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_NODE.get(), pos, state, 0);
        // A pass-through endpoint: every face I/O so it takes power in and hands it on unconfigured.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.STORAGE)
                .autoEject(Channel.ENERGY, true)
                .build());
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if ((level.getGameTime() + this.transferPhase) % TRANSFER_INTERVAL == 0) {
            sendToPartner(level, pos);
        }
        // BER surface: the emitter rings light while the node holds a link.
        setActive(this.partnerPos != null);
        MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(), sideConfig());
    }

    /**
     * One transfer pass: resolve the partner (validating the link on the way), then move up to
     * {@code wirelessNodeTransferPerTick} NE into it. Anything that invalidates the link — a missing
     * block, a partner that points elsewhere, a partner moved out of range — unlinks this node.
     */
    private void sendToPartner(Level level, BlockPos pos) {
        BlockPos target = this.partnerPos;
        if (target == null) {
            return;
        }
        if (!level.dimension().identifier().toString().equals(this.partnerDimension)
                || !withinRange(pos, target)) {
            unlink();
            return;
        }
        if (!level.hasChunkAt(target)) {
            return; // partner asleep — skip the pass, never force-load a chunk
        }
        if (!(level.getBlockEntity(target) instanceof WirelessNodeBlockEntity partner)
                || !pos.equals(partner.partnerPos)) {
            unlink(); // partner broken, replaced, or re-paired elsewhere
            return;
        }
        long budget = NeroTechConfig.wirelessNodeTransferPerTick();
        if (budget <= 0) {
            return;
        }
        long offer = energyBuffer().extract(budget, true);
        if (offer <= 0) {
            return;
        }
        long accepted = partner.energyBuffer().insert(offer, false);
        if (accepted > 0) {
            energyBuffer().extract(accepted, false);
        }
    }

    private static boolean withinRange(BlockPos from, BlockPos to) {
        long range = NeroTechConfig.wirelessNodeRange();
        return from.distSqr(to) <= (double) range * range;
    }

    // --- pairing (driven by the Configurator; see item.ConfiguratorItem) -------------------------

    /** This node's partner position, or null when unlinked. */
    @Nullable
    public BlockPos partner() {
        return this.partnerPos;
    }

    /** Whether {@code other} is a legal partner for this node: a different node, in range. */
    public boolean canPairWith(WirelessNodeBlockEntity other) {
        return other != this && !this.worldPosition.equals(other.worldPosition)
                && withinRange(this.worldPosition, other.worldPosition);
    }

    /**
     * Pair this node with {@code other}, replacing either side's existing link. Both nodes record
     * each other, so the validation pass on either end can always see a symmetric link.
     */
    public void pairWith(WirelessNodeBlockEntity other, String dimension) {
        other.unlink();
        this.unlink();
        this.partnerPos = other.worldPosition.immutable();
        this.partnerDimension = dimension;
        other.partnerPos = this.worldPosition.immutable();
        other.partnerDimension = dimension;
        setChanged();
        other.setChanged();
    }

    /** Drop this node's link and, when the partner still points back, the partner's too. */
    public void unlink() {
        BlockPos target = this.partnerPos;
        this.partnerPos = null;
        this.partnerDimension = "";
        setChanged();
        if (target != null && this.level != null && this.level.hasChunkAt(target)
                && this.level.getBlockEntity(target) instanceof WirelessNodeBlockEntity partner
                && this.worldPosition.equals(partner.partnerPos)) {
            partner.partnerPos = null;
            partner.partnerDimension = "";
            partner.setChanged();
        }
    }

    /** A pass-through endpoint has no work rate for a Grid Controller to throttle. */
    @Override
    public boolean shedable() {
        return false;
    }

    // --- persistence (world data only: a block position and a dimension id) ----------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Linked", this.partnerPos != null);
        if (this.partnerPos != null) {
            output.putInt("PartnerX", this.partnerPos.getX());
            output.putInt("PartnerY", this.partnerPos.getY());
            output.putInt("PartnerZ", this.partnerPos.getZ());
            output.putString("PartnerDim", this.partnerDimension);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (input.getBooleanOr("Linked", false)) {
            this.partnerPos = new BlockPos(input.getIntOr("PartnerX", 0), input.getIntOr("PartnerY", 0),
                    input.getIntOr("PartnerZ", 0));
            this.partnerDimension = input.getStringOr("PartnerDim", "");
        } else {
            this.partnerPos = null;
            this.partnerDimension = "";
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.wireless_node");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WirelessNodeMenu(containerId, playerInventory, this, this.data);
    }
}
