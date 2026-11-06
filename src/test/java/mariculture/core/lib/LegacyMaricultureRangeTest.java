package mariculture.core.lib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;

/**
 * Tests for the legacy Mariculture version-range rewrite used by
 * {@code MixinFMLModContainer} (see issue #4).
 *
 * <p>
 * The regression being pinned down: RTG declares
 * {@code after:Mariculture@[1.7.10-1.2.4.2a-5,)}, which FML's token-based comparison
 * treats as newer than bare-semver {@code 1.3.3}, aborting startup. The rewrite maps such
 * ranges to FML's unbounded form, which accepts any Mariculture version.
 */
public class LegacyMaricultureRangeTest {

    private static ArtifactVersion parse(String reference) {
        return VersionParser.parseVersionReference(reference);
    }

    @Test
    public void rtgRangeIsRewrittenToUnbounded() {
        ArtifactVersion original = parse("Mariculture@[1.7.10-1.2.4.2a-5,)");
        ArtifactVersion rewritten = LegacyMaricultureRange.rewrite(original);

        assertTrue(rewritten != original);
        assertEquals("Mariculture", rewritten.getLabel());
        // The whole point: bare-semver 1.3.3 must now satisfy the requirement.
        assertTrue(rewritten.containsVersion(new DefaultArtifactVersion("Mariculture", "1.3.3")));
        // And so must the old line it claimed to require.
        assertTrue(rewritten.containsVersion(new DefaultArtifactVersion("Mariculture", "1.7.10-1.2.4.2a-5")));
        assertEquals("Mariculture", rewritten.toString());
    }

    @Test
    public void originalRtgRangeRejectsBareSemver() {
        // Documents the bug: without the rewrite, FML's comparison fails.
        ArtifactVersion original = parse("Mariculture@[1.7.10-1.2.4.2a-5,)");
        assertTrue(!original.containsVersion(new DefaultArtifactVersion("Mariculture", "1.3.3")));
    }

    @Test
    public void exclusiveLowerBoundIsAlsoRewritten() {
        ArtifactVersion original = parse("Mariculture@(1.7.10-1.2.4.2a-5,)");
        ArtifactVersion rewritten = LegacyMaricultureRange.rewrite(original);

        assertTrue(rewritten != original);
        assertTrue(rewritten.containsVersion(new DefaultArtifactVersion("Mariculture", "1.3.3")));
    }

    @Test
    public void boundedUpperRangeIsLeftAlone() {
        // Dropping an upper bound would silently widen a real constraint.
        ArtifactVersion original = parse("Mariculture@[1.7.10-1.2.4.2a-5,1.7.10-2.0)");
        assertSame(original, LegacyMaricultureRange.rewrite(original));
    }

    @Test
    public void nonPrefixedMaricultureRangeIsLeftAlone() {
        ArtifactVersion original = parse("Mariculture@[1.2.4,)");
        assertSame(original, LegacyMaricultureRange.rewrite(original));

        ArtifactVersion exact = parse("Mariculture@[1.3.3]");
        assertSame(exact, LegacyMaricultureRange.rewrite(exact));
    }

    @Test
    public void otherModsPrefixedRangesAreLeftAlone() {
        // e.g. RTG's Growthcraft / Lots of Mobs entries carry the same legacy prefix
        // but are not Mariculture's to rewrite.
        ArtifactVersion growthcraft = parse("Growthcraft|Bamboo@[1.7.10-2.5.0,)");
        assertSame(growthcraft, LegacyMaricultureRange.rewrite(growthcraft));

        ArtifactVersion lom = parse("lom@[1.7.10-3.2.0,)");
        assertSame(lom, LegacyMaricultureRange.rewrite(lom));
    }

    @Test
    public void unversionedReferenceIsLeftAlone() {
        ArtifactVersion original = parse("Mariculture");
        assertSame(original, LegacyMaricultureRange.rewrite(original));
        // Already accepts anything.
        assertTrue(original.containsVersion(new DefaultArtifactVersion("Mariculture", "1.3.3")));
    }

    @Test
    public void nullIsLeftAlone() {
        assertSame(null, LegacyMaricultureRange.rewrite(null));
    }
}
