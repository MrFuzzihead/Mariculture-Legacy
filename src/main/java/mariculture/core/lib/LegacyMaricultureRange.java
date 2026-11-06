package mariculture.core.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;

/**
 * Detects and rewrites legacy {@code 1.7.10-}-prefixed Mariculture version ranges in
 * FML dependency lists.
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
 * ranges for other mods and unversioned references.
 *
 * <p>
 * Called from the patched {@code FMLModContainer.bindMetadata} and discoverer methods
 * (see {@code mariculture.mixins.VersionParserTransformer}); the transformer is generated
 * bytecode, so all of the logic lives here and is unit-testable.
 */
public final class LegacyMaricultureRange {

    private LegacyMaricultureRange() {}

    /**
     * Returns FML's unbounded Mariculture version if {@code version} is an open-ended
     * legacy-prefixed Mariculture range, otherwise returns it unchanged (same instance).
     */
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
        try {
            FMLLog.info(
                "[Mariculture] Rewriting legacy version range %s to unbounded Mariculture (any version)",
                version);
        } catch (Throwable ignored) {
            // FML logging is not always initialised when this runs (e.g. unit tests);
            // the rewrite itself must never fail because of a log line.
        }
        return new DefaultArtifactVersion(MCModInfo.MODID, true);
    }

    /** Rewrites the dependency lists of one mod's metadata in place. */
    public static void rewriteModMetadata(ModMetadata metadata) {
        if (metadata == null) {
            return;
        }
        rewriteSet(metadata.requiredMods);
        rewriteList(metadata.dependencies);
        rewriteList(metadata.dependants);
    }

    /** Rewrites the dependency lists of every discovered mod in place. */
    public static void rewriteModContainers(List<ModContainer> containers) {
        if (containers == null) {
            return;
        }
        for (ModContainer container : containers) {
            if (container != null) {
                rewriteModMetadata(container.getMetadata());
            }
        }
    }

    private static void rewriteSet(Set<ArtifactVersion> set) {
        if (set == null) {
            return;
        }
        for (ArtifactVersion original : new ArrayList<>(set)) {
            ArtifactVersion rewritten = rewrite(original);
            if (rewritten != original) {
                set.remove(original);
                set.add(rewritten);
            }
        }
    }

    private static void rewriteList(List<ArtifactVersion> list) {
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            list.set(i, rewrite(list.get(i)));
        }
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
