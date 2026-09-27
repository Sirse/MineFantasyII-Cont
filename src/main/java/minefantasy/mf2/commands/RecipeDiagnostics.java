package minefantasy.mf2.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeRegistry;

/**
 * How a station looks its recipes up, for {@code /mf recipes}: the station a player looks at explains its own lookup; a
 * named station judges a held stack. Each candidate comes in lookup order with why it is not the one crafted. What is
 * shown of it is {@link RecipesCommand}.
 */
final class RecipeDiagnostics {

    private static final double REACH = 6D;
    private static final Map<String, Function<ItemStack, Diagnosis>> LOOKUPS = new LinkedHashMap<>();

    static {
        LOOKUPS.put("bloomery", held -> lookup("bloomery", MFRecipes.BLOOMERY, r -> true, r -> r.getInput(), held));
        LOOKUPS.put("quern", held -> lookup("quern", MFRecipes.QUERN, r -> true, r -> r.getInput(), held));
        LOOKUPS.put("tanning", held -> lookup("tanning", MFRecipes.TANNING, r -> true, r -> r.getInput(), held));
        LOOKUPS.put(
                "big_furnace",
                held -> lookup("big_furnace", MFRecipes.BIG_FURNACE, r -> true, r -> r.getInput(), held));
        LOOKUPS.put(
                "blast_furnace",
                held -> lookup("blast_furnace", MFRecipes.BLAST_FURNACE, r -> true, r -> r.getInput(), held));
        LOOKUPS.put("paint_oil", held -> lookup("paint_oil", MFRecipes.PAINT_OIL, r -> true, r -> r.getInput(), held));
        // A spit and an oven each look up only their own recipes: an oven recipe never shadows a spit one
        LOOKUPS.put("spit", held -> lookup("spit", MFRecipes.COOKING, r -> !r.isBaking(), r -> r.getInput(), held));
        LOOKUPS.put("oven", held -> lookup("oven", MFRecipes.COOKING, r -> r.isBaking(), r -> r.getInput(), held));
        LOOKUPS.put("forge_heat", held -> lookup("forge_heat", MFRecipes.HEATING, r -> true, r -> r.getInput(), held));
        LOOKUPS.put(
                "salvage",
                held -> lookup("salvage", MFRecipes.SALVAGE, r -> true, Salvage.SalvageRecipe::getInput, held));
    }

    private RecipeDiagnostics() {}

    static List<String> stations() {
        return new ArrayList<>(LOOKUPS.keySet());
    }

    static boolean isStation(String name) {
        return LOOKUPS.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /** The candidates of the named station for the stack, as held: a recipe taking several must see them all. */
    static Diagnosis forStack(String station, ItemStack held) {
        return LOOKUPS.get(station.toLowerCase(Locale.ROOT)).apply(held);
    }

    /** The lookup of the station the player looks at, or null when it is none that explains itself. */
    static Diagnosis lookedAt(EntityPlayer player) {
        TileEntity tile = lookedAtTile(player);
        return tile instanceof Diagnosis.Source ? ((Diagnosis.Source) tile).diagnose(player) : null;
    }

    /** Candidates for one stack, in lookup order: the first that takes it wins, later ones are shadowed. */
    private static <R> Diagnosis lookup(String station, RecipeRegistry<R> registry, Predicate<R> inContext,
            Function<R, Input> input, ItemStack held) {
        return Diagnosis.of(
                station,
                Diagnosis.walk(
                        registry.published().candidates(Input.lookupKeys(held)),
                        inContext,
                        entry -> input.apply(entry.getRecipe()).explain(held),
                        entry -> null).getCandidates());
    }

    private static TileEntity lookedAtTile(EntityPlayer player) {
        Vec3 eyes = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLookVec();
        Vec3 end = eyes.addVector(look.xCoord * REACH, look.yCoord * REACH, look.zCoord * REACH);
        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(eyes, end);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return null;
        }
        return player.worldObj.getTileEntity(hit.blockX, hit.blockY, hit.blockZ);
    }
}
