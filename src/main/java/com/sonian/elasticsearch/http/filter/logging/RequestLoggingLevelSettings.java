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

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.path.PathTrie;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestRequest;

/**
 * @author imotov
 */
public class RequestLoggingLevelSettings extends AbstractComponent {
    private final PathTrie<RequestLoggingLevel> getLoggingLevels = new PathTrie<RequestLoggingLevel>();
    private final PathTrie<RequestLoggingLevel> postLoggingLevels = new PathTrie<RequestLoggingLevel>();
    private final PathTrie<RequestLoggingLevel> putLoggingLevels = new PathTrie<RequestLoggingLevel>();
    private final PathTrie<RequestLoggingLevel> deleteLoggingLevels = new PathTrie<RequestLoggingLevel>();
    private final PathTrie<RequestLoggingLevel> optionsLoggingLevels = new PathTrie<RequestLoggingLevel>();
    private final PathTrie<RequestLoggingLevel> headLoggingLevels = new PathTrie<RequestLoggingLevel>();

    private volatile RequestLoggingLevel defaultLoggingLevel;

    @Inject
    public RequestLoggingLevelSettings(Settings settings) {
        super(settings);
        defaultLoggingLevel = new RequestLoggingLevel(RequestLoggingLevel.Level.INFO, false);
        updateSettings(componentSettings);
    }

    public void updateSettings(Settings settings) {
        if (settings != null) {
            RequestLoggingLevel.Level level = RequestLoggingLevel.Level.valueOf(settings.get("level", "INFO"));
            boolean logBody = settings.getAsBoolean("log_body", false);
            defaultLoggingLevel = new RequestLoggingLevel(level, logBody);
            for (Settings logger : settings.getGroups("loggers").values()) {
                for(String path : logger.getAsArray("path")) {
                    for(String method: logger.getAsArray("method")) {
                        RequestLoggingLevel loggingLevel = new RequestLoggingLevel(
                                getLoggingLevel(logger, "level", level),
                                logger.getAsBoolean("log_body", logBody));
                        register(path, method, loggingLevel);
                    }
                }
            }
        }
    }

    private void register(String path, String method, RequestLoggingLevel loggingLevel) {
        try {
            if(method.equalsIgnoreCase("GET")) {
                getLoggingLevels.insert(path, loggingLevel);
            } else if(method.equalsIgnoreCase("POST")) {
                postLoggingLevels.insert(path, loggingLevel);
            } else if(method.equalsIgnoreCase("PUT")) {
                putLoggingLevels.insert(path, loggingLevel);
            } else if(method.equalsIgnoreCase("DELETE")) {
                deleteLoggingLevels.insert(path, loggingLevel);
            } else if(method.equalsIgnoreCase("OPTIONS")) {
                optionsLoggingLevels.insert(path, loggingLevel);
            } else if(method.equalsIgnoreCase("HEAD")) {
                headLoggingLevels.insert(path, loggingLevel);
            } else {
                logger.warn("Unknown method name "+ method);
            }
        } catch (AssertionError e) {
            logger.warn("Ambiguous path " + path + " for method " + method );
        }
    }

    public RequestLoggingLevel getLoggingLevel(RestRequest.Method method, String path) {
        switch (method) {
            case GET:
                return getLoggingLevel(getLoggingLevels, path);
            case POST:
                return getLoggingLevel(postLoggingLevels, path);
            case PUT:
                return getLoggingLevel(putLoggingLevels, path);
            case DELETE:
                return getLoggingLevel(deleteLoggingLevels, path);
            case OPTIONS:
                return getLoggingLevel(optionsLoggingLevels, path);
            case HEAD:
                return getLoggingLevel(headLoggingLevels, path);
        }
        return defaultLoggingLevel;
    }

    private RequestLoggingLevel getLoggingLevel(PathTrie<RequestLoggingLevel> loggingLevels, String path) {
        RequestLoggingLevel loggingLevel = loggingLevels.retrieve(path);
        if(loggingLevel != null) {
            return loggingLevel;
        } else {
            return defaultLoggingLevel;
        }
    }

    private RequestLoggingLevel.Level getLoggingLevel(Settings settings, String setting, RequestLoggingLevel.Level defaultLevel) {
        if(settings.get(setting) != null) {
            return RequestLoggingLevel.Level.valueOf(settings.get("level", "INFO"));
        } else {
            return defaultLevel;
        }
    }
}
