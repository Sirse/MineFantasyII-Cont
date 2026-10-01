package minefantasy.mf2.api.knowledge.client;

import java.util.Collections;
import java.util.List;

/**
 * The research book's search, apart from where the book draws it: what is typed, what it finds, which find the book
 * went to, and whether the line is open for typing. Choosing a card on the map cannot close a search being typed.
 * <p>
 * Closed, it is only its glass. Typing, it shows the query. Browsing, after going to a find, it shows which find of how
 * many beside the chosen entry's button, and the glass brings the query back.
 *
 * @param <T> what is searched for
 */
public final class BookSearch<T> {

    public enum Mode {
        CLOSED,
        TYPING,
        BROWSING
    }

    /** What a query finds, in order. */
    public interface Finder<T> {

        List<T> find(String query);
    }

    public static final int MAX_LENGTH = 24;

    private final Finder<T> finder;
    private Mode mode = Mode.CLOSED;
    private String query = "";
    /** Which find the book went to; -1 before going to any. */
    private int selected = -1;
    /** The finds of the query, kept until the query or what they depend on changes. */
    private List<T> found = Collections.emptyList();
    private String foundQuery;
    private Object foundStamp;

    public BookSearch(Finder<T> finder) {
        this.finder = finder;
    }

    public Mode mode() {
        return mode;
    }

    public boolean isOpen() {
        return mode != Mode.CLOSED;
    }

    public boolean isTyping() {
        return mode == Mode.TYPING;
    }

    public boolean isBrowsing() {
        return mode == Mode.BROWSING;
    }

    public String query() {
        return query;
    }

    public int selected() {
        return selected;
    }

    /**
     * The glass: opens a closed search afresh, closes one being typed, and brings back the line of one being browsed
     * with its query and place kept.
     */
    public void toggle() {
        if (mode == Mode.TYPING) {
            close();
        } else {
            open();
        }
    }

    /** Opens the line for typing: afresh when closed, as it was when browsing. */
    public void open() {
        if (mode == Mode.CLOSED) {
            query = "";
            selected = -1;
        }
        mode = Mode.TYPING;
    }

    /** Ends the search and forgets it. */
    public void close() {
        mode = Mode.CLOSED;
        query = "";
        selected = -1;
    }

    public void type(char c) {
        if (mode == Mode.TYPING && query.length() < MAX_LENGTH) {
            query += c;
            selected = -1;
        }
    }

    public void erase() {
        if (mode == Mode.TYPING && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            selected = -1;
        }
    }

    /**
     * Something was chosen other than through the search, such as an entry on the map or another category: a search
     * being browsed ends, one being typed stays open.
     */
    public void choseElsewhere() {
        if (mode == Mode.BROWSING) {
            close();
        }
    }

    /**
     * Goes to the next or the previous find, round from the last to the first; back from none goes to the last.
     *
     * @param stamp what the finds depend on besides the query, such as the language and what the player knows
     * @return the find gone to, or null when there is none
     */
    public T step(int direction, Object stamp) {
        List<T> finds = results(stamp);
        if (!isOpen() || finds.isEmpty()) {
            return null;
        }
        int from = selected < 0 && direction < 0 ? 0 : selected;
        selected = ((from + direction) % finds.size() + finds.size()) % finds.size();
        mode = Mode.BROWSING;
        return finds.get(selected);
    }

    /** The finds of the query, worked out again only when the query or the stamp has changed. */
    public List<T> results(Object stamp) {
        boolean sameStamp = stamp == null ? foundStamp == null : stamp.equals(foundStamp);
        if (!query.equals(foundQuery) || !sameStamp) {
            foundQuery = query;
            foundStamp = stamp;
            found = query.trim().isEmpty() ? Collections.<T>emptyList()
                    : Collections.unmodifiableList(finder.find(query.trim()));
            if (selected >= found.size()) {
                selected = -1;
            }
        }
        return found;
    }

    /** How many it finds, or which of them is shown out of how many: "36", or "2/36". */
    public String counter(Object stamp) {
        int count = results(stamp).size();
        return selected < 0 ? String.valueOf(count) : (selected + 1) + "/" + count;
    }
}
