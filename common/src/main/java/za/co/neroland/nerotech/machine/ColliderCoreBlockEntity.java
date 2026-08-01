package za.co.neroland.nerotech.machine;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;
import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.AcceleratorPath.Segment;
import za.co.neroland.nerotech.menu.ColliderMenu;
import za.co.neroland.nerotech.recipe.ColliderRecipe;
import za.co.neroland.nerotech.recipe.ColliderRecipeInput;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Accelerator Controller — the head of NeroTech's free-form particle accelerator (mechanic inspired
 * by Oritech's particle accelerator; clean-room implementation).
 *
 * <p>There is no multiblock. The controller traces a beam line out of its {@code FACING} face through
 * {@link AcceleratorGuideBlock}s at its own Y level ({@link AcceleratorPath}); if that line comes back
 * into the controller it is a closed LOOP and can circulate a particle. The particle is virtual — a
 * segment index and a speed on this block entity, never an entity — advanced once per tick and drawn
 * as a vanilla particle streak so players watch it lap the ring.
 *
 * <p>Three rules ({@link AcceleratorMath}) make ring geometry the progression axis:
 * the <b>gap rule</b> (a stretch too long for the current speed loses the particle), the <b>bend
 * rule</b> (a 45° turn taken too fast crashes it) and the <b>energy formula</b>
 * ({@code E = 0.5·v²·scale}) that {@link ColliderRecipe}s gate on. Bigger rings tolerate higher
 * speeds, so a high-energy recipe is really a minimum ring size.
 *
 * <p>Running is automatic: drop a particle in the injection slot and, with a closed loop and NE in
 * the buffer, it launches at the speed the loop's longest stretch demands. Put a second item in the
 * collision slot and the next lap through the controller attempts the collision.
 *
 * <p>The trace is cached: it re-runs on a 100-tick cadence, on the controller's own neighbour
 * changes, and at every launch — never per tick. The per-tick work is O(blocks travelled).
 */
public class ColliderCoreBlockEntity extends NeroTechMachineBlockEntity {

    /** The particle injected on launch. */
    public static final int SLOT_PARTICLE_A = 0;
    /** The collision target, sampled every time the beam passes the controller. */
    public static final int SLOT_PARTICLE_B = 1;
    public static final int SLOT_OUTPUT = 2;

    private static final int MACHINE_SLOTS = 3;

    /** Beam-line re-trace cadence (ticks); a neighbour change re-traces on the very next tick. */
    private static final int RETRACE_CADENCE = 100;

    /** The accelerator runs far hotter than a Tier-1 processor: triple the base heat per guide. */
    private static final int HEAT_MULTIPLIER = 3;

    /** Extra heat dumped into the controller when a particle crashes into a bend. */
    private static final int CRASH_HEAT = 60;

    /** How long a terminal status (collided / fizzled / crashed) stays on the GUI. */
    private static final int STATUS_HOLD_TICKS = 60;

    /** Hard bound on node crossings per tick, so a pathological speed can never spin the loop. */
    private static final int MAX_NODES_PER_TICK = 512;

    /** Blocks between two particles of the beam streak. */
    private static final double TRAIL_SPACING = 2.0D;
    private static final int MAX_TRAIL_PARTICLES = 6;

    /** Cached beam line; null until the first trace. */
    @Nullable
    private AcceleratorPath path;

    /** Set by a neighbour change so the very next tick re-traces instead of waiting out the cadence. */
    private boolean pathDirty = true;

    /**
     * The guides currently showing this controller's beam arrows. Transient on purpose: the arrows
     * live in the guides' own blockstates (which persist by themselves) and the next trace corrects
     * whatever a reload left behind — this set only exists so a trace knows which guides fell OFF the
     * line and must be blanked.
     */
    private Set<BlockPos> litGuides = Set.of();

    // --- the virtual particle (server-authoritative; persisted so a reload does not eat a run) -----

    private boolean running;
    private double speed;
    private int segmentIndex;
    /** Blocks travelled into the current segment. */
    private double segmentProgress;
    /** A single-item copy of what was injected — the circulating half of a collision. */
    private ItemStack circulating = ItemStack.EMPTY;

    private ColliderStatus status = ColliderStatus.NO_PATH;
    private int statusHold;

    /** Last matched collision — passed back as a hint so repeat lookups skip the type scan. */
    @Nullable
    private RecipeHolder<ColliderRecipe> lastRecipe;

