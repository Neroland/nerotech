package za.co.neroland.nerotech.machine.multiblock;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Block tags the shared multiblock shapes match. Referencing the tags (not the blocks) keeps the shapes
 * independent of when their blocks are registered, and lets a datapack add look-alike blocks.
 */
public final class MultiblockTags {

    /** Blast Foundry shell blocks ({@code foundry_brick}). */
    public static final TagKey<Block> FOUNDRY_SHELL = blockTag("foundry_shell");
    /** Blocks allowed in the Blast Foundry shell as ports ({@code foundry_heat_port}). */
    public static final TagKey<Block> FOUNDRY_PORTS = blockTag("foundry_ports");
    /** Non-air blocks allowed inside a Blast Foundry (the molten-batch display). */
    public static final TagKey<Block> FOUNDRY_INTERIOR = blockTag("foundry_interior");
    /** Deep Core Drill derrick column blocks. */
    public static final TagKey<Block> DRILL_DERRICK = blockTag("drill_derrick");

    private MultiblockTags() {
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, path));
    }
}
