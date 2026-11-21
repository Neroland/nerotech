package za.co.neroland.nerotech.guide;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The Tech Guide table shape: chapters fit the menu's 16-bit masks, step ids are unique (they are lang
 * keys), every step's advancement ships in this jar, every milestone step has its gate, and every 0.8.0
 * block and tool has an entry.
 */
class TechGuideTableTest {

    static final List<String> NEW_ENTRIES = List.of("plasma_crucible", "plate_press", "filament_spooler",
            "blast_foundry", "foundry_brick", "foundry_heat_port", "centrifuge", "cryo_distiller",
            "atmospheric_condenser", "reaction_vat", "process_tank", "component_forge", "laser_etcher",
            "crystal_grower", "heat_pipe", "heat_exchanger", "deep_core_drill", "drill_derrick", "fuel_injector",
            "europium_flux_lattice", "induction_charger", "annunciator_panel", "power_drill", "cutting_lance",
            "arc_wrench", "multimeter", "survey_scanner", "thermal_goggles", "filter_mask");

    @Test
    void tableShape() {
        ClassLoader cl = TechGuideTableTest.class.getClassLoader();
        URL marker = cl.getResource("data/nerotech/advancement/root.json");
        assumeTrue(marker != null, "resources not on the test classpath");
        List<String> problems = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (TechGuide.Chapter chapter : TechGuide.CHAPTERS) {
            if (chapter.steps().isEmpty() || chapter.steps().size() > 16) {
                problems.add(chapter.id() + ": " + chapter.steps().size() + " steps (1..16)");
            }
            for (TechGuide.Step step : chapter.steps()) {
                if (!ids.add(step.id())) {
                    problems.add("duplicate step id " + step.id());
                }
                String path = "data/" + step.advancement().getNamespace() + "/advancement/"
                        + step.advancement().getPath() + ".json";
                if (cl.getResource(path) == null) {
                    problems.add(step.id() + ": no " + path);
                }
            }
        }
        for (String milestone : TechGuide.MILESTONE_STEPS) {
            if (!ids.contains(milestone)) {
                problems.add("milestone " + milestone + " has no step");
            }
            if (cl.getResource("data/nerotech/neroland_gates/" + milestone + ".json") == null) {
                problems.add("milestone " + milestone + " has no gate file");
            }
        }
        for (String entry : NEW_ENTRIES) {
            if (!ids.contains(entry)) {
                problems.add("no Tech Guide entry for " + entry);
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
