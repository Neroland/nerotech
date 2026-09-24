package za.co.neroland.nerotech.gas;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * NeroTech's concrete gases (Stage C, fluid &amp; gas machines). Neroland Core deliberately ships
 * <b>no</b> gases — its {@link za.co.neroland.nerolandcore.gas.NeroGases} layer identifies a gas
 * generically by {@link Identifier} and leaves the actual gases to content mods — so NeroTech
 * declares its own two here and stores/moves them through Core's
 * {@link za.co.neroland.nerolandcore.gas.NeroGasStorage} contract and
 * {@link za.co.neroland.nerolandcore.platform.GasLookup} seam. Any Nero (or third-party) mod that
 * speaks that contract interoperates with these tanks for free.
 *
 * <p>Amounts are millibuckets everywhere, exactly like Core's tanks. A "unit" — the balance
 * currency the Electrolyzer produces and the Gas Turbine burns — is {@value #UNIT_MB} mB, so the
 * config numbers stay small and readable while the storage stays on Core's mB scale.
 *
 * <p>Display names resolve through {@code NeroGases.label(...)} as {@code gas.<namespace>.<path>}
 * ({@code gas.nerotech.hydrogen}, {@code gas.nerospace.oxygen}).
 *
 * <p><b>One oxygen.</b> Oxygen is shared with Nerospace's life support: it is
 * {@code nerospace:oxygen}, so an Oxygen Generator's output feeds the Chemical Processor and the
 * Electrolyzer's oxygen can fill a base. It is only an {@link Identifier} — no Nerospace class is
 * touched, so NeroTech still runs without it. Builds before this used {@code nerotech:oxygen};
 * {@link #canonical} maps that legacy id onto the shared one wherever a gas enters a NeroTech tank
 * (fills, pulls, NBT load) or crosses the fluid adapters.
 */
public final class NeroTechGases {

    /** Millibuckets in one balance "unit" of gas (config numbers are expressed in units). */
    public static final int UNIT_MB = 100;

    /** Electrolysis product and the Gas Turbine's launch fuel. */
    public static final Identifier HYDROGEN =
            Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "hydrogen");

    /**
     * Electrolysis product and the Chemical Processor's washing reagent — the ecosystem's single
     * oxygen, shared with Nerospace (its Oxygen Generator and life support use the same id).
     */
    public static final Identifier OXYGEN = Identifier.fromNamespaceAndPath("nerospace", "oxygen");

    /**
     * NeroTech's pre-unification oxygen id. Never produced any more; {@link #canonical} maps it to
     * {@link #OXYGEN} so old saves and Core Gas Tanks still holding it keep working.
     */
    public static final Identifier LEGACY_OXYGEN =
            Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "oxygen");

    private NeroTechGases() {
    }

    /**
     * The canonical id for a gas: {@link #LEGACY_OXYGEN} becomes {@link #OXYGEN}; every other id
     * (including {@code null} and Core's empty gas) passes through unchanged.
     */
    public static Identifier canonical(Identifier gas) {
        return LEGACY_OXYGEN.equals(gas) ? OXYGEN : gas;
    }

    /** Convert a balance unit count to millibuckets. */
    public static long units(int units) {
        return (long) units * UNIT_MB;
    }
}
