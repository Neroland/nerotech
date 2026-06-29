package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.NeroGeneratorMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.tag.NeroTechTags;

/**
 * Fusion Reactor — late-game high-output generation, gated behind orbit + Starsteel possession. Burns a
 * fuel from the datapack-overridable {@code nerotech:fusion_fuels} tag (so Nerospace/Mekanism/packs can
 * supply it — no Nerospace import) into a large NE stream, running very hot. Left unmanaged it reaches
 * max heat and, if the admin allows it ({@code fusionReactorMeltdownEnabled}), melts down destructively;
 * otherwise it simply stalls until it cools. The red heat gauge telegraphs the danger.
 */
public class FusionReactorBlockEntity extends NeroTechMachineBlockEntity {

    public static final int FUEL_SLOT = 0;
    private static final int BURN_TICKS = 1_600;

    public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_REACTOR.get(), pos, state, 1);
        // GENERATOR preset: ENERGY OUTPUT on every face, fuel (ITEM) accepted IN on every face.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .channel(Channel.ITEM, SlotGroup.of("input", FUEL_SLOT), null)
                .defaultPreset(SidePreset.GENERATOR)
                .autoEject(Channel.ENERGY, true)
                .build());
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        // Meltdown / safety check first.
        if (heat() >= NeroTechConfig.heatCapacity()) {
            if (NeroTechConfig.fusionReactorMeltdownEnabled() && level instanceof ServerLevel) {
                meltdown(level, pos);
                return;
            }
            // Survival-friendly: stall and shed heat (base dissipation), still emit stored power.
            MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(), sideConfig());
            return;
        }

        boolean roomToStore = getEnergy().getAmount() < getEnergy().getCapacity();
        if (this.progress > 0) {
            this.progress--;
            UpgradeModifiers mods = modifiers();
            energyBuffer().generate((int) Math.round(NeroTechConfig.fusionReactorNePerTick() * mods.speedMultiplier()));
            addHeat(NeroTechConfig.heatPerOperation() * 4); // runs much hotter than a Nero Generator
            emitPollution(level, pos);
        } else {
            ItemStack fuel = this.items.get(FUEL_SLOT);
            if (fuel.is(NeroTechTags.FUSION_FUELS) && roomToStore && !overheated()) {
                this.progress = BURN_TICKS;
                this.maxProgress = BURN_TICKS;
                fuel.shrink(1);
                setChanged();
            } else if (this.maxProgress != 0) {
                this.maxProgress = 0;
            }
        }

        MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer());
    }

    private void meltdown(Level level, BlockPos pos) {
        level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4.0F,
                Level.ExplosionInteraction.BLOCK);
        level.removeBlock(pos, false);
    }

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return slot == FUEL_SLOT && stack.is(NeroTechTags.FUSION_FUELS);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.fusion_reactor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // Reuses the single-fuel-slot generator menu/screen; the title comes from this block-entity.
        return new NeroGeneratorMenu(containerId, playerInventory, this, this.data);
    }
}
