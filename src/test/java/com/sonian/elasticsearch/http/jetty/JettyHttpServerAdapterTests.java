package com.sonian.elasticsearch.http.jetty;

import static org.elasticsearch.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.collect.MapBuilder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
        Map<String, Object> response = httpClient("server1").request("_cluster/health");
        assertThat(
                (String) response.get("status"),
                equalTo("green")
        );

    }

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

        Map<String, Object> response = httpClient("server1", "user", "Passw0rd").request("PUT", "testidx", settings);
        assertThat((Boolean) response.get("ok"), equalTo(true));
        client("server1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();

        Map<String, Object> data = newHashMap();
        data.put("id", "1");
        data.put("message", "test");
        response = httpClient("server1", "user", "Passw0rd").request("PUT", "testidx/msg/1?refresh=true", data);
        assertThat((Boolean) response.get("ok"), equalTo(true));

        response = httpClient("server1").request("GET", "testidx/msg/_search?q=*:*");
        assertThat((Integer)((Map<String, Object>) response.get("hits")).get("total"), equalTo(1));
    }

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

        try {
            httpClient("server1").request("PUT", "testidx", settings);
            assertThat("Should throw access denied exception", false);
        } catch (ElasticSearchException ex) {
            assertThat(ex.getMessage(), equalTo("HTTP 401"));
        }

        Map<String, Object> response = httpClient("server1", "user", "Passw0rd").request("PUT", "testidx", settings);
        assertThat((Boolean) response.get("ok"), equalTo(true));
        client("server1").admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();

        Map<String, Object> data = newHashMap();
        data.put("id", "1");
        data.put("message", "test");
        response = httpClient("server1", "user", "Passw0rd").request("PUT", "testidx/msg/1?refresh=true", data);
        assertThat((Boolean) response.get("ok"), equalTo(true));

        try {
            data = newHashMap();
            data.put("id", "2");
            data.put("message", "test");
            httpClient("server1").request("PUT", "testidx/msg/2?refresh=true", data);
            assertThat("Should throw access denied exception", false);
        } catch (ElasticSearchException ex) {
            assertThat(ex.getMessage(), equalTo("HTTP 401"));
        }

        response = httpClient("server1").request("GET", "testidx/msg/_search?q=*:*");
        assertThat((Integer)((Map<String, Object>) response.get("hits")).get("total"), equalTo(1));


    }
}
