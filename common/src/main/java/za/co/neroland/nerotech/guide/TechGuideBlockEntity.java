package za.co.neroland.nerotech.guide;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * The Tech Guide pedestal (Nerospace's Star Guide recipe): holds the installed Tech Guide Datapad
 * and, while loaded, projects a hologram of the nearest player's NEXT incomplete progression step
 * (their personal "you are here" marker). The hologram icon is computed server-side on a slow tick
 * and synced via the vanilla block-entity update packet — no custom packets.
 *
 * <p>Privacy (POPIA/GDPR): the hologram is a transient look-up against the nearest player's
 * advancements; nothing player-keyed is stored on the block entity (only the synced icon stack).</p>
 */
public class TechGuideBlockEntity extends BlockEntity implements MenuProvider {

    /** Server ticks between hologram refreshes (1s — progression changes are slow). */
    private static final int HOLOGRAM_INTERVAL = 20;
    /** Players within this radius drive the hologram's next-step lookup. */
    private static final double HOLOGRAM_PLAYER_RANGE = 12.0D;

    private ItemStack datapad = ItemStack.EMPTY;
    /** Icon of the nearest player's next incomplete step (client-synced; EMPTY = show the datapad). */
    private ItemStack hologram = ItemStack.EMPTY;

    public TechGuideBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TECH_GUIDE.get(), pos, state);
    }

    public boolean hasDatapad() {
        return !this.datapad.isEmpty();
    }

    /** Installs one datapad item (lectern-style). @return true if the pedestal accepted it. */
    public boolean installDatapad(ItemStack stack) {
        if (hasDatapad() || stack.isEmpty()) {
            return false;
        }
        this.datapad = stack.split(1);
        markChangedAndSync();
        return true;
    }

    /** Removes and returns the installed datapad (EMPTY when the pedestal is bare). */
    public ItemStack removeDatapad() {
        ItemStack removed = this.datapad;
        this.datapad = ItemStack.EMPTY;
        this.hologram = ItemStack.EMPTY;
        markChangedAndSync();
        return removed;
    }

    public ItemStack getDatapad() {
        return this.datapad;
    }

    /** The hologram stack the client renderer floats above the pedestal. */
    public ItemStack getHologram() {
        return this.hologram;
    }

    public int comparatorSignal() {
        return hasDatapad() ? 15 : 0;
    }

    // --- Ticking (server): refresh the hologram from the nearest player's progress -----------

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !hasDatapad()
                || level.getGameTime() % HOLOGRAM_INTERVAL != 0L) {
            return;
        }
        Player nearest = serverLevel.getNearestPlayer(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, HOLOGRAM_PLAYER_RANGE, false);
        ItemStack next = nearest instanceof ServerPlayer serverPlayer
                ? TechGuideProgress.nextStepIcon(serverPlayer)
                : ItemStack.EMPTY;
        if (!ItemStack.isSameItemSameComponents(next, this.hologram)) {
            this.hologram = next;
            markChangedAndSync();
        }
    }

    private void markChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Breaking a loaded pedestal pops the installed datapad (the block itself drops via loot). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel && hasDatapad()) {
            Containers.dropItemStack(this.level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, removeDatapad());
        }
    }

    // --- Persistence + client sync -----------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Datapad", ItemStack.OPTIONAL_CODEC, this.datapad);
        output.store("Hologram", ItemStack.OPTIONAL_CODEC, this.hologram);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.datapad = input.read("Datapad", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.hologram = input.read("Hologram", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    // --- MenuProvider --------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.tech_guide");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TechGuideMenu(containerId, playerInventory, player);
    }
}
