package mariculture.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // Replaces the vanilla fishing rod and fish items with Mariculture's versions.
    // Previously done by the mariculture.fishery.VanillaOverride ASM coremod.
    VANILLA_ITEMS(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("MixinItem")),

    // Rewrites legacy "1.7.10-"-prefixed Mariculture version ranges (e.g. RTG's
    // after:Mariculture@[1.7.10-1.2.4.2a-5,)) to unbounded, so bare-semver Mariculture
    // releases satisfy them. Early phase: FML checks dependencies during mod discovery,
    // before late mixins exist. Cannot be gated on RTG's presence in the early phase,
    // but no-ops unless a mod declares such a range.
    FML_DEPENDENCY_COMPAT(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("MixinFMLModContainer"));

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
