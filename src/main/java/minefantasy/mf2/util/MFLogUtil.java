package minefantasy.mf2.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLLog;
import minefantasy.mf2.MineFantasyII;

public class MFLogUtil {

    public static final Logger MF_LOGGER = FMLLog.getLogger();
    private static final ConcurrentMap<String, LogState> LOG_STATES = new ConcurrentHashMap<String, LogState>();

    public static final String PREFIX = "[MineFantasyII]: ";

    public static void log(String mes) {
        FMLLog.info(PREFIX + mes);
        // MF_LOGGER.log(Level.INFO, PREFIX + mes);
    }

    public static void logWarn(String mes) {
        FMLLog.warning(PREFIX + "(warning) " + mes);
        // MF_LOGGER.log(Level.WARN, PREFIX + mes);
    }

    public static void logDebug(String mes) {
        if (MineFantasyII.isDebug()) {
            FMLLog.info(PREFIX + "(debug) " + mes);
            // MF_LOGGER.debug(PREFIX + mes);
        }
    }

    public static void warnThrottled(String key, long intervalMs, String message, Object... args) {
        throttled(LogLevel.WARN, key, intervalMs, null, message, args);
    }

    public static void errorThrottled(String key, long intervalMs, String message, Object... args) {
        throttled(LogLevel.ERROR, key, intervalMs, null, message, args);
    }

    public static void errorThrottled(String key, long intervalMs, Throwable throwable, String message,
            Object... args) {
        throttled(LogLevel.ERROR, key, intervalMs, throwable, message, args);
    }

    public static void debugThrottled(String key, long intervalMs, String message, Object... args) {
        throttled(LogLevel.DEBUG, key, intervalMs, null, message, args);
    }

    public static void warnOnce(String key, String message, Object... args) {
        warnThrottled("once|" + key, Long.MAX_VALUE, message, args);
    }

    public static void errorOnce(String key, String message, Object... args) {
        errorThrottled("once|" + key, Long.MAX_VALUE, message, args);
    }

    private static void throttled(LogLevel level, String key, long intervalMs, Throwable throwable, String message,
            Object... args) {
        if (level == LogLevel.DEBUG && !MineFantasyII.isDebug()) {
            return;
        }
        if (key == null) {
            key = "null-key";
        }
        LogState state = getOrCreateState(key);
        long now = System.currentTimeMillis();
        synchronized (state) {
            if (state.lastLogMs != 0L && now - state.lastLogMs < intervalMs) {
                state.suppressedCount++;
                return;
            }
            long suppressed = state.suppressedCount;
            state.suppressedCount = 0L;
            state.lastLogMs = now;

            String fullMessage = PREFIX + message;
            if (suppressed > 0) {
                fullMessage = fullMessage + " [suppressed " + suppressed + "]";
            }

            if (level == LogLevel.WARN) {
                MF_LOGGER.warn(fullMessage, args);
            } else if (level == LogLevel.ERROR) {
                if (throwable != null) {
                    Object[] withThrowable = appendThrowable(args, throwable);
                    MF_LOGGER.error(fullMessage, withThrowable);
                } else {
                    MF_LOGGER.error(fullMessage, args);
                }
            } else {
                MF_LOGGER.info(fullMessage, args);
            }
        }
    }

    private static Object[] appendThrowable(Object[] args, Throwable throwable) {
        int len = args == null ? 0 : args.length;
        Object[] withThrowable = new Object[len + 1];
        if (len > 0) {
            System.arraycopy(args, 0, withThrowable, 0, len);
        }
        withThrowable[len] = throwable;
        return withThrowable;
    }

    private static LogState getOrCreateState(String key) {
        LogState existing = LOG_STATES.get(key);
        if (existing != null) {
            return existing;
        }
        LogState created = new LogState();
        LogState raced = LOG_STATES.putIfAbsent(key, created);
        return raced == null ? created : raced;
    }

    private static class LogState {

        long lastLogMs;
        long suppressedCount;
    }

    private enum LogLevel {
        WARN,
        ERROR,
        DEBUG
    }

}
