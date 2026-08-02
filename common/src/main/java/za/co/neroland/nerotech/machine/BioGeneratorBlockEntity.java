package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.BioGeneratorMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.tag.NeroTechTags;

/**
 * Bio Generator (Stage D) — the Nero Generator's farmable sibling. It burns anything in the
 * datapack-overridable {@link NeroTechTags#BIO_FUELS} tag (NeroTech seeds it with dried kelp blocks;
 * NeroAgriculture and packs add their own feedstock) instead of the fossil switch the Nero Generator
 * hard-codes, produces {@code bioGeneratorNePerBurnTick} NE/tick — 20% above the Nero Generator —
 * and emits <b>half</b> the pollution per operation ({@link #pollutionScalePermille()}).
 *
 * <p>The niche: coal is dug and finite, kelp is farmed and renewable. Bio is the generator you
 * scale by planting, and the one that keeps a region's pollution survivable while you do.
 */
public class BioGeneratorBlockEntity extends NeroTechMachineBlockEntity {

    public static final int FUEL_SLOT = 0;

    /**
     * Burn ticks one tagged fuel item is worth. Flat by design: the tag is open to any mod, so the
     * generator cannot look up a per-item burn value it does not own. 4,000 matches vanilla's dried
     * kelp block — the tag's seed item — so the reference fuel behaves exactly as a player expects.
     */
    public static final int BURN_TICKS = 4_000;

    /** Bio burns cleaner than coal: half the config emission per operation. */
    private static final int POLLUTION_SCALE_PERMILLE = 500;

    public BioGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIO_GENERATOR.get(), pos, state, 1);
        // GENERATOR preset: ENERGY OUTPUT on every face, feedstock (ITEM) accepted IN on every face.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .channel(Channel.ITEM, SlotGroup.of("input", FUEL_SLOT), null)
                .defaultPreset(SidePreset.GENERATOR)
                .autoEject(Channel.ENERGY, true)
                .build());
    }

    /** Whether {@code stack} is accepted feedstock — by TAG, so packs extend it without code. */
    public static boolean isBioFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.is(NeroTechTags.BIO_FUELS);
    }

    /** Burning emits — the analytics panel shows the config-derived nominal rate (already halved). */
    @Override
    public int pollutionPerMinute() {
        return emissionPerMinute();
    }

    @Override
    protected int pollutionScalePermille() {
        return POLLUTION_SCALE_PERMILLE;
    }

    /** A generator is never load-shed by a Grid Controller. */
    @Override
    public boolean shedable() {
        return false;
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        UpgradeModifiers mods = modifiers();
        int rate = (int) Math.round(NeroTechConfig.bioGeneratorNePerBurnTick()
                * mods.speedMultiplier() * presetSpeedFactor());
        boolean roomToStore = getEnergy().getAmount() < getEnergy().getCapacity();

        if (this.progress > 0) {
            this.progress--;
            // Combustion is combustion: it still runs hot and still emits — just half as much.
            addHeat(NeroTechConfig.heatPerOperation());
            emitPollution(level, pos);
            if (roomToStore) {
                energyBuffer().generate(rate);
            }
        } else {
            ItemStack fuel = this.items.get(FUEL_SLOT);
            boolean accepted = isBioFuel(fuel);
            if (accepted && roomToStore) {
                this.progress = BURN_TICKS;
                this.maxProgress = BURN_TICKS;
                fuel.shrink(1);
                setChanged();
            } else {
                // Analytics: waiting on feedstock reads STARVED; feedstock ready but the buffer full, BLOCKED.
                reportStatus(accepted ? MachineStatus.BLOCKED : MachineStatus.STARVED);
                if (this.maxProgress != 0) {
                    this.maxProgress = 0;
                }
            }
        }

        // BER surface: the firebox glows while a feedstock charge is burning.
        setActive(this.progress > 0);

        MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(), sideConfig());
    }

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return slot == FUEL_SLOT && isBioFuel(stack);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.bio_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BioGeneratorMenu(containerId, playerInventory, this, this.data);
    }
}
