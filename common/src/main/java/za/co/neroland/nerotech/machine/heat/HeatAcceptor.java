package za.co.neroland.nerotech.machine.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * Internal alias of the public {@link za.co.neroland.nerotech.api.HeatAcceptor} seam, kept so code that
 * already implements it keeps compiling. Heat Pipes and Heat Exchangers read the public interface, so new
 * code (and add-ons) should implement that one directly.
 *
 * @deprecated implement {@link za.co.neroland.nerotech.api.HeatAcceptor}
 */
@Deprecated
public interface HeatAcceptor extends za.co.neroland.nerotech.api.HeatAcceptor {

    /** For a block with no block entity: the acceptor at a position. */
    @Deprecated
    interface Provider extends za.co.neroland.nerotech.api.HeatAcceptor.Provider {

        @Override
        @Nullable
        HeatAcceptor heatAcceptor(Level level, BlockPos pos, BlockState state);
    }
}
