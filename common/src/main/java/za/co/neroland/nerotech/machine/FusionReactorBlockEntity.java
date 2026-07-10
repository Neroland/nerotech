package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.menu.NeroGeneratorMenu;
import za.co.neroland.nerotech.pollution.PollutionManager;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.tag.NeroTechTags;

/**
 * Fusion Reactor controller — the Stage E (2026-07-10) <b>scalable multiblock</b>: a hollow
 * 3³/5³/7³ shell of fusion casing / containment glass validated by {@link FusionStructure}, with
 * this controller at the centre of one vertical wall, facing outward. <b>Inert until formed</b> —
 * an unformed controller burns nothing and the BER renders the shell dark.
 *
 * <p>Shell size gates fuel tier and scales everything: tier-N fuel (tag
 * {@code nerotech:fusion_fuel/tierN}, datapack-overridable, tier 1 = plain
 * {@code nerotech:fusion_fuels}) needs a shell of tier ≥ N; output multiplies by
 * {@code fusionSizeOutputPermille}; heat scales with size; a meltdown's blast radius grows with
 * the shell ({@code fusionReactorMeltdownEnabled} still admin-disableable, stall otherwise).
 * Breaking the shell while a charge is burning is a <b>containment breach</b>: the charge is
 * lost and vents a pollution burst into the region (aggregate-only — no player data).
 */
public class FusionReactorBlockEntity extends NeroTechMachineBlockEntity {

    public static final int FUEL_SLOT = 0;

    /** Structure re-validation cadence (ticks): eager while unformed, demolition-check while formed. */
    private static final int RECHECK_UNFORMED = 20;
    private static final int RECHECK_FORMED = 100;
    /** Containment breach: pollution burst = this many normal per-operation contributions at once. */
    private static final int BREACH_POLLUTION_OPS = 25;

    /** Formed state (synced to the BER via the update tag; persisted to avoid flicker on load). */
    private boolean formed;
    private int shellSize;
    /** Burning fuel tier (for heat scaling across a charge); 0 when idle. */
    private int burningTier;

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
        revalidateOnCadence(level, pos, state);

