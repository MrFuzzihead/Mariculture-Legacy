package mariculture.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // Replaces the vanilla fishing rod and fish items with Mariculture's versions.
    // Previously done by the mariculture.fishery.VanillaOverride ASM coremod.
    VANILLA_ITEMS(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("MixinItem"));

    // NOTE: the legacy "1.7.10-"-prefixed Mariculture range fix (issue #4) is not a
    // mixin. Every FML class on the dependency-parsing path is already loaded before
    // early mixins are registered in production packs, so Mixin refuses to target them
    // (MixinTargetAlreadyLoadedException). It is implemented as a LaunchWrapper ASM
    // transformer instead: see VersionParserTransformer, registered from
    // EarlyMixinsLoader.getASMTransformerClass(), with rules in LegacyMaricultureRange.

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
