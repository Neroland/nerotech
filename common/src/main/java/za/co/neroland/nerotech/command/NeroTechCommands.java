package za.co.neroland.nerotech.command;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.Command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerolandcore.energy.EnergyBuffer;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.machine.AbstractProcessingBlockEntity;
import za.co.neroland.nerotech.machine.AutoCrafterBlockEntity;
import za.co.neroland.nerotech.machine.FusionReactorBlockEntity;
import za.co.neroland.nerotech.machine.ItemSorterBlockEntity;
import za.co.neroland.nerotech.machine.NeroGeneratorBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.ScrubberBlockEntity;
import za.co.neroland.nerotech.registry.ModBlocks;
import za.co.neroland.nerotech.registry.ModItems;
import za.co.neroland.nerotech.telemetry.NeroTechTelemetry;

/**
 * Creative-only debug commands (cheats / op level 2), the Nerospace gallery recipe.
 * {@code /nerotech gallery} builds a showcase rotunda around the player: every NeroTech block
 * floating on a display grid (all faces visible), every machine RUNNING the way it is meant to be
 * wired in survival — fuelled, powered (Core Creative Battery neighbours + pre-charged buffers so
 * gauges read non-zero on arrival) and fed — the pollution-control pair parked beside the polluting
 * generator so the Scrubber has real emissions to eat, an Analytics Terminal watching the processing
 * strip, a loaded Tech Guide pedestal near the rotunda centre (hologram running), and both a formed
 * 3×3×3 and a formed 5×5×5 Fusion Reactor shell burning their tier fuels.
 * {@code /nerotech gallery clear} wipes that footprint (blocks + label stands) so a rebuild doesn't
 * stack duplicates.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the command acts at the invoking player's position and records
 * nothing — no positions, names or UUIDs are stored anywhere.
 */
public final class NeroTechCommands {

    private static final int SPACING = 3;     // blocks between display-grid cells
    private static final int FLOAT_ABOVE = 3; // display sits this many blocks above the floor (2 air gap)
    private static final int EXHIBIT_STEP = 5; // blocks between live machine exhibits on a strip

    private NeroTechCommands() {
    }

