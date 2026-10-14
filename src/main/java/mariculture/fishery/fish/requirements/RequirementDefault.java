package mariculture.fishery.fish.requirements;

import net.minecraft.item.ItemStack;

import mariculture.api.fishery.IMutationRequirement;

public class RequirementDefault implements IMutationRequirement {

    public static final IMutationRequirement INSTANCE = new RequirementDefault();

    @Override
    public boolean canMutationOccur(ItemStack egg) {
        return true;
    }

    @Override
    public String getMutationInfo() {
        return "";
    }
}
