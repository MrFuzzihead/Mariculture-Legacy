package mariculture.mixins.early;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.discovery.ModDiscoverer;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import mariculture.core.lib.LegacyMaricultureRange;

/**
 * Makes legacy {@code 1.7.10-}-prefixed Mariculture version ranges satisfiable by the
 * current bare-semver release line.
 *
 * <p>
 * This has to be an early mixin: FML builds each mod's dependency lists during mod
 * discovery ({@code JarDiscoverer} / {@code DirectoryDiscoverer} call
 * {@code ModContainer.bindMetadata}, which parses the {@code @Mod} annotation via
 * {@code Loader.computeDependencies} or {@code mcmod.info} via {@code MetadataCollection})
 * and checks them in {@code Loader.sortModList()} immediately after
 * {@code ModDiscoverer.identifyMods()} returns — before any mod is constructed and
 * therefore before late mixins are registered. Rewriting the parsed {@link ModMetadata}
 * lists at the return of {@code identifyMods} runs in time, preserves load ordering, and
 * covers both the annotation and {@code mcmod.info} paths, since both populate the same
 * lists.
 *
 * <p>
 * The target matters: {@code FMLModContainer}, {@code Loader}, and the
 * {@code versioning} classes are already loaded by the time GTNHMixins registers early
 * mixins (mixing them in throws {@code MixinTargetAlreadyLoadedException}), whereas
 * {@code ModDiscoverer} is only loaded when {@code Loader.loadMods()} starts discovering
 * mods — after early mixins are registered. It is also the single funnel for every
 * discovered mod, regardless of whether it came from a jar or a directory.
 *
 * <p>
 * Early mixins cannot be gated on a target mod being present (only core mods are known at
 * this phase), so this loads unconditionally and no-ops unless a mod declares an
 * open-ended legacy-prefixed Mariculture range. See {@link LegacyMaricultureRange} for the
 * rewrite rules.
 */
@Mixin(value = ModDiscoverer.class, remap = false)
public abstract class MixinModDiscoverer {

    @Inject(method = "identifyMods", at = @At("RETURN"))
    private void mariculture$stripLegacyPrefixedMaricultureRanges(CallbackInfoReturnable<List<ModContainer>> cir) {
        List<ModContainer> modList = cir.getReturnValue();
        if (modList == null) {
            return;
        }
        for (ModContainer container : modList) {
            ModMetadata metadata = container.getMetadata();
            if (metadata == null) {
                continue;
            }
            mariculture$rewriteSet(metadata.requiredMods);
            mariculture$rewriteList(metadata.dependencies);
            mariculture$rewriteList(metadata.dependants);
        }
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
