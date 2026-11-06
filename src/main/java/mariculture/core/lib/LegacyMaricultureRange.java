package mariculture.core.lib;

import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;

/**
 * Detects legacy {@code 1.7.10-}-prefixed Mariculture version ranges in FML version
 * references.
 *
 * <p>
 * Legacy mods declare ranges such as RTG's {@code after:Mariculture@[1.7.10-1.2.4.2a-5,)}.
 * FML compares versions token-by-token, so the current bare-semver Mariculture version
 * ({@code 1.3.3}) sorts below {@code 1.7.10-1.2.4.2a-5} and FML aborts startup with a
 * missing-mods error. Rather than renumbering Mariculture to {@code 1.7.10-1.3.3}, such
 * ranges are rewritten to FML's unbounded form ({@code new DefaultArtifactVersion(
 * "Mariculture", true)}, i.e. "any version"), which preserves the load-ordering
 * relationship while dropping the bogus version floor.
 *
 * <p>
 * Only open-ended ranges ({@code ",)"} upper bound) whose lower bound carries the legacy
 * {@code 1.7.10-} prefix are rewritten. Bounded ranges keep their real meaning, as do
 * ranges for other mods and unversioned references. The original instance is returned
 * unchanged when no rewrite applies, so callers can detect a rewrite with {@code !=}.
 */
public final class LegacyMaricultureRange {

    private LegacyMaricultureRange() {}

    public static ArtifactVersion rewrite(ArtifactVersion version) {
        if (version == null || !MCModInfo.MODID.equals(version.getLabel())) {
            return version;
        }
        String range = version.getRangeString();
        if (range == null) {
            return version;
        }
        String trimmed = range.trim();
        // Open-ended ranges only, e.g. "[1.7.10-1.2.4.2a-5,)". A bounded upper limit
        // is a real constraint and must not be silently dropped.
        if (!trimmed.endsWith(",)") || !(trimmed.startsWith("[1.7.10-") || trimmed.startsWith("(1.7.10-"))) {
            return version;
        }
        return new DefaultArtifactVersion(MCModInfo.MODID, true);
    }

    public static boolean isLegacyPrefixedMaricultureRange(String labelledRef) {
        if (labelledRef == null || labelledRef.isEmpty()) {
            return false;
        }
        int at = labelledRef.indexOf('@');
        if (at < 0) {
            return false;
        }
        String label = labelledRef.substring(0, at)
            .trim();
        if (!MCModInfo.MODID.equals(label)) {
            return false;
        }
        String range = labelledRef.substring(at + 1)
            .trim();
        return range.endsWith(",)") && (range.startsWith("[1.7.10-") || range.startsWith("(1.7.10-"));
    }

    /**
     * Returns {@code "Mariculture"} (unversioned) if {@code labelledRef} is a legacy
     * prefixed Mariculture range, otherwise returns it unchanged.
     */
    public static String rewriteReference(String labelledRef) {
        return isLegacyPrefixedMaricultureRange(labelledRef) ? MCModInfo.MODID : labelledRef;
    }
}
