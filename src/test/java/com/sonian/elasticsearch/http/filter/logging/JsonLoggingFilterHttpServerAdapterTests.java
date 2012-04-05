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

import com.sonian.elasticsearch.http.filter.FilterHttpServerTransport;
import com.sonian.elasticsearch.http.filter.FilterHttpServerTransportModule;
import com.sonian.elasticsearch.http.jetty.AbstractJettyHttpServerTests;
import com.sonian.elasticsearch.http.jetty.HttpClientResponse;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.node.internal.InternalNode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static com.sonian.elasticsearch.http.filter.logging.RequestLoggingLevel.Level.TRACE;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author imotov
 */
public class JsonLoggingFilterHttpServerAdapterTests extends AbstractJettyHttpServerTests {

    private MockESLoggerFactory mockESLoggerFactory;

    @BeforeMethod
    public void setup() {
        mockESLoggerFactory = new MockESLoggerFactory("INFO", "com.sonian.elasticsearch.http.filter.jsonlog");
        ESLoggerFactory.setDefaultFactory(mockESLoggerFactory);
        putDefaultSettings(ImmutableSettings.settingsBuilder()
                .put("http.type", FilterHttpServerTransportModule.class.getName())
        );
    }

    @AfterMethod
    public void closeNodes() {
        closeAllNodes();
        ESLoggerFactory.setDefaultFactory(mockESLoggerFactory.getRealFactory());
    }

    @Test
    public void testClusterHealth() throws Exception {
        startNode("server1");
        createTestIndex();
        // Shouldn't log cluster health call
        HttpClientResponse response = httpClient("server1").request("_cluster/health");
        assertThat((String) response.get("status"), equalTo("green"));

        Map<String, Object> data = createSearchQuery("user:kimchy");
        httpClient("server1").request("POST", "_search", data);
        // Should start with logging for the POST /_search request
        Map<String, Object> logJson = XContentFactory.xContent(XContentType.JSON)
                .createParser(mockESLoggerFactory.getMessage().substring(5)).mapAndClose();
        assertThat((Integer) logJson.get("size"), greaterThan(100));
        assertThat((String) logJson.get("data"), equalTo("{\"query\":{\"query_string\":{\"query\":\"user:kimchy\"}}}"));
    }
}
