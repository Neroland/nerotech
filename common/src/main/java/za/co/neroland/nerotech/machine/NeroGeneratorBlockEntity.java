package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

/**
 * Nero Generator — the entry-level power source. Burns a solid fuel into NE in Core's
 * {@link za.co.neroland.nerolandcore.energy.EnergyBuffer} and pushes it to adjacent machines/storage
 * through Core's {@link za.co.neroland.nerolandcore.platform.EnergyLookup} seam. Generation stays
 * thin (it talks only to Core's energy surface), per the NeroPower split discipline.
 */
public class NeroGeneratorBlockEntity extends NeroTechMachineBlockEntity {

    public static final int FUEL_SLOT = 0;

    public NeroGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NERO_GENERATOR.get(), pos, state, 1);
        // GENERATOR preset: ENERGY OUTPUT on every face, fuel (ITEM) accepted IN on every face.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .channel(Channel.ITEM, SlotGroup.of("input", FUEL_SLOT), null)
                .defaultPreset(SidePreset.GENERATOR)
                .autoEject(Channel.ENERGY, true)
                .build());
    }

    /** Burn value (ticks) for a fuel item, or 0 if not accepted. */
    public static int fuelValue(ItemStack stack) {
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return 1_600;
        }
        if (stack.is(Items.COAL_BLOCK)) {
            return 16_000;
        }
        if (stack.is(Items.BLAZE_ROD)) {
            return 2_400;
        }
        if (stack.is(Items.DRIED_KELP_BLOCK)) {
            return 4_000;
        }
        return 0;
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        UpgradeModifiers mods = modifiers();
        int rate = (int) Math.round(NeroTechConfig.neroGeneratorNePerTick() * mods.speedMultiplier());
        boolean roomToStore = getEnergy().getAmount() < getEnergy().getCapacity();

        if (this.progress > 0) {
            this.progress--;
            // Burning runs hot and dirty — couples the generator into heat + regional pollution.
            addHeat(NeroTechConfig.heatPerOperation());
            emitPollution(level, pos);
            if (roomToStore) {
                energyBuffer().generate(rate);
            }
        } else {
            ItemStack fuel = this.items.get(FUEL_SLOT);
            int value = fuelValue(fuel);
            if (value > 0 && roomToStore) {
                this.progress = value;
                this.maxProgress = value;
                fuel.shrink(1);
                setChanged();
            } else if (this.maxProgress != 0) {
                this.maxProgress = 0;
            }
        }

        MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(), sideConfig());
    }

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return slot == FUEL_SLOT && fuelValue(stack) > 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.nero_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NeroGeneratorMenu(containerId, playerInventory, this, this.data);
    }
}
