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
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.logging.log4j.Log4jESLoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author imotov
 */

public class MockESLoggerFactory extends ESLoggerFactory {
    protected final ESLogger logger = Loggers.getLogger(getClass());

    private final ESLoggerFactory realFactory = new Log4jESLoggerFactory();

    private final QueueMessageCollector messageCollector = new QueueMessageCollector();

    private final String level;

    private final String suffix;

    public MockESLoggerFactory(String level, String suffix) {
        this.level = level;
        this.suffix = suffix;
    }

    @Override
    protected ESLogger newInstance(String prefix, String name) {
        if (name.endsWith(suffix)) {
            logger.info("Asked for Logger " + prefix + ":" + name);
            return new CollectingESLogger(level, messageCollector, prefix, name);
        }
        return realFactory.newInstance(name);
    }

    public String getMessage(int timeout, TimeUnit unit) {
        try {
            return messageCollector.getMessage(timeout, unit);
        } catch (InterruptedException ex) {
            return null;
        }
    }

    public String getMessage() {
        return getMessage(1, TimeUnit.SECONDS);
    }

    public ESLoggerFactory getRealFactory() {
        return this.realFactory;
    }

    private class QueueMessageCollector implements CollectingESLogger.LogMessageCollector {
        private LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<String>();

        public void log(String message) {
            try {
                messages.put(message);
            } catch (InterruptedException ex) {
                // Ignore
            }
        }

        public String getMessage(int timeout, TimeUnit unit) throws InterruptedException{
            return messages.poll(timeout, unit);
        }

    }

    @Override
    protected ESLogger rootLogger() {
        Method m;
        try {
            m = realFactory.getClass().getMethod("rootLogger");
            return (ESLogger) m.invoke(realFactory);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

