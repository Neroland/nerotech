package za.co.neroland.nerotech.command.gallery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import za.co.neroland.nerolandcore.energy.EnergyBuffer;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;

import za.co.neroland.nerotech.NeroTechCommon;
import za.co.neroland.nerotech.command.NeroTechCommands;
import za.co.neroland.nerotech.command.gallery.GalleryLayout.Bay;
import za.co.neroland.nerotech.command.gallery.GalleryLayout.Kind;
import za.co.neroland.nerotech.command.gallery.GalleryLayout.Part;
import za.co.neroland.nerotech.config.NeroTechConfig;
import za.co.neroland.nerotech.fluid.NeroTechFluids;
import za.co.neroland.nerotech.gas.NeroTechGases;
import za.co.neroland.nerotech.item.tools.ChargedToolItem;
import za.co.neroland.nerotech.item.tools.ToolsContent;
import za.co.neroland.nerotech.machine.AbstractProcessingBlockEntity;
import za.co.neroland.nerotech.machine.MachinePreset;
import za.co.neroland.nerotech.machine.NeroGeneratorBlockEntity;
import za.co.neroland.nerotech.machine.NeroTechMachineBlock;
import za.co.neroland.nerotech.machine.NeroTechMachineBlockEntity;
import za.co.neroland.nerotech.machine.ProcessingMachineBlockEntity;
import za.co.neroland.nerotech.machine.chemistry.ChemistryContent;
import za.co.neroland.nerotech.machine.components.ComponentsContent;
import za.co.neroland.nerotech.machine.extraction.DeepCoreDrillBlockEntity;
import za.co.neroland.nerotech.machine.extraction.ExtractionContent;
import za.co.neroland.nerotech.machine.extraction.ExtractionSettings;
import za.co.neroland.nerotech.machine.extraction.ScannerState;
import za.co.neroland.nerotech.machine.foundry.FoundryContent;
import za.co.neroland.nerotech.machine.foundry.FoundryHeatPortBlockEntity;
import za.co.neroland.nerotech.machine.fusion.FusionContent;
import za.co.neroland.nerotech.machine.heat.HeatContent;
import za.co.neroland.nerotech.machine.heat.HeatNodes;
import za.co.neroland.nerotech.machine.metallurgy.MetallurgyContent;
import za.co.neroland.nerotech.machine.tank.ProcessTankBlockEntity;
import za.co.neroland.nerotech.machine.tank.TankMedium;
import za.co.neroland.nerotech.machine.utility.UtilityContent;
import za.co.neroland.nerotech.registry.ModBlocks;
import za.co.neroland.nerotech.registry.ModItems;

/**
 * Builds the 0.8.0 plant from {@link GalleryLayout}: places every part in layout order, then fills each
 * machine, tank and chest for its role, then labels the bays (lang keys). Items are referenced through the
 * registry constants and the catalogue reads the item registry, so a renamed item still lands in the right
 * place. Holds and records no player data.
 */
public final class GalleryPlant {

    /** Heat held by the overheat exhibit, as a share of the heat ceiling (permille). */
    private static final int OVERHEAT_PERMILLE = 950;
    /** Heat held by the hot, unfuelled reactor (below the meltdown line, it has no fuel to climb). */
    private static final int FAILING_PERMILLE = 900;
    /** Pre-heat of the heat-loop generator so the Foundry run reads hot on arrival. */
    private static final int GENERATOR_PERMILLE = 700;

    private GalleryPlant() {
    }

    /** Build the plant around {@code origin}. Returns the number of parts placed. */
    public static int build(ServerLevel level, BlockPos origin) {
        List<Part> parts = GalleryLayout.allParts();
        List<GalleryCatalogue.Chest> catalogue = catalogue();
        for (Part part : parts) {
            if (!unusedCatalogueSlot(part, catalogue)) {
                place(level, at(origin, part), part);
            }
        }
        for (Part part : parts) {
            if (!part.role().isEmpty()) {
                fill(level, at(origin, part), part, catalogue);
            }
        }
        for (Bay bay : GalleryLayout.bays()) {
            label(level, origin, bay, catalogue);
        }
        return parts.size();
    }

