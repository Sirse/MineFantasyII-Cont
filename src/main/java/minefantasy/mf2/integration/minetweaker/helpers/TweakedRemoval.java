package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Removes entries from a recipe list and puts them back where they were.
 * <p>
 * The adapters used to undo a removal with {@code addAll}, which appends. Every MineFantasy recipe manager picks the
 * first matching entry, so an appended recipe loses to anything that overlaps it and the bench keeps producing the
 * wrong result until the game is restarted.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class TweakedRemoval {

    // The recipe managers all expose raw lists, so this one stays raw rather than forcing casts on every caller
    private final List list;
    private final Collection targets;
    private final List removed = new ArrayList();
    private final List<Integer> indices = new ArrayList<Integer>();

    public TweakedRemoval(List list, Collection targets) {
        this.list = list;
        this.targets = targets;
    }

    public void apply() {
        removed.clear();
        indices.clear();
        // Walk forwards so the recorded indices come out ascending, which is what undo() relies on
        for (int i = 0; i < list.size(); i++) {
            if (containsIdentity(list.get(i))) {
                indices.add(i);
                removed.add(list.get(i));
            }
        }
        // Remove backwards so the earlier positions stay valid while we go
        for (int i = indices.size() - 1; i >= 0; i--) {
            list.remove((int) indices.get(i));
        }
    }

    public void undo() {
        for (int i = 0; i < indices.size(); i++) {
            int at = indices.get(i);
            if (at >= 0 && at <= list.size()) {
                list.add(at, removed.get(i));
            } else {
                list.add(removed.get(i));
            }
        }
        removed.clear();
        indices.clear();
    }

    public int size() {
        return targets.size();
    }

    private boolean containsIdentity(Object entry) {
        for (Object target : targets) {
            if (target == entry) {
                return true;
            }
        }
        return false;
    }
}
