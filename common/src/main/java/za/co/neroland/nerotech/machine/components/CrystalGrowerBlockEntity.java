package za.co.neroland.nerotech.machine.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.upgrade.UpgradeModifiers;

import za.co.neroland.nerotech.compat.neroeconomy.MachineCharges;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.recipe.ProcessRecipeInput;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Crystal Grower: a seed or feedstock plus a fluid, given time, grows a boule or a crystal
 * ({@code nerotech:crystal_growing}). It is the one machine that works unpowered: with no energy it keeps
 * growing at a slow base rate (the lamp goes dark, the crystal still grows); power brings it to full speed,
 * and cold ambient air ({@code ThermalEnvironment}, read through the base's cached ambient) speeds it
 * further, up to a cap. The rate is {@link CrystalGrowthMath}.
 *
 * <p>Because growth can run without paying energy, this machine runs its own tick over the shared
 * processing state (slots, input tank, recipe type, upgrades, presets, analytics) instead of the base's
 * power-gated one. Heat and pollution come only from powered ticks; unpowered growth is passive.
 */
public class CrystalGrowerBlockEntity extends ProcessingMachineBlockEntity {

    public static final ProcessingSpec SPEC = ComponentsSpecs.CRYSTAL_GROWER;

    /** 176x166: seed + two feedstock slots, the arrow, one output, the fluid gauge right of it. */
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC)
            .output(0, 110, 34)
            .fluidIn(134, 28, 40)
            .build();

    private static final int CHARGE_RETRY_TICKS = 600;

    @Nullable
    private RecipeHolder<ProcessRecipe> cached;
    private long fingerprint = Long.MIN_VALUE;
    /** Growth carried between ticks, in permille of a progress tick (transient: under one tick is lost on unload). */
    private int growthCarry;
    private int detailCode = DETAIL_NONE;
    private int energyRate;
    /** Operations already paid for (transient; a reload at worst charges once more). */
    private int paidOps;
    private long chargeRetryAt;
    @Nullable
    private String chargeRefusalKey;
    /** Unpowered growth rate in permille of a progress tick (0 while powered or stopped); render-synced. */
    private int passiveRate;
    private int syncedPassiveRate = -1;
    private int syncedPassiveMax = -1;
    /** Client: crystal growth per tick (fraction of a whole crystal) while unpowered, from the last sync. */
    private float clientPassivePerTick;

    public CrystalGrowerBlockEntity(BlockPos pos, BlockState state) {
        super(ComponentsContent.CRYSTAL_GROWER_BE.get(), pos, state, SPEC);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.CRYSTAL_GROWING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.crystal_grower");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ProcessingMenu(ComponentsContent.CRYSTAL_GROWER_MENU, id, inv, this, this.data, LAYOUT);
    }

    // --- tick ---------------------------------------------------------------------------------------

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        ProcessRecipe recipe = recipe(level);
        if (recipe == null) {
            stall(MachineStatus.STARVED, starvedDetail());
            return;
        }
        if (trackRecipeProgress(this.cached)) {
            this.growthCarry = 0; // a different crystal starts from nothing
        }
        if (!fits(recipe)) {
            stall(MachineStatus.BLOCKED, DETAIL_OUTPUT_FULL);
            return;
        }
        UpgradeModifiers mods = modifiers();
        int ticks = Math.max(1, (int) Math.round(Math.max(1, recipe.time())
                / Math.max(0.01D, mods.speedMultiplier() * presetSpeedFactor())));
        int cost = (int) Math.max(0, Math.round(recipe.energy() * mods.energyMultiplier() * presetEnergyFactor()));
        this.maxProgress = ticks;

        if (overheated()) {
            this.detailCode = DETAIL_OVERHEATED;
            this.energyRate = 0;
            reportStatus(MachineStatus.THROTTLED, DETAIL_KEYS[DETAIL_OVERHEATED]);
            setActive(false);
            return;
        }
        boolean powered = energyBuffer().has(cost);
        // The per-operation charge applies to every grow, powered or not: unplugging must not avoid it.
        if (this.progress == 0 && !ensurePaid(level)) {
            this.detailCode = DETAIL_UNPAID;
            this.energyRate = 0;
            reportStatus(MachineStatus.UNPAID, this.chargeRefusalKey);
            setActive(false);
            return;
        }
        int rate = CrystalGrowthMath.ratePermille(powered, ambient(level, pos));
        this.passiveRate = powered ? 0 : Math.max(0, rate);
        if (rate <= 0) {
            this.detailCode = DETAIL_NO_ENERGY;
            this.energyRate = 0;
            reportStatus(MachineStatus.NO_ENERGY, DETAIL_KEYS[DETAIL_NO_ENERGY]);
            setActive(false);
            return;
        }
        if (powered) {
            energyBuffer().consume(cost);
            addHeat(NeroTechConfig.heatPerOperation());
            emitPollution(level, pos);
            this.detailCode = DETAIL_NONE;
            this.energyRate = cost;
        } else {
            // Unpowered: still growing, slowly, as designed. It reads RUNNING (no starved alarm, no dark
            // lamp) and the status line adds the cause ("growing slowly, no power").
            this.detailCode = DETAIL_GROWING_UNPOWERED;
            this.energyRate = 0;
            reportStatus(MachineStatus.RUNNING, DETAIL_KEYS[DETAIL_GROWING_UNPOWERED]);
        }
        setActive(true);
        this.growthCarry += rate;
        int steps = this.growthCarry / CrystalGrowthMath.NOMINAL;
        this.growthCarry -= steps * CrystalGrowthMath.NOMINAL;
        this.progress = Math.min(ticks, this.progress + steps);
        if (this.progress >= ticks) {
            finish(level, recipe);
            markCompleted();
            this.progress = 0;
            if (this.paidOps > 0) {
                this.paidOps--;
            }
        }
        setChanged();
    }

    /**
     * Crystal growth per tick, as a fraction of the whole crystal, while it grows unpowered (machine
     * contract C1 extra, synced when the rate or the job length changes; 0 when powered or stopped).
     * The BER advances the crystal at this rate between syncs.
     */
    public float passiveGrowthPerTick() {
        if (this.level != null && this.level.isClientSide()) {
            return this.clientPassivePerTick;
        }
        return this.maxProgress <= 0 ? 0.0F
                : this.passiveRate / (float) CrystalGrowthMath.NOMINAL / this.maxProgress;
    }

    @Override
    protected boolean renderSyncDirty() {
        if (this.passiveRate != this.syncedPassiveRate || (this.passiveRate > 0 && this.maxProgress != this.syncedPassiveMax)) {
            this.syncedPassiveRate = this.passiveRate;
            this.syncedPassiveMax = this.maxProgress;
            return true;
        }
        return false;
    }

    @Override
    protected void writeRenderExtras(CompoundTag tag) {
        tag.putInt("PassiveRate", Math.round(passiveGrowthPerTick() * 1.0E7F)); // 1e-7 of a crystal per tick
    }

    @Override
    protected void readRenderExtras(ValueInput input) {
        this.clientPassivePerTick = input.getIntOr("PassiveRate", 0) / 1.0E7F;
    }

    private void stall(MachineStatus status, int code) {
        this.passiveRate = 0;
        this.detailCode = code;
        this.energyRate = 0;
        reportStatus(status, code == DETAIL_NONE ? null : DETAIL_KEYS[code]);
        setActive(false);
        this.growthCarry = 0;
        if (this.progress != 0 || this.maxProgress != 0) {
            this.progress = 0;
            this.maxProgress = 0;
            setChanged();
        }
    }

    private int starvedDetail() {
        boolean any = this.fluidIn != null && this.fluidIn.getAmount() > 0;
        for (int i = 0; i < SPEC.inputs(); i++) {
            any |= !this.items.get(SPEC.inputSlot(i)).isEmpty();
        }
        return any ? DETAIL_NO_RECIPE : DETAIL_NO_INPUT;
    }

    @Nullable
    private ProcessRecipe recipe(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        long now = fingerprintNow();
        boolean changed = now != this.fingerprint;
        this.fingerprint = now;
        if (!changed && this.cached != null) {
            return this.cached.value();
        }
        if (!changed && (level.getGameTime() % 20L) != 0L) {
            return null;
        }
        ProcessRecipeInput input = recipeInput();
        Optional<RecipeHolder<ProcessRecipe>> match = serverLevel.recipeAccess()
                .getRecipeFor(recipeType(), input, serverLevel, this.cached);
        this.cached = match.orElse(null);
        return this.cached == null ? null : this.cached.value();
    }

    private long fingerprintNow() {
        long h = 17L;
        for (int i = 0; i < SPEC.inputs(); i++) {
            ItemStack stack = this.items.get(SPEC.inputSlot(i));
            h = h * 31L + (stack.isEmpty() ? 0 : System.identityHashCode(stack.getItem()));
            h = h * 31L + stack.getCount();
        }
        if (this.fluidIn != null) {
            h = h * 31L + System.identityHashCode(this.fluidIn.getFluid());
            h = h * 31L + this.fluidIn.getAmount();
        }
        return h;
    }

    /** Whether every result fits the output slot at its full count. */
    private boolean fits(ProcessRecipe recipe) {
        ItemStack slot = this.items.get(SPEC.outputSlot(0)).copy();
        for (ProcessRecipe.ChanceResult result : recipe.results()) {
            ItemStack add = result.create();
            if (add.isEmpty()) {
                continue;
            }
            if (slot.isEmpty()) {
                slot = add.copy();
            } else if (ItemStack.isSameItemSameComponents(slot, add)
                    && slot.getCount() + add.getCount() <= slot.getMaxStackSize()) {
                slot.grow(add.getCount());
            } else {
                return false;
            }
        }
        return true;
    }

    private void finish(Level level, ProcessRecipe recipe) {
        for (ProcessRecipe.CountedIngredient counted : recipe.items()) {
            int need = counted.count();
            for (int i = 0; i < SPEC.inputs() && need > 0; i++) {
                ItemStack stack = this.items.get(SPEC.inputSlot(i));
                if (!stack.isEmpty() && counted.ingredient().test(stack)) {
                    int take = Math.min(need, stack.getCount());
                    stack.shrink(take);
                    need -= take;
                }
            }
        }
        recipe.fluidIn().ifPresent(m -> {
            if (this.fluidIn != null) {
                this.fluidIn.drain(m.amount(), false);
            }
        });
        List<ItemStack> rolled = new ArrayList<>(recipe.results().size());
        for (ProcessRecipe.ChanceResult result : recipe.results()) {
            if (result.guaranteed() || level.getRandom().nextInt(1000) < result.chancePermille()) {
                rolled.add(result.create());
            }
        }
        int out = SPEC.outputSlot(0);
        for (ItemStack add : rolled) {
            ItemStack slot = this.items.get(out);
            if (slot.isEmpty()) {
                this.items.set(out, add.copy());
            } else if (ItemStack.isSameItemSameComponents(slot, add)) {
                slot.grow(Math.min(add.getCount(), slot.getMaxStackSize() - slot.getCount()));
            }
        }
        this.fingerprint = Long.MIN_VALUE;
    }

    /** The optional per-operation charge, settled as the shared processing base settles it. */
    private boolean ensurePaid(Level level) {
        int price = NeroTechConfig.machineOperationChargeCost();
        UUID owner = this.ownerId;
        if (price <= 0 || owner == null || this.paidOps > 0 || !MachineCharges.active()
                || !(level instanceof ServerLevel serverLevel)) {
            this.chargeRefusalKey = null;
            return true;
        }
        long now = level.getGameTime();
        if (this.chargeRefusalKey != null && now < this.chargeRetryAt) {
            return false;
        }
        MachineCharges.Outcome outcome = MachineCharges.charge(serverLevel.getServer(), owner, price);
        if (outcome.ok()) {
            this.paidOps = NeroTechConfig.machineOperationChargeBatch();
            this.chargeRefusalKey = null;
            return true;
        }
        String key = outcome.refusalKey();
        this.chargeRefusalKey = key != null ? key : MachineCharges.GENERIC_REFUSAL_KEY;
        this.chargeRetryAt = now + CHARGE_RETRY_TICKS;
        return false;
    }

    // --- GUI sync: this tick's detail and draw replace the base's (which its own tick would set) ------

    @Override
    protected int extraData(int index) {
        if (index == X_DETAIL) {
            return this.detailCode;
        }
        if (index == X_DETAIL_PARAM) {
            return 0;
        }
        if (index == X_ENERGY_RATE) {
            return Math.min(Short.MAX_VALUE, this.energyRate);
        }
        return super.extraData(index);
    }
}
