package minefantasy.mf2.commands;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.mojang.authlib.GameProfile;

import minefantasy.mf2.gametest.Modders;

/**
 * A player typing {@code /mf} commands the way the server runs them, through its command manager: permission checks and
 * the errors of wrong use included. It keeps the chat messages sent to it, checked by translation key so the server's
 * language does not matter.
 */
final class CommandPlayer extends FakePlayer {

    final List<IChatComponent> messages = new ArrayList<>();
    private final boolean operator;

    private CommandPlayer(GameTestHelper helper, boolean operator) {
        super(helper.getWorld(), new GameProfile(UUID.randomUUID(), operator ? Modders.SMITH : Modders.NOVICE));
        this.operator = operator;
    }

    /** An operator, allowed the commands, holding the given item (or nothing). */
    static CommandPlayer operator(GameTestHelper helper, ItemStack held) {
        return holding(new CommandPlayer(helper, true), held);
    }

    /** A player without operator rights. */
    static CommandPlayer player(GameTestHelper helper, ItemStack held) {
        return holding(new CommandPlayer(helper, false), held);
    }

    private static CommandPlayer holding(CommandPlayer player, ItemStack held) {
        player.inventory.currentItem = 0;
        player.inventory.setInventorySlotContents(0, held);
        return player;
    }

    @Override
    public boolean canCommandSenderUseCommand(int level, String command) {
        return operator;
    }

    @Override
    public void addChatMessage(IChatComponent message) {
        messages.add(message);
    }

    /** Runs the command line, without the slash, and returns the messages it sent. */
    List<IChatComponent> run(String line) {
        messages.clear();
        MinecraftServer.getServer().getCommandManager().executeCommand(this, line);
        return new ArrayList<>(messages);
    }

    /** Runs the command line and returns the one message it answered with. */
    IChatComponent answer(String line) {
        List<IChatComponent> sent = run(line);
        assertEquals("messages for " + line + ": " + sent, 1, sent.size());
        return sent.get(0);
    }

    static String key(IChatComponent message) {
        assertTrue("not a translated message: " + message, message instanceof ChatComponentTranslation);
        return ((ChatComponentTranslation) message).getKey();
    }

    static Object[] args(IChatComponent message) {
        key(message);
        return ((ChatComponentTranslation) message).getFormatArgs();
    }
}
