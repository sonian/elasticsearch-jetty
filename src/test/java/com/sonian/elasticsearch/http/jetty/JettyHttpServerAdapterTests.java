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

import static org.elasticsearch.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.elasticsearch.common.collect.MapBuilder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.util.Map;

/**
 * @author imotov
 */
public class JettyHttpServerAdapterTests extends AbstractJettyHttpServerTests {

    @BeforeMethod
    public void startNodes() {
        startNode("server1");
    }

    @AfterMethod
    public void closeNodes() {
        closeAllNodes();
    }


    @Test
    public void testClusterHealth() throws Exception {
        HttpClientResponse response = httpClient("server1").request("_cluster/health");
        assertThat(
                (String) response.get("status"),
                equalTo("green")
        );

    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testIndexingAndSearching() throws Exception {
        Map<String, Object> settings = MapBuilder.<String, Object>newMapBuilder().put("settings",
                MapBuilder.newMapBuilder().put("index",
                        MapBuilder.newMapBuilder()
                                .put("number_of_shards", 1)
                                .put("number_of_replicas", 0)
                                .immutableMap()).immutableMap()
        ).map();
        // Create Index

        HttpClientResponse response = httpClient("server1", "user", "Passw0rd").request("PUT", "testidx", settings);
        assertThat((Boolean) response.get("acknowledged"), equalTo(true));
        client("server1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();

        Map<String, Object> data = newHashMap();
        data.put("id", "1");
        data.put("message", "test");
        response = httpClient("server1", "user", "Passw0rd").request("PUT", "testidx/msg/1?refresh=true", data);
        assertThat((Boolean) response.get("created"), equalTo(true));

        response = httpClient("server1").request("GET", "testidx/msg/_search?q=*:*");
        assertThat((Integer)((Map<String, Object>) response.get("hits")).get("total"), equalTo(1));
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testIndexingCreationFailsWithoutPassword() throws Exception {
        Map<String, Object> settings = MapBuilder.<String, Object>newMapBuilder().put("settings",
                MapBuilder.newMapBuilder().put("index",
                        MapBuilder.newMapBuilder()
                                .put("number_of_shards", 1)
                                .put("number_of_replicas", 0)
                                .immutableMap()).immutableMap()
        ).map();
        // Create Index

        HttpClientResponse response;
        response = httpClient("server1").request("PUT", "testidx", settings);
        assertThat(response.errorCode(), equalTo(HttpURLConnection.HTTP_UNAUTHORIZED));

        response = httpClient("server1", "user", "Passw0rd").request("PUT", "testidx", settings);
        assertThat((Boolean) response.get("acknowledged"), equalTo(true));
        client("server1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();

        Map<String, Object> data = newHashMap();
        data.put("id", "1");
        data.put("message", "test");
        response = httpClient("server1", "user", "Passw0rd").request("PUT", "testidx/msg/1?refresh=true", data);
        assertThat((Boolean) response.get("created"), equalTo(true));

        data = newHashMap();
        data.put("id", "2");
        data.put("message", "test");
        response = httpClient("server1").request("PUT", "testidx/msg/2?refresh=true", data);
        assertThat(response.errorCode(), equalTo(HttpURLConnection.HTTP_UNAUTHORIZED));

        response = httpClient("server1").request("GET", "testidx/msg/_search?q=*:*");
        assertThat((Integer)((Map<String, Object>) response.get("hits")).get("total"), equalTo(1));
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testDefaultPermissions() throws Exception {
        HttpClientResponse response = httpClient("server1").request("POST", "_cluster/health");
        assertThat(response.errorCode(), equalTo(HttpURLConnection.HTTP_UNAUTHORIZED));
    }
}
