package za.co.neroland.nerotech.link;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import za.co.neroland.nerotech.NeroTechCommon;

/**
 * Reads NeroTech's wiki pages from the runtime classpath so the NeroLink {@code wiki} section can
 * serve them in-app (WIKI CONTRACT v1). The in-repo {@code wiki/*.md} pages are bundled under
 * {@code assets/nerotech/wiki/} by a Gradle copy step (see each loader's {@code build.gradle}), which
 * also writes a generated {@code index.json} of {@code {slug,title}} entries — classpath directory
 * listing is unreliable across loaders, so the index is the authoritative page list.
 *
 * <p>All content here is <b>public</b>: the pages are the same for every player and carry no personal
 * data, so the {@code wiki} section ignores {@code playerId}. If the generated index is missing (e.g.
 * a build without the copy step, or a plain-JVM unit test) a hardcoded fallback list of the known
 * slugs is used, with titles derived from the slug — the index still works, only page CONTENT needs
 * the bundled resources.
 */
public final class WikiLibrary {

    /** Classpath base for the bundled wiki resources. */
    private static final String BASE = "/assets/nerotech/wiki/";

    /** One wiki page's directory entry: its slug (filename without {@code .md}) and display title. */
    public record Page(String slug, String title) {
    }

    /**
     * Fallback page list, used only when the generated {@code index.json} is absent. Slugs match the
     * committed {@code wiki/*.md} filenames; titles are derived from the slug. Keep in rough sync with
     * the {@code wiki/} folder — it is a resilience fallback, not the source of truth.
     */
    private static final List<String> FALLBACK_SLUGS = List.of(
            "Home", "Machines", "Materials-and-Components", "Tech-Guide", "Thermal-System",
            "Consequence-Systems", "Pollution-and-Mitigation", "Analytics", "Overclock-Presets",
            "Side-Config-and-Configurator", "Advanced-Tier", "Fusion-Reactor", "Automation",
            "Fluids-and-Gases", "Particle-Collider", "Power-Generation", "Exotic-Endgame",
            "Planets", "Power-and-NeroPower-Split", "Companion-App");

    private static volatile List<Page> cachedIndex;

    private WikiLibrary() {
    }

    /** The wiki page index (slug + title), loaded once from the generated {@code index.json}. */
    public static List<Page> pages() {
        List<Page> local = cachedIndex;
        if (local == null) {
            synchronized (WikiLibrary.class) {
                local = cachedIndex;
                if (local == null) {
                    local = loadIndex();
                    cachedIndex = local;
                }
            }
        }
        return local;
    }

    /** The display title for {@code slug}, or the slug with hyphens as spaces if unknown. */
    public static String titleOf(String slug) {
        for (Page page : pages()) {
            if (page.slug().equals(slug)) {
                return page.title();
            }
        }
        return slug.replace('-', ' ');
    }

    /** Whether {@code slug} is a known, servable page. */
    public static boolean has(String slug) {
        if (!isSafeSlug(slug)) {
            return false;
        }
        for (Page page : pages()) {
            if (page.slug().equals(slug)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The raw markdown of a page, or empty if the slug is unknown/unsafe or the resource is not
     * bundled. Only slugs present in {@link #pages()} are ever read (no path traversal).
     */
    public static Optional<String> content(String slug) {
        if (!has(slug)) {
            return Optional.empty();
        }
        return readResource(BASE + slug + ".md");
    }

    // --- internals ----------------------------------------------------------

    private static List<Page> loadIndex() {
        Optional<String> json = readResource(BASE + "index.json");
        if (json.isPresent()) {
            try {
                JsonElement root = JsonParser.parseString(json.get());
                if (root.isJsonObject()) {
                    JsonArray arr = root.getAsJsonObject().getAsJsonArray("pages");
                    if (arr != null) {
                        List<Page> out = new ArrayList<>(arr.size());
                        for (JsonElement el : arr) {
                            JsonObject obj = el.getAsJsonObject();
                            String slug = obj.get("slug").getAsString();
                            if (!isSafeSlug(slug)) {
                                continue;
                            }
                            String title = obj.has("title") && !obj.get("title").isJsonNull()
                                    ? obj.get("title").getAsString() : slug.replace('-', ' ');
                            out.add(new Page(slug, title));
                        }
                        if (!out.isEmpty()) {
                            return List.copyOf(out);
                        }
                    }
                }
            } catch (RuntimeException e) {
                NeroTechCommon.LOGGER.warn("[NeroTech] Could not parse bundled wiki index.json; using fallback.", e);
            }
        }
        // Fallback: derive the index from the known slug list.
        List<Page> out = new ArrayList<>(FALLBACK_SLUGS.size());
        for (String slug : FALLBACK_SLUGS) {
            out.add(new Page(slug, slug.replace('-', ' ')));
        }
        return List.copyOf(out);
    }

    private static Optional<String> readResource(String path) {
        try (InputStream in = WikiLibrary.class.getResourceAsStream(path)) {
            if (in == null) {
                return Optional.empty();
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                char[] buf = new char[4096];
                int read;
                while ((read = reader.read(buf)) != -1) {
                    sb.append(buf, 0, read);
                }
            }
            return Optional.of(sb.toString());
        } catch (IOException e) {
            NeroTechCommon.LOGGER.warn("[NeroTech] Could not read wiki resource {}", path, e);
            return Optional.empty();
        }
    }

    /** Slugs are single path segments of the safe charset — never a path traversal. */
    private static boolean isSafeSlug(String slug) {
        if (slug == null || slug.isEmpty() || slug.length() > 128) {
            return false;
        }
        for (int i = 0; i < slug.length(); i++) {
            char c = slug.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
