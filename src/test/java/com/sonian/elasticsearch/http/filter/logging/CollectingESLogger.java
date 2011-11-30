package com.sonian.elasticsearch.http.filter.logging;

import org.elasticsearch.common.logging.support.AbstractESLogger;

/**
 * @author imotov
 */
public class CollectingESLogger extends AbstractESLogger {

    public interface LogMessageCollector {
        public void log(String message);
    }

    private LogMessageCollector out;

    private Level level;

    private String name;

    public enum Level {TRACE, DEBUG, INFO, WARN, ERROR}

    public CollectingESLogger(String level, LogMessageCollector out, String prefix, String name) {
        super(prefix);
        this.out = out;
        this.name = name;
        setLevel(level);
    }

    @Override
    protected void internalTrace(String msg) {
        log("TRACE", msg);
    }

    @Override
    protected void internalTrace(String msg, Throwable cause) {
        log("TRACE", msg, cause);
    }

    @Override
    protected void internalDebug(String msg) {
        log("DEBUG", msg);
    }

    @Override
    protected void internalDebug(String msg, Throwable cause) {
        log("DEBUG", msg, cause);
    }

    @Override
    protected void internalInfo(String msg) {
        log("INFO", msg);
    }

    @Override
    protected void internalInfo(String msg, Throwable cause) {
        log("INFO", msg, cause);
    }

    @Override
    protected void internalWarn(String msg) {
        log("WARN", msg);
    }

    @Override
    protected void internalWarn(String msg, Throwable cause) {
        log("WARN", msg, cause);
    }

    @Override
    protected void internalError(String msg) {
        log("ERROR", msg);
    }

    @Override
    protected void internalError(String msg, Throwable cause) {
        log("ERROR", msg, cause);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setLevel(String level) {
        this.level = Level.valueOf(level);
    }

    @Override
    public boolean isTraceEnabled() {
        return level.ordinal() <= Level.TRACE.ordinal();
    }

    @Override
    public boolean isDebugEnabled() {
        return level.ordinal() <= Level.DEBUG.ordinal();
    }

    @Override
    public boolean isInfoEnabled() {
        return level.ordinal() <= Level.INFO.ordinal();
    }

    @Override
    public boolean isWarnEnabled() {
        return level.ordinal() <= Level.WARN.ordinal();
    }

    @Override
    public boolean isErrorEnabled() {
        return level.ordinal() <= Level.ERROR.ordinal();
    }

    private void log(String level, String message, Throwable cause) {
        out.log(level + ":" + message + "/" + cause);
    }

    private void log(String level, String message) {
        out.log(level + ":" + message);
    }
}
