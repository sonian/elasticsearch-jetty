package com.sonian.elasticsearch.http.filter.logging;

import org.elasticsearch.common.logging.ESLogger;

/**
 * @author imotov
 */
public class RequestLoggingLevel {

    public enum Level {TRACE, DEBUG, INFO, WARN, ERROR}
    private final Level logLevel;
    private final boolean logBody;

    public RequestLoggingLevel(Level logLevel, boolean logBody) {
        this.logLevel = logLevel;
        this.logBody = logBody;
    }

    public Level logLevel() {
        return logLevel;
    }

    public boolean logBody() {
        return logBody;
    }

    public boolean shouldLog(ESLogger logger) {
        switch (logLevel) {
            case TRACE:
                return logger.isTraceEnabled();
            case DEBUG:
                return logger.isDebugEnabled();
            case INFO:
                return logger.isInfoEnabled();
            case WARN:
                return logger.isWarnEnabled();
            case ERROR:
                return logger.isErrorEnabled();
            default:
                return false;
        }
    }

}
