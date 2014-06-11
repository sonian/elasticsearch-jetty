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
import org.elasticsearch.cluster.ClusterName;
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

    private final String clusterName;

    @Inject
    public LoggingFilterHttpServerAdapter(Settings settings, @Assisted String name, @Assisted Settings filterSettings,
                                          RequestLoggingLevelSettings requestLoggingLevelSettings, ClusterName clusterName) {
        String loggerName = filterSettings.get("logger", Classes.getPackageName(getClass()));
        this.logFormat = filterSettings.get("format", "text");
        if (logFormat.equals("json")) {
            this.logger = Loggers.getLogger(loggerName);
        } else {
            this.logger = Loggers.getLogger(loggerName, settings);
        }

        this.clusterName = clusterName.value();

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

    private class LoggingHttpChannel extends HttpChannel {
        private final HttpRequest request;
        
        private final HttpChannel channel;

        private final String method;

        private final String path;

        private final StringBuilder params;

        private final long timestamp;

        private final String format;

        private final String content;

        private final String localaddr;

        private final long localport;

        private final String remoteaddr;

        private final long remoteport;

        private final String scheme;

        private final String remoteuser;

        private final String opaqueId;

        public LoggingHttpChannel(HttpRequest request, HttpChannel channel, String format, boolean logBody) {
            super(request);
            this.channel = channel;
            this.request = request;

            this.format = format;
            method = request.method().name();
            path = request.rawPath();
            params = mapToString(request.params());
            timestamp = System.currentTimeMillis();
            if(logBody) {
                content = request.content().toUtf8();
            } else {
                content = null;
            }

            JettyHttpServerRestRequest req = (JettyHttpServerRestRequest) request;
            localaddr = req.localAddr();
            localport = req.localPort();
            remoteaddr = req.remoteAddr();
            remoteport = req.remotePort();
            scheme = req.scheme();
            remoteuser = req.remoteUser();
            opaqueId = req.opaqueId();
        }

        public void logText(RestResponse response, long contentLength, long now) {
            long latency = now - timestamp;
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

        public void logJson(RestResponse response, long contentLength, long now) {
            long latency = now - timestamp;
            DateTime nowdt = new DateTime(now);
            DateTime startdt = new DateTime(timestamp);
            try {
                XContentBuilder json = XContentFactory.jsonBuilder().startObject();
                json.field("time", nowdt.toDateTimeISO().toString());
                json.field("starttime", startdt.toDateTimeISO().toString());
                json.field("localaddr", localaddr);
                json.field("localport", localport);
                json.field("remoteaddr", remoteaddr);
                json.field("remoteport", remoteport);
                json.field("scheme", scheme);
                json.field("method", method);
                json.field("path", path);
                json.field("querystr", params);
                json.field("code", response.status().getStatus());
                json.field("status", response.status());
                json.field("size", contentLength);
                json.field("duration", latency);
                json.field("year", nowdt.toString("yyyy"));
                json.field("month", nowdt.toString("MM"));
                json.field("day", nowdt.toString("dd"));
                json.field("hour", nowdt.toString("HH"));
                json.field("minute", nowdt.toString("mm"));
                json.field("dow", nowdt.toString("EEE"));
                json.field("cluster", clusterName);
                if (remoteuser != null) {
                    json.field("user", remoteuser);
                }
                if (opaqueId != null) {
                    json.field("opaque-id", opaqueId);
                }
                if (content != null) {
                    json.field("data", content);
                }
                json.endObject();
                logger.info(json.string());
            } catch (IOException e) {
                logger.info("## Could not serialize to json: {} {} {} {} {} {} {} {} [{}]",
                        nowdt.toDateTimeISO().toString(),
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
                contentLength = response.content().length();
            } catch (RuntimeException ex) {
                // Ignore
            }
            channel.sendResponse(response);
            long now = System.currentTimeMillis();

            if (this.format.equals("json")) {
                logJson(response, contentLength, now);
            } else {
                logText(response, contentLength, now);
            }
        }
    }
}
