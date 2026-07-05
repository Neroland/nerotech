package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.SolarArrayMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Solar Array — daytime, fuel-free generation. Output is gated by sky access and daylight; it produces
 * into Core's energy buffer and pushes to neighbours via Core's energy seam. Per-planet output
 * modifiers (Nerospace) are deferred behind a Core-config fallback (Stage 4), so on Earth it runs
 * fully standalone.
 *
 * <p><b>Niche vs Nerospace's Solar Panels:</b> this is deliberately the basic, single-block Earth-tier
 * panel — low output (default 10 NE/t vs Nerospace Tier 1's 20), no tiers, no array pooling, no airless
 * 2× bonus — but it takes NeroTech upgrade modules and side config like every other Tier-1 machine.
 * Nerospace's three-tier, poolable Solar Panels are the scalable solar line; both generate onto the
 * same Core energy network, so the two products complement rather than duplicate each other.
 */
public class SolarArrayBlockEntity extends NeroTechMachineBlockEntity {

    public SolarArrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_ARRAY.get(), pos, state, 0);
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.GENERATOR)
                .autoEject(Channel.ENERGY, true)
                .build());
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        // Solar is clean, cool generation — no heat, no pollution.
        // Daylight factor: 1.0 at full day, fading to 0 by night; 0 when roofed over (26.x sky-darken API).
        boolean canSeeSky = level.canSeeSky(pos.above());
        int darken = level.getSkyDarken();
        float factor = canSeeSky ? Math.max(0f, Math.min(1f, (10 - darken) / 10f)) : 0f;
        boolean producing = factor > 0f;

        // Display hook: show "working" in the GUI while generating.
        this.maxProgress = producing ? 1 : 0;
        this.progress = producing ? 1 : 0;

        if (producing && getEnergy().getAmount() < getEnergy().getCapacity()) {
            UpgradeModifiers mods = modifiers();
            // Per-planet output is the deferred Core-config fallback (1.0 on Earth) until a Nerospace API lands.
            double planet = PlanetModifiers.solarMultiplier(level);
            int rate = (int) Math.round(NeroTechConfig.solarArrayNePerTick() * mods.speedMultiplier() * factor * planet);
            if (rate > 0) {
                energyBuffer().generate(rate);
            }
        }

        MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(), sideConfig());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.solar_array");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SolarArrayMenu(containerId, playerInventory, this, this.data);
    }
}
