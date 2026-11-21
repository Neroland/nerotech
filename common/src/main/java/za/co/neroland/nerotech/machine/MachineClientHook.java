package za.co.neroland.nerotech.machine;

import net.minecraft.world.level.block.entity.BlockEntityType;

import org.jetbrains.annotations.Nullable;

/**
 * The seam through which the client drives a machine's in-world sounds and particles from its synced
 * readout (no extra packets). Common code holds no client class: the client installs a {@link Handler}
 * during client setup, and {@link NeroTechMachineBlock#getTicker} hands client-side machines of the types
 * it {@link Handler#handles handles} a ticker that calls it. On a dedicated server nothing is installed
 * and no client ticker exists. Machine-scoped state only; no player data.
 */
public final class MachineClientHook {

    /** Client-side effects for machines (installed once by the client). */
    public interface Handler {

        /** Whether this machine type has any client effect (a client ticker is created only then). */
        boolean handles(BlockEntityType<?> type);

        /** Once per client tick for each loaded machine of a handled type. Integer compares; cheap. */
        void tick(NeroTechMachineBlockEntity machine);
    }

    @Nullable
    private static volatile Handler handler;

    private MachineClientHook() {
    }

    /** Client setup: install the effects handler (idempotent). */
    public static void install(Handler effects) {
        handler = effects;
    }

    /** The installed handler, or null (always null on a dedicated server). */
    @Nullable
    public static Handler handler() {
        return handler;
    }
}
