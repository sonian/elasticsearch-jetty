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

import org.elasticsearch.common.settings.ImmutableSettings;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.elasticsearch.rest.RestRequest.Method.*;
import static com.sonian.elasticsearch.http.filter.logging.RequestLoggingLevel.Level.*;


/**
 * @author imotov
 */
public class RequestLoggingLevelSettingsTest {

    @Test
    public void testUpdateSettings() throws Exception {
        RequestLoggingLevelSettings settings = new RequestLoggingLevelSettings(ImmutableSettings.settingsBuilder().build());

        settings.updateSettings(ImmutableSettings.settingsBuilder()
                .put("level", "INFO")
                .put("log_body", false)
                .putArray("loggers.stats.path", new String[]{"/_cluster/health", "/_cluster/nodes", "/_cluster/state",
                        "/_cluster/nodes/{node}/stats"})
                .put("loggers.stats.method", "GET")
                .put("loggers.stats.level", "TRACE")

                .put("loggers.count.path", "/_count,/{index}/_count,/{index}/{type}/_count")
                .put("loggers.count.method", "GET,POST")
                .put("loggers.count.log_body", true)

                .put("loggers.mget.path", "/_mget,/{index}/_mget,/{index}/{type}/_mget")
                .put("loggers.mget.method", "GET,POST")
                .put("loggers.mget.log_body", true)

                .build()
        );
        assertThat(settings.getLoggingLevel(GET, "/test").logBody(), equalTo(false));
        assertThat(settings.getLoggingLevel(GET, "/_cluster/health").logBody(), equalTo(false));
        assertThat(settings.getLoggingLevel(GET, "/_cluster/health").logLevel(), equalTo(TRACE));
        assertThat(settings.getLoggingLevel(POST, "/_cluster/health").logLevel(), equalTo(INFO));
        assertThat(settings.getLoggingLevel(GET, "/test").logLevel(), equalTo(INFO));

        assertThat(settings.getLoggingLevel(GET, "/_count").logBody(), equalTo(true));
        assertThat(settings.getLoggingLevel(GET, "/index/_count").logBody(), equalTo(true));
        assertThat(settings.getLoggingLevel(POST, "/index/_count").logBody(), equalTo(true));
        assertThat(settings.getLoggingLevel(DELETE, "/index/_count").logBody(), equalTo(false));
        assertThat(settings.getLoggingLevel(GET, "/index/_count/").logBody(), equalTo(true));
        assertThat(settings.getLoggingLevel(GET, "/index/id/_count").logBody(), equalTo(true));

        assertThat(settings.getLoggingLevel(GET, "/_mget").logBody(), equalTo(true));

    }
}
