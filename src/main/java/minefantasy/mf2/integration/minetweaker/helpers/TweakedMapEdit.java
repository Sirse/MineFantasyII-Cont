package minefantasy.mf2.integration.minetweaker.helpers;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Records what a keyed registry held before a script overwrote it.
 * <p>
 * The map based registries replace on put, so an action that only remembers what it added cannot be undone: removing
 * its own key throws away whatever recipe or heating profile used to answer for that key.
 */
public class TweakedMapEdit<V> {

    private final Map<String, V> map;
    private final Map<String, V> replaced = new LinkedHashMap<String, V>();
    private final Set<String> added = new LinkedHashSet<String>();

    public TweakedMapEdit(Map<String, V> map) {
        this.map = map;
    }

    public void put(String key, V value) {
        record(key);
        map.put(key, value);
    }

    public void undo() {
        for (String key : added) {
            map.remove(key);
        }
        for (Map.Entry<String, V> entry : replaced.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        added.clear();
        replaced.clear();
    }

    /** Only the first touch of a key is recorded, so repeated writes still roll back to the pristine value. */
    private void record(String key) {
        if (replaced.containsKey(key) || added.contains(key)) {
            return;
        }
        if (map.containsKey(key)) {
            replaced.put(key, map.get(key));
        } else {
            added.add(key);
        }
    }
}
