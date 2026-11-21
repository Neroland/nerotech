package za.co.neroland.nerotech.machine.chemistry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

import za.co.neroland.nerotech.api.Atmosphere;
import za.co.neroland.nerotech.machine.MachineStatus;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Atmospheric Condenser ({@code nerotech:condensing}): slowly draws the dimension's air into gas. What
 * it draws is the {@link Atmosphere} {@code PlanetApi.atmosphereAt} reports, matched
 * against each recipe's {@code atmosphere} field; the reading is cached and refreshed every 200 ticks.
 * Twilight worlds draw at half rate (folded into the preset speed factor). In {@link Atmosphere#VACUUM}
 * it idles, stalled with a "no atmosphere" cause on the status line and in analytics.
 */
public class AtmosphericCondenserBlockEntity extends ProcessingMachineBlockEntity {

    /** No inputs, one output slot (trace by-products), a gas tank out. */
    public static final ProcessingSpec SPEC = new ProcessingSpec(0, false, 1, 0L, 0L, 0L, 4_000L);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC)
            .arrow(84, 34, 22, 16)
            .output(0, 110, 30)
            .gasOut(136, 28, 40)
            .build();

    /** Chemistry's own detail code (above the base's range; {@link ChemistryScreen} renders it). */
    public static final int DETAIL_NO_ATMOSPHERE = 100;
    public static final String NO_ATMOSPHERE_KEY = "nerotech.machine.detail.no_atmosphere";

    private static final int READING_INTERVAL = 200;

    private AtmosphereMapping.Reading reading = AtmosphereMapping.Reading.of(Atmosphere.EARTHLIKE);
    private long readingUntil = Long.MIN_VALUE;
    private boolean vacuum;
    /** Render sync (C1 extra): the atmosphere last pushed to clients, and the client's copy. */
    private Atmosphere syncedAtmosphere;
    private Atmosphere clientAtmosphere = Atmosphere.EARTHLIKE;
    private boolean clientTwilight;

    public AtmosphericCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ChemistryContent.ATMOSPHERIC_CONDENSER_BE.get(), pos, state, SPEC);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.CONDENSING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.atmospheric_condenser");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ProcessingMenu(ChemistryContent.ATMOSPHERIC_CONDENSER_MENU, id, inv, this, this.data, LAYOUT);
    }

    /** The current reading (server side refreshed on an interval; the default elsewhere). */
    public AtmosphereMapping.Reading reading() {
        Level level = this.level;
        if (level != null && !level.isClientSide() && level.getGameTime() >= this.readingUntil) {
            this.reading = AtmosphereLookup.readingAt(level);
            this.readingUntil = level.getGameTime() + READING_INTERVAL;
        }
        return this.reading;
    }

    @Override
    protected String atmosphere() {
        return reading().atmosphere().name();
    }

    /** The preset factor times the place's draw rate (0.5 on a twilight world). */
    @Override
    public double presetSpeedFactor() {
        double rate = reading().rate();
        return super.presetSpeedFactor() * (rate > 0.0D ? rate : 1.0D);
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (!reading().atmosphere().condensable()) {
            this.vacuum = true;
            reportStatus(MachineStatus.STARVED, NO_ATMOSPHERE_KEY);
            setActive(false);
            if (this.progress != 0 || this.maxProgress != 0) {
                this.progress = 0;
                this.maxProgress = 0;
                setChanged();
            }
            return;
        }
        this.vacuum = false;
        super.tickMachine(level, pos, state);
    }

    /**
     * The air being drawn (machine contract C1 extra, synced on change): the louvres close in a vacuum
     * and turn at half speed on a twilight world. Server: the cached reading; client: the last sync.
     */
    public Atmosphere renderAtmosphere() {
        Level level = this.level;
        if (level != null && level.isClientSide()) {
            return this.clientAtmosphere;
        }
        return reading().atmosphere();
    }

    /** The draw rate as last synced (1, or 0.5 on a twilight world). */
    public float renderDrawRate() {
        Level level = this.level;
        if (level != null && level.isClientSide()) {
            return this.clientTwilight ? (float) AtmosphereMapping.TWILIGHT_RATE : 1.0F;
        }
        double rate = reading().rate();
        return rate > 0.0D && rate < 1.0D ? (float) rate : 1.0F;
    }

    @Override
    protected boolean renderSyncDirty() {
        Atmosphere now = reading().atmosphere();
        if (now != this.syncedAtmosphere) {
            this.syncedAtmosphere = now;
            return true;
        }
        return false;
    }

    @Override
    protected void writeRenderExtras(CompoundTag tag) {
        tag.putInt("Atmo", reading().atmosphere().ordinal());
        tag.putBoolean("AtmoTwilight", renderDrawRate() < 1.0F);
    }

    @Override
    protected void readRenderExtras(ValueInput input) {
        int ordinal = input.getIntOr("Atmo", Atmosphere.EARTHLIKE.ordinal());
        Atmosphere[] values = Atmosphere.values();
        this.clientAtmosphere = values[Math.max(0, Math.min(values.length - 1, ordinal))];
        this.clientTwilight = input.getBooleanOr("AtmoTwilight", false);
    }

    @Override
    protected int extraData(int index) {
        if (index == X_DETAIL && this.vacuum) {
            return DETAIL_NO_ATMOSPHERE;
        }
        return super.extraData(index);
    }
}
