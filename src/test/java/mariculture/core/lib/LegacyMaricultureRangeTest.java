package mariculture.core.lib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;

/**
 * Tests for the legacy Mariculture version-range rewrite used by {@code VersionParserTransformer}
 * (see issue #4).
 *
 * <p>
 * The regression being pinned down: RTG declares
 * {@code after:Mariculture@[1.7.10-1.2.4.2a-5,)}, which FML's token-based comparison
 * treats as newer than bare-semver {@code 1.3.3}, aborting startup. The patch maps such
 * ranges to FML's unbounded form, which accepts any Mariculture version.
 */
public class LegacyMaricultureRangeTest {

    @Test
    public void rtgRangeIsRewrittenToUnbounded() {
        ArtifactVersion original = VersionParser.parseVersionReference("Mariculture@[1.7.10-1.2.4.2a-5,)");
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
        ArtifactVersion original = VersionParser.parseVersionReference("Mariculture@[1.7.10-1.2.4.2a-5,)");
        assertTrue(!original.containsVersion(new DefaultArtifactVersion("Mariculture", "1.3.3")));
    }

    @Test
    public void exclusiveLowerBoundIsAlsoRewritten() {
        ArtifactVersion original = VersionParser.parseVersionReference("Mariculture@(1.7.10-1.2.4.2a-5,)");
        ArtifactVersion rewritten = LegacyMaricultureRange.rewrite(original);

        assertTrue(rewritten != original);
        assertTrue(rewritten.containsVersion(new DefaultArtifactVersion("Mariculture", "1.3.3")));
    }

    @Test
    public void boundedUpperRangeIsLeftAlone() {
        // Dropping an upper bound would silently widen a real constraint.
        ArtifactVersion original = VersionParser.parseVersionReference("Mariculture@[1.7.10-1.2.4.2a-5,1.7.10-2.0)");
        assertSame(original, LegacyMaricultureRange.rewrite(original));
    }

    @Test
    public void nonPrefixedMaricultureRangeIsLeftAlone() {
        ArtifactVersion original = VersionParser.parseVersionReference("Mariculture@[1.2.4,)");
        assertSame(original, LegacyMaricultureRange.rewrite(original));

        ArtifactVersion exact = VersionParser.parseVersionReference("Mariculture@[1.3.3]");
        assertSame(exact, LegacyMaricultureRange.rewrite(exact));
    }

    @Test
    public void otherModsPrefixedRangesAreLeftAlone() {
        // e.g. RTG's Growthcraft / Lots of Mobs entries carry the same legacy prefix
        // but are not Mariculture's to rewrite.
        ArtifactVersion growthcraft = VersionParser.parseVersionReference("Growthcraft|Bamboo@[1.7.10-2.5.0,)");
        assertSame(growthcraft, LegacyMaricultureRange.rewrite(growthcraft));

        ArtifactVersion lom = VersionParser.parseVersionReference("lom@[1.7.10-3.2.0,)");
        assertSame(lom, LegacyMaricultureRange.rewrite(lom));
    }

    @Test
    public void maricultureAsSubstringOfAnotherModIdIsLeftAlone() {
        ArtifactVersion original = VersionParser.parseVersionReference("MaricultureFoo@[1.7.10-1.0,)");
        assertSame(original, LegacyMaricultureRange.rewrite(original));
    }

    @Test
    public void unversionedReferenceIsLeftAlone() {
        ArtifactVersion original = VersionParser.parseVersionReference("Mariculture");
        assertSame(original, LegacyMaricultureRange.rewrite(original));
        // Already accepts anything.
        assertTrue(original.containsVersion(new DefaultArtifactVersion("Mariculture", "1.3.3")));
    }

    @Test
    public void nullIsLeftAlone() {
        assertSame(null, LegacyMaricultureRange.rewrite(null));
    }

    @Test
    public void rewriteModMetadataRewritesAllThreeLists() {
        // This is what the transformer calls for each discovered mod.
        ModMetadata metadata = new ModMetadata();
        metadata.requiredMods.add(VersionParser.parseVersionReference("Mariculture@[1.7.10-1.2.4.2a-5,)"));
        metadata.dependencies.add(VersionParser.parseVersionReference("Mariculture@[1.7.10-1.2.4.2a-5,)"));
        metadata.dependencies.add(VersionParser.parseVersionReference("Forge@[10.13.4.1614,)"));
        metadata.dependants.add(VersionParser.parseVersionReference("Mariculture@(1.7.10-1.2.4.2a-5,)"));

        LegacyMaricultureRange.rewriteModMetadata(metadata);

        assertEquals(1, metadata.requiredMods.size());
        ArtifactVersion required = metadata.requiredMods.iterator()
            .next();
        assertTrue(required.containsVersion(new DefaultArtifactVersion("Mariculture", "1.3.3")));

        assertEquals(2, metadata.dependencies.size());
        for (ArtifactVersion dependency : metadata.dependencies) {
            if ("Mariculture".equals(dependency.getLabel())) {
                assertTrue(dependency.containsVersion(new DefaultArtifactVersion("Mariculture", "1.3.3")));
            } else {
                // Other mods' ranges keep their floor.
                assertEquals("Forge", dependency.getLabel());
                assertNotNull(dependency.getRangeString());
            }
        }

        assertEquals(1, metadata.dependants.size());
        assertTrue(
            metadata.dependants.get(0)
                .containsVersion(new DefaultArtifactVersion("Mariculture", "1.3.3")));

        // Idempotent: the transformer patches both bindMetadata and the discoverers,
        // so the same metadata can be rewritten twice.
        LegacyMaricultureRange.rewriteModMetadata(metadata);
        assertEquals(1, metadata.requiredMods.size());
        assertEquals(2, metadata.dependencies.size());
        assertEquals(1, metadata.dependants.size());
    }

    @Test
    public void rewriteModMetadataToleratesNull() {
        LegacyMaricultureRange.rewriteModMetadata(null);
        LegacyMaricultureRange.rewriteModContainers(null);
    }

    @Test
    public void stringDetectionMatchesArtifactVersionRewrite() {
        String reference = "Mariculture@[1.7.10-1.2.4.2a-5,)";

        assertTrue(LegacyMaricultureRange.isLegacyPrefixedMaricultureRange(reference));
        assertEquals("Mariculture", LegacyMaricultureRange.rewriteReference(reference));
        assertFalse(LegacyMaricultureRange.isLegacyPrefixedMaricultureRange("Mariculture@[1.2.4,)"));
        assertFalse(LegacyMaricultureRange.isLegacyPrefixedMaricultureRange(null));
        assertFalse(LegacyMaricultureRange.isLegacyPrefixedMaricultureRange(""));
    }
}
