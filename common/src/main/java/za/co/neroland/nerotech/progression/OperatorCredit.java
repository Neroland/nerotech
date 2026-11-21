package za.co.neroland.nerotech.progression;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.api.MachineFailureEvents;

/**
 * The pure decisions behind operator-in-the-moment credit (machine contract, Credit): when a machine event
 * credits {@code lastOperator} with a milestone gate. No world access, so they are unit-tested directly;
 * {@link MilestoneGates#credit} then checks the player is online and not opted out, and opens the gate
 * idempotently. Nothing here stores a player: the "already tried" operator is the machine's own transient
 * field, cleared on unload.
 */
public final class OperatorCredit {

    private OperatorCredit() {
    }

    /**
     * Meltdown averted: a machine whose worst failure stage this episode was {@code worstStage} has just been
     * restored (formed and running again, or back below the failure line). Credits only from
     * {@link MachineFailureEvents#STAGE_FAILURE} or worse, and only with a known operator.
     */
    public static boolean meltdownAverted(int worstStage, boolean restored, @Nullable UUID operator) {
        return restored && operator != null && worstStage >= MachineFailureEvents.STAGE_FAILURE;
    }

    /**
     * Vault operated: the vault just moved {@code moved} items into its store. Credits a known operator once
     * per machine lifetime per operator ({@code alreadyTried} is the last operator this vault tried to
     * credit), so a hopper feeding the vault every tick does not re-run the gate lookup.
     */
    public static boolean vaultOperated(long moved, @Nullable UUID operator, @Nullable UUID alreadyTried) {
        return moved > 0 && operator != null && !Objects.equals(operator, alreadyTried);
    }
}
