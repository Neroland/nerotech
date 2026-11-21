package za.co.neroland.nerotech.compliance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * POPIA/GDPR source proof for 0.8.0 ("this release adds no player-keyed store"). A pragmatic regex scan of
 * the shared and loader sources: every place that could persist a player's identity, every SavedData, every
 * data component and every UUID-keyed map must be one already reviewed. A new one fails here so it gets a
 * privacy review (and a PRIVACY.md entry) before it ships, instead of slipping in.
 *
 * <p>The allowlist is the pre-0.8.0 opt-in set: the placing player's UUID on machines (operating charges and
 * pollution attribution), pollution attribution rows, the attribution opt-out and pending-erasure sets, and
 * the Tech Guide "seen" masks — all erased through Core's shared hook. Plus the in-memory link state (handles,
 * rate windows, alarm throttle) and the quest hook's online set, which are never written to disk.
 */
class NoNewPlayerKeyedDataTest {

    /** Files that may persist a player UUID (pre-existing, opt-in or erasable; see PRIVACY.md). */
    private static final Set<String> IDENTITY_PERSISTENCE_ALLOWED = Set.of(
            "machine/NeroTechMachineBlockEntity.java",      // OwnerMost/OwnerLeast (opt-in owner)
            "pollution/PollutionState.java",                // opt-in attribution rows
            "pollution/PollutionAttributionPrefs.java",     // opt-out set + pending owner erasure
            "guide/TechGuideSeenState.java",                // Tech Guide seen masks
            "progression/QuestHooks.java",                  // parses a UUID from a Core event; in memory only
            "machine/utility/UtilitySaveKeys.java");        // the save-key detector's own word list

    /** Identity persistence, or a player's name taken as data. */
    private static final Pattern IDENTITY = Pattern.compile(
            "putUUID|UUIDUtil\\.|UUID\\.fromString|getMostSignificantBits|getLeastSignificantBits"
                    + "|getGameProfile|getScoreboardName|getPlainTextName|\"Owner|\"owner\"|\"uuid\"|\"player\"");

    /** The reviewed SavedData stores. A new one needs a row in the Stage 6 store table and PRIVACY.md. */
    private static final Set<String> SAVED_DATA = Set.of(
            "deposit/DepositState.java",
            "command/gallery/GalleryState.java",             // gallery sites: dimension + origin, no player
            "pollution/PollutionState.java",
            "pollution/PollutionAttributionPrefs.java",
            "guide/TechGuideSeenState.java");

    /** Files holding a UUID-keyed map or set as a field (persisted ones above, the rest in memory). */
    private static final Set<String> UUID_KEYED_FIELDS = Set.of(
            "pollution/PollutionState.java",
            "pollution/PollutionAttributionPrefs.java",
            "guide/TechGuideSeenState.java",
            "progression/QuestHooks.java",
            "link/NearbyHandles.java",
            "link/LinkRateLimiter.java",
            "link/AlarmThrottle.java");

    /** Every registered data component (item stack data): tool state, tank and vault contents only. */
    private static final Set<String> DATA_COMPONENTS = Set.of(
            "configurator_state", "vault_contents", "tank_contents", "tool_charge", "tool_charged",
            "fitted_lens", "fitted_filter", "drill_mode", "carried_machine", "survey_scanner_state",
            // An int: operations a drill head has used. No player data.
            "drill_head_wear");

    private static final Pattern SAVED_DATA_DECL = Pattern.compile("extends\\s+SavedData\\b");
    private static final Pattern UUID_FIELD = Pattern.compile(
            "^\\s*(?:(?:private|protected|public|static|final)\\s+)+(?:Map|Set|HashMap|HashSet|ConcurrentHashMap)<UUID\\b[^;(]*[=;]",
            Pattern.MULTILINE);
    private static final Pattern COMPONENT = Pattern.compile("COMPONENTS\\.register\\(\\s*\"([a-z0-9_]+)\"");
    private static final Pattern LOG_CALL = Pattern.compile(
            "(?:LOGGER|LOG)\\s*\\.\\s*(info|warn|error)\\s*\\((.*?)\\);", Pattern.DOTALL);
    private static final Pattern LOG_IDENTITY = Pattern.compile(
            "getUUID|getStringUUID|UUID|getName\\(\\)|getGameProfile|getScoreboardName|getDisplayName"
                    + "|getBlockPos|BlockPos|\\bpos\\b|getX\\(|getY\\(|getZ\\(|blockPosition|ownerId|lastOperator"
                    + "|getMessage\\(\\)\\.getString|\\bmessage\\b|\\btext\\b");

