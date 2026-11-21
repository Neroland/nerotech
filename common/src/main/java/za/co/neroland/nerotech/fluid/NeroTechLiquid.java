package za.co.neroland.nerotech.fluid;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;

import za.co.neroland.nerotech.registry.ModBlocks;
import za.co.neroland.nerotech.registry.ModItems;

/**
 * NeroTech's three placeable liquids (0.8.0): real source + flowing fluids with a liquid block and a
 * bucket, unlike the gases, which are transport identities only (see {@link GasFluid}).
 *
 * <p>This enum is the loader-neutral description each loader builds its fluid from — NeoForge and
 * Forge turn it into a {@code FluidType} plus flowing-fluid properties (one set per liquid, built
 * lazily, never shared between two liquids), Fabric into a plain {@code FlowingFluid}. The
 * {@link #source()}, {@link #flowing()}, {@link #block()} and {@link #bucket()} accessors resolve the
 * registered objects lazily, so registration order across the four registries does not matter.
 */
public enum NeroTechLiquid {

    /** Cold process coolant: superconductor chain and heat exchangers. Runny and cold. */
    CRYO_COOLANT("cryo_coolant", 1100, 800, 90, 2, 1, 5, false),
    /** Chemistry reagent. Dense, and it hurts anything standing in it. */
    SULFURIC_ACID("sulfuric_acid", 1830, 2600, 300, 2, 2, 10, true),
    /** Polymer feedstock. Thick and slow. */
    POLYMER_RESIN("polymer_resin", 1200, 6000, 300, 1, 3, 30, false);

    private final String id;
    private final int density;
    private final int viscosity;
    private final int temperature;
    private final int slopeFindDistance;
    private final int levelDecreasePerBlock;
    private final int tickDelay;
    private final boolean corrosive;

    NeroTechLiquid(String id, int density, int viscosity, int temperature, int slopeFindDistance,
            int levelDecreasePerBlock, int tickDelay, boolean corrosive) {
        this.id = id;
        this.density = density;
        this.viscosity = viscosity;
        this.temperature = temperature;
        this.slopeFindDistance = slopeFindDistance;
        this.levelDecreasePerBlock = levelDecreasePerBlock;
        this.tickDelay = tickDelay;
        this.corrosive = corrosive;
    }

    /** Registry path of the source fluid, liquid block and fluid type ({@code cryo_coolant}). */
    public String id() {
        return this.id;
    }

    /** Registry path of the flowing fluid ({@code flowing_cryo_coolant}). */
    public String flowingId() {
        return "flowing_" + this.id;
    }

    /** Registry path of the bucket item ({@code cryo_coolant_bucket}). */
    public String bucketId() {
        return this.id + "_bucket";
    }

    public int density() {
        return this.density;
    }

    public int viscosity() {
        return this.viscosity;
    }

    /** Fluid-type temperature in kelvin. */
    public int temperature() {
        return this.temperature;
    }

    public int slopeFindDistance() {
        return this.slopeFindDistance;
    }

    public int levelDecreasePerBlock() {
        return this.levelDecreasePerBlock;
    }

    public int tickDelay() {
        return this.tickDelay;
    }

    /** Whether the liquid block damages entities inside it. */
    public boolean corrosive() {
        return this.corrosive;
    }

    /** Still texture id path ({@code block/cryo_coolant_still}). */
    public String stillTexture() {
        return "block/" + this.id + "_still";
    }

    /** Flowing texture id path ({@code block/cryo_coolant_flow}). */
    public String flowTexture() {
        return "block/" + this.id + "_flow";
    }

    public Fluid source() {
        return switch (this) {
            case CRYO_COOLANT -> NeroTechFluids.CRYO_COOLANT.get();
            case SULFURIC_ACID -> NeroTechFluids.SULFURIC_ACID.get();
            case POLYMER_RESIN -> NeroTechFluids.POLYMER_RESIN.get();
        };
    }

    public Fluid flowing() {
        return switch (this) {
            case CRYO_COOLANT -> NeroTechFluids.FLOWING_CRYO_COOLANT.get();
            case SULFURIC_ACID -> NeroTechFluids.FLOWING_SULFURIC_ACID.get();
            case POLYMER_RESIN -> NeroTechFluids.FLOWING_POLYMER_RESIN.get();
        };
    }

    public LiquidBlock block() {
        return switch (this) {
            case CRYO_COOLANT -> ModBlocks.CRYO_COOLANT.get();
            case SULFURIC_ACID -> ModBlocks.SULFURIC_ACID.get();
            case POLYMER_RESIN -> ModBlocks.POLYMER_RESIN.get();
        };
    }

    public Item bucket() {
        return switch (this) {
            case CRYO_COOLANT -> ModItems.CRYO_COOLANT_BUCKET.get();
            case SULFURIC_ACID -> ModItems.SULFURIC_ACID_BUCKET.get();
            case POLYMER_RESIN -> ModItems.POLYMER_RESIN_BUCKET.get();
        };
    }

    /** Whether {@code fluid} is this liquid's source or flowing form. */
    public boolean is(Fluid fluid) {
        return fluid == source() || fluid == flowing();
    }
}
