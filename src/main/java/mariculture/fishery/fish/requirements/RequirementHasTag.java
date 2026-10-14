package mariculture.fishery.fish.requirements;

import net.minecraft.item.ItemStack;

import mariculture.api.fishery.IMutationRequirement;

public class RequirementHasTag implements IMutationRequirement {

    private final String[] keys;

    public RequirementHasTag(String[] keys) {
        this.keys = keys;
    }

    public RequirementHasTag(String key) {
        this.keys = new String[] { key };
    }

    @Override
    public boolean canMutationOccur(ItemStack egg) {
        for (String key : keys) {
            if (!egg.getTagCompound()
                .hasKey(key)) return false;
        }

        return true;
    }

    @Override
    public String getMutationInfo() {
        return "Requires a Block of " + keys[0] + " in the water.";
    }
}
