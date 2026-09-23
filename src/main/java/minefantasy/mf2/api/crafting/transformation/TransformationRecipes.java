package minefantasy.mf2.api.crafting.transformation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import minefantasy.mf2.api.rpg.Skill;
import minefantasy.mf2.block.list.BlockListMF;
import minefantasy.mf2.config.ConfigCrafting;
import minefantasy.mf2.mechanics.TransformationHandler;

/**
 * Registry for world-level block transformations (chopping logs into planks, splitting blocks with a saw, etc). See
 * {@link TransformationRecipe} for the semantics.
 */
public class TransformationRecipes {

    private static final List recipes = new ArrayList();

    private TransformationRecipes() {}

    public static TransformationRecipe addRecipe(Block input, int inputMeta, Block output, int outputMeta,
            boolean copyMeta, String toolType, int toolTier, int hits, ItemStack consumable, ItemStack dropPerHit,
            String sound, Skill skill, int skillXp, String research, int outputCount) {
        TransformationRecipe recipe = new TransformationRecipe(
                input,
                inputMeta,
                output,
                outputMeta,
                copyMeta,
                toolType,
                toolTier,
                hits,
                consumable,
                dropPerHit,
                sound,
                skill,
                skillXp,
                research,
                outputCount);
        recipes.add(recipe);
        return recipe;
    }

    public static TransformationRecipe addRecipe(Block input, int inputMeta, Block output, int outputMeta,
            boolean copyMeta, String toolType, int toolTier, int hits, ItemStack consumable, ItemStack dropPerHit,
            String sound, Skill skill, int skillXp, String research, int outputCount, int copyMetaMask) {
        TransformationRecipe recipe = new TransformationRecipe(
                input,
                inputMeta,
                output,
                outputMeta,
                copyMeta,
                toolType,
                toolTier,
                hits,
                consumable,
                dropPerHit,
                sound,
                skill,
                skillXp,
                research,
                outputCount,
                copyMetaMask);
        recipes.add(recipe);
        return recipe;
    }

    public static TransformationRecipe findRecipe(Block block, int meta, String heldTool, int heldTier) {
        Iterator it = recipes.iterator();
        while (it.hasNext()) {
            TransformationRecipe recipe = (TransformationRecipe) it.next();
            if (recipe.matches(block, meta, heldTool, heldTier)) {
                return recipe;
            }
        }
        return null;
    }

    public static List getRecipeList() {
        return recipes;
    }

    /**
     * Registers the built-in recipes and the interaction handler. Called once from mod init.
     */
    public static void init() {
        if (!recipes.isEmpty()) {
            return;
        }
        if (!ConfigCrafting.enableTransformations) {
            return;
        }

        if (ConfigCrafting.transformationLogChopping) {
            // Any log chops into planks of the same species; mask out the orientation bits of the log meta
            addRecipe(
                    Blocks.log,
                    -1,
                    Blocks.planks,
                    0,
                    true,
                    "axe",
                    -1,
                    1,
                    null,
                    null,
                    "dig.wood",
                    null,
                    0,
                    null,
                    1,
                    3);
        }
        if (ConfigCrafting.transformationLogChopping) {
            // Acacia and dark oak logs follow the four vanilla species in the planks meta
            addRecipe(
                    Blocks.log2,
                    -1,
                    Blocks.planks,
                    4,
                    true,
                    "axe",
                    -1,
                    1,
                    null,
                    null,
                    "dig.wood",
                    null,
                    0,
                    null,
                    1,
                    1);
        }
        if (ConfigCrafting.transformationPlankSawing) {
            // Any plank saws into two slabs of the same species
            addRecipe(
                    Blocks.planks,
                    -1,
                    Blocks.wooden_slab,
                    0,
                    true,
                    "saw",
                    -1,
                    1,
                    null,
                    null,
                    "dig.wood",
                    null,
                    0,
                    null,
                    2);
        }
        if (ConfigCrafting.transformationCobbleHammering) {
            // Cobblestone hammers into stone bricks over three stages (stage stored in unused metadata)
            addRecipe(
                    Blocks.cobblestone,
                    0,
                    Blocks.stonebrick,
                    0,
                    false,
                    "hammer",
                    -1,
                    3,
                    null,
                    null,
                    "dig.stone",
                    null,
                    0,
                    null,
                    1);
        }
        if (ConfigCrafting.transformationRefinedPlankSawing) {
            // Refined MF planks saw into vanilla planks
            addRecipe(
                    BlockListMF.refined_planks,
                    -1,
                    Blocks.planks,
                    0,
                    false,
                    "saw",
                    -1,
                    1,
                    null,
                    null,
                    "dig.wood",
                    null,
                    0,
                    null,
                    2);
        }

        if (recipes.isEmpty()) {
            return;
        }
        TransformationHandler handler = new TransformationHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }
}