    @Test
    void identityIsPersistedOnlyByTheReviewedOptInStores() {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaSources()) {
            String rel = rel(file);
            if (IDENTITY_PERSISTENCE_ALLOWED.contains(rel)) {
                continue;
            }
            String[] lines = read(file).split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String code = stripComment(line);
                // Stripping the owner keys (Arc Wrench pick-up, utility blocks) removes identity: allowed.
                if (code.contains(".remove(") || code.contains("STRIPPED_OWNER_KEYS")) {
                    continue;
                }
                if (IDENTITY.matcher(code).find()) {
                    offenders.add(rel + ":" + (i + 1) + ": " + line.trim());
                }
            }
        }
        assertTrue(offenders.isEmpty(), "unreviewed player-identity persistence:\n" + String.join("\n", offenders));
    }

    @Test
    void savedDataStoresAreTheReviewedOnes() {
        Set<String> found = new TreeSet<>();
        for (Path file : javaSources()) {
            if (SAVED_DATA_DECL.matcher(read(file)).find()) {
                found.add(rel(file));
            }
        }
        assertEquals(new TreeSet<>(SAVED_DATA), found, "SavedData set changed: review it for player data");
    }

    @Test
    void theDepositStoreIsWorldDataOnly() {
        String src = read(root().resolve("deposit/DepositState.java"));
        String code = stripComments(src);
        assertFalse(code.contains("UUID"), "deposits are chunk-keyed world data");
        assertFalse(code.toLowerCase(java.util.Locale.ROOT).contains("player"), "deposits hold no player");
    }

    @Test
    void uuidKeyedFieldsAreTheReviewedOnes() {
        Set<String> found = new TreeSet<>();
        for (Path file : javaSources()) {
            if (UUID_FIELD.matcher(stripComments(read(file))).find()) {
                found.add(rel(file));
            }
        }
        assertEquals(new TreeSet<>(UUID_KEYED_FIELDS), found, "a UUID-keyed map/set appeared: review it");
    }

    @Test
    void dataComponentsHoldOnlyToolAndContentState() {
        Set<String> found = new TreeSet<>();
        for (Path file : javaSources()) {
            Matcher m = COMPONENT.matcher(read(file));
            while (m.find()) {
                found.add(m.group(1));
            }
        }
        assertEquals(new TreeSet<>(DATA_COMPONENTS), found, "data component set changed: review it");
        for (String type : List.of("item/ConfiguratorState.java", "machine/VaultContents.java",
                "machine/tank/TankContents.java", "item/tools/ToolCharge.java", "item/tools/CarriedMachine.java",
                "machine/extraction/ScannerState.java")) {
            String code = stripComments(read(root().resolve(type)));
            assertFalse(code.contains("UUID") || code.contains("Player"), type + " holds player data");
        }
    }

    /** The Arc Wrench strips the owner before a machine rides on the item. */
    @Test
    void theCarriedMachineDropsTheOwner() {
        String code = stripComments(read(root().resolve("item/tools/ArcWrenchItem.java")));
        assertTrue(code.contains("tag.remove(\"OwnerMost\")") && code.contains("tag.remove(\"OwnerLeast\")"));
    }

    @Test
    void theTransientOperatorIsNeverSaved() {
        String code = stripComments(read(root().resolve("machine/NeroTechMachineBlockEntity.java")));
        Matcher m = Pattern.compile("\\.(put[A-Za-z]*|store[A-Za-z]*)\\([^;]*lastOperator").matcher(code);
        assertFalse(m.find(), "lastOperator is credit in the moment: never persisted");
    }

    /** Log sweep: nothing at INFO or above names a player, a UUID, a position or player-written text. */
    @Test
    void noIdentityInLogsAtInfoOrAbove() {
        List<String> offenders = new ArrayList<>();
        for (Path file : javaSources()) {
            String code = stripComments(read(file));
            Matcher m = LOG_CALL.matcher(code);
            while (m.find()) {
                String args = m.group(2);
                // Only the arguments after the format string can carry data.
                int fmtEnd = args.lastIndexOf('"');
                String data = fmtEnd >= 0 ? args.substring(fmtEnd + 1) : args;
                if (LOG_IDENTITY.matcher(data).find()) {
                    offenders.add(rel(file) + ": " + m.group().replaceAll("\\s+", " "));
                }
            }
        }
        assertTrue(offenders.isEmpty(), "identity in INFO+ logs:\n" + String.join("\n", offenders));
    }

    // --- helpers ------------------------------------------------------------------------------------------------

    private static List<Path> javaSources() {
        List<Path> out = new ArrayList<>();
        Path repo = root();
        for (int i = 0; i < 8; i++) { // .../common/src/main/java/za/co/neroland/nerotech -> repo root
            repo = repo.getParent();
        }
        for (String loader : List.of("common", "fabric", "forge", "neoforge")) {
            Path dir = repo.resolve(loader).resolve("src/main/java");
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> s = Files.walk(dir)) {
                s.filter(p -> p.toString().endsWith(".java")).forEach(out::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        assertFalse(out.isEmpty(), "no sources found");
        return out;
    }

    /** Path relative to the {@code nerotech} package root (loader sources keep their own prefix). */
    private static String rel(Path file) {
        String s = file.toString().replace('\\', '/');
        int i = s.indexOf("/za/co/neroland/nerotech/");
        return i >= 0 ? s.substring(i + "/za/co/neroland/nerotech/".length()) : s;
    }

    /** The shared main source package root, found by walking up from the test's working directory. */
    private static Path root() {
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

    private static String stripComment(String line) {
        String t = line.trim();
        if (t.startsWith("*") || t.startsWith("/*") || t.startsWith("//")) {
            return "";
        }
        int i = line.indexOf("//");
        return i >= 0 && !line.substring(0, i).contains("\"") ? line.substring(0, i) : line;
    }

    private static String stripComments(String src) {
        String noBlock = src.replaceAll("(?s)/\\*.*?\\*/", "");
        StringBuilder sb = new StringBuilder();
        for (String line : noBlock.split("\n")) {
            sb.append(stripComment(line)).append('\n');
        }
        return sb.toString();
    }
}
