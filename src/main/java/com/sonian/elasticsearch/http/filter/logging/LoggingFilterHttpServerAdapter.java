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

import com.sonian.elasticsearch.http.filter.FilterChain;
import com.sonian.elasticsearch.http.filter.FilterHttpServerAdapter;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerRestRequest;
import org.elasticsearch.common.Classes;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.http.HttpChannel;
import org.elasticsearch.http.HttpRequest;
import org.elasticsearch.rest.RestResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * @author imotov
 */
public class LoggingFilterHttpServerAdapter implements FilterHttpServerAdapter {
    protected volatile ESLogger logger;

    private final RequestLoggingLevelSettings requestLoggingLevelSettings;

    private final String logFormat;

    @Inject
    public LoggingFilterHttpServerAdapter(Settings settings, @Assisted String name, @Assisted Settings filterSettings, RequestLoggingLevelSettings requestLoggingLevelSettings) {
        String loggerName = filterSettings.get("logger", Classes.getPackageName(getClass()));
        this.logFormat = filterSettings.get("format", "text");
        if (logFormat.equals("json")) {
            this.logger = Loggers.getLogger(loggerName);
        } else {
            this.logger = Loggers.getLogger(loggerName, settings);
        }

        this.requestLoggingLevelSettings = requestLoggingLevelSettings;
        requestLoggingLevelSettings.updateSettings(filterSettings);
    }

    public RequestLoggingLevelSettings requestLoggingLevelSettings() {
        return requestLoggingLevelSettings;
    }

    @Override
    public void doFilter(HttpRequest request, HttpChannel channel, FilterChain filterChain) {
        RequestLoggingLevel level = requestLoggingLevelSettings.getLoggingLevel(request.method(), request.path());
        if (level.shouldLog(logger)) {
            filterChain.doFilter(request, new LoggingHttpChannel(request, channel, this.logFormat, level.logBody()));
        } else {
            filterChain.doFilter(request, channel);
        }
    }

    private StringBuilder mapToString(Map<String, String> params) {
        StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (first) {
                first = false;
            } else {
                buf.append("&");
            }

            buf.append(param.getKey());
            buf.append("=");
            try {
                buf.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                logger.error("UnsupportedEncodingException", ex);
            }
        }
        if (first) {
            buf.append("-");
        }
        return buf;
    }

    private class LoggingHttpChannel implements HttpChannel {
        private final HttpRequest request;
        
        private final HttpChannel channel;

        private final String method;

        private final String path;

        private final StringBuilder params;

        private final long timestamp;

        private final String format;

        private final String content;

        public LoggingHttpChannel(HttpRequest request, HttpChannel channel, String format, boolean logBody) {
            this.channel = channel;
            this.request = request;
            this.format = format;
            method = request.method().name();
            path = request.rawPath();
            params = mapToString(request.params());
            timestamp = System.currentTimeMillis();
            if(logBody) {
                content = request.contentAsString();
            } else {
                content = null;
            }
        }

        public void logText(RestResponse response, long contentLength, long latency) {
            if(content != null) {
                logger.info("{} {} {} {} {} {} {} [{}]",
                        method,
                        path,
                        params,
                        response.status().getStatus(),
                        response.status(),
                        contentLength >= 0 ? contentLength : "-",
                        latency,
                        content);
            } else {
                logger.info("{} {} {} {} {} {} {}",
                        method,
                        path,
                        params,
                        response.status().getStatus(),
                        response.status(),
                        contentLength >= 0 ? contentLength : "-",
                        latency);
            }

        }

        public void logJson(RestResponse response, long contentLength, long latency) {
            DateTime now = new DateTime();
            DateTime start = new DateTime(timestamp);
            JettyHttpServerRestRequest req = (JettyHttpServerRestRequest) request;
            try {
                XContentBuilder json = XContentFactory.jsonBuilder().startObject();
                json.field("time", now.toDateTimeISO().toString());
                json.field("starttime", start.toDateTimeISO().toString());
                json.field("local_addr", req.localAddr());
                json.field("local_port", req.localPort());
                json.field("remote_addr", req.remoteAddr());
                json.field("remote_port", req.remotePort());
                if (req.remoteUser() != null) {
                    json.field("user", req.remoteUser());
                }
                json.field("scheme", req.scheme());
                json.field("method", method);
                json.field("path", path);
                json.field("querystr", params);
                json.field("code", response.status().getStatus());
                json.field("status", response.status());
                json.field("size", contentLength);
                json.field("duration", latency);
                json.field("year", now.toString("yyyy"));
                json.field("month", now.toString("MM"));
                json.field("day", now.toString("dd"));
                json.field("hour", now.toString("HH"));
                json.field("minute", now.toString("mm"));
                json.field("dow", now.toString("EEE"));
                if (content != null) {
                    json.field("data", content);
                }
                json.endObject();
                logger.info(json.string());
            } catch (IOException e) {
                logger.info("## Could not serialize to json: {} {} {} {} {} {} {} {} [{}]",
                        now.toDateTimeISO().toString(),
                        method,
                        path,
                        params,
                        response.status().getStatus(),
                        response.status(),
                        contentLength >= 0 ? contentLength : "-",
                        latency,
                        content);
            }
        }

        @Override
        public void sendResponse(RestResponse response) {
            int contentLength = -1;
            try {
                contentLength = response.contentLength();
            } catch (IOException ex) {
                // Ignore
            }
            channel.sendResponse(response);
            long latency = System.currentTimeMillis() - timestamp;

            if (this.format.equals("json")) {
                logJson(response, contentLength, latency);
            } else {
                logText(response, contentLength, latency);
            }
        }
    }
}
