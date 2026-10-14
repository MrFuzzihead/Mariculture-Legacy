package mariculture.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // Replaces the vanilla fishing rod and fish items with Mariculture's versions.
    // Previously done by the mariculture.fishery.VanillaOverride ASM coremod.
    VANILLA_ITEMS(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("MixinItem"));

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
