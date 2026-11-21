package za.co.neroland.nerotech.registry;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerotech.api.MachineTypeRegistry;
import za.co.neroland.nerotech.client.RendererSink;
import za.co.neroland.nerotech.client.ScreenSink;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;

/**
 * One 0.8.0 machine family as the central registries see it: the five static hooks of its
 * {@code machine/<family>/<Family>Content} class, bound by method reference in {@link FamilyRegistry}.
 * The client hooks are only ever invoked from loader client setup.
 */
public record MachineFamily(
        String id,
        Runnable init,
        Function<MachineTypeRegistry.Surface, List<Supplier<BlockEntityType<? extends NeroTechMachineBlockEntity>>>> machineTypes,
        Consumer<ScreenSink> screens,
        Consumer<RendererSink> renderers,
        Supplier<List<ItemLike>> creativeContents) {
}