    private static boolean unusedCatalogueSlot(Part part, List<GalleryCatalogue.Chest> catalogue) {
        return part.role().startsWith("catalogue.")
                && Integer.parseInt(part.role().substring("catalogue.".length())) >= catalogue.size();
    }

    /** Mark every NeroTech machine in the gallery volume as an exhibit (no gates, trophies or statistics). */
    public static int markExhibits(ServerLevel level, BlockPos origin) {
        int minX = origin.getX() + GalleryLayout.MIN_X;
        int maxX = origin.getX() + GalleryLayout.MAX_X;
        int minY = origin.getY() + GalleryLayout.MIN_Y;
        int maxY = origin.getY() + GalleryLayout.MAX_Y;
        int minZ = origin.getZ() + GalleryLayout.MIN_Z;
        int maxZ = origin.getZ() + GalleryLayout.MAX_Z;
        int marked = 0;
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    BlockPos p = be.getBlockPos();
                    if (be instanceof NeroTechMachineBlockEntity machine && p.getX() >= minX && p.getX() <= maxX
                            && p.getY() >= minY && p.getY() <= maxY && p.getZ() >= minZ && p.getZ() <= maxZ) {
                        machine.markGalleryExhibit();
                        marked++;
                    }
                }
            }
        }
        return marked;
    }

    /** The absolute position of a part. */
    public static BlockPos at(BlockPos origin, Part part) {
        return origin.offset(part.x(), part.y(), part.z());
    }

    // --- placement ------------------------------------------------------------------------------------------

    private static void place(ServerLevel level, BlockPos pos, Part part) {
        BlockState state = blockFor(part.kind()).defaultBlockState();
        Direction facing = facing(part.facing());
        if (facing != null && state.hasProperty(NeroTechMachineBlock.FACING)) {
            state = state.setValue(NeroTechMachineBlock.FACING, facing);
        }
        level.setBlockAndUpdate(pos, state);
    }

    private static Block blockFor(Kind kind) {
        return switch (kind) {
            case FLOOR, CASING, FUSION_CASING -> ModBlocks.FUSION_CASING.get();
            case FUSION_GLASS -> ModBlocks.FUSION_CONTAINMENT_GLASS.get();
            case CREATIVE_BATTERY -> za.co.neroland.nerolandcore.registry.ModBlocks.CREATIVE_BATTERY.get();
            case BATTERY -> za.co.neroland.nerolandcore.registry.ModBlocks.BATTERY.get();
            case CHEST -> Blocks.CHEST;
            case AIR -> Blocks.AIR;
            case PLASMA_CRUCIBLE -> MetallurgyContent.PLASMA_CRUCIBLE.get();
            case PLATE_PRESS -> MetallurgyContent.PLATE_PRESS.get();
            case FILAMENT_SPOOLER -> MetallurgyContent.FILAMENT_SPOOLER.get();
            case BLAST_FOUNDRY -> FoundryContent.BLAST_FOUNDRY.get();
            case FOUNDRY_BRICK -> FoundryContent.FOUNDRY_BRICK.get();
            case FOUNDRY_HEAT_PORT -> FoundryContent.FOUNDRY_HEAT_PORT.get();
            case CENTRIFUGE -> ChemistryContent.CENTRIFUGE.get();
            case CRYO_DISTILLER -> ChemistryContent.CRYO_DISTILLER.get();
            case ATMOSPHERIC_CONDENSER -> ChemistryContent.ATMOSPHERIC_CONDENSER.get();
            case REACTION_VAT -> ChemistryContent.REACTION_VAT.get();
            case PROCESS_TANK -> ModBlocks.PROCESS_TANK.get();
            case COMPONENT_FORGE -> ComponentsContent.COMPONENT_FORGE.get();
            case LASER_ETCHER -> ComponentsContent.LASER_ETCHER.get();
            case CRYSTAL_GROWER -> ComponentsContent.CRYSTAL_GROWER.get();
            case HEAT_PIPE -> HeatContent.HEAT_PIPE.get();
            case HEAT_EXCHANGER -> HeatContent.HEAT_EXCHANGER.get();
            case DEEP_CORE_DRILL -> ExtractionContent.DEEP_CORE_DRILL.get();
            case DRILL_DERRICK -> ExtractionContent.DRILL_DERRICK.get();
            case FUEL_INJECTOR -> FusionContent.FUEL_INJECTOR.get();
            case EUROPIUM_FLUX_LATTICE -> FusionContent.EUROPIUM_FLUX_LATTICE.get();
            case INDUCTION_CHARGER -> UtilityContent.INDUCTION_CHARGER.get();
            case ANNUNCIATOR_PANEL -> UtilityContent.ANNUNCIATOR_PANEL.get();
            case NERO_GENERATOR -> ModBlocks.NERO_GENERATOR.get();
            case ORE_PROCESSOR -> ModBlocks.ORE_PROCESSOR.get();
            case FUSION_REACTOR -> ModBlocks.FUSION_REACTOR.get();
            case COOLANT_PUMP -> ModBlocks.COOLANT_PUMP.get();
            case RADIATOR -> ModBlocks.RADIATOR.get();
        };
    }

    private static Direction facing(char c) {
        return switch (c) {
            case 'N' -> Direction.NORTH;
            case 'S' -> Direction.SOUTH;
            case 'E' -> Direction.EAST;
            case 'W' -> Direction.WEST;
            default -> null;
        };
    }

    // --- contents -------------------------------------------------------------------------------------------

    private static void fill(ServerLevel level, BlockPos pos, Part part, List<GalleryCatalogue.Chest> catalogue) {
        String role = part.role();
        BlockEntity be = level.getBlockEntity(pos);
        if (role.startsWith("tank.")) {
            fillTank(be, role.substring("tank.".length()));
            return;
        }
        if (role.startsWith("catalogue.")) {
            int index = Integer.parseInt(role.substring("catalogue.".length()));
            if (be instanceof Container chest && index < catalogue.size()) {
                stock(chest, catalogue.get(index));
            }
            return;
        }
        switch (role) {
            case "structural.raw" -> chest(be, ModItems.IRON_DUST.get(), Items.COAL, Items.BLAZE_POWDER);
            case "structural.crucible" -> {
                inputs(be, ModItems.IRON_DUST.get(), Items.COAL, Items.BLAZE_POWDER);
                if (be instanceof ProcessingMachineBlockEntity machine) {
                    int first = machine.spec().slotCount();
                    if (machine.getContainerSize() > first + 1) {
                        machine.setItem(first, new ItemStack(ModItems.HEAT_SINK_MODULE.get()));
                        machine.setItem(first + 1, new ItemStack(ModItems.SCRUBBER_MODULE.get()));
                    }
                }
                charge(be);
            }
            case "structural.plate" -> press(be, ModItems.CINDERSTEEL_INGOT.get(), MetallurgyContent.PLATE_DIE.get());
            case "structural.rod" -> press(be, ModItems.CINDERSTEEL_INGOT.get(), MetallurgyContent.ROD_DIE.get());
            case "structural.wire" -> powered(be, ModItems.CINDERSTEEL_INGOT.get());
            case "superconductor.raw" -> chest(be, za.co.neroland.nerolandcore.registry.ModItems.VOID_CRYSTAL.get(),
                    Items.SNOW_BLOCK, Items.PACKED_ICE);
            case "superconductor.glacite" -> {
                powered(be, za.co.neroland.nerolandcore.registry.ModItems.VOID_CRYSTAL.get(), Items.SNOW_BLOCK,
                        Items.PACKED_ICE);
                fluid(be, NeroTechFluids.CRYO_COOLANT.get());
            }
            case "superconductor.wire" -> powered(be, ModItems.SYNTHETIC_GLACITE.get(), ModItems.CINDERSTEEL_WIRE.get());
            case "superconductor.coil" -> {
                powered(be, ModItems.SUPERCONDUCTOR_WIRE.get(), ModItems.CINDERSTEEL_COIL.get(),
                        ModItems.HEAT_SINK.get());
                fluid(be, NeroTechFluids.CRYO_COOLANT.get());
            }
            case "semiconductor.raw" -> chest(be, ModItems.CRYSTAL_SEED.get(), Items.QUARTZ, Items.AMETHYST_SHARD);
            case "semiconductor.boule" -> {
                powered(be, ModItems.CRYSTAL_SEED.get(), Items.QUARTZ);
                fluid(be, Fluids.WATER);
            }
            case "semiconductor.wafer" -> press(be, ModItems.CRYSTAL_BOULE.get(), MetallurgyContent.WAFER_DIE.get());
            case "semiconductor.logic" -> {
                powered(be, ModItems.WAFER.get(), ModItems.COPPER_WIRE.get());
                catalyst(be, ModItems.QUARTZ_FOCUS_LENS.get());
            }
            case "semiconductor.quantum" -> {
                powered(be, ModItems.WAFER.get(), ModItems.SUPERCONDUCTOR_WIRE.get(), ModItems.LOGIC_CHIP.get());
                catalyst(be, ModItems.VOID_FOCUS_LENS.get());
            }
            case "optics.raw" -> chest(be, Items.QUARTZ, Items.GLASS_PANE,
                    za.co.neroland.nerolandcore.registry.ModItems.VOID_CRYSTAL.get());
            case "optics.quartz" -> {
                powered(be, Items.QUARTZ, Items.GLASS_PANE);
                fluid(be, Fluids.WATER);
            }
            case "optics.void" -> {
                powered(be, za.co.neroland.nerolandcore.registry.ModItems.VOID_CRYSTAL.get(), Items.GLASS_PANE);
                fluid(be, Fluids.WATER);
            }
            case "chemistry.raw" -> chest(be, Items.GUNPOWDER, Items.COAL, Items.OAK_LOG);
            case "chemistry.sulfur" -> powered(be, Items.GUNPOWDER);
            case "chemistry.acid" -> {
                powered(be, ModItems.SULFUR_DUST.get());
                fluid(be, Fluids.WATER);
                gas(be, NeroTechGases.OXYGEN);
            }
            case "chemistry.air" -> charge(be);
            case "chemistry.co2" -> {
                powered(be, Items.COAL);
                gas(be, NeroTechGases.OXYGEN);
            }
            case "chemistry.resin" -> {
                powered(be, Items.OAK_LOG);
                gas(be, NeroTechGases.METHANE);
            }
            case "chemistry.sheet" -> {
                catalyst(be, MetallurgyContent.PLATE_DIE.get());
                fluid(be, NeroTechFluids.POLYMER_RESIN.get());
                charge(be);
            }
            case "fusion_fuel.raw" -> chest(be, Items.GLOWSTONE_DUST, ModItems.EUROPIUM_DUST.get(),
                    ModItems.QUANTUM_CHIP.get());
            case "fusion_fuel.deuterium" -> {
                charge(be);
                fluid(be, Fluids.WATER);
                gas(be, NeroTechGases.HYDROGEN);
            }
            case "fusion_fuel.cascade" -> {
                charge(be);
                gas(be, NeroTechGases.DEUTERIUM);
            }
            case "fusion_fuel.europium" -> powered(be, Items.GLOWSTONE_DUST);
            case "fusion_fuel.lattice" -> {
                powered(be, ModBlocks.FUSION_CASING.get(), ModItems.EUROPIUM_DUST.get(),
                        ModItems.CINDERSTEEL_INGOT.get(), ModItems.QUANTUM_CHIP.get(),
                        ModItems.SUPERCONDUCTOR_WIRE.get());
                fluid(be, NeroTechFluids.CRYO_COOLANT.get());
            }
            case "foundry.port" -> {
                if (be instanceof FoundryHeatPortBlockEntity port) {
                    port.acceptHeat(port.heatCapacity());
                }
            }
            case "foundry.batch" -> powered(be, ModItems.IRON_DUST.get(), Items.COAL, Items.BLAZE_POWDER);
            case "heat.generator" -> {
                if (be instanceof NeroGeneratorBlockEntity generator) {
                    generator.setItem(NeroGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 64));
                }
                heat(be, GENERATOR_PERMILLE);
            }
            case "drill.head" -> {
                if (be instanceof DeepCoreDrillBlockEntity drill) {
                    drill.setItem(DeepCoreDrillBlockEntity.HEAD_SLOT, new ItemStack(ModItems.CINDERSTEEL_DRILL_HEAD.get()));
                }
                charge(be);
            }
            case "drill.heads" -> chest(be, ModItems.IRON_DRILL_HEAD.get(), ModItems.CINDERSTEEL_DRILL_HEAD.get(),
                    ModItems.GLACITE_DRILL_HEAD.get());
            case "alarm.overheat" -> {
                oreInput(be);
                preset(be, MachinePreset.OVERDRIVE);
                heat(be, OVERHEAT_PERMILLE);
                charge(be);
            }
            case "alarm.starved" -> oreInput(be); // powered by nothing, buffer empty: NO_ENERGY
            case "alarm.full" -> {
                oreInput(be);
                if (be instanceof AbstractProcessingBlockEntity processor) {
                    processor.setItem(AbstractProcessingBlockEntity.OUTPUT_SLOT, new ItemStack(Items.COBBLESTONE, 64));
                }
                charge(be);
            }
            case "alarm.polluting" -> {
                if (be instanceof NeroGeneratorBlockEntity generator) {
                    generator.setItem(NeroGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 64));
                }
                preset(be, MachinePreset.OVERDRIVE);
            }
            case "alarm.failing" -> heat(be, FAILING_PERMILLE);
            case "rack.tools" -> {
                if (be instanceof Container chest) {
                    List<ItemStack> tools = tools();
                    for (int i = 0; i < tools.size() && i < chest.getContainerSize(); i++) {
                        chest.setItem(i, tools.get(i));
                    }
                }
            }
            case "rack.spares" -> chest(be, ModItems.FILTER_CARTRIDGE.get(), ModItems.QUARTZ_FOCUS_LENS.get(),
                    ModItems.GLACITE_FOCUS_LENS.get(), ModItems.EMBER_FOCUS_LENS.get(), ModItems.VOID_FOCUS_LENS.get(),
                    ModItems.HEAT_SINK_MODULE.get(), ModItems.SCRUBBER_MODULE.get());
            case "charger", "coolant" -> charge(be);
            case "fusion.injector" -> {
                gas(be, NeroTechGases.DEUTERIUM);
                gas(be, NeroTechGases.HELIUM_3);
            }
            default -> {
                // product chests, pipes and the gas reactor's controller need nothing
            }
        }
    }

    /** The seven tools charged full (plus the Configurator), the Filter Mask fitted with a cartridge. */
    private static List<ItemStack> tools() {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : List.of(ToolsContent.POWER_DRILL.get(), ToolsContent.CUTTING_LANCE.get(),
                ToolsContent.ARC_WRENCH.get(), ToolsContent.MULTIMETER.get(), ToolsContent.THERMAL_GOGGLES.get(),
                ToolsContent.FILTER_MASK.get())) {
            ItemStack stack = new ItemStack(item);
            if (item instanceof ChargedToolItem tool) {
                tool.acceptCharge(stack, Integer.MAX_VALUE, false);
            }
            out.add(stack);
        }
        ItemStack mask = out.get(out.size() - 1);
        mask.set(ToolsContent.FITTED_FILTER.get(), NeroTechConfig.filterMaskPulsesPerCartridge());
        ItemStack scanner = new ItemStack(ExtractionContent.SURVEY_SCANNER.get());
        scanner.set(ExtractionContent.SCANNER_STATE.get(),
                new ScannerState(ExtractionSettings.surveyScannerCapacity(), 0L));
        out.add(scanner);
        out.add(new ItemStack(ModItems.CONFIGURATOR.get()));
        return out;
    }

    private static void oreInput(BlockEntity be) {
        if (be instanceof AbstractProcessingBlockEntity processor) {
            processor.setItem(AbstractProcessingBlockEntity.INPUT_SLOT, new ItemStack(Items.RAW_IRON, 64));
        }
    }

    private static void press(BlockEntity be, ItemLike input, ItemLike die) {
        powered(be, input);
        catalyst(be, die);
    }

    /** Fill the input slots in order with full stacks, and charge the buffer. */
    private static void powered(BlockEntity be, ItemLike... inputs) {
        inputs(be, inputs);
        charge(be);
    }

    private static void inputs(BlockEntity be, ItemLike... inputs) {
        if (be instanceof ProcessingMachineBlockEntity machine) {
            for (int i = 0; i < inputs.length && i < machine.spec().inputs(); i++) {
                machine.setItem(machine.spec().inputSlot(i), full(inputs[i]));
            }
        }
    }

    private static void catalyst(BlockEntity be, ItemLike item) {
        if (be instanceof ProcessingMachineBlockEntity machine && machine.spec().catalystSlot() >= 0) {
            machine.setItem(machine.spec().catalystSlot(), new ItemStack(item));
        }
    }

    private static void chest(BlockEntity be, ItemLike... items) {
        if (be instanceof Container chest) {
            for (int i = 0; i < items.length && i < chest.getContainerSize(); i++) {
                chest.setItem(i, full(items[i]));
            }
        }
    }

    private static ItemStack full(ItemLike item) {
        ItemStack stack = new ItemStack(item);
        stack.setCount(stack.getMaxStackSize());
        return stack;
    }

    private static void charge(BlockEntity be) {
        if (be instanceof NeroTechMachineBlockEntity machine && machine.getEnergy() instanceof EnergyBuffer buffer) {
            buffer.setRaw(Integer.MAX_VALUE);
            machine.setChanged();
        }
    }

    private static void heat(BlockEntity be, int permille) {
        if (be instanceof NeroTechMachineBlockEntity machine) {
            HeatNodes.deposit(machine, machine.heatCapacity() * permille / 1000);
        }
    }

    private static void preset(BlockEntity be, MachinePreset preset) {
        if (be instanceof NeroTechMachineBlockEntity machine) {
            machine.setPreset(preset);
        }
    }

    private static void fluid(BlockEntity be, Fluid fluid) {
        if (be instanceof NeroTechMachineBlockEntity machine) {
            NeroFluidStorage tank = machine.fluidStorage(null);
            if (tank != null) {
                tank.fill(fluid, tank.getCapacity(), false);
                machine.setChanged();
            }
        }
    }

    private static void gas(BlockEntity be, Identifier gas) {
        if (be instanceof NeroTechMachineBlockEntity machine) {
            NeroGasStorage tank = machine.gasStorage(null);
            if (tank != null) {
                tank.fill(gas, tank.getCapacity(), false);
                machine.setChanged();
            }
        }
    }

    private static void fillTank(BlockEntity be, String media) {
        if (be instanceof ProcessTankBlockEntity tank) {
            TankMedium medium = medium(media);
            if (medium != null) {
                tank.fillColumn(medium, tank.columnCapacity(), false);
            }
        }
    }

    private static TankMedium medium(String media) {
        return switch (media) {
            case "water" -> fluidMedium(Fluids.WATER);
            case "cryo_coolant" -> fluidMedium(NeroTechFluids.CRYO_COOLANT.get());
            case "sulfuric_acid" -> fluidMedium(NeroTechFluids.SULFURIC_ACID.get());
            case "polymer_resin" -> fluidMedium(NeroTechFluids.POLYMER_RESIN.get());
            case "hydrogen" -> TankMedium.gas(NeroTechGases.HYDROGEN.toString());
            case "deuterium" -> TankMedium.gas(NeroTechGases.DEUTERIUM.toString());
            case "helium_3" -> TankMedium.gas(NeroTechGases.HELIUM_3.toString());
            default -> null;
        };
    }

    private static TankMedium fluidMedium(Fluid fluid) {
        return TankMedium.fluid(BuiltInRegistries.FLUID.getKey(fluid).toString());
    }

    // --- catalogue ------------------------------------------------------------------------------------------

    /** Every NeroTech item, read from the registry, packed into display chests. */
    private static List<GalleryCatalogue.Chest> catalogue() {
        List<GalleryCatalogue.Entry> entries = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (NeroTechCommon.MOD_ID.equals(id.getNamespace())) {
                entries.add(new GalleryCatalogue.Entry(id.getPath(), item instanceof BlockItem));
            }
        }
        return GalleryCatalogue.plan(entries);
    }

    private static void stock(Container chest, GalleryCatalogue.Chest plan) {
        for (int i = 0; i < plan.items().size() && i < chest.getContainerSize(); i++) {
            Identifier id = Identifier.fromNamespaceAndPath(NeroTechCommon.MOD_ID, plan.items().get(i));
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != null && item != Items.AIR) {
                chest.setItem(i, full(item));
            }
        }
    }

    // --- labels ---------------------------------------------------------------------------------------------

    private static void label(ServerLevel level, BlockPos origin, Bay bay, List<GalleryCatalogue.Chest> catalogue) {
        String key = "gallery.nerotech.bay." + bay.id();
        int topY = 4;
        for (Part part : bay.parts()) {
            topY = Math.max(topY, part.y() + 2);
        }
        NeroTechCommands.spawnLabelStand(level, origin.offset(bay.labelX(), topY + 1, bay.z0() + 3),
                Component.translatable(key + ".title"));
        NeroTechCommands.spawnLabelStand(level, origin.offset(bay.labelX(), topY, bay.z0() + 3),
                Component.translatable(key + ".flow"));
        Set<String> labelled = new HashSet<>();
        for (Part part : bay.parts()) {
            String role = part.role();
            Component text = partLabel(role, catalogue);
            if (text == null || !labelled.add(role.startsWith("catalogue.") ? role + part.x() : role)) {
                continue;
            }
            NeroTechCommands.spawnLabelStand(level, at(origin, part).above(role.equals("fusion.gas") ? 4 : 2), text);
        }
    }

    private static Component partLabel(String role, List<GalleryCatalogue.Chest> catalogue) {
        if (role.startsWith("tank.")) {
            TankMedium medium = medium(role.substring("tank.".length()));
            return medium == null ? null : ProcessTankBlockEntity.displayName(medium);
        }
        if (role.startsWith("catalogue.")) {
            int index = Integer.parseInt(role.substring("catalogue.".length()));
            return index < catalogue.size()
                    ? Component.translatable("gallery.nerotech.catalogue." + catalogue.get(index).family()) : null;
        }
        if (role.startsWith("alarm.") || role.startsWith("pipe.") || role.startsWith("rack.")
                || role.equals("charger") || role.equals("drill.head") || role.equals("foundry.batch")
                || role.equals("fusion.gas") || role.equals("product")) {
            return Component.translatable("gallery.nerotech.part." + role.replace('.', '_'));
        }
        return null;
    }
}
