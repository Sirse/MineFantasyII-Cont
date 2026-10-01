package minefantasy.mf2.api.knowledge.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lays an entry's pages out for reading: text too long for one page runs on to the next, the first page shorter to
 * leave room for the heading, and a page carried on starts with its text, not with the blank line of a paragraph break.
 * Other pages, such as recipes, keep a page of their own.
 */
public final class PageFlow {

    private PageFlow() {}

    /** One page as laid out: a run of a text page's lines, or another page as it is. */
    public static final class Page {

        /** Which of the entry's own pages this comes from. */
        public final int source;
        /** The lines on this page; null for a page that is not text. */
        public final List<String> lines;
        /** Whether a run of text ends here, before another kind of page or the end of the entry. */
        public final boolean endsText;

        Page(int source, List<String> lines, boolean endsText) {
            this.source = source;
            this.lines = lines;
            this.endsText = endsText;
        }
    }

    /**
     * @param sources   each of the entry's pages: its text already broken into lines, or null for a page that is not
     *                  text
     * @param firstRoom how many lines fit on the first page, under the heading
     * @param room      how many lines fit on any other page
     */
    public static List<Page> lay(List<List<String>> sources, int firstRoom, int room) {
        List<Page> pages = new ArrayList<Page>();
        for (int i = 0; i < sources.size(); i++) {
            List<String> lines = sources.get(i);
            if (lines == null) {
                pages.add(new Page(i, null, false));
                continue;
            }
            boolean endsText = i + 1 == sources.size() || sources.get(i + 1) == null;
            int from = skipBlank(lines, 0);
            while (from < lines.size()) {
                int to = Math.min(from + Math.max(1, pages.isEmpty() ? firstRoom : room), lines.size());
                int next = skipBlank(lines, to);
                pages.add(
                        new Page(
                                i,
                                Collections.unmodifiableList(new ArrayList<String>(lines.subList(from, to))),
                                endsText && next >= lines.size()));
                from = next;
            }
        }
        return pages;
    }

    private static int skipBlank(List<String> lines, int from) {
        while (from < lines.size() && lines.get(from).trim().isEmpty()) {
            from++;
        }
        return from;
    }
}
