package minefantasy.mf2.commands;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

/**
 * {@code /mf zs}: the held item as a script would write it, as a result and as an ingredient. Clicking a line puts it
 * in the chat box to copy, when it fits there.
 */
final class ZsCommand {

    private ZsCommand() {}

    static void run(EntityPlayer player, String[] args) {
        if (args.length != 0) {
            throw new WrongUsageException("command.mf.zs.usage");
        }
        ItemStack held = player.getHeldItem();
        if (held == null) {
            throw new CommandException("command.mf.recipes.no_item");
        }
        player.addChatMessage(line("command.mf.zs.output", ItemScript.output(held)));
        player.addChatMessage(line("command.mf.zs.input", ItemScript.input(held)));
        List<String> dropped = ItemScript.droppedTags(held);
        if (!dropped.isEmpty()) {
            IChatComponent note = new ChatComponentTranslation("command.mf.zs.dropped", String.join(", ", dropped));
            note.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GRAY));
            player.addChatMessage(note);
        }
    }

    private static IChatComponent line(String label, String script) {
        IChatComponent text = new ChatComponentText(script);
        ChatStyle style = new ChatStyle().setColor(EnumChatFormatting.AQUA);
        if (script.length() <= RecipesCommand.CHAT_LIMIT) {
            style.setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, script));
        }
        text.setChatStyle(style);
        return new ChatComponentTranslation(label).appendText(" ").appendSibling(text);
    }
}
