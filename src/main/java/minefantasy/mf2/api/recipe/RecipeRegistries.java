package minefantasy.mf2.api.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the station registries and publishes them together: one generation number covers all stations, and the published
 * set is swapped in one volatile write, so readers see either the old or the new set of every station.
 *
 * <p>
 * Lifecycle: {@code LOADING} (native and integration recipes go to the base layer) → {@link #finishLoading()} →
 * {@code IDLE}. A script reload: {@link #beginReload()} copies base to a draft → scripts write the draft →
 * {@link #publish()}. Repeated reloads start from the base again, so they never accumulate duplicates.
 */
public final class RecipeRegistries {

    public enum State {
        LOADING,
        IDLE,
        RELOADING
    }

    private static final class Published {

        final long generation;
        final Map<RecipeRegistry<?>, RecipeSnapshot<?>> snapshots;

        Published(long generation, Map<RecipeRegistry<?>, RecipeSnapshot<?>> snapshots) {
            this.generation = generation;
            this.snapshots = snapshots;
        }
    }

    private final List<RecipeRegistry<?>> registries = new ArrayList<>();
    private State state = State.LOADING;
    private volatile Published published = new Published(0, Collections.emptyMap());

    public <R> RecipeRegistry<R> create(String station, RecipeRegistry.Indexer<R> indexer) {
        return create(station, indexer, null);
    }

    /** With a validator refusing recipes the station cannot honour, such as input amounts it would ignore. */
    public synchronized <R> RecipeRegistry<R> create(String station, RecipeRegistry.Indexer<R> indexer,
            RecipeRegistry.Validator<R> validator) {
        if (state != State.LOADING) {
            throw new RecipeRegistrationException("Station registries can only be created while mods load");
        }
        for (RecipeRegistry<?> existing : registries) {
            if (existing.getStation().equals(station)) {
                throw new RecipeRegistrationException("Station already has a registry: " + station);
            }
        }
        RecipeRegistry<R> registry = new RecipeRegistry<>(this, station, indexer, validator);
        registries.add(registry);
        return registry;
    }

    /** Every station registry, in creation order. */
    public synchronized List<RecipeRegistry<?>> all() {
        return Collections.unmodifiableList(new ArrayList<>(registries));
    }

    public State getState() {
        return state;
    }

    public long getGeneration() {
        return published.generation;
    }

    public RecipeTransaction begin(RecipeSource source) {
        return new RecipeTransaction(this, source);
    }

    /** Ends mod loading and publishes the base layer. */
    public synchronized void finishLoading() {
        requireState(State.LOADING);
        publishLayers();
        state = State.IDLE;
    }

    /** Starts a script reload: every registry gets a draft copied from its base layer. */
    public synchronized void beginReload() {
        if (state == State.RELOADING) {
            // A reload that never published (script engine aborted): start over from base.
            abortReload();
        }
        requireState(State.IDLE);
        for (RecipeRegistry<?> registry : registries) {
            registry.openDraft();
        }
        state = State.RELOADING;
    }

    /** Publishes the drafts of all registries at once and bumps the generation. */
    public synchronized void publish() {
        requireState(State.RELOADING);
        publishLayers();
        for (RecipeRegistry<?> registry : registries) {
            registry.closeDraft();
        }
        state = State.IDLE;
    }

    /** Drops the drafts; the published set stays as it was. */
    public synchronized void abortReload() {
        requireState(State.RELOADING);
        for (RecipeRegistry<?> registry : registries) {
            registry.closeDraft();
        }
        state = State.IDLE;
    }

    @SuppressWarnings("unchecked")
    <R> RecipeSnapshot<R> published(RecipeRegistry<R> registry) {
        Published current = published;
        RecipeSnapshot<?> snapshot = current.snapshots.get(registry);
        if (snapshot == null) {
            return new RecipeSnapshot<>(current.generation, Collections.emptyList(), recipe -> null);
        }
        return (RecipeSnapshot<R>) snapshot;
    }

    void checkOwned(RecipeRegistry<?> registry) {
        if (!registries.contains(registry)) {
            throw new IllegalArgumentException(registry + " belongs to another RecipeRegistries");
        }
    }

    private void publishLayers() {
        long generation = published.generation + 1;
        Map<RecipeRegistry<?>, RecipeSnapshot<?>> snapshots = new IdentityHashMap<>();
        for (RecipeRegistry<?> registry : registries) {
            snapshots.put(registry, registry.snapshot(generation));
        }
        published = new Published(generation, Collections.unmodifiableMap(snapshots));
    }

    private void requireState(State expected) {
        if (state != expected) {
            throw new IllegalStateException("Recipe registries are " + state + ", expected " + expected);
        }
    }
}
