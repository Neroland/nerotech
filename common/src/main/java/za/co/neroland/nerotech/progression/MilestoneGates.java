package za.co.neroland.nerotech.progression;

import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.progression.ProgressionGates;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.pollution.PollutionAttributionPrefs;

/**
 * NeroTech's personal milestone records: player-scope Neroland Core gates
 * ({@code data/nerotech/neroland_gates/*.json}) a machine opens for its owner the first time
 * something notable happens — the Fusion Reactor forms, the Collider lands a product, the
 * Remediator removes pollution, a Grid Controller's grid reaches a configured size. They are
 * <b>records</b>, not locks: nothing in NeroTech checks them for access, and a quest pack reads them
 * with a read-only {@code gate_open} objective.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the only player-keyed input is the machine's stored owner UUID,
 * which exists solely when per-player pollution attribution or operating charges captured it at
 * placement (see {@code PRIVACY.md}). An unowned machine records nothing, and an owner who has opted
 * out of attribution ({@link PollutionAttributionPrefs}, the NeroLink own-data control) is skipped
 * too. The UUID goes only into {@link ProgressionGates#open(MinecraftServer, UUID, Identifier)}; it
 * is never logged or broadcast. The gate rows themselves live in Core's progression store and are
 * erased through Core's shared erasure hook.
 */
public final class MilestoneGates {

    /** The Fusion Reactor's multiblock shell formed for the first time. */
    public static final Identifier FUSION_ONLINE = gate("fusion_online");
    /** The Particle Collider consumed a particle pair and landed its product. */
    public static final Identifier FIRST_COLLISION = gate("first_collision");
    /** A Remediator actually removed pollution from its region. */
    public static final Identifier REMEDIATED = gate("remediated");
    /** A Grid Controller's summed machine capacity reached {@code gridScaleMilestoneCapacity}. */
    public static final Identifier GRID_SCALE = gate("grid_scale");
    /** A Blast Foundry finished its first batch. Credited to the online operator in the moment. */
    public static final Identifier FIRST_FOUNDRY_POUR = gate("first_foundry_pour");
    /** A Laser Etcher etched a Quantum Chip. Credited to the online operator in the moment. */
    public static final Identifier FIRST_QUANTUM_CHIP = gate("first_quantum_chip");
    /** A Deep Core Drill pulled ore from a deposit. Credited to the online operator in the moment. */
    public static final Identifier FIRST_DEPOSIT_TAPPED = gate("first_deposit_tapped");
    /** A Fusion Reactor started a gas-fed burn. Credited to the online operator in the moment. */
    public static final Identifier GAS_FED_IGNITION = gate("gas_fed_ignition");
    /**
     * A machine was brought back from failure stage 3 or worse ({@code MachineFailureEvents}): credited to the
     * online operator in the moment who restored it. Hidden trophy hook.
     */
    public static final Identifier MELTDOWN_AVERTED = gate("meltdown_averted");
    /** A Singularity Vault stored its first items. Credited to the online operator in the moment. */
    public static final Identifier VAULT_OPERATED = gate("vault_operated");

    private static boolean initialised;

    private MilestoneGates() {
    }

    private static Identifier gate(String path) {
        return Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, path);
    }

    /** Subscribe the event-driven milestones (from {@code NeroTechCommon.init()}). Idempotent. */
    public static synchronized void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        za.co.neroland.nerotech.machine.fusion.FusionEvents.onGasIgnition(event ->
                credit(event.level(), event.operator().orElse(null), GAS_FED_IGNITION));
    }

    /**
     * Open {@code gate} for the player operating a machine in the moment ({@code lastOperator}, transient,
     * never saved), only while that player is online. Nothing is stored about the player here and no
     * owner is needed: the gate row lives in Core's progression store (erased through Core's hook). An
     * operator who has opted out of attribution is skipped. Idempotent.
     *
     * @return {@code true} if the gate was newly opened
     */
    public static boolean credit(ServerLevel level, @Nullable UUID operator, Identifier gate) {
        if (operator == null) {
            return false;
        }
        MinecraftServer server = level.getServer();
        if (server == null || server.getPlayerList().getPlayer(operator) == null) {
            return false;
        }
        if (PollutionAttributionPrefs.get(server).isOptedOut(operator)
                || ProgressionGates.isOpen(server, operator, gate)) {
            return false;
        }
        return ProgressionGates.open(server, operator, gate);
    }

    /**
     * Record {@code gate} for the machine owner {@code ownerId}, online or offline. Silently does
     * nothing when the owner is unknown or has opted out of attribution. Idempotent: an already-open
     * gate stays open and fires no event.
     *
     * @return {@code true} if the gate was newly opened
     */
    public static boolean record(ServerLevel level, @Nullable UUID ownerId, Identifier gate) {
        if (ownerId == null) {
            return false;
        }
        MinecraftServer server = level.getServer();
        if (server == null || PollutionAttributionPrefs.get(server).isOptedOut(ownerId)) {
            return false;
        }
        if (ProgressionGates.isOpen(server, ownerId, gate)) {
            return false;
        }
        return ProgressionGates.open(server, ownerId, gate);
    }
}
