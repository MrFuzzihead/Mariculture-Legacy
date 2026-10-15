package mariculture.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Guards the English lang file and the names that were previously missing from it.
 *
 * <p>
 * The missing keys were found by dumping every registered Mariculture/MariTech item and block through
 * {@code StatCollector} on a dedicated server; a unit test cannot rebuild the registries, so this keeps the file
 * well formed and pins the keys that were added.
 */
public class LangFileTest {

    private static final String LANG = "/assets/mariculture/lang/en_US.lang";

    /** Keys that previously resolved to their raw unlocalised name. */
    private static final String[] REQUIRED = { "mariculture.ring", "mariculture.bracelet", "mariculture.necklace",
        "mariculture.limestone.pillar2", "mariculture.limestone.pillar3", "mariculture.limestone.pedestal2",
        "mariculture.limestone.pedestal3", "mariculture.limestone.pedestal4", "mariculture.limestone.pedestal5",
        "mariculture.limestone.pedestal6", "mariculture.air.naturalgas", "mariculture.plans.unnamed",
        "tile.ender.molten.name", "tile.blood.name", "tile.customSlabsDouble", "mariculture.tile.customSlabsDouble",
        "mariculture.tooltip.vat", "mariculture.tooltip.pressurevessel" };

    private static Set<String> readKeys() throws Exception {
        InputStream in = LangFileTest.class.getResourceAsStream(LANG);
        assertNotNull(LANG + " is not on the test classpath", in);

        Set<String> keys = new HashSet<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            int equals = line.indexOf('=');
            assertTrue("malformed lang line: " + line, equals > 0);
            String key = line.substring(0, equals);
            String value = line.substring(equals + 1);
            assertFalse(
                "empty value for " + key,
                value.trim()
                    .isEmpty());
            assertTrue("duplicate lang key " + key, keys.add(key));
        }
        reader.close();
        return keys;
    }

    @Test
    public void langFileIsWellFormed() throws Exception {
        assertTrue("expected a populated lang file", readKeys().size() > 500);
    }

    @Test
    public void previouslyMissingNamesAreLocalised() throws Exception {
        Set<String> keys = readKeys();
        for (String key : REQUIRED) {
            assertTrue("missing lang key " + key, keys.contains(key));
        }
    }
}
