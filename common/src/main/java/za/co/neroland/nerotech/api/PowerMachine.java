package za.co.neroland.nerotech.api;

import java.util.Optional;
import java.util.UUID;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;

import za.co.neroland.nerotech.machine.MachinePreset;
import za.co.neroland.nerotech.machine.MachineStatus;

/**
 * The stable read/steer surface of a NeroTech machine, implemented by
 * {@link za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity} and therefore by every
 * add-on machine that subclasses it (NeroPower's reactors and turbines). Consumers — a grid
 * controller, a coolant loop, a monitoring block from another mod — talk to a machine through
 * this rather than through the base class, so the base can keep evolving underneath.
 *
 * <p>All methods are <b>server-side</b> unless noted: heat, presets and status live on the
 * server and reach clients only through the machine's own sync discipline.
 *
 * <p>Privacy (POPIA/GDPR): {@link #owner()} is the placing player's UUID, captured only when
 * per-player pollution attribution is enabled and erased through Core's shared data-erasure hook.
 * Add-ons must treat it as personal data: never log it, never send it to a client, never encode it
 * into an event scope.
 */
public interface PowerMachine {

    /** The machine's NE buffer (Core's shared energy surface). */
    NeroEnergyStorage getEnergy();

    /** Current heat (0..{@link #heatCapacity()}). */
    int heat();

    /** The heat ceiling every machine shares ({@code heatCapacity} config). */
    int heatCapacity();

    /**
     * Add heat, clamped to capacity and scaled by the active {@link MachinePreset} (Eco halves it,
     * Overdrive doubles it, floor 1). Server side; a no-op for {@code amount <= 0}.
     */
    void addHeat(int amount);

    /**
     * Remove up to {@code amount} heat, never below {@code floor} — the coolant-loop extraction
     * a Coolant Pump performs on its neighbours. The heat is deleted, not moved.
     *
     * @return heat actually removed
     */
    int extractHeat(int amount, int floor);

    /**
     * The ambient heat level at this machine's position (dimension + biome; see
     * {@link PlanetApi#ambientAt}), cached and refreshed on an interval. Returns the configured
     * default before the machine is placed in a level.
     */
    int ambient();

    /** The active overclock preset (server-authoritative). */
    MachinePreset preset();

    /** Apply a preset; a real change marks the machine dirty and syncs watching clients. */
    void setPreset(MachinePreset preset);

    /**
     * Whether a Grid Controller may drop this machine to {@link MachinePreset#ECO} during a
     * brownout. Consumers say yes; generators and buffers say no.
     */
    boolean shedable();

    /**
     * The placing player's UUID, or empty when unknown (attribution was off at placement, a
     * pre-attribution machine, or an erased owner). Personal data — see the class note.
     */
    Optional<UUID> owner();

    /**
     * Name what currently limits this machine for the analytics window (call from the machine's
     * own tick); on any tick without a report the RUNNING/IDLE default applies.
     */
    void reportStatus(MachineStatus status);

    /** True once heat reaches the throttle threshold — processing machines stall until cooled. */
    boolean overheated();
}
