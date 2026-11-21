package za.co.neroland.nerotech.machine.metallurgy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Plate Press ({@code nerotech:pressing}): one input (an item, or polymer resin in the fluid tank) and a
 * die in the catalyst slot, held and never used up. The die selects the shape: every pressing recipe
 * names its die, so the same ingot makes plates under a plate die and rods under a rod die, and an empty
 * die slot matches nothing. The slot only takes {@code nerotech:press_dies}. The fitted die's item id is
 * client-visible ({@link #die()}, machine contract C3) for the renderer.
 */
public class PlatePressBlockEntity extends ProcessingMachineBlockEntity {

    /** One input, a die slot, one output, a 4,000 mB fluid input for resin. */
    public static final ProcessingSpec SPEC = new ProcessingSpec(1, true, 1, 4_000L, 0L, 0L, 0L);
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC)
            .catalystHint(() -> new ItemStack(MetallurgyContent.PLATE_DIE.get()), "nerotech.gui.slot.die")
            // A 16x24 resin gauge ending at y 52, clear of the upgrade row at y 54 (screens/plate_press.md).
            .fluidIn(62, 28, 24)
            .build();

    /** Last die id pushed to clients (compare-and-record for {@link #renderSyncDirty()}). */
    private String syncedDie = "";

    public PlatePressBlockEntity(BlockPos pos, BlockState state) {
        super(MetallurgyContent.PLATE_PRESS_BE.get(), pos, state, SPEC);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.PRESSING.get();
    }

    @Override
    protected boolean isValidCatalyst(ItemStack stack) {
        return stack.is(MetallurgyContent.PRESS_DIES);
    }

    /** Name the die: an empty die slot says "needs a die", a fitted die that shapes nothing says to swap it. */
    @Override
    protected int refineStarvedDetail(net.minecraft.world.level.Level level, int detailCode) {
        if (detailCode == DETAIL_NEEDS_CATALYST) {
            return DETAIL_NEEDS_DIE;
        }
        if (detailCode == DETAIL_NO_RECIPE && !this.items.get(SPEC.catalystSlot()).isEmpty()) {
            return DETAIL_WRONG_DIE;
        }
        return detailCode;
    }

    /** The fitted die's item id ({@code ""} when empty). Server: live slot; client: the synced slot. */
    public String die() {
        ItemStack stack = this.items.get(SPEC.catalystSlot());
        return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    @Override
    protected boolean renderSyncDirty() {
        String die = die();
        if (die.equals(this.syncedDie)) {
            return false;
        }
        this.syncedDie = die;
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.plate_press");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ProcessingMenu(MetallurgyContent.PLATE_PRESS_MENU, containerId, playerInventory, this,
                this.data, LAYOUT);
    }
}
