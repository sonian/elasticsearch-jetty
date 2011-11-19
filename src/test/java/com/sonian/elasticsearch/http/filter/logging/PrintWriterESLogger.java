package com.sonian.elasticsearch.http.filter.logging;

import org.elasticsearch.common.logging.support.AbstractESLogger;

import java.io.PrintWriter;

/**
 * @author imotov
 */
public class PrintWriterESLogger extends AbstractESLogger {

    private PrintWriter out;

    private Level level;

    private String name;

    public enum Level {TRACE, DEBUG, INFO, WARN, ERROR}

    public PrintWriterESLogger(String level, PrintWriter out, String prefix, String name) {
        super(prefix);
        this.out = out;
        this.name = name;
        setLevel(level);
    }

    @Override
    protected void internalTrace(String msg) {
        out.println("TRACE:" + msg);
        out.flush();
    }

    @Override
    protected void internalTrace(String msg, Throwable cause) {
        out.println("TRACE:" + msg);
        cause.printStackTrace(out);
        out.flush();
    }

    @Override
    protected void internalDebug(String msg) {
        out.println("DEBUG:" + msg);
        out.flush();
    }

    @Override
    protected void internalDebug(String msg, Throwable cause) {
        out.println("DEBUG:" + msg);
        cause.printStackTrace(out);
        out.flush();
    }

    @Override
    protected void internalInfo(String msg) {
        out.println("INFO:" + msg);
        out.flush();
    }

    @Override
    protected void internalInfo(String msg, Throwable cause) {
        out.println("INFO:" + msg);
        cause.printStackTrace(out);
        out.flush();
    }

    @Override
    protected void internalWarn(String msg) {
        out.println("WARN:" + msg);
        out.flush();
    }

    @Override
    protected void internalWarn(String msg, Throwable cause) {
        out.println("WARN:" + msg);
        cause.printStackTrace(out);
        out.flush();
    }

    @Override
    protected void internalError(String msg) {
        out.println("ERROR:" + msg);
        out.flush();
    }

    @Override
    protected void internalError(String msg, Throwable cause) {
        out.println("ERROR:" + msg);
        cause.printStackTrace(out);
        out.flush();
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
}
