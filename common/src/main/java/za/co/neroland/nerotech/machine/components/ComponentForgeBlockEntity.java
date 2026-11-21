package za.co.neroland.nerotech.machine.components;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingSpec;
import za.co.neroland.nerotech.menu.ProcessingLayout;
import za.co.neroland.nerotech.menu.ProcessingMenu;
import za.co.neroland.nerotech.recipe.ProcessRecipe;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Component Forge: up to six item inputs plus one fluid make one component ({@code nerotech:component_forging}).
 * The home of the component ladder (servo, capacitor, heat sink and what later tiers add). It runs one recipe
 * at a time from its own slots: no patterns, no network awareness and no reaching into neighbours, which is
 * the Auto Crafter's and NeroLogistics' job. Everything else (side config, upgrades, presets, heat,
 * pollution, analytics, alarms) is the shared processing base.
 */
public class ComponentForgeBlockEntity extends ProcessingMachineBlockEntity {

    public static final ProcessingSpec SPEC = ComponentsSpecs.COMPONENT_FORGE;

    /** 176x186: a 3x2 input grid, the arrow, one output and the fluid gauge right of it. */
    public static final ProcessingLayout LAYOUT = ProcessingLayout.builder(SPEC)
            .height(ProcessingLayout.TALL_HEIGHT)
            .output(0, 110, 39)
            .fluidIn(134, 28, 44)
            .build();

    /** Render sync (C1 extra): the result item last pushed to clients, and the client's copy. */
    @Nullable
    private Item syncedResult;
    private ItemStack clientResult = ItemStack.EMPTY;

    public ComponentForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ComponentsContent.COMPONENT_FORGE_BE.get(), pos, state, SPEC);
    }

    /** The first result of the matched recipe (server), or null with no match. */
    @Nullable
    private Item liveResult() {
        ProcessRecipe recipe = matchedRecipe();
        if (recipe == null || recipe.results().isEmpty()) {
            return null;
        }
        return recipe.results().get(0).item();
    }

    /**
     * The part being assembled, for the hologram over the pedestal (machine contract C1 extra: the
     * item only, synced when the matched recipe's result changes). Empty with no job.
     */
    public ItemStack currentResult() {
        if (this.level != null && this.level.isClientSide()) {
            return this.clientResult;
        }
        Item item = liveResult();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    protected boolean renderSyncDirty() {
        Item now = liveResult();
        if (now != this.syncedResult) {
            this.syncedResult = now;
            return true;
        }
        return false;
    }

    @Override
    protected void writeRenderExtras(CompoundTag tag) {
        Item item = liveResult();
        if (item != null) {
            tag.putString("Result", BuiltInRegistries.ITEM.getKey(item).toString());
        }
    }

    @Override
    protected void readRenderExtras(ValueInput input) {
        Identifier id = Identifier.tryParse(input.getStringOr("Result", ""));
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
        this.clientResult = item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    protected RecipeType<ProcessRecipe> recipeType() {
        return ModRecipeTypes.COMPONENT_FORGING.get();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.component_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ProcessingMenu(ComponentsContent.COMPONENT_FORGE_MENU, id, inv, this, this.data, LAYOUT);
    }
}
