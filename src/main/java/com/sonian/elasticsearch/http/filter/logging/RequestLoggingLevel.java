/*
 * Copyright 2011 Sonian Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
