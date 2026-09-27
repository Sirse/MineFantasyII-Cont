package minefantasy.mf2.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

import minefantasy.mf2.api.helpers.ItemQuality;
import minefantasy.mf2.api.material.CustomMaterial;

/**
 * {@code /mf edit}: changes the held item's material, quality or breaking. The arguments are read here; the change
 * itself is {@link ItemEdits}.
 */
final class EditCommand {

    static final String USAGE = "command.mf.edit.usage";

    private EditCommand() {}

    /** @param args the arguments after {@code edit} */
    static void run(EntityPlayer player, String[] args) {
        if (args.length < 2) {
            throw new WrongUsageException(USAGE);
        }
        ItemStack held = player.getHeldItem();
        if (held == null) {
            throw new CommandException("command.mf.edit.no_item");
        }
        ItemEdits.Result result;
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "material":
                if (args.length > 3) {
                    throw new WrongUsageException(USAGE);
                }
                ItemEdits.Slot slot = null;
                if (args.length == 3) {
                    slot = ItemEdits.Slot.byName(args[2]);
                    if (slot == null) {
                        throw new CommandException("command.mf.edit.unknown_slot", args[2]);
                    }
                }
                result = ItemEdits.material(held, args[1], slot);
                break;
            case "quality":
                exactly(args, 2);
                result = ItemEdits.quality(
                        held,
                        CommandBase.parseIntBounded(player, args[1], (int) ItemQuality.MIN, (int) ItemQuality.MAX));
                break;
            case "unbreakable":
                exactly(args, 2);
                result = ItemEdits.unbreakable(held, CommandBase.parseBoolean(player, args[1]));
                break;
            default:
                throw new WrongUsageException(USAGE);
        }
        if (!result.success) {
            throw new CommandException(result.key, result.args);
        }
        player.addChatMessage(new ChatComponentTranslation(result.key, result.args));
    }

    private static void exactly(String[] args, int count) {
        if (args.length != count) {
            throw new WrongUsageException(USAGE);
        }
    }

    /** @param args the arguments after {@code edit}, the last one being typed */
    static List<String> complete(String[] args) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(args, "material", "quality", "unbreakable");
        }
        String what = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && "material".equals(what)) {
            return CommandBase.getListOfStringsFromIterableMatchingLastWord(args, materials());
        }
        if (args.length == 2 && "unbreakable".equals(what)) {
            return CommandBase.getListOfStringsMatchingLastWord(args, "true", "false");
        }
        if (args.length == 3 && "material".equals(what)) {
            return CommandBase.getListOfStringsMatchingLastWord(args, "main", "haft");
        }
        return Collections.emptyList();
    }

    /** Materials a part is made of: the metals and woods. */
    private static List<String> materials() {
        List<String> names = new ArrayList<>();
        for (CustomMaterial material : CustomMaterial.materialList.values()) {
            if ("metal".equalsIgnoreCase(material.type) || "wood".equalsIgnoreCase(material.type)) {
                names.add(material.getName());
            }
        }
        Collections.sort(names);
        return names;
    }
}