    public ColliderCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLLIDER_CORE.get(), pos, state, MACHINE_SLOTS);
        // PROCESSOR preset: ITEM input on every face except BOTTOM (=output), ENERGY input everywhere.
        setupSideConfig(SideConfig.builder()
                .channel(Channel.ITEM,
                        SlotGroup.of("input", SLOT_PARTICLE_A, SLOT_PARTICLE_B),
                        SlotGroup.of("output", SLOT_OUTPUT))
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .build());
    }

    /** Drop the cached beam line; the next tick re-traces it. Called by the block. */
    public void invalidatePath() {
        this.pathDirty = true;
    }

    /** Accelerating emits — the analytics panel shows the config-derived nominal rate. */
    @Override
    public int pollutionPerMinute() {
        return emissionPerMinute();
    }

    // --- tick ------------------------------------------------------------------------------------

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        retraceOnCadence(level, pos, state);
        if (this.statusHold > 0) {
            this.statusHold--;
        }

        if (this.running) {
            advance(level, pos);
        } else {
            tryLaunch(pos);
        }

        setActive(this.running);
        reportStatus(analyticsStatus());
    }

    /** Bounded beam-line re-trace, on a phase-spread cadence or immediately after a neighbour change. */
    private void retraceOnCadence(Level level, BlockPos pos, BlockState state) {
        boolean due = this.pathDirty
                || (level.getGameTime() + Math.floorMod(pos.hashCode(), RETRACE_CADENCE)) % RETRACE_CADENCE == 0;
        if (!due) {
            return;
        }
        this.pathDirty = false;
        AcceleratorPath traced = trace(level, pos, state);
        this.path = traced;
        applyIndicators(level, traced);
        // A loop broken (or shortened) mid-run drops the particle: the beam had nowhere to go. A
        // re-trace that still yields a closed loop of the same length keeps the run alive, which is
        // what lets a particle survive a chunk reload.
        if (this.running && (!traced.closed() || this.segmentIndex >= traced.segments().size())) {
            abort(ColliderStatus.FIZZLED_GAP);
        }
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    /**
     * Light the ring: point every guide on the new beam line at the heading the beam LEAVES it on, and
     * blank the ones that just fell off (an open or failed trace blanks the lot, which is exactly the
     * reading a player wants — an unlit coil is not part of a closed loop).
     *
     * <p>Display only: the arrows are a blockstate on the guides, never read back by the trace. The
     * writes are compare-before-set inside {@link AcceleratorGuideBlock#setIndicator} and carry no
     * neighbour update, so a settled ring re-traces into zero block changes and can never ping-pong
     * with {@link #invalidatePath()}. Only positions the trace already visited are touched, so no
     * chunk is ever loaded on their account.
     */
    private void applyIndicators(Level level, AcceleratorPath traced) {
        if (level.isClientSide()) {
            return;
        }
        Set<BlockPos> lit = new LinkedHashSet<>();
        if (traced.closed()) {
            for (Segment segment : traced.segments()) {
                if (segment.bend() == null) {
                    continue; // the controller itself (or the point of loss) — not a guide
                }
                BlockPos guide = segment.end();
                if (AcceleratorGuideBlock.setIndicator(level, guide,
                        AcceleratorGuideBlock.Indicator.of(segment.outgoing()))) {
                    lit.add(guide);
                }
            }
        }
        for (BlockPos stale : this.litGuides) {
            if (!lit.contains(stale)) {
                AcceleratorGuideBlock.setIndicator(level, stale, AcceleratorGuideBlock.Indicator.NONE);
            }
        }
        this.litGuides = lit.isEmpty() ? Set.of() : Set.copyOf(lit);
    }

    private AcceleratorPath trace(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.hasProperty(NeroTechMachineBlock.FACING)
                ? state.getValue(NeroTechMachineBlock.FACING) : Direction.NORTH;
        return AcceleratorPath.trace(level, pos, facing,
                NeroTechConfig.acceleratorMaxGap(), NeroTechConfig.acceleratorMaxGuides());
    }

    /**
     * Inject a particle when everything lines up: a closed loop, something in the injection slot, a
     * cool enough controller and enough NE for the first boost. The injection speed is the slowest
     * speed the loop's LONGEST stretch tolerates (the gap rule inverted), floored at
     * {@code acceleratorLaunchSpeed} — a wide ring starts fast, a tight one crawls.
     */
    private void tryLaunch(BlockPos pos) {
        AcceleratorPath current = this.path;
        if (current == null || current.segments().isEmpty()) {
            setStatus(ColliderStatus.NO_PATH);
            return;
        }
        if (!current.closed()) {
            setStatus(ColliderStatus.OPEN_LOOP);
            return;
        }
        ItemStack injected = this.items.get(SLOT_PARTICLE_A);
        if (injected.isEmpty()) {
            setStatus(ColliderStatus.READY);
            return;
        }
        if (overheated()) {
            setStatus(ColliderStatus.THROTTLED);
            return;
        }
        int cost = guideCost();
        if (!energyBuffer().has(cost)) {
            setStatus(ColliderStatus.NO_ENERGY);
            return;
        }
        energyBuffer().consume(cost);
        this.circulating = injected.copyWithCount(1);
        injected.shrink(1);
        this.speed = Math.max(NeroTechConfig.acceleratorLaunchSpeed(),
                current.injectionSpeed(NeroTechConfig.acceleratorMinGapAllowance(),
                        NeroTechConfig.acceleratorGapPerSpeed()));
        this.segmentIndex = 0;
        this.segmentProgress = 0.0D;
        this.running = true;
        setStatus(ColliderStatus.ACCELERATING);
        setChanged();
        markPos(pos);
    }

    /**
     * Move the particle one tick: {@code speed × acceleratorTickScale} blocks, resolving every node it
     * crosses on the way (gap rule, boost or coast, bend rule, and — at the controller — the collision
     * attempt). Costs O(nodes crossed), bounded by {@link #MAX_NODES_PER_TICK}.
     */
    private void advance(Level level, BlockPos pos) {
        AcceleratorPath current = this.path;
        if (current == null || !current.closed() || current.segments().isEmpty()) {
            abort(ColliderStatus.FIZZLED_GAP);
            return;
        }
        double remaining = this.speed * NeroTechConfig.acceleratorTickScale();
        if (remaining <= 0.0D) {
            // A coasting particle that ran completely out of momentum simply dies where it stands.
            abort(ColliderStatus.FIZZLED_GAP);
            return;
        }

        int crossings = 0;
        while (this.running && remaining > 0.0D && crossings < MAX_NODES_PER_TICK) {
            Segment segment = current.segments().get(this.segmentIndex);
            double left = segment.length() - this.segmentProgress;
            if (remaining < left) {
                this.segmentProgress += remaining;
                break;
            }
            remaining -= left;
            this.segmentProgress = 0.0D;
            crossings++;
            arriveAt(level, pos, current, segment);
            if (!this.running) {
                return;
            }
            this.segmentIndex = (this.segmentIndex + 1) % current.segments().size();
        }

        trail(level, current);
        setChanged();
    }

    /** Resolve one node crossing: the physics rules, then either the collider or the guide's boost. */
    private void arriveAt(Level level, BlockPos pos, AcceleratorPath current, Segment segment) {
        // GAP RULE first, against the speed the stretch was actually crossed at.
        if (!AcceleratorMath.gapAllowed(segment.length(), this.speed,
                NeroTechConfig.acceleratorMinGapAllowance(), NeroTechConfig.acceleratorGapPerSpeed())) {
            fizzle(level, pos, ColliderStatus.FIZZLED_GAP);
            return;
        }

        AcceleratorGuideBlock.Bend bend = segment.bend();
        if (bend == null) {
            // Back at the controller: the one place a collision can happen.
            collide(level, pos);
            return;
        }

        // BEND RULE: a 45° turn needs a run-up proportional to the speed it is taken at.
        if (bend != AcceleratorGuideBlock.Bend.STRAIGHT
                && !AcceleratorMath.bendAllowed(this.speed, segment.length(),
                        NeroTechConfig.acceleratorBendSpeedBase())) {
            addHeat(CRASH_HEAT);
            fizzle(level, pos, ColliderStatus.CRASHED_BEND);
            return;
        }

        // Powered guides boost; an unpowered one lets the particle coast and bleed speed.
        UpgradeModifiers mods = modifiers();
        int cost = guideCost();
        if (energyBuffer().has(cost)) {
            energyBuffer().consume(cost);
            this.speed = AcceleratorMath.boosted(this.speed, NeroTechConfig.acceleratorBoostPerGuide()
                    * mods.speedMultiplier() * presetSpeedFactor());
            addHeat(NeroTechConfig.heatPerOperation() * HEAT_MULTIPLIER);
            emitPollution(level, pos);
            setStatus(ColliderStatus.ACCELERATING);
        } else {
            this.speed = AcceleratorMath.coasted(this.speed, NeroTechConfig.acceleratorDragPerGuide());
            setStatus(ColliderStatus.COASTING);
        }
    }

    /**
     * The beam is back at the controller. If there is a target in the collision slot and a recipe
     * matching the pair, and the current collision energy clears its floor, both particles are
     * consumed and the product lands in the output slot. Otherwise the particle keeps circulating —
     * an under-energy pass is simply a miss, and the player can wait for more laps.
     */
    private void collide(Level level, BlockPos pos) {
        ItemStack target = this.items.get(SLOT_PARTICLE_B);
        if (target.isEmpty() || this.circulating.isEmpty() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ColliderRecipeInput input = new ColliderRecipeInput(this.circulating, target);
        Optional<RecipeHolder<ColliderRecipe>> match = serverLevel.recipeAccess()
                .getRecipeFor(ModRecipeTypes.COLLIDER.get(), input, serverLevel, this.lastRecipe);
        if (match.isEmpty()) {
            return;
        }
        this.lastRecipe = match.get();
        ColliderRecipe recipe = match.get().value();
        if (collisionEnergy() < recipe.minEnergy()) {
            return; // not fast enough yet — keep lapping
        }
        ItemStack result = recipe.assemble(input);
        if (!canOutput(result)) {
            setStatus(ColliderStatus.BLOCKED);
            return;
        }
        target.shrink(1);
        mergeOutput(result);
        addHeat(NeroTechConfig.heatPerOperation() * HEAT_MULTIPLIER);
        emitPollution(level, pos);
        burst(serverLevel, pos);
        pulseClient();
        endRun(ColliderStatus.COLLIDED);
    }

    // --- run lifecycle ---------------------------------------------------------------------------

    /** A lost particle: a soft failure with a visible puff at the point of loss. */
    private void fizzle(Level level, BlockPos pos, ColliderStatus reason) {
        if (level instanceof ServerLevel serverLevel) {
            AcceleratorPath current = this.path;
            if (current != null && !current.segments().isEmpty()) {
                Segment segment = current.segments().get(this.segmentIndex);
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        segment.end().getX() + 0.5D, segment.end().getY() + 1.1D, segment.end().getZ() + 0.5D,
                        8, 0.2D, 0.2D, 0.2D, 0.01D);
            }
        }
        endRun(reason);
        markPos(pos);
    }

    /** Clear the particle without any effect (a structural loss, e.g. the loop was broken mid-run). */
    private void abort(ColliderStatus reason) {
        endRun(reason);
    }

    private void endRun(ColliderStatus reason) {
        this.running = false;
        this.speed = 0.0D;
        this.segmentIndex = 0;
        this.segmentProgress = 0.0D;
        this.circulating = ItemStack.EMPTY;
        setStatus(reason);
        this.statusHold = STATUS_HOLD_TICKS;
        setChanged();
    }

    private void setStatus(ColliderStatus next) {
        if (this.statusHold > 0 && next != ColliderStatus.COLLIDED
                && next != ColliderStatus.FIZZLED_GAP && next != ColliderStatus.CRASHED_BEND) {
            return; // let a terminal status stay readable for a moment
        }
        this.status = next;
    }

    /** Map the beam status onto the shared analytics vocabulary. */
    private MachineStatus analyticsStatus() {
        return switch (this.status) {
            case NO_PATH, OPEN_LOOP -> MachineStatus.UNFORMED;
            case READY -> MachineStatus.STARVED;
            case THROTTLED -> MachineStatus.THROTTLED;
            case NO_ENERGY, COASTING -> MachineStatus.NO_ENERGY;
            case BLOCKED -> MachineStatus.BLOCKED;
            case ACCELERATING -> MachineStatus.RUNNING;
            default -> MachineStatus.IDLE;
        };
    }

    // --- helpers ---------------------------------------------------------------------------------

    /** NE per guide, scaled by Efficiency modules and the overclock preset. */
    private int guideCost() {
        UpgradeModifiers mods = modifiers();
        return (int) Math.max(0, Math.round(NeroTechConfig.acceleratorNePerGuide()
                * mods.energyMultiplier() * presetEnergyFactor()));
    }

    /** The current collision energy in joules ({@code E = 0.5·v²·acceleratorEnergyScale}). */
    public int collisionEnergy() {
        return AcceleratorMath.collisionEnergy(this.speed, NeroTechConfig.acceleratorEnergyScale());
    }

    /** Whether the output slot can take {@code result} (empty, or a mergeable identical stack). */
    private boolean canOutput(ItemStack result) {
        ItemStack out = this.items.get(SLOT_OUTPUT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void mergeOutput(ItemStack result) {
        ItemStack out = this.items.get(SLOT_OUTPUT);
        if (out.isEmpty()) {
            this.items.set(SLOT_OUTPUT, result.copy());
        } else {
            out.grow(result.getCount());
        }
    }

    /** Push a BE update so the GUI's beam readouts change the moment a run starts or ends. */
    private void markPos(BlockPos pos) {
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // --- visuals (server-sent vanilla particles; no BER, no custom packet) ------------------------

    /**
     * Draw the beam as a short streak of END_ROD particles trailing the particle's current point,
     * roughly one every {@value #TRAIL_SPACING} blocks and capped at {@value #MAX_TRAIL_PARTICLES}
     * per tick — so a fast beam reads as a line without ever flooding the particle budget.
     */
    private void trail(Level level, AcceleratorPath current) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Segment segment = current.segments().get(this.segmentIndex);
        double travelled = this.speed * NeroTechConfig.acceleratorTickScale();
        int samples = Mth.clamp((int) (travelled / TRAIL_SPACING), 1, MAX_TRAIL_PARTICLES);
        for (int i = 0; i < samples; i++) {
            double back = Math.min(this.segmentProgress, i * TRAIL_SPACING);
            double along = this.segmentProgress - back;
            double t = segment.length() <= 0.0D ? 0.0D : along / segment.length();
            double x = Mth.lerp(t, segment.start().getX() + 0.5D, segment.end().getX() + 0.5D);
            double z = Mth.lerp(t, segment.start().getZ() + 0.5D, segment.end().getZ() + 0.5D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, segment.start().getY() + 1.1D, z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** The tachyon flash of a successful collision, centred on the controller. */
    private static void burst(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 40, 0.4D, 0.4D, 0.4D, 0.25D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 30, 0.3D, 0.3D, 0.3D, 0.4D);
    }

    // --- GUI sync (five accelerator gauges after the seven shared ones) ---------------------------

    @Override
    protected int extraDataCount() {
        return 5;
    }

    /**
     * ContainerData rides shorts, so both scalars are pre-scaled and clamped here:
     * [0] speed × 10 (tenths of a "metre per second"), [1] collision energy / 10 (tens of joules),
     * [2] guide count, [3] 1 when the beam line is a closed loop, [4] {@link ColliderStatus} ordinal.
     */
    @Override
    protected int extraData(int index) {
        AcceleratorPath current = this.path;
        return switch (index) {
            case 0 -> Mth.clamp((int) Math.round(this.speed * 10.0D), 0, 32_000);
            case 1 -> Mth.clamp(collisionEnergy() / 10, 0, 32_000);
            case 2 -> current == null ? 0 : Math.min(32_000, current.guides());
            case 3 -> current != null && current.closed() ? 1 : 0;
            case 4 -> this.status.ordinal();
            default -> 0;
        };
    }

    // --- container rules -------------------------------------------------------------------------

    @Override
    public boolean canPlaceMachineItem(int slot, ItemStack stack) {
        // Any item may be a particle: which pairs collide into what is the recipe's business, and the
        // GUI status line reports a pair with no recipe rather than refusing the item outright.
        return slot == SLOT_PARTICLE_A || slot == SLOT_PARTICLE_B;
    }

    @Override
    public boolean canTakeMachineItem(int slot) {
        return slot == SLOT_OUTPUT;
    }

    // --- persistence -----------------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Running", this.running);
        output.putDouble("Speed", this.speed);
        output.putInt("SegmentIndex", this.segmentIndex);
        output.putDouble("SegmentProgress", this.segmentProgress);
        output.putInt("BeamStatus", this.status.ordinal());
        output.store("Circulating", ItemStack.OPTIONAL_CODEC, this.circulating);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.running = input.getBooleanOr("Running", false);
        this.speed = input.getDoubleOr("Speed", 0.0D);
        this.segmentIndex = input.getIntOr("SegmentIndex", 0);
        this.segmentProgress = input.getDoubleOr("SegmentProgress", 0.0D);
        this.status = ColliderStatus.byOrdinal(input.getIntOr("BeamStatus", ColliderStatus.NO_PATH.ordinal()));
        this.circulating = input.read("Circulating", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.pathDirty = true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.collider_core");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ColliderMenu(containerId, playerInventory, this, this.data);
    }
}
