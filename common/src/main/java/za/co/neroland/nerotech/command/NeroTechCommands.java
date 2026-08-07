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
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerolandcore.energy.EnergyBuffer;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.machine.AbstractProcessingBlockEntity;
import za.co.neroland.nerotech.machine.AcceleratorGuideBlock;
import za.co.neroland.nerotech.machine.AutoCrafterBlockEntity;
import za.co.neroland.nerotech.machine.BioGeneratorBlockEntity;
import za.co.neroland.nerotech.machine.ColliderCoreBlockEntity;
import za.co.neroland.nerotech.machine.ElectrolyzerBlockEntity;
import za.co.neroland.nerotech.machine.FusionReactorBlockEntity;
import za.co.neroland.nerotech.machine.ItemSorterBlockEntity;
import za.co.neroland.nerotech.machine.NeroGeneratorBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.RoboticArmBlockEntity;
import za.co.neroland.nerotech.machine.ScrubberBlockEntity;
import za.co.neroland.nerotech.machine.SingularityVaultBlockEntity;
import za.co.neroland.nerotech.machine.WirelessNodeBlockEntity;
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
 *
 * <p>The tech expansion (shipped in 0.1.0-beta.1) adds five more spokes, each wired the way survival
 * would wire it:
 * a <b>closed octagonal Particle Accelerator</b> with a circulating, colliding beam (plus its own
 * coolant tower), the <b>gas chain</b> (Electrolyzer → Gas Turbine / Chemical Processor), a
 * <b>coolant loop</b> draining the 5×5×5 reactor, a <b>power park</b> (Wind Turbine on a mast,
 * Geothermal Generator over a contained lava basin, Bio Generator, Battery Bank, Grid Controller and
 * a linked Wireless Node pair) and an <b>automation lane</b> (a Conveyor Belt line with a corner and
 * live item entities, a Robotic Arm shuttling between two chests, and a stocked Singularity Vault).
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
        BlockPos bigReactor = buildFusionExhibit(level, floor, origin.getX() + 15, origin.getZ() - 25, fy, 5,
                new ItemStack(ModItems.PLASMA_CELL.get(), 16),
                Component.literal("Fusion Reactor — formed 5×5×5 shell, Plasma Cells"));

        // --- Tech expansion spokes (shipped in 0.1.0-beta.1) -------------------------------------
        buildAcceleratorExhibit(level, floor, origin.getX() - 20, origin.getZ() - 12, fy);
        buildGasChainExhibit(level, floor, origin.getX() + 3, origin.getZ() - 14, fy);
        buildCoolantExhibit(level, bigReactor);
        buildPowerPark(level, floor, origin.getX() + 24, origin.getZ() + 24, fy);
        buildAutomationLane(level, floor, origin.getX() - 28, origin.getZ() + 22, fy);

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

        // Footprint of buildGallery plus margin. The far corners are: the accelerator ring
        // (x -22, z -20), fusion 3³ (z -29), the block grid (x +40, z +11), the power park
        // (x +38, z +32) and the automation lane (x -30, z +26); the Wind Turbine mast is the
        // tallest thing at oy + 16 with its label above it.
        // The floor sits at oy, so clearing oy..topY restores the original flat ground at oy - 1.
        int minX = origin.getX() - 34;
        int maxX = origin.getX() + 46;
        int minZ = origin.getZ() - 34;
        int maxZ = origin.getZ() + 36;
        int topY = oy + 22;

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int cleared = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = oy; y <= topY; y++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).isAir()) {
                        // flag 2 = notify clients, skip the neighbour cascade — which is also what
                        // keeps the Geothermal exhibit's lava basin from re-flowing as its casing
                        // rim comes away: no neighbour update, no fluid tick scheduled.
                        level.setBlock(cursor, air, 2);
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
     *
     * @return the controller's position, so a caller can hang further exhibits off the shell
     */
    private static BlockPos buildFusionExhibit(ServerLevel level, BlockState floor, int bx, int bz, int fy,
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
        return controllerPos;
    }

    // --- Tech-expansion exhibits (shipped in 0.1.0-beta.1) ----------------------------------------

    /**
     * The demo accelerator ring: the eight Accelerator Guide Coil offsets from the controller, in
     * trace order, each set to {@link AcceleratorGuideBlock.Bend#RIGHT}.
     *
     * <p><b>Why this shape.</b> {@code AcceleratorMath.Heading} is a clockwise ring, so a
     * {@code RIGHT} guide turns the beam 45° clockwise; eight of them turn it a full 360°. Starting
     * from the controller's NORTH facing the headings run
     * N, NE, E, SE, S, SW, W, NW, N — nine straight runs, so the controller sits <i>inside</i> the
     * north-going side (offsets {@code (0,-3)} out and {@code (0,+3)} back). Displacement closes for
     * any octagon with axis sides {@code a}, diagonal sides {@code d} and
     * {@code n(out) + n(back) = a}; this ring uses {@code a = 6}, {@code d = 3}, {@code 3 + 3 = 6}.
     *
     * <p><b>Why this size</b> ({@code AcceleratorMath} with stock config —
     * allowance 4, gap/speed 0.12, bend base 20, boost 2/guide, energy scale 500‰):
     * <ul>
     *   <li>Longest run 6 blocks ⇒ injection speed {@code (6 - 4) / 0.12 ≈ 16.7} (over the launch
     *       floor of 10), and 6 ≤ {@code acceleratorMaxGap} 16 so the trace never opens.</li>
     *   <li>Shortest run <i>before a bend</i> is 3 blocks ⇒ bend ceiling {@code 20 × 3 = 60}.</li>
     *   <li>Eight powered guides per lap ⇒ +16 speed per lap. Speed at the controller reads
     *       ≈32.7 / 48.7 / 64.7 on laps 1 / 2 / 3, i.e. {@code E = 0.5·v²·0.5} ≈ 267 / 592 / 1045 J,
     *       and the bend check on the 3-block run sees ≈16.7 / 32.7 / 48.7 — always under 60.</li>
     * </ul>
     * So the ring reaches ~1045 J on lap 3 without ever crashing: it clears
     * {@code collider_transmute_iron_dust} (Copper Dust + Copper Dust → Iron Dust, min 800 J) and
     * stays under the next tier (Gold Dust, 1500 J). Both particle slots are stocked with Copper
     * Dust, so the beam launches, laps, collides and relaunches on its own, for ever.
     */
    private static final int[][] RING_GUIDES = {
            {0, -3}, {3, -6}, {9, -6}, {12, -3}, {12, 3}, {9, 6}, {3, 6}, {0, 3},
    };

    /**
     * ACCELERATOR RING (NORTH-WEST spoke): a closed octagon of Accelerator Guide Coils around an
     * Accelerator Controller, sized by {@link #RING_GUIDES} so the beam circulates, collides and
     * relaunches unattended. A Coolant Pump tower sits on the controller because the accelerator runs
     * three times a processor's heat and would otherwise throttle itself between runs.
     */
    private static void buildAcceleratorExhibit(ServerLevel level, BlockState floor, int cx, int cz, int fy) {
        tileFloor(level, floor, cx - 2, cx + 14, cz - 8, cz + 8, fy);

        // Guides first, controller last: its very first trace then sees the whole ring closed.
        BlockState coil = ModBlocks.ACCELERATOR_COIL.get().defaultBlockState()
                .setValue(AcceleratorGuideBlock.BEND, AcceleratorGuideBlock.Bend.RIGHT);
        for (int[] offset : RING_GUIDES) {
            level.setBlockAndUpdate(new BlockPos(cx + offset[0], fy + 1, cz + offset[1]), coil);
        }

        BlockPos controllerPos = new BlockPos(cx, fy + 1, cz);
        placeMachine(level, controllerPos, ModBlocks.COLLIDER_CORE.get(), Direction.NORTH);
        placeCreativeBattery(level, controllerPos.west()); // off-ring, so it never blocks the trace
        chargeMachine(level, controllerPos);
        if (level.getBlockEntity(controllerPos) instanceof ColliderCoreBlockEntity collider) {
            collider.setItem(ColliderCoreBlockEntity.SLOT_PARTICLE_A,
                    new ItemStack(ModItems.COPPER_DUST.get(), 64));
            collider.setItem(ColliderCoreBlockEntity.SLOT_PARTICLE_B,
                    new ItemStack(ModItems.COPPER_DUST.get(), 64));
        }

        // Coolant tower straight up the controller: pump on top, two Radiators above it (the pump's
        // straight-line scan reaches three blocks on each axis, so both count).
        BlockPos pumpPos = controllerPos.above();
        level.setBlockAndUpdate(pumpPos, ModBlocks.COOLANT_PUMP.get().defaultBlockState());
        level.setBlockAndUpdate(pumpPos.above(), ModBlocks.RADIATOR.get().defaultBlockState());
        level.setBlockAndUpdate(pumpPos.above(2), ModBlocks.RADIATOR.get().defaultBlockState());
        placeCreativeBattery(level, pumpPos.west());
        chargeMachine(level, pumpPos);

        spawnLabelStand(level, controllerPos.above(5),
                Component.literal("Accelerator Controller — closed octagon, Copper Dust → Iron Dust"));
        spawnLabelStand(level, new BlockPos(cx + 6, fy + 6, cz),
                Component.literal("Particle Accelerator — 8 RIGHT guides, ~1045 J on lap 3"));
    }

    /**
     * GAS CHAIN (NORTH spoke): an Electrolyzer splitting water, flanked by the two consumers of its
     * products. Both sit directly against it, and each tank type-filters, so one push pass feeds both
     * — hydrogen east into the Gas Turbine (which burns it back into NE), oxygen west into the
     * Chemical Processor (which washes raw iron into 3 dust). Both product tanks are seeded too, so
     * the turbine is already spinning and the processor already washing on arrival.
     */
    private static void buildGasChainExhibit(ServerLevel level, BlockState floor, int ex, int ez, int fy) {
        tileFloor(level, floor, ex - 3, ex + 3, ez - 2, ez + 2, fy);

        BlockPos electrolyzerPos = new BlockPos(ex, fy + 1, ez);
        placeMachine(level, electrolyzerPos, ModBlocks.ELECTROLYZER.get(), Direction.SOUTH);
        placeCreativeBattery(level, electrolyzerPos.north());
        chargeMachine(level, electrolyzerPos);
        if (level.getBlockEntity(electrolyzerPos) instanceof ElectrolyzerBlockEntity electrolyzer) {
            // Public bucket-fill API, one bucket a call; it refuses a partial fill, which ends the loop.
            for (int bucket = 0; bucket < 64 && electrolyzer.fillFromBucket(); bucket++) {
                // filling until the tank is one bucket from full
            }
        }
        spawnLabelStand(level, electrolyzerPos.above(2),
                Component.literal("Electrolyzer — water → hydrogen + oxygen"));

        BlockPos turbinePos = electrolyzerPos.east();
        placeMachine(level, turbinePos, ModBlocks.GAS_TURBINE.get(), Direction.SOUTH);
        placeBattery(level, turbinePos.east()); // a real sink, so the turbine never idles on a full buffer
        seedGas(level, turbinePos, NeroTechGases.HYDROGEN);
        spawnLabelStand(level, turbinePos.above(2),
                Component.literal("Gas Turbine — burning the hydrogen back into NE"));

        BlockPos chemicalPos = electrolyzerPos.west();
        placeMachine(level, chemicalPos, ModBlocks.CHEMICAL_PROCESSOR.get(), Direction.SOUTH);
        placeCreativeBattery(level, chemicalPos.west());
        chargeMachine(level, chemicalPos);
        seedGas(level, chemicalPos, NeroTechGases.OXYGEN);
        if (level.getBlockEntity(chemicalPos) instanceof AbstractProcessingBlockEntity processor) {
            processor.setItem(AbstractProcessingBlockEntity.INPUT_SLOT, new ItemStack(Items.RAW_IRON, 64));
        }
        spawnLabelStand(level, chemicalPos.above(2),
                Component.literal("Chemical Processor — oxygen wash, raw iron → 3 dust"));
    }

    /**
     * COOLANT LOOP: a Coolant Pump parked against the 5×5×5 Fusion Reactor's controller face with a
     * two-Radiator stack above it, so the reactor's heat visibly drains instead of throttling it. The
     * pump stands on a short Fusion Casing plinth that runs down to the exhibit floor.
     */
    private static void buildCoolantExhibit(ServerLevel level, BlockPos reactorController) {
        BlockState casing = ModBlocks.FUSION_CASING.get().defaultBlockState();
        BlockPos pumpPos = reactorController.south(); // the controller faces SOUTH, so this is clear air
        for (int dy = 1; dy <= 2; dy++) {
            level.setBlockAndUpdate(pumpPos.below(dy), casing);
        }
        level.setBlockAndUpdate(pumpPos, ModBlocks.COOLANT_PUMP.get().defaultBlockState());
        level.setBlockAndUpdate(pumpPos.above(), ModBlocks.RADIATOR.get().defaultBlockState());
        level.setBlockAndUpdate(pumpPos.above(2), ModBlocks.RADIATOR.get().defaultBlockState());
        placeCreativeBattery(level, pumpPos.east());
        chargeMachine(level, pumpPos);
        spawnLabelStand(level, pumpPos.above(4),
                Component.literal("Coolant Pump + 2 Radiators — draining the reactor"));
    }

    /**
     * POWER PARK (SOUTH-EAST spoke): the whole Stage-D generation tier side by side — a Wind Turbine
     * on a 15-block Fusion Casing mast (altitude is the only reason to build tall), a Geothermal
     * Generator standing on a fully-walled 3×3 lava basin, a Bio Generator burning dried kelp, a
     * Battery Bank sandwiched between the two chemical/thermal generators taking both their pushes, a
     * Grid Controller overlooking the lot, and a linked Wireless Node pair 10 blocks apart with the
     * far node's only job being to power an Ore Processor no cable reaches.
     */
    private static void buildPowerPark(ServerLevel level, BlockState floor, int px, int pz, int fy) {
        tileFloor(level, floor, px - 2, px + 14, pz - 3, pz + 8, fy);
        BlockState casing = ModBlocks.FUSION_CASING.get().defaultBlockState();

        // Wind Turbine on a mast. Nothing is ever built above it, so it keeps its sky access.
        for (int dy = 1; dy <= 15; dy++) {
            level.setBlockAndUpdate(new BlockPos(px, fy + dy, pz), casing);
        }
        BlockPos turbinePos = new BlockPos(px, fy + 16, pz);
        placeMachine(level, turbinePos, ModBlocks.WIND_TURBINE.get(), Direction.NORTH);
        spawnLabelStand(level, new BlockPos(px + 1, fy + 2, pz),
                Component.literal("Wind Turbine — 15 blocks up; output climbs to y=200"));

        // Geothermal basin: a casing rim laid FIRST, then the 3×3 of lava inside it. The park floor
        // is the basin's bottom and the rim its walls, so the lava has nowhere to go but stay put.
        int bx = px + 7;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                if (rim) {
                    level.setBlockAndUpdate(new BlockPos(bx + dx, fy + 1, pz + dz), casing);
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                // flag 2: no neighbour cascade, so the fill never even attempts to spread.
                level.setBlock(new BlockPos(bx + dx, fy + 1, pz + dz), Blocks.LAVA.defaultBlockState(), 2);
            }
        }
        BlockPos geothermalPos = new BlockPos(bx, fy + 2, pz);
        placeMachine(level, geothermalPos, ModBlocks.GEOTHERMAL_GENERATOR.get(), Direction.NORTH);
        spawnLabelStand(level, geothermalPos.above(2),
                Component.literal("Geothermal Generator — 9 lava sources beneath it"));

        BlockPos bankPos = geothermalPos.east();
        placeMachine(level, bankPos, ModBlocks.BATTERY_BANK.get(), Direction.NORTH);
        spawnLabelStand(level, bankPos.above(3),
                Component.literal("Battery Bank — taking both generators' pushes"));

        BlockPos bioPos = bankPos.east();
        placeMachine(level, bioPos, ModBlocks.BIO_GENERATOR.get(), Direction.NORTH);
        if (level.getBlockEntity(bioPos) instanceof BioGeneratorBlockEntity bio) {
            bio.setItem(BioGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.DRIED_KELP_BLOCK, 64));
        }
        spawnLabelStand(level, bioPos.above(2), Component.literal("Bio Generator — burning dried kelp"));

        BlockPos gridPos = new BlockPos(px + 12, fy + 1, pz);
        placeMachine(level, gridPos, ModBlocks.GRID_CONTROLLER.get(), Direction.WEST);
        placeCreativeBattery(level, gridPos.north());
        chargeMachine(level, gridPos);
        spawnLabelStand(level, gridPos.above(2),
                Component.literal("Grid Controller — watching the park, load-shedding on brownout"));

        // Wireless pair: A is fed, B is not — everything B hands on came over the link.
        BlockPos nodeA = new BlockPos(px, fy + 1, pz + 6);
        BlockPos nodeB = new BlockPos(px + 10, fy + 1, pz + 6);
        placeMachine(level, nodeA, ModBlocks.WIRELESS_NODE.get(), Direction.NORTH);
        placeMachine(level, nodeB, ModBlocks.WIRELESS_NODE.get(), Direction.NORTH);
        placeCreativeBattery(level, nodeA.west());
        chargeMachine(level, nodeA);
        if (level.getBlockEntity(nodeA) instanceof WirelessNodeBlockEntity a
                && level.getBlockEntity(nodeB) instanceof WirelessNodeBlockEntity b
                && a.canPairWith(b)) {
            a.pairWith(b, level.dimension().identifier().toString());
        }
        BlockPos remotePos = nodeB.east();
        placeMachine(level, remotePos, ModBlocks.ORE_PROCESSOR.get(), Direction.NORTH);
        if (level.getBlockEntity(remotePos) instanceof AbstractProcessingBlockEntity processor) {
            processor.setItem(AbstractProcessingBlockEntity.INPUT_SLOT, new ItemStack(Items.RAW_IRON, 64));
        }
        spawnLabelStand(level, nodeA.above(2), Component.literal("Wireless Node A — fed, sending"));
        spawnLabelStand(level, nodeB.above(2),
                Component.literal("Wireless Node B — 10 blocks away, running the Ore Processor"));
    }

    /**
     * AUTOMATION LANE (SOUTH-WEST spoke): a Conveyor Belt run with a right-angle corner carrying live
     * Iron Dust item entities, a Robotic Arm shuttling a filled chest into an empty one, and a
     * Singularity Vault already holding 10 000 Iron Dust in its virtual store.
     */
    private static void buildAutomationLane(ServerLevel level, BlockState floor, int lx, int lz, int fy) {
        tileFloor(level, floor, lx - 2, lx + 16, lz - 2, lz + 4, fy);

        // Five belts east, then a corner and three south — a plain chain of independent pushers.
        for (int dx = 0; dx <= 4; dx++) {
            placeMachine(level, new BlockPos(lx + dx, fy + 1, lz), ModBlocks.CONVEYOR_BELT.get(), Direction.EAST);
        }
        for (int dz = 0; dz <= 2; dz++) {
            placeMachine(level, new BlockPos(lx + 5, fy + 1, lz + dz), ModBlocks.CONVEYOR_BELT.get(),
                    Direction.SOUTH);
        }
        for (int i = 0; i < 4; i++) {
            ItemEntity cargo = new ItemEntity(level, lx + i + 0.5D, fy + 1.5D, lz + 0.5D,
                    new ItemStack(ModItems.IRON_DUST.get(), 16));
            cargo.setDeltaMovement(0.0D, 0.0D, 0.0D); // the belt supplies all the motion there is
            cargo.setDefaultPickUpDelay();
            cargo.setUnlimitedLifetime(); // a gallery exhibit should still be there in five minutes
            level.addFreshEntity(cargo);
        }
        spawnLabelStand(level, new BlockPos(lx + 2, fy + 3, lz),
                Component.literal("Conveyor Belt — no power, no GUI, just a nudge"));

        // Robotic Arm: source chest BEHIND (west), target chest IN FRONT (east). Two blocks apart
        // with the arm between them, so they can never merge into one double chest.
        BlockPos sourceChest = new BlockPos(lx + 8, fy + 1, lz);
        BlockPos armPos = sourceChest.east();
        BlockPos targetChest = armPos.east();
        level.setBlockAndUpdate(sourceChest, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(targetChest, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(sourceChest) instanceof Container chest) {
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                chest.setItem(slot, new ItemStack(ModItems.IRON_DUST.get(), 64));
            }
            chest.setChanged();
        }
        placeMachine(level, armPos, ModBlocks.ROBOTIC_ARM.get(), Direction.EAST);
        placeCreativeBattery(level, armPos.north());
        chargeMachine(level, armPos);
        if (level.getBlockEntity(armPos) instanceof RoboticArmBlockEntity arm) {
            arm.setItem(RoboticArmBlockEntity.FILTER_SLOT, new ItemStack(ModItems.IRON_DUST.get()));
        }
        spawnLabelStand(level, armPos.above(2),
                Component.literal("Robotic Arm — chest → chest, filtered to Iron Dust"));

        BlockPos vaultPos = new BlockPos(lx + 13, fy + 1, lz);
        placeMachine(level, vaultPos, ModBlocks.SINGULARITY_VAULT.get(), Direction.NORTH);
        if (level.getBlockEntity(vaultPos) instanceof SingularityVaultBlockEntity vault) {
            // deposit() takes at most the stack it is handed, so stock it a stack at a time.
            long target = Math.min(10_000L, SingularityVaultBlockEntity.capacity());
            while (vault.totalStored() < target) {
                if (vault.deposit(new ItemStack(ModItems.IRON_DUST.get(), 64)) <= 0) {
                    break;
                }
            }
        }
        spawnLabelStand(level, vaultPos.above(2),
                Component.literal("Singularity Vault — 10 000 Iron Dust of one type"));
    }

    // --- shared exhibit plumbing ------------------------------------------------------------------

    /** Lay the exhibit floor over an inclusive x/z rectangle at the floor layer. */
    private static void tileFloor(ServerLevel level, BlockState floor, int minX, int maxX, int minZ, int maxZ,
            int fy) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlockAndUpdate(new BlockPos(x, fy, z), floor);
            }
        }
    }

    /** The hidden endless source behind a consumer exhibit — Core's Creative Battery. */
    private static void placeCreativeBattery(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos,
                za.co.neroland.nerolandcore.registry.ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
    }

    /** A real Core Battery — the sink a generator exhibit needs so it never idles on a full buffer. */
    private static void placeBattery(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos,
                za.co.neroland.nerolandcore.registry.ModBlocks.BATTERY.get().defaultBlockState());
    }

    /**
     * Prime a gas machine's tank so it is already working on arrival. Type-filtered tanks simply
     * refuse a gas they do not take, so this is safe to call with either product.
     */
    private static void seedGas(ServerLevel level, BlockPos pos, Identifier gas) {
        if (level.getBlockEntity(pos) instanceof NeroTechMachineBlockEntity machine) {
            NeroGasStorage tank = machine.gasStorage(null);
            if (tank != null) {
                tank.fill(gas, tank.getCapacity(), false);
                machine.setChanged();
            }
        }
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
