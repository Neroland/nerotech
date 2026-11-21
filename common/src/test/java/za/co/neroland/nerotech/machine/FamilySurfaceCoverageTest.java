package za.co.neroland.nerotech.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerotech.api.MachineTypeRegistry.Surface;

/**
 * Proof that every 0.8.0 machine reaches the capability surfaces on all three loaders, without a game
 * bootstrap. NeoForge ({@code RegisterCapabilitiesEvent}) and Fabric ({@code MachineTypeRegistry.onRegistered})
 * expose exactly the types in {@link za.co.neroland.nerotech.api.MachineTypeRegistry}, which
 * {@code ModBlockEntities.seed()} fills from NeroTech's own lists followed by every family's
 * {@code <Family>Content.machineTypes(surface)} ({@code withFamilies}). Forge attaches one provider to every
 * {@code NeroTechMachineBlockEntity}, so it covers each type regardless; its per-face views come from the
 * same machine methods.
 *
 * <p>The family classes register into live game registries when they load, so this test reads their
 * {@code machineTypes} declarations from source and locks them against the expected surface table, checks
 * every declared block-entity type is on a surface (or deliberately on none), and checks the seeding path
 * routes all four surfaces through the families. It also checks no new machine leaves Core's side-config
 * builder on its ALL_DISABLED default.
 */
class FamilySurfaceCoverageTest {

    private static final List<String> FAMILIES = List.of("metallurgy", "foundry", "chemistry", "components",
            "heat", "extraction", "fusion", "utility");

    /** Block-entity types deliberately on no surface: a passive alarm board and a heat-only port. */
    private static final Set<String> NO_SURFACE = Set.of("ANNUNCIATOR_PANEL_BE", "FOUNDRY_HEAT_PORT_BE");

