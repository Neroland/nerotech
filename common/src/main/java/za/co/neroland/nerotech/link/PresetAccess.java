package za.co.neroland.nerotech.link;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.link.LinkActionResult;

import za.co.neroland.nerotech.machine.MachinePreset;

/**
 * The pure rules of a remote {@code set_machine_preset}. Nothing
 * here touches the world, so every rule is unit-testable.
 *
 * <ul>
 *   <li><b>One refusal.</b> Once the body is well formed, every failure (unknown dimension, chunk not
 *       loaded, no machine, someone else's machine, unknown or expired handle, out of reach, not
 *       permitted to interact there) is the same answer, {@link #UNAVAILABLE}, so no failure tells a
 *       requester where chunks are loaded or where a machine stands.</li>
 *   <li><b>Owner only.</b> Either body changes only a machine whose recorded owner is the requester.
 *       An ownerless machine (the default: owners are recorded only under the opt-in settings) cannot
 *       be changed from the app at all, because no claim or protection mod sees an app action and
 *       OVERDRIVE can push a reactor into meltdown. {@code nearby} still lists ownerless machines,
 *       read-only.</li>
 *   <li><b>In-person reach.</b> The handle form also needs the requester within {@link #ACTION_REACH}
 *       blocks of the machine and the interaction check a right-click passes. {@code nearby} reads keep
 *       their 128-block reach.</li>
 *   <li><b>Typed bodies.</b> A wrong JSON type is a {@code VALIDATION} answer, never an exception.</li>
 * </ul>
 */
final class PresetAccess {

    /** Blocks from the requester to the machine's centre for a handle-form preset change. */
    static final int ACTION_REACH = 16;

    /** The one message every refusal after validation carries. */
    static final String UNAVAILABLE = "unavailable";

    /** Why a change was refused. Server-side only: never sent, never logged. */
    enum Refusal {
        UNKNOWN_DIMENSION, NOT_LOADED, NO_MACHINE, NOT_OWNER, UNKNOWN_HANDLE, WRONG_DIMENSION,
        OUT_OF_REACH, NOT_PERMITTED
    }

    private PresetAccess() {
    }

    /** The uniform refusal: the same error code and message whatever {@code why} is. */
    static LinkActionResult refuse(Refusal why) {
        Objects.requireNonNull(why, "why");
        return LinkActionResult.error(LinkActionResult.Error.NOT_OWNER, UNAVAILABLE);
    }

    /** Whether the requester at {@code (px, py, pz)} is within {@link #ACTION_REACH} of the block's centre. */
    static boolean inActionReach(double px, double py, double pz, int bx, int by, int bz) {
        double dx = bx + 0.5D - px;
        double dy = by + 0.5D - py;
        double dz = bz + 0.5D - pz;
        return dx * dx + dy * dy + dz * dz <= (double) ACTION_REACH * ACTION_REACH;
    }

    /** Only a machine whose recorded owner is the requester; an ownerless machine is refused. */
    static boolean ownerAllows(@Nullable UUID owner, UUID requester) {
        return owner != null && requester != null && owner.equals(requester);
    }

    /** The {@code ownedByYou} flag of a {@code nearby} entry: the same test, so the app never offers a refused change. */
    static boolean ownedBy(@Nullable UUID owner, UUID requester) {
        return ownerAllows(owner, requester);
    }

    /** The preset named by a JSON string, or empty for anything else (null, number, array, unknown name). */
    static Optional<MachinePreset> preset(@Nullable JsonElement el) {
        Optional<String> name = string(el);
        if (name.isEmpty()) {
            return Optional.empty();
        }
        for (MachinePreset p : MachinePreset.values()) {
            if (p.name().equals(name.get())) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /** A JSON string, or empty. */
    static Optional<String> string(@Nullable JsonElement el) {
        return el instanceof JsonPrimitive p && p.isString() ? Optional.of(p.getAsString()) : Optional.empty();
    }

    /** A JSON boolean, or empty (the string {@code "false"} is not a boolean). */
    static Optional<Boolean> bool(@Nullable JsonObject params, String key) {
        JsonElement el = params == null ? null : params.get(key);
        return el instanceof JsonPrimitive p && p.isBoolean() ? Optional.of(p.getAsBoolean()) : Optional.empty();
    }

    /** A JSON number that is a whole {@code int}, or empty. */
    static OptionalInt integer(@Nullable JsonElement el) {
        if (!(el instanceof JsonPrimitive p) || !p.isNumber()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(p.getAsBigDecimal().intValueExact());
        } catch (ArithmeticException | NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
