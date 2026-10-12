package minefantasy.mf2.gametest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.common.MinecraftForge;

import minefantasy.mf2.api.heating.AmbientTemperature;

/**
 * Global settings changed for one test and put back when it closes, last first, even when the test fails. Use in a
 * try-with-resources block.
 */
public final class Overrides implements AutoCloseable {

    private final Deque<Runnable> undo = new ArrayDeque<Runnable>();

    /** Sets a static setting, remembering what it was. */
    public <T> Overrides set(Supplier<T> get, Consumer<T> set, T value) {
        T before = get.get();
        undo.push(() -> set.accept(before));
        set.accept(value);
        return this;
    }

    /** Remembers a static setting to restore at close, without changing it yet. */
    public <T> Overrides keep(Supplier<T> get, Consumer<T> set) {
        T before = get.get();
        undo.push(() -> set.accept(before));
        return this;
    }

    /** Runs setup now and undo at close, for swaps without a getter to save. */
    public Overrides run(Runnable setup, Runnable undo) {
        this.undo.push(undo);
        setup.run();
        return this;
    }

    /** Replaces the air temperature everywhere; the provider before it comes back on close. */
    public Overrides air(AmbientTemperature.Provider provider) {
        return set(AmbientTemperature::getProvider, AmbientTemperature::setProvider, provider);
    }

    /** Listens on Forge's event bus until close, as a protection mod or another handler would. */
    public Overrides listen(Object handler) {
        undo.push(() -> MinecraftForge.EVENT_BUS.unregister(handler));
        MinecraftForge.EVENT_BUS.register(handler);
        return this;
    }

    @Override
    public void close() {
        // Every undo runs even when one fails, so a broken restore does not leak the rest into other tests
        RuntimeException failure = null;
        while (!undo.isEmpty()) {
            try {
                undo.pop().run();
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }
}