    private static final Map<String, Set<Surface>> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("PLASMA_CRUCIBLE_BE", EnumSet.of(Surface.ENERGY, Surface.ITEM));
        EXPECTED.put("PLATE_PRESS_BE", EnumSet.of(Surface.ENERGY, Surface.ITEM, Surface.FLUID));
        EXPECTED.put("FILAMENT_SPOOLER_BE", EnumSet.of(Surface.ENERGY, Surface.ITEM));
        EXPECTED.put("BLAST_FOUNDRY_BE", EnumSet.of(Surface.ENERGY, Surface.ITEM));
        EXPECTED.put("CENTRIFUGE_BE", EnumSet.allOf(Surface.class));
        EXPECTED.put("CRYO_DISTILLER_BE", EnumSet.allOf(Surface.class));
        EXPECTED.put("ATMOSPHERIC_CONDENSER_BE", EnumSet.of(Surface.ENERGY, Surface.ITEM, Surface.GAS));
        EXPECTED.put("REACTION_VAT_BE", EnumSet.allOf(Surface.class));
        EXPECTED.put("COMPONENT_FORGE_BE", EnumSet.of(Surface.ENERGY, Surface.ITEM, Surface.FLUID));
        EXPECTED.put("LASER_ETCHER_BE", EnumSet.of(Surface.ENERGY, Surface.ITEM));
        EXPECTED.put("CRYSTAL_GROWER_BE", EnumSet.of(Surface.ENERGY, Surface.ITEM, Surface.FLUID));
        EXPECTED.put("HEAT_EXCHANGER_BE", EnumSet.of(Surface.ENERGY, Surface.ITEM));
        EXPECTED.put("DEEP_CORE_DRILL_BE", EnumSet.of(Surface.ENERGY, Surface.ITEM));
        EXPECTED.put("FUEL_INJECTOR_BE", EnumSet.of(Surface.GAS));
        EXPECTED.put("INDUCTION_CHARGER_BE", EnumSet.of(Surface.ENERGY));
    }

    private static final Pattern BE_FIELD = Pattern.compile(
            "RegistryEntry<BlockEntityType<\\w+>>\\s+(\\w+_BE)\\s*=");
    private static final Pattern CASE = Pattern.compile(
            "case\\s+([A-Z, ]+?)\\s*->\\s*List\\.of\\(([^;]*?)\\);", Pattern.DOTALL);
    private static final Pattern REF = Pattern.compile("(\\w+_BE)::get");

    @Test
    void everyFamilyMachineIsOnItsSurfaces() {
        Map<String, Set<Surface>> actual = new TreeMap<>();
        Set<String> declared = new TreeSet<>();
        for (String family : FAMILIES) {
            String src = read(machineDir().resolve(family).resolve(contentName(family) + ".java"));
            Matcher field = BE_FIELD.matcher(src);
            while (field.find()) {
                declared.add(field.group(1));
            }
            int at = src.indexOf("machineTypes(");
            assertTrue(at >= 0, family + ": no machineTypes(surface) declaration");
            String body = src.substring(at, src.indexOf("\n    }", at));
            Set<Surface> covered = EnumSet.noneOf(Surface.class);
            Matcher c = CASE.matcher(body);
            while (c.find()) {
                for (String label : c.group(1).split(",")) {
                    Surface surface = Surface.valueOf(label.trim());
                    covered.add(surface);
                    Matcher ref = REF.matcher(c.group(2));
                    while (ref.find()) {
                        actual.computeIfAbsent(ref.group(1), k -> EnumSet.noneOf(Surface.class)).add(surface);
                    }
                }
            }
            assertEquals(EnumSet.allOf(Surface.class), covered, family + ": machineTypes must answer every surface");
        }
        for (String be : declared) {
            if (NO_SURFACE.contains(be)) {
                assertFalse(actual.containsKey(be), be + " is meant to be on no surface");
            } else {
                assertTrue(actual.containsKey(be), be + " is declared but on no capability surface");
            }
        }
        assertEquals(new TreeMap<>(EXPECTED), actual, "family surface table");
    }

    @Test
    void seedingRoutesEverySurfaceThroughTheFamilies() {
        String src = read(srcRoot().resolve("registry/ModBlockEntities.java"));
        for (Surface surface : Surface.values()) {
            assertTrue(src.contains("withFamilies(MachineTypeRegistry.Surface." + surface.name()),
                    "own " + surface + " list must include the families");
        }
        for (String call : List.of("ownEnergyMachineTypes().forEach(MachineTypeRegistry::registerEnergy)",
                "ownItemMachineTypes().forEach(MachineTypeRegistry::registerItem)",
                "ownGasMachineTypes().forEach(MachineTypeRegistry::registerGas)",
                "ownFluidMachineTypes().forEach(MachineTypeRegistry::registerFluid)")) {
            assertTrue(src.contains(call), "seed() must register: " + call);
        }
        String registry = read(srcRoot().resolve("registry/FamilyRegistry.java"));
        for (String family : FAMILIES) {
            assertTrue(registry.contains(contentName(family) + "::machineTypes"),
                    family + " must be listed in FamilyRegistry");
        }
    }

    @Test
    void noNewMachineKeepsTheAllDisabledSidePreset() {
        List<String> dirs = new java.util.ArrayList<>(FAMILIES);
        dirs.add("tank");
        for (String dir : dirs) {
            try (Stream<Path> files = Files.list(machineDir().resolve(dir))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String src = read(file);
                    assertFalse(src.contains("SidePreset.ALL_DISABLED"), file.getFileName() + " uses ALL_DISABLED");
                    if (src.contains("SideConfig.builder(")) {
                        assertTrue(src.contains(".defaultPreset("),
                                file.getFileName() + " builds a SideConfig without a default preset");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static String contentName(String family) {
        return Character.toUpperCase(family.charAt(0)) + family.substring(1) + "Content";
    }

    private static Path machineDir() {
        return srcRoot().resolve("machine");
    }

    /** The shared main source root, found by walking up from the test's working directory. */
    private static Path srcRoot() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            Path candidate = p.resolve("common/src/main/java/za/co/neroland/nerotech");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("shared source root not found above " + dir);
    }

    private static String read(Path file) {
        try {
            return Files.readString(file).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
