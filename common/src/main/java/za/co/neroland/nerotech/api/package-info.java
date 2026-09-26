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
 * </ul>
 *
 * <p>Privacy (POPIA/GDPR): nothing in this package carries player data across mod boundaries —
 * the only player-shaped value, {@link za.co.neroland.nerotech.api.PowerMachine#owner()}, is the
 * placing player's UUID that NeroTech already stores for pollution attribution, and failure events
 * are keyed by machine id + place only.
 */
package za.co.neroland.nerotech.api;
