package za.co.neroland.nerotech.api;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;

/**
 * The cross-loader list of machine block-entity types that get NeroTech's capability wiring —
 * one list per {@link Surface}. NeroTech seeds it with its own machines once, from
 * {@code NeroTechCommon.init()}; an add-on registers its machines the same way and they are wired
 * on every loader exactly like NeroTech's own, with no loader code in the add-on:
 *
 * <pre>{@code
 * MachineTypeRegistry.registerEnergy(MY_REACTOR::get);
 * MachineTypeRegistry.registerItem(MY_REACTOR::get);
 * }</pre>
 *
 * <p>Registrations are {@link Supplier}s, not resolved types: the lists are read (and the
 * suppliers called) only when a loader wires its capabilities, which is after every mod's
 * registries have been populated. An add-on therefore registers from its mod constructor /
 * initializer <b>before</b> NeroTech's loader entry points run their wiring:
 * <ul>
 *   <li><b>NeoForge</b> — capabilities are wired in {@code RegisterCapabilitiesEvent}, which fires
 *       after every mod constructor; a registration from an add-on's mod constructor is in time.</li>
 *   <li><b>Fabric</b> — NeroTech wires through {@link #onRegistered} listeners, so a registration
 *       from an add-on initializer that happens to run <i>after</i> NeroTech's is still wired the
 *       moment it lands.</li>
 *   <li><b>Forge</b> — capabilities attach by {@code instanceof NeroTechMachineBlockEntity} at
 *       block-entity creation, so every subclass is covered without a registration; registering
 *       anyway keeps the add-on loader-agnostic.</li>
 * </ul>
 *
 * <p>Thread model: main / mod-loading thread only. The backing lists are copy-on-write so a
 * listener replay never sees a half-added entry, but this is not a general-purpose concurrent
 * registry.
 */
public final class MachineTypeRegistry {

    /** The four capability surfaces a machine can be wired on. */
    public enum Surface {
        /** Core's shared {@code nerolandcore:energy} capability / lookup. */
        ENERGY,
        /** The loader's standard item-handler surface (pipes, hoppers, NeroLogistics). */
        ITEM,
        /** Core's shared {@code nerolandcore:gas} capability / lookup. */
        GAS,
        /** Core's shared {@code nerolandcore:fluid} capability / lookup (plus the loader's own). */
        FLUID
    }

    private static final Map<Surface, List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>>> TYPES =
            new EnumMap<>(Surface.class);
    private static final Map<Surface, List<Consumer<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>>>> LISTENERS =
            new EnumMap<>(Surface.class);

    static {
        for (Surface surface : Surface.values()) {
            TYPES.put(surface, new CopyOnWriteArrayList<>());
            LISTENERS.put(surface, new CopyOnWriteArrayList<>());
        }
    }

    private MachineTypeRegistry() {
    }

    // --- registration -------------------------------------------------------

    /** Wire {@code type} on Core's shared energy capability on every loader. */
    public static void registerEnergy(Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type) {
        register(Surface.ENERGY, type);
    }

    /** Expose {@code type}'s sided inventory on every loader's standard item surface. */
    public static void registerItem(Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type) {
        register(Surface.ITEM, type);
    }

    /** Wire {@code type}'s {@code gasStorage(side)} on Core's shared gas capability on every loader. */
    public static void registerGas(Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type) {
        register(Surface.GAS, type);
    }

    /** Wire {@code type}'s {@code fluidStorage(side)} on Core's shared fluid capability on every loader. */
    public static void registerFluid(Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type) {
        register(Surface.FLUID, type);
    }

    /**
     * Register {@code type} on {@code surface}. Every {@link #onRegistered} listener for that
     * surface is notified synchronously, after the type is visible in the snapshot.
     */
    public static void register(Surface surface, Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type) {
        if (surface == null || type == null) {
            throw new IllegalArgumentException("Machine type registration needs a surface and a supplier");
        }
        TYPES.get(surface).add(type);
        for (Consumer<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> listener : LISTENERS.get(surface)) {
            listener.accept(type);
        }
    }

    // --- snapshots ------------------------------------------------------------

    /** Unmodifiable snapshot of the energy-surface types, in registration order. */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> energyTypes() {
        return types(Surface.ENERGY);
    }

    /** Unmodifiable snapshot of the item-surface types, in registration order. */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> itemTypes() {
        return types(Surface.ITEM);
    }

    /** Unmodifiable snapshot of the gas-surface types, in registration order. */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> gasTypes() {
        return types(Surface.GAS);
    }

    /** Unmodifiable snapshot of the fluid-surface types, in registration order. */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> fluidTypes() {
        return types(Surface.FLUID);
    }

    /** Unmodifiable snapshot of {@code surface}'s types, in registration order. */
    public static List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> types(Surface surface) {
        return List.copyOf(TYPES.get(surface));
    }

    // --- listeners ------------------------------------------------------------

    /**
     * Subscribe to {@code surface}: the listener receives every already-registered type
     * immediately (replay, in registration order) and every later one as it is added. This is
     * how a loader whose wiring is a plain method call (Fabric) picks up an add-on that registers
     * after NeroTech's own initializer ran.
     */
    public static void onRegistered(Surface surface,
            Consumer<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>> listener) {
        if (surface == null || listener == null) {
            throw new IllegalArgumentException("Machine type listener needs a surface and a consumer");
        }
        LISTENERS.get(surface).add(listener);
        for (Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>> type : TYPES.get(surface)) {
            listener.accept(type);
        }
    }
}
