package za.co.neroland.nerotech.machine.chemistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Centrifuge ({@code nerotech:centrifuging}): dusts, slurries (fluid in) and gases into by-products
 * with chance outputs, and isotope separation in cascades ({@link CentrifugeCascade}). Side-by-side
 * centrifuges facing the same way link into a chain of up to four; each offers recipes its chain
 * position as {@code cascade_<n>} and pushes its output gas straight into the next one's input tank
 * (adjacent exchange, no pipe). The chain is resolved as cached links: dropped on any neighbour change
 * (passed down the chain) and re-checked every 100 ticks, at most four block-entity lookups — never a
 * per-tick scan. Machine state only; no player data.
 */
public class CentrifugeBlockEntity extends ProcessingMachineBlockEntity {

    /** 1 item in, 4 outputs, a slurry tank, a gas tank in and a gas tank out. */
    public static final ProcessingSpec SPEC = new ProcessingSpec(1, false, 4, 4_000L, 0L, 4_000L, 4_000L);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC)
            .input(0, 26, 30)
            .fluidIn(44, 28, 40)
            .gasIn(62, 28, 40)
            .arrow(82, 34, 22, 16)
            .output(0, 106, 30).output(1, 124, 30).output(2, 106, 48).output(3, 124, 48)
            .gasOut(143, 28, 40)
            .build();

    /** Ticks between safety re-checks of the chain (neighbour changes already drop it at once). */
    private static final int RESOLVE_INTERVAL = 100;

    /** Chain position, 0 until resolved. Transient: rebuilt from the world, never saved. */
    private int cascadePosition;
    private long resolvedUntil;
    @Nullable
    private CentrifugeBlockEntity downstream;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ChemistryContent.CENTRIFUGE_BE.get(), pos, state, SPEC);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.CENTRIFUGING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.centrifuge");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ProcessingMenu(ChemistryContent.CENTRIFUGE_MENU, id, inv, this, this.data, LAYOUT);
    }

    /** Recipes see this centrifuge's chain position ({@code cascade_1} for a lone one). */
    @Override
    protected String atmosphere() {
        return CentrifugeCascade.stageKey(cascadePosition());
    }

    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        super.tickMachine(level, pos, state);
        pushDownstream(level);
    }

    /** Inside a cascade, "no recipe" is usually the wrong place in the line: say which stage this is. */
    @Override
    protected int refineStarvedDetail(Level level, int detailCode) {
        if (detailCode == DETAIL_NO_RECIPE && (cascadePosition() > 1 || this.downstream != null)) {
            return DETAIL_WRONG_STAGE;
        }
        return detailCode;
    }

    @Override
    protected int starvedDetailParam(int detailCode) {
        return detailCode == DETAIL_WRONG_STAGE ? cascadePosition() : 0;
    }

    /** The cascade passes gas between linked centrifuges itself; the adjacent exchange leaves them alone. */
    @Override
    protected boolean skipsPullFrom(@Nullable net.minecraft.world.level.block.entity.BlockEntity neighbour) {
        return neighbour instanceof CentrifugeBlockEntity;
    }

    /** This centrifuge's place in its chain (1 = head, up to {@link CentrifugeCascade#MAX}). */
    public int cascadePosition() {
        Level level = this.level;
        if (level == null) {
            return Math.max(1, this.cascadePosition);
        }
        if (this.cascadePosition == 0 || level.getGameTime() >= this.resolvedUntil) {
            resolveCascade(level);
        }
        return this.cascadePosition;
    }

    /** Hand our output gas to the next stage, a bounded amount per tick. */
    private void pushDownstream(Level level) {
        if (this.gasOut == null || this.gasOut.getAmount() <= 0) {
            return;
        }
        cascadePosition();
        CentrifugeBlockEntity next = this.downstream;
        if (next == null || next.isRemoved() || next.gasIn == null) {
            return;
        }
        long offer = Math.min(this.gasOut.getAmount(), ChemistrySettings.centrifugeCascadePushPerTick());
        long accepted = next.gasIn.fill(this.gasOut.getGas(), offer, true);
        if (accepted > 0) {
            next.gasIn.fill(this.gasOut.getGas(), accepted, false);
            this.gasOut.drain(accepted, false);
        }
    }

    private void resolveCascade(Level level) {
        Direction facing = facingOf(getBlockState());
        this.resolvedUntil = level.getGameTime() + RESOLVE_INTERVAL;
        this.downstream = null;
        if (facing == null) {
            this.cascadePosition = 1;
            return;
        }
        Direction upstream = facing.getCounterClockWise();
        int behind = CentrifugeCascade.upstreamCount(
                i -> linkedAt(level, this.worldPosition.relative(upstream, i), facing) != null);
        this.cascadePosition = CentrifugeCascade.position(behind);
        if (CentrifugeCascade.pushesDownstream(this.cascadePosition)) {
            this.downstream = linkedAt(level, this.worldPosition.relative(facing.getClockWise()), facing);
        }
    }

    @Nullable
    private static CentrifugeBlockEntity linkedAt(Level level, BlockPos pos, Direction facing) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        return level.getBlockEntity(pos) instanceof CentrifugeBlockEntity other && !other.isRemoved()
                && facing == facingOf(other.getBlockState()) ? other : null;
    }

    @Nullable
    private static Direction facingOf(BlockState state) {
        return state.hasProperty(NeroTechMachineBlock.FACING) ? state.getValue(NeroTechMachineBlock.FACING) : null;
    }

    /** A neighbour changed: drop our chain links and those of the centrifuges downstream of us. */
    @Override
    public void invalidateThermalLinks() {
        super.invalidateThermalLinks();
        invalidateCascade(0);
    }

    private void invalidateCascade(int depth) {
        this.cascadePosition = 0;
        this.downstream = null;
        Level level = this.level;
        Direction facing = facingOf(getBlockState());
        if (level == null || facing == null || depth + 1 >= CentrifugeCascade.MAX) {
            return;
        }
        CentrifugeBlockEntity next = linkedAt(level, this.worldPosition.relative(facing.getClockWise()), facing);
        if (next != null) {
            next.invalidateCascade(depth + 1);
        }
    }

    @Override
    public void setRemoved() {
        this.downstream = null;
        super.setRemoved();
    }
}
