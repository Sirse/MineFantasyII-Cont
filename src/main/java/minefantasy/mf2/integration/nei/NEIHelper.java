package minefantasy.mf2.integration.nei;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import codechicken.nei.PositionedStack;
import minefantasy.mf2.api.crafting.ITieredComponent;
import minefantasy.mf2.api.crafting.anvil.IAnvilRecipe;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.util.MFLogUtil;

public class NEIHelper {

    public static ItemStack fillMaterials(IAnvilRecipe recipe, ItemStack componentStack, ItemStack outputStack) {
        if (!isValidStack(componentStack) || !isValidStack(outputStack)) {
            return componentStack;
        }
        if (componentStack.getItem() instanceof ITieredComponent) {
            String componentType = ((ITieredComponent) componentStack.getItem()).getMaterialType(componentStack);
            if (componentType != null) {
                if (componentType.equalsIgnoreCase("metal")) {
                    CustomMaterial resultPrimaryMaterial = CustomToolHelper.getCustomPrimaryMaterial(outputStack);
                    if (resultPrimaryMaterial != null) {
                        CustomMaterial
                                .addMaterial(componentStack, CustomToolHelper.slot_main, resultPrimaryMaterial.name);
                    }
                }
                if (componentType.equalsIgnoreCase("wood")) {
                    CustomMaterial resultSecondaryMaterial = CustomToolHelper.getCustomSecondaryMaterial(outputStack);
                    if (resultSecondaryMaterial != null) {
                        CustomMaterial
                                .addMaterial(componentStack, CustomToolHelper.slot_main, resultSecondaryMaterial.name);
                    }
                }
            }

            /*
             * if (recipe.outputHot() && componentStack.getItem() instanceof IHotItem) {
             * ItemHeated.setTemp(componentStack, 1400); }
             */
        }

        return componentStack;
    }

    public static boolean isValidStack(ItemStack stack) {
        return stack != null && stack.getItem() != null && stack.stackSize >= 0;
    }

    public static ItemStack validCopy(ItemStack stack) {
        return isValidStack(stack) ? stack.copy() : null;
    }

    public static boolean canViewResearch(net.minecraft.entity.player.EntityPlayer player, String research) {
        if (research == null || research.isEmpty()) {
            return true;
        }
        return player != null
                && (player.capabilities.isCreativeMode || ResearchLogic.hasInfoUnlocked(player, research));
    }

    public static boolean canViewResearch(net.minecraft.entity.player.EntityPlayer player, InformationBase research) {
        return research == null || player != null
                && (player.capabilities.isCreativeMode || ResearchLogic.hasInfoUnlocked(player, research));
    }

    public static boolean matchesCrafting(ItemStack recipeStack, ItemStack inputStack) {
        if (!isValidStack(recipeStack) || !isValidStack(inputStack)) {
            return false;
        }
        return matchesItemDamage(recipeStack, inputStack)
                && CustomToolHelper.doesMatchForRecipe(recipeStack, inputStack);
    }

    public static boolean matchesItemDamage(ItemStack recipeStack, ItemStack inputStack) {
        if (!isValidStack(recipeStack) || !isValidStack(inputStack)) {
            return false;
        }
        return recipeStack.getItem() == inputStack.getItem()
                && (recipeStack.getItemDamage() == OreDictionary.WILDCARD_VALUE
                        || recipeStack.getItemDamage() == inputStack.getItemDamage());
    }

    public static PositionedStack positionedStack(ItemStack stack, int x, int y) {
        return positionedStack(stack, x, y, true);
    }

    public static PositionedStack positionedStack(ItemStack stack, int x, int y, boolean genPerm) {
        ItemStack copy = validCopy(stack);
        if (copy == null) {
            return null;
        }
        try {
            return new PositionedStack(copy, x, y, genPerm);
        } catch (RuntimeException e) {
            MFLogUtil.warnOnce(
                    "nei-positioned-stack",
                    "Failed to create NEI positioned stack for {} at {},{}",
                    copy,
                    x,
                    y,
                    e);
            return null;
        }
    }

    public static MFPositionedStack mfPositionedStack(Object object, int x, int y) {
        return mfPositionedStack(object, x, y, true);
    }

    public static MFPositionedStack mfPositionedStack(Object object, int x, int y, boolean genPerm) {
        if (object instanceof ItemStack && !isValidStack((ItemStack) object)) {
            return null;
        }
        try {
            return new MFPositionedStack(object, x, y, genPerm);
        } catch (RuntimeException e) {
            MFLogUtil.warnOnce(
                    "nei-mf-positioned-stack",
                    "Failed to create MineFantasy NEI positioned stack for {} at {},{}",
                    object,
                    x,
                    y,
                    e);
            return null;
        }
    }
}
