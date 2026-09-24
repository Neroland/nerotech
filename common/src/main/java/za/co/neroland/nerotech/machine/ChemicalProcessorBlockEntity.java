package za.co.neroland.nerotech.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import java.util.List;

import za.co.neroland.nerolandcore.fluid.GatedFluidView;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.fluid.NeroTechFluids;
import za.co.neroland.nerotech.gas.MachineGasTank;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.menu.ChemicalProcessorMenu;
import za.co.neroland.nerotech.recipe.MachineRecipe;
import za.co.neroland.nerotech.registry.ModBlockEntities;
import za.co.neroland.nerotech.registry.ModRecipeTypes;

/**
 * Chemical Processor — the oxygen consumer of the Stage C gas chain. It "washes" raw ores with
 * oxygen: same one-in/one-out shape as every other processing machine
 * ({@code nerotech:chemical_processing} recipes, datapack-driven), but each completed operation also
 * costs {@code chemicalProcessorGasPerOp} mB of oxygen on top of the NE. The reward is yield —
 * raw ore washes to <b>3</b> dust where the Ore Processor gives 2, so the gas chain pays for itself
 * in throughput rather than in free energy.
 *
 * <p>Oxygen — the shared {@code nerospace:oxygen}, so a Nerospace Oxygen Generator feeds it too —
 * arrives through Core's gas capability (pushed by an adjacent Electrolyzer, from a Core Gas Tank,
 * or out of a pipe on that capability); the tank refuses every other gas, so it can never jam on
 * hydrogen, and legacy {@code nerotech:oxygen} is accepted as the same gas. The GAS
 * side-config channel gates which faces accept it — and its auto-input, on by default, makes the
 * processor <i>pull</i> from an adjacent gas source rather than waiting to be fed.
 */
public class ChemicalProcessorBlockEntity extends AbstractProcessingBlockEntity {

    private final MachineGasTank oxygen = MachineGasTank.of(NeroTechGases.OXYGEN,
            NeroTechConfig.machineGasCapacity(), this::setChanged);

    public ChemicalProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_PROCESSOR.get(), pos, state);
        sideConfig().withGas(() -> this.oxygen);
    }

    @Override
    protected void configureSideChannels(SideConfig.Builder builder) {
        // STORAGE (I/O on every face) rather than ALL_INPUT: the tank was extractable on every face
        // before it had a channel at all, and a world full of setups that pull reagent back out must
        // keep working. Auto-input makes it pull its own oxygen rather than waiting to be fed.
        builder.channel(Channel.GAS)
                .preset(Channel.GAS, SidePreset.STORAGE)
                .autoInput(Channel.GAS, true);
    }

    @Override
    protected RecipeType<MachineRecipe> recipeType() {
        return ModRecipeTypes.CHEMICAL_PROCESSING.get();
    }

    @Override
    public List<GatedFluidView> standardFluidViews(@Nullable Direction side) {
        return List.of(gatedView(NeroTechFluids.asFluid(this.oxygen), Channel.GAS, side));
    }

    @Nullable
    @Override
    public NeroGasStorage gasStorage(@Nullable Direction side) {
        SideConfigComponent config = sideConfig();
        return config == null ? this.oxygen : config.gasView(side);
    }

    @Override
    protected int extraDataCount() {
        return 1;
    }

    @Override
    protected int extraData(int index) {
        return index == 0 ? permille(this.oxygen.getAmount(), this.oxygen.getCapacity()) : 0;
    }

    /**
     * Gate the shared processing loop on having the reagent: with a dry oxygen tank the machine
     * reports STARVED and does not even draw power, so an unfed processor is not a silent NE sink.
     */
    @Override
    protected void tickMachine(Level level, BlockPos pos, BlockState state) {
        if (this.oxygen.getAmount() < NeroTechConfig.chemicalProcessorGasPerOp()) {
            reportStatus(MachineStatus.STARVED);
            setActive(false);
            if (this.maxProgress != 0 || this.progress != 0) {
                this.progress = 0;
                this.maxProgress = 0;
                setChanged();
            }
            return;
        }
        super.tickMachine(level, pos, state);
    }

    /** One completed wash also consumes the reagent. */
    @Override
    protected void craft(ItemStack result) {
        super.craft(result);
        this.oxygen.drain(NeroTechConfig.chemicalProcessorGasPerOp(), false);
    }

    // --- persistence ---------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.oxygen.save(output, "Oxygen");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.oxygen.resize(NeroTechConfig.machineGasCapacity());
        this.oxygen.load(input, "Oxygen");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.nerotech.chemical_processor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ChemicalProcessorMenu(containerId, playerInventory, this, this.data);
    }
}
