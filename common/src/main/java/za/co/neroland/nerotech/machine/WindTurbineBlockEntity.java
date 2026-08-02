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
import za.co.neroland.nerotech.menu.WindTurbineMenu;
import za.co.neroland.nerotech.registry.ModBlockEntities;

/**
 * Wind Turbine (Stage D) — clean generation for remote, high, open builds. Three factors decide the
 * output, and the player controls all three by <i>where</i> they build:
 *
 * <ul>
 *   <li><b>Altitude</b> — {@link WindMath#heightFactor}: 0.5× at or below y=80 rising linearly to
 *       2× at or above y=200. Building tall is the only upgrade path that costs nothing but effort.</li>
 *   <li><b>Sky access</b> — the Solar Array's {@code canSeeSky} check on the block above, but with
 *       <i>no</i> daylight term: a turbine runs all night, which is precisely its niche against solar.</li>
 *   <li><b>Atmosphere</b> — {@link PlanetModifiers#windMultiplier}: the
 *       {@code windDimensionMultipliers} table, ahead of which Nerospace's planet traits force
 *       <b>0 on an airless world</b> (no atmosphere, no wind). Runtime-guarded, so NeroTech alone
 *       simply reads the config table.</li>
 * </ul>
 *
 * <p>No fuel, no heat, no pollution, no item slots — a slotless generator whose menu is the Solar
 * Array's: the energy gauge, the upgrade column and the side-config tab.
 */
public class WindTurbineBlockEntity extends NeroTechMachineBlockEntity {

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_TURBINE.get(), pos, state, 0);
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.GENERATOR)
                .autoEject(Channel.ENERGY, true)
                .build());
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        // Sky access only — unlike solar there is no daylight term, so the turbine works at night.
        boolean exposed = level.canSeeSky(pos.above());
        double dimension = PlanetModifiers.windMultiplier(level);
        boolean producing = exposed && dimension > 0.0D;

        // Display hook: show "working" in the GUI while generating (the Solar Array recipe).
        this.maxProgress = producing ? 1 : 0;
        this.progress = producing ? 1 : 0;
        // BER surface: the rotor spins while the turbine has air and sky.
        setActive(producing);

        if (producing && getEnergy().getAmount() < getEnergy().getCapacity()) {
            UpgradeModifiers mods = modifiers();
            int rate = WindMath.ratePerTick(NeroTechConfig.windTurbineNePerTick(), pos.getY(),
                    mods.speedMultiplier() * presetSpeedFactor() * dimension);
            if (rate > 0) {
                energyBuffer().generate(rate);
            }
        }

        MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(), sideConfig());
    }

    /** A generator is never load-shed by a Grid Controller. */
    @Override
    public boolean shedable() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.wind_turbine");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new WindTurbineMenu(containerId, playerInventory, this, this.data);
    }
}
