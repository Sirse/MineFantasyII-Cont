package minefantasy.mf2.commands;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import minefantasy.mf2.api.recipe.CheckResult;
import minefantasy.mf2.api.recipe.Diagnosis;

/**
 * {@code /mf recipes [station] [page]}: shows a pack maker which recipe a station picks and why the others lose.
 * Looking at a station, it lists the candidates for what the station holds; with a station name, those for the held
 * item. A long list comes a page at a time.
 */
final class RecipesCommand {

    static final String USAGE = "command.mf.recipes.usage";
    static final int PAGE_SIZE = 8;

    private RecipesCommand() {}

    /** @param args the arguments after {@code recipes} */
    static void run(EntityPlayer player, String[] args) {
        if (args.length > 2) {
            throw new WrongUsageException(USAGE);
        }
        String station = null;
        int page = 1;
        if (args.length == 2) {
            station = args[0];
            page = CommandBase.parseIntWithMin(player, args[1], 1);
        } else if (args.length == 1) {
            if (RecipeDiagnostics.isStation(args[0])) {
                station = args[0];
            } else {
                page = parsePageOrStation(player, args[0]);
            }
        }
        Diagnosis diagnosis;
        if (station == null) {
            diagnosis = RecipeDiagnostics.lookedAt(player);
            if (diagnosis == null) {
                throw new CommandException("command.mf.recipes.no_station");
            }
        } else {
            if (!RecipeDiagnostics.isStation(station)) {
                throw new CommandException("command.mf.recipes.unknown_station", station);
            }
            if (player.getHeldItem() == null) {
                throw new CommandException("command.mf.recipes.no_item");
            }
            diagnosis = RecipeDiagnostics.forStack(station, player.getHeldItem());
        }
        report(player, diagnosis, station, page);
    }

    /** A lone argument is a page of the station looked at when it is a number, and else an unknown station. */
    private static int parsePageOrStation(EntityPlayer player, String arg) {
        try {
            Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            throw new CommandException("command.mf.recipes.unknown_station", arg);
        }
        return CommandBase.parseIntWithMin(player, arg, 1);
    }

    private static void report(EntityPlayer player, Diagnosis diagnosis, String station, int page) {
        List<Diagnosis.Candidate> candidates = diagnosis.getCandidates();
        int pages = Math.max(1, (candidates.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > pages) {
            throw new CommandException("command.mf.recipes.no_page", page, pages);
        }
        player.addChatMessage(
                new ChatComponentTranslation("command.mf.recipes.header", diagnosis.getStation(), candidates.size()));
        if (diagnosis.getProblem() != null) {
            player.addChatMessage(color(reason(diagnosis.getProblem()), EnumChatFormatting.RED));
            return;
        }
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(candidates.size(), from + PAGE_SIZE);
        for (int n = from; n < to; n++) {
            Diagnosis.Candidate candidate = candidates.get(n);
            IChatComponent line = new ChatComponentText(
                    "#" + (n + 1) + " " + candidate.getId() + " [" + candidate.getPriority() + "] ");
            player.addChatMessage(line.appendSibling(verdict(candidate.getReason())));
        }
        if (pages > 1) {
            String next = "/mf recipes " + (station == null ? "" : station + " ") + Math.min(page + 1, pages);
            player.addChatMessage(
                    color(
                            new ChatComponentTranslation("command.mf.recipes.page", page, pages, next),
                            EnumChatFormatting.GRAY));
        }
    }

    private static IChatComponent verdict(CheckResult.Reason reason) {
        if (reason == null) {
            return color(new ChatComponentTranslation("command.mf.recipes.crafts"), EnumChatFormatting.GREEN);
        }
        EnumChatFormatting color = reason.isPenalty() ? EnumChatFormatting.YELLOW
                : reason.isShadowed() ? EnumChatFormatting.GRAY : EnumChatFormatting.RED;
        return color(reason(reason), color);
    }

    private static IChatComponent reason(CheckResult.Reason reason) {
        return new ChatComponentTranslation(reason.getTranslationKey(), reason.getArgs());
    }

    private static IChatComponent color(IChatComponent component, EnumChatFormatting color) {
        component.setChatStyle(new ChatStyle().setColor(color));
        return component;
    }

    /** @param args the arguments after {@code recipes}, the last one being typed */
    static List<String> complete(String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsFromIterableMatchingLastWord(args, RecipeDiagnostics.stations());
        }
        return Collections.emptyList();
    }
}
