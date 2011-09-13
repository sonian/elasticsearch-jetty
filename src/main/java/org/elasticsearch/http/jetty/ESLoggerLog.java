package org.elasticsearch.http.jetty;


import org.eclipse.jetty.util.log.Logger;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

/**
 * @author imotov
 */
public class ESLoggerLog implements Logger {

    public final ESLogger logger;

    public final Settings settings;

    public ESLoggerLog(String name, Settings settings) {
        this.logger =  Loggers.getLogger(name, settings);
        this.settings = settings;
    }

    public ESLoggerLog(Settings settings) {
        this("org.eclipse.jetty", settings);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public void warn(String msg, Object... args) {
        logger.warn(msg, args);
    }

    @Override
    public void warn(Throwable thrown) {
        logger.warn("", thrown);
    }

    @Override
    public void warn(String msg, Throwable thrown) {
        logger.warn(msg, thrown);
    }

    @Override
    public void info(String msg, Object... args) {
        logger.info(msg, args);
    }

    @Override
    public void info(Throwable thrown) {
        logger.info("", thrown);
    }

    @Override
    public void info(String msg, Throwable thrown) {
        logger.info(msg, thrown);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void setDebugEnabled(boolean enabled) {
        // Ignore
    }

    @Override
    public void debug(String msg, Object... args) {
        logger.debug(msg, args);
    }

    @Override
    public void debug(Throwable thrown) {
        logger.debug("", thrown);
    }

    @Override
    public void debug(String msg, Throwable thrown) {
        logger.debug(msg, thrown);
    }

    @Override
    public Logger getLogger(String name) {
        return new ESLoggerLog(name, settings);
    }

    @Override
    public void ignore(Throwable ignored) {

    }
}
