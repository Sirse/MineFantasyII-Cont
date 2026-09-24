package minefantasy.mf2.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import minefantasy.mf2.api.crafting.MFRecipes;
import minefantasy.mf2.api.crafting.Salvage;
import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.Diagnosis;
import minefantasy.mf2.api.recipe.Input;
import minefantasy.mf2.api.recipe.RecipeEntry;
import minefantasy.mf2.api.recipe.RecipeRegistry;

/**
 * {@code /mf recipes}: how a station looks its recipes up. Looking at a station, it lists the candidates for what the
 * station holds; with a station name, the candidates for the held item. Each line gives the recipe id, its priority and
 * why it is not the one crafted, in lookup order, so a pack maker sees which recipe wins and why the others lose.
 */
final class RecipeDiagnostics {

    private static final double REACH = 6D;
    private static final Map<String, Function<ItemStack, Diagnosis>> LOOKUPS = new LinkedHashMap<>();

    static {
        LOOKUPS.put("bloomery", held -> lookup(MFRecipes.BLOOMERY, r -> r.getInput(), single(held)));
        LOOKUPS.put("quern", held -> lookup(MFRecipes.QUERN, r -> r.getInput(), held));
        LOOKUPS.put("tanning", held -> lookup(MFRecipes.TANNING, r -> r.getInput(), held));
        LOOKUPS.put("big_furnace", held -> lookup(MFRecipes.BIG_FURNACE, r -> r.getInput(), held));
        LOOKUPS.put("blast_furnace", held -> lookup(MFRecipes.BLAST_FURNACE, r -> r.getInput(), held));
        LOOKUPS.put("paint_oil", held -> lookup(MFRecipes.PAINT_OIL, r -> r.getInput(), held));
        LOOKUPS.put("cooking", held -> lookup(MFRecipes.COOKING, r -> r.getInput(), single(held)));
        LOOKUPS.put("forge_heat", held -> lookup(MFRecipes.HEATING, r -> r.getInput(), single(held)));
        LOOKUPS.put("salvage", held -> lookup(MFRecipes.SALVAGE, Salvage.SalvageRecipe::getInput, single(held)));
    }

    private RecipeDiagnostics() {}

    static List<String> stations() {
        return new ArrayList<>(LOOKUPS.keySet());
    }

    static void run(EntityPlayer player, String station) {
        Diagnosis diagnosis;
        if (station == null) {
            TileEntity tile = lookedAt(player);
            if (!(tile instanceof Diagnosis.Source)) {
                player.addChatMessage(grey(new ChatComponentTranslation("command.mf.recipes.no_station")));
                return;
            }
            diagnosis = ((Diagnosis.Source) tile).diagnose(player);
        } else {
            Function<ItemStack, Diagnosis> lookup = LOOKUPS.get(station.toLowerCase());
            if (lookup == null) {
                player.addChatMessage(
                        grey(new ChatComponentTranslation("command.mf.recipes.unknown_station", station)));
                return;
            }
            if (player.getHeldItem() == null) {
                player.addChatMessage(grey(new ChatComponentTranslation("command.mf.recipes.no_item")));
                return;
            }
            diagnosis = lookup.apply(player.getHeldItem());
        }
        report(player, diagnosis);
    }

    private static void report(EntityPlayer player, Diagnosis diagnosis) {
        player.addChatMessage(
                new ChatComponentTranslation(
                        "command.mf.recipes.header",
                        diagnosis.getStation(),
                        diagnosis.getCandidates().size()));
        if (diagnosis.getProblem() != null) {
            player.addChatMessage(color(reason(diagnosis.getProblem()), EnumChatFormatting.RED));
            return;
        }
        int n = 1;
        for (Diagnosis.Candidate candidate : diagnosis.getCandidates()) {
            CheckResult.Reason reason = candidate.getReason();
            IChatComponent verdict = reason == null
                    ? color(new ChatComponentTranslation("command.mf.recipes.crafts"), EnumChatFormatting.GREEN)
                    : color(
                            reason(reason),
                            "harder".equals(reason.getId()) ? EnumChatFormatting.YELLOW
                                    : "shadowed".equals(reason.getId()) ? EnumChatFormatting.GRAY
                                            : EnumChatFormatting.RED);
            IChatComponent line = new ChatComponentText(
                    "#" + n++ + " " + candidate.getId() + " [" + candidate.getPriority() + "] ");
            player.addChatMessage(line.appendSibling(verdict));
        }
    }

    /** Candidates for one stack, in lookup order: the first that takes it wins, later ones are shadowed. */
    private static <R> Diagnosis lookup(RecipeRegistry<R> registry, Function<R, Input> input, ItemStack held) {
        List<Diagnosis.Candidate> candidates = new ArrayList<>();
        boolean chosen = false;
        for (RecipeEntry<R> entry : registry.published().candidates(Input.lookupKeys(held))) {
            CheckResult.Reason reason = input.apply(entry.getRecipe()).explain(held);
            if (reason == null && chosen) {
                reason = CheckResult.Reason.of("shadowed");
            }
            chosen |= reason == null;
            candidates.add(Diagnosis.candidate(entry, reason));
        }
        return Diagnosis.of(registry.getStation(), candidates);
    }

    /** Stations that take one item at a time judge a single one of the held stack. */
    private static ItemStack single(ItemStack held) {
        ItemStack one = held.copy();
        one.stackSize = 1;
        return one;
    }

    private static TileEntity lookedAt(EntityPlayer player) {
        Vec3 eyes = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLookVec();
        Vec3 end = eyes.addVector(look.xCoord * REACH, look.yCoord * REACH, look.zCoord * REACH);
        MovingObjectPosition hit = player.worldObj.rayTraceBlocks(eyes, end);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return null;
        }
        return player.worldObj.getTileEntity(hit.blockX, hit.blockY, hit.blockZ);
    }

    private static IChatComponent reason(CheckResult.Reason reason) {
        return new ChatComponentTranslation(reason.getTranslationKey(), reason.getArgs());
    }

    private static IChatComponent color(IChatComponent component, EnumChatFormatting color) {
        component.setChatStyle(new ChatStyle().setColor(color));
        return component;
    }

    private static IChatComponent grey(IChatComponent component) {
        return color(component, EnumChatFormatting.GRAY);
    }
}
