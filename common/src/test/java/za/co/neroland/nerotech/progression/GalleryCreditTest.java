package za.co.neroland.nerotech.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Gallery exhibits credit nothing: the decision, and that the machine base routes every credit through it. */
class GalleryCreditTest {

    private static final UUID OPERATOR = new UUID(1L, 2L);

    @Test
    void anExhibitHasNoCreditableOperator() {
        assertNull(OperatorCredit.creditable(true, OPERATOR));
        assertNull(OperatorCredit.creditable(true, null));
        assertEquals(OPERATOR, OperatorCredit.creditable(false, OPERATOR));
        // With no operator every credit path is already a no-op (MilestoneGates.credit returns false on null).
        assertTrue(!OperatorCredit.meltdownAverted(4, true, OperatorCredit.creditable(true, OPERATOR)));
        assertTrue(!OperatorCredit.vaultOperated(64, OperatorCredit.creditable(true, OPERATOR), null));
    }

    /** The base class gates lastOperator() and setOwner() on the saved flag, so a reload keeps exhibits silent. */
    @Test
    void theMachineBaseHonoursTheFlag() throws Exception {
        String src = Files.readString(root().resolve("machine/NeroTechMachineBlockEntity.java"));
        assertTrue(src.contains("OperatorCredit.creditable(this.galleryExhibit, this.lastOperator)"));
        assertTrue(src.contains("this.ownerId = this.galleryExhibit ? null : owner;"));
        assertTrue(src.contains("output.putBoolean(\"GalleryExhibit\", true);"));
        assertTrue(src.contains("input.getBooleanOr(\"GalleryExhibit\", false)"));
        String gallery = Files.readString(root().resolve("command/NeroTechCommands.java"));
        assertTrue(gallery.contains("GalleryPlant.markExhibits(level, origin);"));
    }

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
}
