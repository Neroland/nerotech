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

import za.co.neroland.nerotech.api.MachineFailureEvents;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.link.NeroTechLinkModule;
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
 *
 * <p><b>Meltdown safety (0.4.0):</b> the blast radius is {@code min(shellSize + 1,
 * fusionMeltdownRadiusCap)} ({@link #meltdownRadius}), and whether it breaks blocks at all follows
 * {@code fusionMeltdownTerrainDamage} — {@code auto} (default) means no terrain damage on a
 * dedicated server and full damage in singleplayer/LAN; {@code on}/{@code off} force it. With
 * terrain damage off the explosion still deals damage and knockback and the reactor block itself
 * is still removed; only the crater is spared.
 *
 * <p><b>Failure events:</b> the reactor publishes its failure stages on
 * {@link MachineFailureEvents} — stage 1 once per overheat episode (too hot to ignite / stalled),
 * stage 3 on meltdown, stage 4 on a containment breach — keyed by machine + place, never the owner.
 */
public class FusionReactorBlockEntity extends NeroTechMachineBlockEntity {

    public static final int FUEL_SLOT = 0;

    /** Structure re-validation cadence (ticks): eager while unformed, demolition-check while formed. */
    private static final int RECHECK_UNFORMED = 20;
    private static final int RECHECK_FORMED = 100;
    /** Containment breach: pollution burst = this many normal per-operation contributions at once. */
    private static final int BREACH_POLLUTION_OPS = 25;
    /** Extra multiples of the base heat rate while a tier-4 (antimatter) charge burns. */
    private static final int ANTIMATTER_HEAT_SURCHARGE = 2;
    /** Shell edge size that alone can contain a fuel one tier above its own shell tier. */
    private static final int MAX_SHELL_SIZE = 7;

    /** Formed state (synced to the BER via the update tag; persisted to avoid flicker on load). */
    private boolean formed;
    private int shellSize;
    /** Burning fuel tier (for heat scaling across a charge); 0 when idle. */
    private int burningTier;

    /** The failure-event machine id (the block-entity type id; a machine class, never a player). */
    private static final String MACHINE_ID = "nerotech:fusion_reactor";

    /**
     * Whether the stage-1 (warning) failure event has been fired for the current overheat episode —
     * transient, so the episode reads as one event per rising edge rather than one per tick.
     */
    private boolean overheatWarned;

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

    /** A burning reactor emits — the analytics panel shows the config-derived nominal rate. */
    @Override
    public int pollutionPerMinute() {
        return emissionPerMinute();
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

        // Meltdown / safety check first — blast radius scales with the shell (capped by config).
        if (heat() >= NeroTechConfig.heatCapacity()) {
            if (NeroTechConfig.fusionReactorMeltdownEnabled() && level instanceof ServerLevel) {
                meltdown(level);
                return;
            }
            // Survival-friendly: stall until the thermal model cools it; stored power still flows.
            warnOverheat(level, pos);
            reportStatus(MachineStatus.THROTTLED);
            setActive(false); // torus dies; only the BER warning strobe telegraphs the overheat
            MachineEnergy.pushToNeighbours(level, pos, energyBuffer(), NeroTechConfig.machineMaxTransfer(),
                    sideConfig());
            return;
        }
        // The overheat episode ends once the reactor is cool enough to ignite again (falling edge).
        if (this.overheatWarned && !overheated()) {
            this.overheatWarned = false;
            fireFailure(level, pos, MachineFailureEvents.STAGE_WARNING, false);
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
            // Bigger shells run hotter: ×4 (3³) / ×5 (5³) / ×6 (7³) the base heat rate — and burning
            // ANTIMATTER (tier 4) adds a flat +2 on top. That is the whole trade: the longest burn in
            // the mod, bought by running the reactor close enough to the meltdown line to feel it.
            addHeat(NeroTechConfig.heatPerOperation()
                    * (3 + shellTier() + (this.burningTier >= 4 ? ANTIMATTER_HEAT_SURCHARGE : 0)));
            emitPollution(level, pos);
        } else {
            ItemStack fuel = this.items.get(FUEL_SLOT);
            int tier = fuelTier(fuel);
            if (canContain(tier) && roomToStore && !overheated()) {
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
                    warnOverheat(level, pos);
                    reportStatus(MachineStatus.THROTTLED);
                } else if (!canContain(tier)) {
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
            // World-event broadcast + a CRITICAL alert to the owner (no personal data in the broadcast).
            NeroTechLinkModule.onReactorCritical(serverLevel.getServer(), this.ownerId, pos,
                    "containment_breach", this.shellSize);
            fireFailure(serverLevel, pos, MachineFailureEvents.STAGE_BREACH, true);
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

    /**
     * Whether this shell can contain a charge of {@code tier}. Normally a tier-N fuel needs a
     * shell of tier ≥ N — but the <b>maximal</b> 7³ shell reaches one tier further, which is what
     * makes tier-4 antimatter burnable at all (and only there).
     */
    private boolean canContain(int tier) {
        if (tier <= 0) {
            return false;
        }
        return tier <= shellTier()
                || (tier <= shellTier() + 1 && this.shellSize == MAX_SHELL_SIZE);
    }

    /** Highest fuel tier a stack provides (datapack tags), or 0 for non-fuel. */
    private static int fuelTier(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(NeroTechTags.FUSION_FUEL_TIER4)) {
            return 4;
        }
        if (stack.is(NeroTechTags.FUSION_FUEL_TIER3)) {
            return 3;
        }
        if (stack.is(NeroTechTags.FUSION_FUEL_TIER2)) {
            return 2;
        }
        return stack.is(NeroTechTags.FUSION_FUELS) ? 1 : 0;
    }

    /**
     * Stage-1 failure event, once per overheat episode: the reactor is too hot to ignite (or has
     * stalled at capacity with meltdown disabled). Cleared on the falling edge in {@link #tickMachine}.
     */
    private void warnOverheat(Level level, BlockPos pos) {
        if (!this.overheatWarned) {
            this.overheatWarned = true;
            fireFailure(level, pos, MachineFailureEvents.STAGE_WARNING, true);
        }
    }

    /** Publish a failure stage for this reactor on the ecosystem bus (server side only). */
    private void fireFailure(Level level, BlockPos pos, int stage, boolean rising) {
        if (level instanceof ServerLevel) {
            MachineFailureEvents.fire(MACHINE_ID, level.dimension().identifier().toString(), pos, stage, rising);
        }
    }

    /**
     * A meltdown's blast radius: the shell edge plus one (4/6/8 for the 3/5/7 shells), never above
     * {@code cap} ({@code fusionMeltdownRadiusCap}) and never below 1. Pure, so the balance is
     * unit-testable.
     */
    public static int meltdownRadius(int shellSize, int cap) {
        return MeltdownMath.meltdownRadius(shellSize, cap);
    }

    private void meltdown(Level level) {
        // Telegraph the meltdown to NeroLink: a world-event broadcast plus a CRITICAL alert to the
        // owner, and the stage-3 failure event for any listener on the ecosystem bus. Fired before the
        // explosion removes this block-entity. (meltdown() is only reached on a ServerLevel — see the
        // heat check in tickMachine.)
        boolean terrainDamage = true;
        if (level instanceof ServerLevel serverLevel) {
            NeroTechLinkModule.onReactorCritical(serverLevel.getServer(), this.ownerId, this.worldPosition,
                    "meltdown", this.shellSize);
            fireFailure(serverLevel, this.worldPosition, MachineFailureEvents.STAGE_FAILURE, true);
            // auto → no terrain damage on a dedicated server (a shared world), full damage otherwise.
            terrainDamage = NeroTechConfig.fusionMeltdownTerrainDamage(serverLevel.getServer().isDedicatedServer());
        }
        // Epicentre is the shell's interior centre; radius grows with the shell (4/6/8), capped by config.
        Direction facing = getBlockState().getValue(NeroTechMachineBlock.FACING);
        BlockPos center = this.worldPosition.relative(facing.getOpposite(), (this.shellSize - 1) / 2);
        float radius = meltdownRadius(this.shellSize, NeroTechConfig.fusionMeltdownRadiusCap());
        level.explode(null, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, radius,
                terrainDamage ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
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

    /** A generator is never load-shed by a Grid Controller (Stage D). */
    @Override
    public boolean shedable() {
        return false;
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
