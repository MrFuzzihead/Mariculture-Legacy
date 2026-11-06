package mariculture.mixins.early;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.FMLModContainer;
import cpw.mods.fml.common.MetadataCollection;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import mariculture.core.lib.LegacyMaricultureRange;

/**
 * Makes legacy {@code 1.7.10-}-prefixed Mariculture version ranges satisfiable by the
 * current bare-semver release line.
 *
 * <p>
 * This has to be an early mixin: FML builds each mod's dependency lists in
 * {@link FMLModContainer#bindMetadata(MetadataCollection)} during mod discovery and checks
 * them in {@code Loader.sortModList()} immediately afterwards, before any mod is constructed
 * and therefore before late mixins are registered. Rewriting the parsed
 * {@link ModMetadata} lists at the tail of {@code bindMetadata} runs in time, preserves
 * load ordering, and also covers mods that supply dependencies via {@code mcmod.info}
 * ({@code useDependencyInformation=true}), which populate the same lists.
 *
 * <p>
 * Early mixins cannot be gated on a target mod being present (only core mods are known at
 * this phase), so this loads unconditionally and no-ops unless a mod declares an
 * open-ended legacy-prefixed Mariculture range. See {@link LegacyMaricultureRange} for the
 * rewrite rules.
 */
@Mixin(value = FMLModContainer.class, remap = false)
public abstract class MixinFMLModContainer {

    @Shadow
    private ModMetadata modMetadata;

    @Inject(method = "bindMetadata", at = @At("TAIL"))
    private void mariculture$stripLegacyPrefixedMaricultureRanges(MetadataCollection mc, CallbackInfo ci) {
        mariculture$rewriteSet(modMetadata.requiredMods);
        mariculture$rewriteList(modMetadata.dependencies);
        mariculture$rewriteList(modMetadata.dependants);
    }

    @Unique
    private void mariculture$rewriteSet(Set<ArtifactVersion> set) {
        for (ArtifactVersion original : new ArrayList<>(set)) {
            ArtifactVersion rewritten = LegacyMaricultureRange.rewrite(original);
            if (rewritten != original) {
                mariculture$logRewrite(original);
                set.remove(original);
                set.add(rewritten);
            }
        }
    }

    @Unique
    private void mariculture$rewriteList(List<ArtifactVersion> list) {
        for (int i = 0; i < list.size(); i++) {
            ArtifactVersion rewritten = LegacyMaricultureRange.rewrite(list.get(i));
            if (rewritten != list.get(i)) {
                mariculture$logRewrite(list.get(i));
                list.set(i, rewritten);
            }
        }
    }

    @Unique
    private void mariculture$logRewrite(ArtifactVersion original) {
        FMLLog.info("[Mariculture] Rewriting legacy version range %s to unbounded Mariculture (any version)", original);
    }
}