    /**
     * Cross-loader registration: each loader calls this from its command hook (Forge/NeoForge
     * {@code RegisterCommandsEvent}, Fabric {@code CommandRegistrationCallback}) with the dispatcher.
     */
    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        // Player-only; the executor further restricts to creative. (Commands themselves require the
        // world to have cheats/commands enabled, so this is effectively creative + commands gated.)
        dispatcher.register(
                Commands.literal("nerotech")
                        .requires(src -> src.getPlayer() != null)
                        .then(Commands.literal("gallery")
                                .executes(ctx -> runSafely(ctx.getSource(), "gallery",
                                        () -> buildGallery(ctx.getSource())))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> runSafely(ctx.getSource(), "gallery clear",
                                                () -> clearGallery(ctx.getSource()))))));
    }

    private static int runSafely(CommandSourceStack source, String commandName, CommandBody body) {
        try {
            return body.run();
        } catch (RuntimeException ex) {
            NeroTechTelemetry.captureHandledException(ex, "command", "/nerotech " + commandName);
            NeroTechCommon.LOGGER.error("[NeroTech] /nerotech {} failed", commandName, ex);
            source.sendFailure(Component.translatable("command.nerotech.gallery.failed", commandName));
            return 0;
        }
    }

    @FunctionalInterface
    private interface CommandBody {
        int run();
    }

    private static int buildGallery(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.nerotech.gallery.player_only"));
            return 0;
        }
        if (!player.getAbilities().instabuild) {
            source.sendFailure(Component.translatable("command.nerotech.gallery.creative_only"));
            return 0;
        }
        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition();
        int fy = origin.getY();

        // Exhibit floor: Fusion Casing — the mod's own structural plate (the Nerospace gallery's
        // station-floor recipe, in NeroTech's teal).
        BlockState floor = ModBlocks.FUSION_CASING.get().defaultBlockState();

        // BLOCK GRID (EAST spoke): the cross-loader RegistrationProvider has no entry iteration, so
        // walk the vanilla block registry filtered to this mod's namespace. Blocks float two air
        // blocks above the floor so every face is visible.
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier bid = BuiltInRegistries.BLOCK.getKey(block);
            if (NeroTechCommon.MOD_ID.equals(bid.getNamespace())) {
                blocks.add(block);
            }
        }
        int cols = (int) Math.ceil(Math.sqrt(Math.max(1, blocks.size())));
        int rows = (int) Math.ceil(blocks.size() / (double) cols);
        int gx = origin.getX() + 22;
        int gz = origin.getZ() - 6;
        for (int dx = -1; dx <= cols * SPACING; dx++) {
            for (int dz = -1; dz <= rows * SPACING; dz++) {
                level.setBlockAndUpdate(new BlockPos(gx + dx, fy, gz + dz), floor);
            }
        }
        for (int i = 0; i < blocks.size(); i++) {
            level.setBlockAndUpdate(
                    new BlockPos(gx + (i % cols) * SPACING, fy + FLOAT_ABOVE, gz + (i / cols) * SPACING),
                    blocks.get(i).defaultBlockState());
        }

        // PROCESSING STRIP (SOUTH spoke), all four processors RUNNING: each machine faces NORTH
        // (toward the rotunda centre) with a Core Creative Battery hidden behind it — the endless
        // source pushes into adjacent receivers every tick, exactly the survival hookup minus the
        // cabling — plus a pre-charged buffer so the gauges read non-zero the moment the player looks.
        int px = origin.getX() - 8;
        int pz = origin.getZ() + 24;
        for (int dx = -1; dx <= 3 * EXHIBIT_STEP + 6; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(new BlockPos(px + dx, fy, pz + dz), floor);
            }
        }
        placeLiveProcessor(level, new BlockPos(px, fy + 1, pz), ModBlocks.ORE_PROCESSOR.get(),
                new ItemStack(Items.RAW_IRON, 64), "Ore Processor — raw iron → dust");
        placeLiveProcessor(level, new BlockPos(px + EXHIBIT_STEP, fy + 1, pz), ModBlocks.ADVANCED_ORE_PROCESSOR.get(),
                new ItemStack(Items.RAW_IRON, 64), "Advanced Ore Processor — bonus dust yield");
        placeLiveProcessor(level, new BlockPos(px + EXHIBIT_STEP * 2, fy + 1, pz), ModBlocks.FABRICATOR.get(),
                new ItemStack(Items.IRON_INGOT, 64), "Fabricator — iron → Machine Frames");
        placeLiveProcessor(level, new BlockPos(px + EXHIBIT_STEP * 3, fy + 1, pz), ModBlocks.ADVANCED_FABRICATOR.get(),
                new ItemStack(za.co.neroland.nerolandcore.registry.ModItems.VOID_CRYSTAL.get(), 64),
                "Advanced Fabricator — Void Crystal → Fusion Cells");
        // Analytics Terminal at the strip's east end: a passive reader; the whole strip sits inside
        // its scan radius, so the dashboard has live rows on arrival. No battery — it consumes no NE.
        BlockPos terminalPos = new BlockPos(px + EXHIBIT_STEP * 3 + 4, fy + 1, pz);
        placeMachine(level, terminalPos, ModBlocks.ANALYTICS_TERMINAL.get(), Direction.NORTH);
        spawnLabelStand(level, terminalPos.above(2), Component.literal("Analytics Terminal — watching the strip"));

        // POWER + POLLUTION CONTROL (SOUTH-WEST spoke): the polluting generator and its mitigation
        // pair share one tight cluster ON PURPOSE — regional pollution (64×64 regions) means the
        // Scrubber only spins when there are emissions in reach, and the burning generator supplies
        // them. Generators are NOT pre-charged: a full buffer refuses to ignite (roomToStore), and
        // each Core Battery sink keeps the output flowing so the turbine keeps spinning.
        int wx = origin.getX() - 26;
        int wz = origin.getZ() + 12;
        for (int dx = -1; dx <= 13; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                level.setBlockAndUpdate(new BlockPos(wx + dx, fy, wz + dz), floor);
            }
        }
        BlockPos genPos = new BlockPos(wx, fy + 1, wz);
        placeMachine(level, genPos, ModBlocks.NERO_GENERATOR.get(), Direction.NORTH);
        level.setBlockAndUpdate(genPos.south(),
                za.co.neroland.nerolandcore.registry.ModBlocks.BATTERY.get().defaultBlockState());
        if (level.getBlockEntity(genPos) instanceof NeroGeneratorBlockEntity generator) {
            generator.setItem(NeroGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 64));
        }
        spawnLabelStand(level, genPos.above(2), Component.literal("Nero Generator — burning coal into a Battery"));
        BlockPos solarPos = new BlockPos(wx + 4, fy + 1, wz);
        placeMachine(level, solarPos, ModBlocks.SOLAR_ARRAY.get(), Direction.NORTH);
        level.setBlockAndUpdate(solarPos.south(),
                za.co.neroland.nerolandcore.registry.ModBlocks.BATTERY.get().defaultBlockState());
        spawnLabelStand(level, solarPos.above(2), Component.literal("Solar Array — clean daytime power"));
        BlockPos scrubPos = new BlockPos(wx + 8, fy + 1, wz);
        placeMachine(level, scrubPos, ModBlocks.SCRUBBER.get(), Direction.NORTH);
        level.setBlockAndUpdate(scrubPos.south(),
                za.co.neroland.nerolandcore.registry.ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
        chargeMachine(level, scrubPos);
        if (level.getBlockEntity(scrubPos) instanceof ScrubberBlockEntity scrubber) {
            scrubber.setItem(ScrubberBlockEntity.FILTER_SLOT, new ItemStack(ModItems.FILTER_CARTRIDGE.get(), 64));
        }
        spawnLabelStand(level, scrubPos.above(2),
                Component.literal("Scrubber — eating the generator's emissions"));
        BlockPos remedPos = new BlockPos(wx + 12, fy + 1, wz);
        placeMachine(level, remedPos, ModBlocks.REMEDIATOR.get(), Direction.NORTH);
        level.setBlockAndUpdate(remedPos.south(),
                za.co.neroland.nerolandcore.registry.ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
        chargeMachine(level, remedPos);
        spawnLabelStand(level, remedPos.above(2), Component.literal("Remediator — restoring scarred ground"));

        // AUTOMATION STRIP (WEST spoke): the Auto Crafter mid-craft (2×2 planks → crafting tables,
        // hologram + press-stamp pulses) and the Item Sorter routing a matched stream into its
        // front buffer (port-cap pulses). Both face EAST toward the centre, batteries hidden behind.
        int ax = origin.getX() - 26;
        int az = origin.getZ() - 4;
        for (int dx = -2; dx <= 1; dx++) {
            for (int dz = -1; dz <= 6; dz++) {
                level.setBlockAndUpdate(new BlockPos(ax + dx, fy, az + dz), floor);
            }
        }
        BlockPos crafterPos = new BlockPos(ax, fy + 1, az);
        placeMachine(level, crafterPos, ModBlocks.AUTO_CRAFTER.get(), Direction.EAST);
        level.setBlockAndUpdate(crafterPos.west(),
                za.co.neroland.nerolandcore.registry.ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
        chargeMachine(level, crafterPos);
        if (level.getBlockEntity(crafterPos) instanceof AutoCrafterBlockEntity crafter) {
            // A 2×2 planks square in the 3×3 grid (slots 0,1 / 3,4) — the vanilla crafting-table recipe.
            crafter.setItem(0, new ItemStack(Items.OAK_PLANKS, 64));
            crafter.setItem(1, new ItemStack(Items.OAK_PLANKS, 64));
            crafter.setItem(3, new ItemStack(Items.OAK_PLANKS, 64));
            crafter.setItem(4, new ItemStack(Items.OAK_PLANKS, 64));
        }
        spawnLabelStand(level, crafterPos.above(2),
                Component.literal("Auto Crafter — 2×2 planks → crafting tables"));
        BlockPos sorterPos = new BlockPos(ax, fy + 1, az + 4);
        placeMachine(level, sorterPos, ModBlocks.ITEM_SORTER.get(), Direction.EAST);
        chargeMachine(level, sorterPos);
        if (level.getBlockEntity(sorterPos) instanceof ItemSorterBlockEntity sorter) {
            // FRONT filter (slot 1) matches the input stream, so batches pulse into the front buffer.
            sorter.setItem(ItemSorterBlockEntity.INPUT_SLOT, new ItemStack(Items.COBBLESTONE, 64));
            sorter.setItem(ItemSorterBlockEntity.FILTER_START, new ItemStack(Items.COBBLESTONE));
        }
        spawnLabelStand(level, sorterPos.above(2),
                Component.literal("Item Sorter — routing cobblestone out the front"));

        // TECH GUIDE PEDESTAL near the rotunda centre (the Nerospace gallery's loaded Star Guide
        // recipe): datapad installed, so the next-step hologram spins for whoever walks up.
        int tgx = origin.getX() + 5;
        int tgz = origin.getZ() - 5;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(new BlockPos(tgx + dx, fy, tgz + dz), floor);
            }
        }
        BlockPos guidePos = new BlockPos(tgx, fy + 1, tgz);
        level.setBlockAndUpdate(guidePos, ModBlocks.TECH_GUIDE.get().defaultBlockState());
        if (level.getBlockEntity(guidePos)
                instanceof za.co.neroland.nerotech.guide.TechGuideBlockEntity guide) {
            guide.installDatapad(new ItemStack(ModItems.TECH_GUIDE_DATAPAD.get()));
        }
        spawnLabelStand(level, guidePos.above(2),
                Component.literal("Tech Guide — your next step, holographically"));

        // FUSION EXHIBITS: a formed shell per size gets its own spoke (the Nerospace room-per-exhibit
        // recipe). Both controllers sit at the centre of the SOUTH wall facing SOUTH — toward the
        // rotunda centre — so the viewer meets the console with the torus glowing behind the glass.
        buildFusionExhibit(level, floor, origin.getX() - 1, origin.getZ() - 27, fy, 3,
                new ItemStack(ModItems.FUSION_CELL.get(), 16),
                Component.literal("Fusion Reactor — formed 3×3×3 shell, Fusion Cells"));
        buildFusionExhibit(level, floor, origin.getX() + 15, origin.getZ() - 25, fy, 5,
                new ItemStack(ModItems.PLASMA_CELL.get(), 16),
                Component.literal("Fusion Reactor — formed 5×5×5 shell, Plasma Cells"));

        int blockCount = blocks.size();
        source.sendSuccess(() -> Component.translatable("command.nerotech.gallery.built", blockCount), false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Wipe the gallery built at the player's feet so a rebuild doesn't stack duplicates. Clears the
     * whole rotunda footprint to air from the floor layer ({@code origin.y}) up, leaving the natural
     * ground at {@code origin.y - 1} intact, and removes every non-player entity in the box (label
     * stands). Run it standing where you ran {@code gallery}.
     */
    private static int clearGallery(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.nerotech.gallery.player_only"));
            return 0;
        }
        if (!player.getAbilities().instabuild) {
            source.sendFailure(Component.translatable("command.nerotech.gallery.creative_only"));
            return 0;
        }
        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition();
        int oy = origin.getY();

        // Footprint of buildGallery (spokes reach ~27 out on N/NE/E/S/SW/W bearings) plus margin.
        // The floor sits at oy, so clearing oy..topY restores the original flat ground at oy - 1.
        int minX = origin.getX() - 32;
        int maxX = origin.getX() + 42;
        int minZ = origin.getZ() - 32;
        int maxZ = origin.getZ() + 30;
        int topY = oy + 16;

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int cleared = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = oy; y <= topY; y++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).isAir()) {
                        level.setBlock(cursor, air, 2); // flag 2 = notify clients, skip neighbour cascade
                        cleared++;
                    }
                }
            }
        }

        // Remove the spawned label stands — everything in the box but players.
        AABB box = new AABB(minX, oy - 1, minZ, maxX + 1, topY + 4, maxZ + 1);
        int removed = 0;
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) {
            entity.discard();
            removed++;
        }

        int clearedBlocks = cleared;
        int removedEntities = removed;
        source.sendSuccess(() -> Component.translatable("command.nerotech.gallery.cleared",
                clearedBlocks, removedEntities), false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * One live processing exhibit: the machine facing NORTH (toward the rotunda centre), a Core
     * Creative Battery hidden behind it (pushes into adjacent receivers every tick), a pre-charged
     * buffer, a full input stack, and a floating label.
     */
    private static void placeLiveProcessor(ServerLevel level, BlockPos pos, Block block,
            ItemStack input, String label) {
        placeMachine(level, pos, block, Direction.NORTH);
        level.setBlockAndUpdate(pos.south(),
                za.co.neroland.nerolandcore.registry.ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
        chargeMachine(level, pos);
        if (level.getBlockEntity(pos) instanceof AbstractProcessingBlockEntity processor) {
            processor.setItem(AbstractProcessingBlockEntity.INPUT_SLOT, input);
        }
        spawnLabelStand(level, pos.above(2), Component.literal(label));
    }

    /** Place a machine block with its {@code FACING} toward the given direction. */
    private static void placeMachine(ServerLevel level, BlockPos pos, Block block, Direction facing) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(NeroTechMachineBlock.FACING)) {
            state = state.setValue(NeroTechMachineBlock.FACING, facing);
        }
        level.setBlockAndUpdate(pos, state);
    }

    /**
     * Top up a consumer's energy buffer so its gauge reads full on arrival ({@code setRaw} clamps to
     * capacity). Never used on generators — a full generator refuses to ignite ({@code roomToStore}).
     */
    private static void chargeMachine(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine
                && machine.getEnergy() instanceof EnergyBuffer buffer) {
            buffer.setRaw(Integer.MAX_VALUE);
            machine.setChanged();
        }
    }

    /**
     * A formed Fusion Reactor exhibit: a hollow {@code size³} shell of Fusion Casing with a
     * containment-glass viewport band around the equator, the controller at the centre of the SOUTH
     * wall facing SOUTH (so {@link za.co.neroland.nerotech.machine.FusionStructure} validates with the
     * interior centre {@code half} blocks north of it), strictly-air interior, and tier fuel loaded.
     * The controller's own unformed-cadence revalidation (≤ 1 s) forms it and lights the torus.
     */
    private static void buildFusionExhibit(ServerLevel level, BlockState floor, int bx, int bz, int fy,
            int size, ItemStack fuel, Component label) {
        int half = (size - 1) / 2;
        for (int dx = -2; dx <= size + 1; dx++) {
            for (int dz = -2; dz <= size + 1; dz++) {
                level.setBlockAndUpdate(new BlockPos(bx + dx, fy, bz + dz), floor);
            }
        }
        BlockState casing = ModBlocks.FUSION_CASING.get().defaultBlockState();
        BlockState glass = ModBlocks.FUSION_CONTAINMENT_GLASS.get().defaultBlockState();
        int y0 = fy + 1;
        int equatorY = y0 + half;
        BlockPos controllerPos = new BlockPos(bx + half, equatorY, bz + size - 1); // south-wall centre
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                for (int dz = 0; dz < size; dz++) {
                    BlockPos pos = new BlockPos(bx + dx, y0 + dy, bz + dz);
                    boolean surface = dx == 0 || dx == size - 1
                            || dy == 0 || dy == size - 1
                            || dz == 0 || dz == size - 1;
                    if (pos.equals(controllerPos)) {
                        continue; // placed last, after the shell, so its first validation sees it whole
                    }
                    if (!surface) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); // strictly-air interior
                    } else {
                        // Equator band = glass viewport (the plasma torus reads through it); rest casing.
                        level.setBlockAndUpdate(pos, pos.getY() == equatorY ? glass : casing);
                    }
                }
            }
        }
        level.setBlockAndUpdate(controllerPos, ModBlocks.FUSION_REACTOR.get().defaultBlockState()
                .setValue(NeroTechMachineBlock.FACING, Direction.SOUTH));
        // Fuel only — never pre-charge a generator: a full buffer refuses to ignite (roomToStore).
        if (level.getBlockEntity(controllerPos) instanceof FusionReactorBlockEntity reactor) {
            reactor.setItem(FusionReactorBlockEntity.FUEL_SLOT, fuel);
        }
        spawnLabelStand(level, new BlockPos(bx + half, fy + size + 2, bz + half), label);
    }

    /** Small floating label for gallery display clusters (the Nerospace label-stand recipe). */
    private static void spawnLabelStand(ServerLevel level, BlockPos pos, Component name) {
        ArmorStand stand = new ArmorStand(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        // Marker = zero-size bounding box, and ArmorStand#isPickable() is false while it is set.
        // Without it an invisible stand overlapping a machine hijacks pick-block (middle click
        // returned an Armor Stand item instead of the machine). ArmorStand#setMarker is PRIVATE in
        // 26.x, so the flag goes in the only public way: replay {Marker:1b} through Entity#load.
        CompoundTag markerTag = new CompoundTag();
        markerTag.putBoolean("Marker", true);
        stand.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), markerTag));
        stand.refreshDimensions(); // adopt the zero-size marker hitbox immediately
        // load() re-reads every field from that tag, so re-apply the label state afterwards.
        stand.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5); // load() zeroed Pos (absent)
        stand.setCustomName(name);
        stand.setCustomNameVisible(true);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        level.addFreshEntity(stand);
    }
}
