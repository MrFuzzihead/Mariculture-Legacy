package mariculture.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

/**
 * Mods that can be targeted by late mixins. Add an entry here and reference it from {@link Mixins} with
 * {@code addRequiredMod(...)} when a mixin should only apply while that mod is present.
 */
public enum TargetMods implements ITargetMod {

    BIOMESOPLENTY("BiomesOPlenty"),
    HIGHLANDS("Highlands"),
    THAUMCRAFT("Thaumcraft"),
    WAILA("Waila"),
    MFR("MFR"),
    EE3("EE3"),
    AQUACULTURE("Aquaculture");

    private final TargetModBuilder builder;

    TargetMods(String modId) {
        this.builder = new TargetModBuilder().setModId(modId);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
