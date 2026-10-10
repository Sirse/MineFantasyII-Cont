package minefantasy.mf2.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;

/**
 * {@code /minefantasy} ({@code /mf}): routes {@code edit} and {@code recipes} to their handlers. Both act on the
 * player's held item or view, need operator level 2, and report a wrong argument through the usual command errors.
 */
public class CommandMF extends CommandBase {

    static final String USAGE = "command.mf.usage";

    private static final List<String> ALIASES = Collections.unmodifiableList(Arrays.asList("mf"));

    @Override
    public String getCommandName() {
        return "minefantasy";
    }

    @Override
    public List<String> getCommandAliases() {
        return ALIASES;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayer player = getCommandSenderAsPlayer(sender);
        if (args.length == 0) {
            throw new WrongUsageException(USAGE);
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "edit":
                EditCommand.run(player, rest);
                break;
            case "recipes":
                RecipesCommand.run(player, rest);
                break;
            case "zs":
                ZsCommand.run(player, rest);
                break;
            default:
                throw new WrongUsageException(USAGE);
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "edit", "recipes", "zs");
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "edit":
                return EditCommand.complete(rest);
            case "recipes":
                return RecipesCommand.complete(rest);
            default:
                return Collections.emptyList();
        }
    }
}
