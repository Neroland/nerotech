package za.co.neroland.nerotech.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.event.ThresholdEvents;
import za.co.neroland.nerolandcore.event.ThresholdEvents.ThresholdCrossing;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Machine failure notifications on Core's {@link ThresholdEvents} bus — one channel for the whole
 * ecosystem, so a mod reacting to "a reactor is about to go" (NeroEvents, a NeroLink alert, an
 * add-on's own safety interlock) listens once and hears NeroTech's Fusion Reactor and NeroPower's
 * reactors alike, without importing either.
 *
 * <p>Listen with {@code ThresholdEvents.onCrossing(c -> { if (c.channel().equals(CHANNEL)) ... })}.
 * The crossing's {@code value} and {@code threshold} both carry the {@linkplain #fire stage}, and
 * {@code rising} says whether the machine entered ({@code true}) or left ({@code false}) it.
 *
 * <p><b>Stages</b> (the {@code stage} int, worst last):
 * <ol>
 *   <li>{@link #STAGE_WARNING} — warning / throttled: the machine refuses to work until it cools.</li>
 *   <li>{@link #STAGE_UNSTABLE} — unstable: still running, but past the point a careful operator
 *       would intervene.</li>
 *   <li>{@link #STAGE_FAILURE} — failure / meltdown: the machine is destroying itself.</li>
 *   <li>{@link #STAGE_BREACH} — containment breach: the structure around it gave way.</li>
 * </ol>
 *
 * <p>Privacy (POPIA/GDPR): the scope names a <b>machine and a place</b> — {@code machineId} +
 * dimension + packed position — never a player. Publishers must not put the owner in it.
 * Server thread only, like every {@code ThresholdEvents} publication.
 */
public final class MachineFailureEvents {

    /** The threshold channel every machine failure is published on. */
    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, "machine_failure");

    /** Stage 1: warning / throttled. */
    public static final int STAGE_WARNING = 1;
    /** Stage 2: unstable. */
    public static final int STAGE_UNSTABLE = 2;
    /** Stage 3: failure / meltdown. */
    public static final int STAGE_FAILURE = 3;
    /** Stage 4: containment breach. */
    public static final int STAGE_BREACH = 4;

    private MachineFailureEvents() {
    }

    /**
     * Publish a failure-stage crossing for one machine.
     *
     * @param machineId   a stable, non-personal machine identifier — the block-entity type id
     *                    ({@code "nerotech:fusion_reactor"}) is the convention
     * @param dimensionId the dimension's id string ({@code level.dimension().identifier().toString()})
     * @param pos         the machine's position
     * @param stage       the failure stage entered or left (1..4, see the class note)
     * @param rising      {@code true} when the machine enters the stage, {@code false} on recovery
     */
    public static void fire(String machineId, String dimensionId, BlockPos pos, int stage, boolean rising) {
        ThresholdEvents.fire(new ThresholdCrossing(CHANNEL, scope(machineId, dimensionId, pos), stage, stage, rising));
    }

    /**
     * The scope key {@link #fire} publishes: {@code <machineId>@<dimensionId>:<pos.asLong()>} —
     * a machine and a place, no player data.
     */
    public static String scope(String machineId, String dimensionId, BlockPos pos) {
        return machineId + "@" + dimensionId + ":" + pos.asLong();
    }
}
