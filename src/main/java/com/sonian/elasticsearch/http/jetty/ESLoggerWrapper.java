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
package com.sonian.elasticsearch.http.jetty;


import org.eclipse.jetty.util.log.Logger;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

/**
 * @author imotov
 */
public class ESLoggerWrapper extends AbstractComponent implements Logger  {

    public final ESLogger logger;

    public final Settings settings;

    @Inject
    public ESLoggerWrapper(Settings settings) {
        this("org.eclipse.jetty", settings);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    public ESLoggerWrapper(String name, Settings settings) {
        super(settings);
        this.logger =  Loggers.getLogger(name, settings);
        this.settings = settings;
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
        return new ESLoggerWrapper(name, settings);
    }

    @Override
    public void ignore(Throwable ignored) {

    }
}
