package za.co.neroland.nerotech.item.tools;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21.6 {
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
//?}

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.registry.ModSounds;
import za.co.neroland.nerotech.machine.FusionReactorBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.SingularityVaultBlockEntity;
import za.co.neroland.nerotech.machine.extraction.DeepCoreDrillBlockEntity;
import za.co.neroland.nerotech.machine.foundry.BlastFoundryBlockEntity;
import za.co.neroland.nerotech.machine.multiblock.MultiblockController;
import za.co.neroland.nerotech.machine.tank.ProcessTankBlockEntity;

/**
 * Arc Wrench — use on a NeroTech machine to rotate it a quarter turn; sneak-use to pick it up with its
 * configuration and contents kept ({@link CarriedMachine} in the {@code nerotech:carried_machine}
 * component), then use on a block face to put it back. The rules ({@link WrenchRules}) make duplication
 * impossible: the machine is emptied and removed in the same server tick the wrench is filled, a wrench
 * carries one machine at a time, and machines that already carry their contents on their own dropped item
 * (Process Tank, Singularity Vault), formed multiblock controllers and blocks inside a multiblock's box are
 * refused. Side configuration is relative to the machine's facing, so it survives both rotation and a move;
 * the Configurator's {@code ConfiguratorState} is untouched (the wrench copies the block entity's own save,
 * which already includes Core's side config).
 *
 * <p><b>Protection.</b> Using the wrench on a block is a right-click, which the loaders' interaction events
 * (NeoForge and Forge {@code RightClickBlock}, Fabric {@code UseBlockCallback}) let protection mods cancel
 * before this runs; on top of that the wrench checks {@code mayInteract}, {@code mayUseItemAt} and
 * {@code blockActionRestricted}. It charges at an Induction Charger. No player data is stored.
 */
public class ArcWrenchItem extends ChargedToolItem {

    public ArcWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public int capacity() {
        return NeroTechConfig.arcWrenchCapacity();
    }

    public static Optional<CarriedMachine> carried(ItemStack stack) {
        return Optional.ofNullable(stack.get(ToolsContent.CARRIED_MACHINE.get()));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player) || !(context.getLevel() instanceof ServerLevel level)) {
            return context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        Optional<CarriedMachine> carrying = carried(stack);
        if (carrying.isPresent()) {
            placeBack(player, level, context, stack, carrying.get());
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        if (player.isSecondaryUseActive()) {
            pickUp(player, level, pos, context.getClickedFace(), stack);
        } else {
            rotate(player, level, pos, context.getClickedFace(), stack);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean permitted(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face,
            ItemStack stack) {
        return level.mayInteract(player, pos) && player.mayUseItemAt(pos, face, stack)
                && !player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer());
    }

    static boolean isNeroTech(BlockState state) {
        return "nerotech".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }

    static boolean formedController(BlockEntity be) {
        return (be instanceof FusionReactorBlockEntity fusion && fusion.renderFormed())
                || (be instanceof BlastFoundryBlockEntity foundry && foundry.formedSize() > 0)
                || (be instanceof DeepCoreDrillBlockEntity drill && drill.formed());
    }

    static WrenchRules.Target target(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face,
            ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        boolean machine = be instanceof NeroTechMachineBlockEntity && isNeroTech(state);
        return new WrenchRules.Target(machine,
                be instanceof ProcessTankBlockEntity || be instanceof SingularityVaultBlockEntity,
                be != null && formedController(be), MultiblockController.insideTrackedBox(level, pos),
                permitted(player, level, pos, face, stack));
    }

    private void rotate(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, ItemStack stack) {
        WrenchRules.Refusal refusal = WrenchRules.check(target(player, level, pos, face, stack), false);
        BlockState state = level.getBlockState(pos);
        if (refusal == WrenchRules.Refusal.CARRIES_OWN_CONTENTS) {
            refusal = WrenchRules.Refusal.OK; // rotating moves nothing; only lifting is refused for these
        }
        if (refusal != WrenchRules.Refusal.OK || !state.hasProperty(NeroTechMachineBlock.FACING)) {
            refuse(player, refusal == WrenchRules.Refusal.OK ? WrenchRules.Refusal.NOT_A_MACHINE : refusal);
            return;
        }
        if (!spend(player, stack, NeroTechConfig.arcWrenchEnergyPerUse(), NeroTechConfig.arcWrenchCooldownTicks())) {
            return;
        }
        Direction facing = state.getValue(NeroTechMachineBlock.FACING);
        level.setBlock(pos, state.setValue(NeroTechMachineBlock.FACING, facing.getClockWise()), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine) {
            machine.invalidateThermalLinks();
            machine.setChanged();
        }
        level.playSound(null, pos, ModSounds.TOOL_ARC_WRENCH.get(), SoundSource.BLOCKS, 0.5F, 1.6F);
    }

    private void pickUp(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, ItemStack stack) {
        WrenchRules.Refusal refusal = WrenchRules.check(target(player, level, pos, face, stack), false);
        if (refusal == WrenchRules.Refusal.OK
                && !za.co.neroland.nerotech.platform.Services.PLATFORM.mayBreak(player, level, pos)) {
            // Lifting removes the block: a claim that allows use but not breaking refuses it here.
            refusal = WrenchRules.Refusal.NOT_PERMITTED;
        }
        if (refusal != WrenchRules.Refusal.OK) {
            refuse(player, refusal);
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine)) {
            return;
        }
        if (!spend(player, stack, NeroTechConfig.arcWrenchEnergyPerUse(), NeroTechConfig.arcWrenchCooldownTicks())) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        HolderLookup.Provider registries = level.registryAccess();
        Optional<CompoundTag> lifted = WrenchRules.pickUp(new WrenchRules.Site<CompoundTag>() {
            @Override
            public CompoundTag snapshot() {
                CompoundTag tag = machine.saveCustomOnly(registries);
                // POPIA/GDPR: the owner never rides on the item.
                tag.remove("OwnerMost");
                tag.remove("OwnerLeast");
                return tag;
            }

            @Override
            public void clear() {
                machine.clearContent();
            }

            @Override
            public void restore(CompoundTag snapshot) {
                load(machine, snapshot, registries);
            }

            @Override
            public boolean remove() {
                return level.removeBlock(pos, false);
            }
        });
        // The carried copy drops the owner and the gallery-exhibit flag (the in-world restore path above
        // keeps the exhibit intact if the lift fails).
        lifted.map(tag -> WrenchRules.stripCarried(tag, CompoundTag::remove)).ifPresent(tag -> {
            stack.set(ToolsContent.CARRIED_MACHINE.get(), new CarriedMachine(state, tag));
            level.playSound(null, pos, ModSounds.TOOL_ARC_WRENCH.get(), SoundSource.BLOCKS, 0.6F, 0.8F);
            tell(player, Component.translatable("message.nerotech.arc_wrench.picked_up", state.getBlock().getName()));
        });
    }

    /**
     * Put the carried machine down through the <b>normal block-placement path</b>: a stand-in of the
     * machine's own block item is used on the clicked face ({@code ItemStack.useOn}), so vanilla's
     * {@code mayBuild} / adventure-mode checks, {@code BlockItem.place}'s {@code canPlace} and survival
     * checks, and each loader's place hook (NeoForge and Forge fire {@code EntityPlaceEvent} from
     * {@code useOn} and restore the old block on cancel; Fabric claim mods hook {@code BlockItem.place})
     * all apply exactly as for a hand-placed block. A denied or vetoed place keeps the machine on the
     * wrench. The stand-in never reaches the player's inventory.
     */
    private void placeBack(ServerPlayer player, ServerLevel level, UseOnContext context, ItemStack stack,
            CarriedMachine carried) {
        Block block = carried.state().getBlock();
        if (!(block.asItem() instanceof BlockItem blockItem) || blockItem.getBlock() != block) {
            tell(player, Component.translatable("message.nerotech.arc_wrench.cannot_place"));
            return;
        }
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), context.isInside());
        // Two, so survival's consume(1) never empties the stand-in (no item-destroyed event for the hand).
        ItemStack standIn = new ItemStack(blockItem, 2);
        BlockPlaceContext place = new BlockPlaceContext(level, player, context.getHand(), standIn, hit);
        BlockPos pos = place.getClickedPos();
        if (!place.canPlace()) {
            tell(player, Component.translatable("message.nerotech.arc_wrench.cannot_place"));
            return;
        }
        if (charge(stack).energy() < NeroTechConfig.arcWrenchEnergyPerUse()) {
            tell(player, Component.translatable("message.nerotech.tool.no_energy", NeroTechConfig.arcWrenchEnergyPerUse()));
            return;
        }
        HolderLookup.Provider registries = level.registryAccess();
        boolean done = WrenchRules.placeBack(carried.data(), new WrenchRules.Placement<CompoundTag>() {
            @Override
            public boolean permitted() {
                return player.getAbilities().mayBuild && ArcWrenchItem.permitted(player, level, pos,
                        context.getClickedFace(), stack);
            }

            @Override
            public boolean place() {
                // setPlacedBy runs inside BlockItem.place, as for any hand-placed block.
                return standIn.useOn(place).consumesAction() && level.getBlockState(pos).is(block);
            }

            @Override
            public void load(CompoundTag snapshot) {
                if (level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine) {
                    ArcWrenchItem.load(machine, snapshot, registries);
                    machine.invalidateThermalLinks();
                    machine.setChanged();
                }
            }
        });
        if (!done) {
            tell(player, Component.translatable("message.nerotech.arc_wrench.cannot_place"));
        }
        if (done) {
            stack.remove(ToolsContent.CARRIED_MACHINE.get());
            drain(stack, NeroTechConfig.arcWrenchEnergyPerUse());
            level.playSound(null, pos, ModSounds.TOOL_ARC_WRENCH.get(), SoundSource.BLOCKS, 0.6F, 1.2F);
        }
    }

    /** Load a carried save into a placed machine (the loader-line split of {@code loadCustomOnly}). */
    static void load(NeroTechMachineBlockEntity machine, CompoundTag tag, HolderLookup.Provider registries) {
        //? if >=1.21.6 {
        machine.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
        //?} else {
        /*machine.loadCustomOnly(tag, registries);
        *///?}
    }

    private static void refuse(ServerPlayer player, WrenchRules.Refusal refusal) {
        tell(player, Component.translatable("message.nerotech.arc_wrench.refused."
                + refusal.name().toLowerCase(java.util.Locale.ROOT)));
    }
}
