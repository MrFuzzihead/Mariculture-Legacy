package mariculture.fishery.fish.dna;

import net.minecraft.item.ItemStack;

import mariculture.api.fishery.fish.FishDNA;
import mariculture.api.fishery.fish.FishSpecies;
import mariculture.core.config.FishMechanics.FussyFish;

public class FishDNAFertility extends FishDNA {

    @Override
    public Integer getDNAFromSpecies(FishSpecies species) {
        return species.getFertility();
    }

    @Override
    public int getCopyChance() {
        return 20;
    }

    public Integer sanitize(Integer i) {
        return Math.min(Integer.MAX_VALUE, Math.max(1, i));
    }

    @Override
    public Integer getDNA(ItemStack stack) {
        return sanitize(super.getDNA(stack) + FussyFish.FERTILITY_BOOSTER);
    }

    @Override
    public Integer getLowerDNA(ItemStack stack) {
        return sanitize(super.getLowerDNA(stack) + FussyFish.FERTILITY_BOOSTER);
    }
}
