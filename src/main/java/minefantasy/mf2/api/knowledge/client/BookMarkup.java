package minefantasy.mf2.api.knowledge.client;

import net.minecraft.util.EnumChatFormatting;

/**
 * The research book's own markup, as written in the language files, turned into the font's formatting codes: {@code $h}
 * starts a heading, {@code $d} red ink, {@code $y} gold, {@code $u} underline, {@code $r} back to plain ink, and
 * {@code ^} starts a new paragraph. Any other code, and a {@code $} with nothing after it, is dropped.
 */
public final class BookMarkup {

    private BookMarkup() {}

    public static String parse(String source) {
        StringBuilder text = new StringBuilder(source.length() + 16);
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '^') {
                text.append("\n\n");
            } else if (c == '$') {
                if (++i < source.length()) {
                    text.append(code(source.charAt(i)));
                }
            } else {
                text.append(c);
            }
        }
        return text.toString();
    }

    private static String code(char c) {
        switch (c) {
            case 'h':
                // Bold in the page's own ink, not a bright colour of the chat's
                return EnumChatFormatting.BOLD.toString();
            case 'd':
                return EnumChatFormatting.DARK_RED.toString();
            case 'y':
                return EnumChatFormatting.GOLD.toString();
            case 'u':
                return EnumChatFormatting.UNDERLINE.toString();
            case 'r':
                // Reset returns the font to the colour the text is drawn in, the ink
                return EnumChatFormatting.RESET.toString();
            default:
                return "";
        }
    }
}