        if (!this.formed) {
            // Inert until formed: no burn, no fuel consumption; residual stored NE still drains out.
            reportStatus(MachineStatus.UNFORMED);
            setActive(false);
            MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(),
                    sideConfig());
            return;
        }

        // Meltdown / safety check first — blast radius scales with the shell.
        if (heat() >= NeroTechConfig.heatCapacity()) {
            if (NeroTechConfig.fusionReactorMeltdownEnabled() && level instanceof ServerLevel) {
                meltdown(level);
                return;
            }
            // Survival-friendly: stall until the thermal model cools it; stored power still flows.
            reportStatus(MachineStatus.THROTTLED);
            setActive(false); // torus dies; only the BER warning strobe telegraphs the overheat
            MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(),
                    sideConfig());
            return;
        }

        boolean roomToStore = getEnergy().getAmount() < getEnergy().getCapacity();
        if (this.progress > 0) {
            this.progress--;
            UpgradeModifiers mods = modifiers();
            // Stage H preset: output NE scales with the speed factor (heat scales at the base —
            // an overdriven reactor courts its own meltdown threshold).
            long output = Math.round(NeroTechConfig.fusionReactorNePerTick()
                    * mods.speedMultiplier() * presetSpeedFactor()
                    * FusionStructure.outputPermille(this.shellSize) / 1000.0D);
            energyBuffer().generate((int) Math.min(Integer.MAX_VALUE, output));
            // Bigger shells run hotter: ×4 (3³) / ×5 (5³) / ×6 (7³) the base heat rate.
            addHeat(NeroTechConfig.heatPerOperation() * (3 + shellTier()));
            emitPollution(level, pos);
        } else {
            ItemStack fuel = this.items.get(FUEL_SLOT);
            int tier = fuelTier(fuel);
            if (tier > 0 && tier <= shellTier() && roomToStore && !overheated()) {
                int burn = NeroTechConfig.fusionFuelBurnTicks(tier);
                this.progress = burn;
                this.maxProgress = burn;
                this.burningTier = tier;
                fuel.shrink(1);
                setChanged();
            } else {
                // Analytics: too hot to ignite reads THROTTLED; no fuel or a tier the shell can't
                // contain, STARVED; a full buffer just idles (the default covers it).
                if (overheated()) {
                    reportStatus(MachineStatus.THROTTLED);
                } else if (tier <= 0 || tier > shellTier()) {
                    reportStatus(MachineStatus.STARVED);
                }
                if (this.maxProgress != 0) {
                    this.maxProgress = 0;
                    this.burningTier = 0;
                }
            }
        }

        // BER surface: the plasma torus spins while a fusion charge is burning.
        setActive(this.progress > 0);

        MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(),
                sideConfig());
    }

    /** Bounded structure re-validation on a phase-spread cadence; handles form + demolition. */
    private void revalidateOnCadence(Level level, BlockPos pos, BlockState state) {
        int cadence = this.formed ? RECHECK_FORMED : RECHECK_UNFORMED;
        if ((level.getGameTime() + Math.floorMod(pos.hashCode(), cadence)) % cadence != 0) {
            return;
        }
        Direction facing = state.getValue(NeroTechMachineBlock.FACING);
        FusionStructure.Shell shell = FusionStructure.validate(level, pos, facing);
        // Direct null checks (not via a flag) so ecj's null-flow analysis can track them.
        int nowSize = shell != null ? shell.size() : 0;
        boolean nowFormed = shell != null;
        if (nowFormed == this.formed && nowSize == this.shellSize) {
            return;
        }
        // Containment breach: the shell broke while a charge was burning — lose it, vent pollution.
        if (this.formed && !nowFormed && this.progress > 0 && level instanceof ServerLevel serverLevel) {
            this.progress = 0;
            this.maxProgress = 0;
            this.burningTier = 0;
            PollutionManager.record(serverLevel, pos,
                    NeroTechConfig.pollutionPerOperation() * BREACH_POLLUTION_OPS, this.ownerId);
        }
        this.formed = nowFormed;
        this.shellSize = nowSize;
        setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    /** 1-based shell tier (3³→1, 5³→2, 7³→3); 0 when unformed. */
    private int shellTier() {
        return this.formed ? (this.shellSize - 1) / 2 : 0;
    }

    /** Highest fuel tier a stack provides (datapack tags), or 0 for non-fuel. */
    private static int fuelTier(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(NeroTechTags.FUSION_FUEL_TIER3)) {
            return 3;
        }
        if (stack.is(NeroTechTags.FUSION_FUEL_TIER2)) {
            return 2;
        }
        return stack.is(NeroTechTags.FUSION_FUELS) ? 1 : 0;
    }

    private void meltdown(Level level) {
        // Epicentre is the shell's interior centre; radius grows with the shell (4/6/8).
        Direction facing = getBlockState().getValue(NeroTechMachineBlock.FACING);
        BlockPos center = this.worldPosition.relative(facing.getOpposite(), (this.shellSize - 1) / 2);
        float radius = this.shellSize + 1.0F;
        level.explode(null, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, radius,
                Level.ExplosionInteraction.BLOCK);
        level.removeBlock(this.worldPosition, false);
    }

    // --- BER read surface (synced via the update tag) -------------------------------------------

    /** Whether the multiblock shell is formed (BER: torus + glow only when formed). */
    public boolean renderFormed() {
        return this.formed;
    }

    /** The formed shell's edge size (3/5/7), or 0 — scales and positions the BER torus. */
    public int renderShellSize() {
        return this.shellSize;
    }

    // --- persistence (formed state rides saveAdditional and therefore the update tag) ------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Formed", this.formed);
        output.putInt("ShellSize", this.shellSize);
        output.putInt("BurningTier", this.burningTier);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.formed = input.getBooleanOr("Formed", false);
        this.shellSize = input.getIntOr("ShellSize", 0);
        this.burningTier = input.getIntOr("BurningTier", 0);
    }

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        return slot == FUEL_SLOT && fuelTier(stack) > 0;
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
