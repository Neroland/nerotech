/**
 * NeroTech's public add-on API — the seam optional companion mods build on without reaching into
 * NeroTech's internals.
 *
 * <p><b>Stability:</b> public, stable within a 0.x minor; NeroPower depends on it. Types in this
 * package keep their signatures across patch and beta releases of the same minor
 * ({@code 0.4.x}); a breaking change bumps the minor and is called out in {@code CHANGELOG.md}.
 * Everything <i>outside</i> this package — {@code machine}, {@code registry}, {@code heat},
 * {@code pollution}, ... — is internal and may change without notice, even where it is
 * {@code public} for cross-package reasons.
 *
 * <p>What is here:
 * <ul>
 *   <li>{@link za.co.neroland.nerotech.api.PowerMachine} — the read/steer surface every
 *       {@link za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity} implements (energy, heat,
 *       preset, status, owner). Add-ons subclass the base and get it for free, or consume other
 *       machines through it.</li>
 *   <li>{@link za.co.neroland.nerotech.api.MachineTypeRegistry} — declare which block-entity
 *       types get NeroTech's energy/item/gas/fluid capability wiring on every loader.</li>
 *   <li>{@link za.co.neroland.nerotech.api.MachineFailureEvents} — publish machine failure
 *       stages (warning, unstable, failure, breach) on Core's threshold-event bus.</li>
 *   <li>{@link za.co.neroland.nerotech.api.PlanetApi} — the per-dimension solar/wind multipliers
 *       and the ambient heat model, so add-on generators scale exactly like NeroTech's own.</li>
 *   <li>{@link za.co.neroland.nerotech.api.Atmosphere} — the air an Atmospheric Condenser can draw at a
 *       place, as {@link za.co.neroland.nerotech.api.PlanetApi} reports it (no Nerospace class needed).</li>
 *   <li>{@link za.co.neroland.nerotech.api.WirelessCharging} — draw charge from Induction Chargers in
 *       reach for an entity's own energy, and register item-energy adapters for players' held or worn
 *       items. Adapters run first; anything they do not accept is filled through Neroland Core's
 *       item-energy lookup. Takes a level, a position and an amount only.</li>
 *   <li>{@link za.co.neroland.nerotech.api.MachineSettings} — read {@code machineMaxTransfer} and
 *       {@code renderAnimationsEnabled} without the internal config class.</li>
 *   <li>{@link za.co.neroland.nerotech.api.Configurator} — read a Configurator stack: copy/paste mode
 *       and its pending link target.</li>
 *   <li>{@link za.co.neroland.nerotech.api.Pollution} — emit pollution into the region model, with
 *       owner attribution only where the server and the player allow it.</li>
 *   <li>{@link za.co.neroland.nerotech.api.PrivacyPrefs} — whether a player opted out of NeroTech
 *       recording data about them.</li>
 *   <li>{@link za.co.neroland.nerotech.api.HeatAcceptor} — trade heat with Heat Pipes and Heat
 *       Exchangers from any block entity, with no NeroTech base class.</li>
 * </ul>
 *
 * <p>Outside this package, four machine types are also <b>add-on stable</b> because add-ons subclass
 * or read them directly: {@code machine.NeroTechMachineBlockEntity}, {@code machine.NeroTechMachineBlock},
 * {@code machine.MachineStatus} and {@code machine.MachineEnergy}. They follow the same rule as this
 * package.
 *
 * <p>Privacy (POPIA/GDPR): nothing in this package carries player data across mod boundaries —
 * the only player-shaped value, {@link za.co.neroland.nerotech.api.PowerMachine#owner()}, is the
 * placing player's UUID that NeroTech already stores for pollution attribution, and failure events
 * are keyed by machine id + place only.
 */
package za.co.neroland.nerotech.api;
