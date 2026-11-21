package za.co.neroland.nerotech.item.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.platform.EnergyLookup;

import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.heat.HeatPipeBlock;
import za.co.neroland.nerotech.machine.heat.HeatRunMath;
import za.co.neroland.nerotech.machine.tank.ProcessTankBlockEntity;
import za.co.neroland.nerotech.pollution.PollutionManager;

/**
 * Multimeter — right-click any block to read what it exposes through the existing surfaces: Core energy
 * (any mod's block on Core's energy lookup), a NeroTech machine's heat, a Heat Pipe's heat level, a
 * machine's fluid and gas tank or a Process Tank column, and the pollution band of the region the block
 * is in. The reading goes to that player's chat only; it names no coordinates, no owner and nothing is
 * logged. Each reading costs a little charge and has a short cooldown. Sneak-use charges it from a
 * powered block.
 */
public class MultimeterItem extends ChargedToolItem {

    public MultimeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public int capacity() {
        return NeroTechConfig.multimeterCapacity();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player) || !(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        NeroEnergyStorage energy = EnergyLookup.INSTANCE.find(level, pos, context.getClickedFace());
        if (player.isSecondaryUseActive() && energy != null) {
            chargeFrom(player, stack, energy);
            return InteractionResult.SUCCESS;
        }
        if (!spend(player, stack, NeroTechConfig.multimeterEnergyPerReading(), NeroTechConfig.multimeterCooldownTicks())) {
            return InteractionResult.SUCCESS;
        }
        BlockState state = level.getBlockState(pos);
        player.sendSystemMessage(Component.translatable("message.nerotech.multimeter.header", state.getBlock().getName())
                .withStyle(ChatFormatting.AQUA));
        int lines = 0;
        if (energy != null) {
            line(player, Component.translatable("message.nerotech.multimeter.energy", energy.getAmount(),
                    energy.getCapacity()));
            lines++;
        }
        if (level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine) {
            line(player, Component.translatable("message.nerotech.multimeter.heat", machine.heat(),
                    machine.heatCapacity(), machine.heatThrottleThreshold()));
            lines++;
            if (machine instanceof ProcessTankBlockEntity tank) {
                line(player, Component.translatable("message.nerotech.multimeter.tank",
                        ProcessTankBlockEntity.displayName(tank.columnMedium()), tank.columnAmount(),
                        tank.columnCapacity()));
                lines++;
            } else {
                NeroFluidStorage fluid = machine.fluidStorage(null);
                if (fluid != null) {
                    line(player, Component.translatable("message.nerotech.multimeter.fluid", fluid.getAmount(),
                            fluid.getCapacity()));
                    lines++;
                }
                NeroGasStorage gas = machine.gasStorage(null);
                if (gas != null) {
                    line(player, Component.translatable("message.nerotech.multimeter.gas", gas.getAmount(),
                            gas.getCapacity()));
                    lines++;
                }
            }
        } else if (state.hasProperty(HeatPipeBlock.HEAT)) {
            line(player, Component.translatable("message.nerotech.multimeter.pipe_heat", state.getValue(HeatPipeBlock.HEAT),
                    HeatRunMath.MAX_LEVEL));
            lines++;
        }
        line(player, Component.translatable("message.nerotech.multimeter.pollution",
                Component.translatable("message.nerotech.multimeter.pollution." + PollutionManager.band(
                        PollutionManager.regionPollution(level, pos)))));
        if (lines == 0) {
            line(player, Component.translatable("message.nerotech.multimeter.nothing"));
        }
        return InteractionResult.SUCCESS;
    }

    private static void line(ServerPlayer player, MutableComponent text) {
        player.sendSystemMessage(text.withStyle(ChatFormatting.GRAY));
    }
}
