package com.sonian.elasticsearch.http.jetty.security;

import com.sonian.elasticsearch.http.jetty.AbstractJettyHttpServerTests;
import com.sonian.elasticsearch.http.jetty.HttpClient;
import com.sonian.elasticsearch.http.jetty.HttpClientResponse;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author drewr
 */
public class ESLoginServiceTests extends AbstractJettyHttpServerTests {
    @BeforeMethod
    public void setup() {
        startNode("server1", ImmutableSettings
                .settingsBuilder()
                .put("sonian.elasticsearch.http.jetty.config", "jetty-esauth.xml"));
    }

    @AfterMethod
    public void stop() {
        closeAllNodes();
    }

    @Test
    public void testSuccess() {
        publishAuth("server1", "foo", "MD5:37b51d194a7513e45b56f6524f2d51f2", "readwrite:pray:love"); // password bar
        HttpClient http = httpClient("server1", "foo", "bar");
        String data;
        try {
            data = jsonBuilder().startObject().field("blip", 1).endObject().string();
        } catch (IOException e) {
            throw new ElasticSearchException("", e);
        }
        HttpClientResponse resp = http.request("PUT", "/foo/bar/1", data.getBytes());
        assertThat(resp.errorCode(), equalTo(201));
    }

    @Test
    public void testFail() {
        publishAuth("server1", "foo", "MD5:37b51d194a7513e45b56f6524f2d51f2", "readwrite"); // password bar
        HttpClient http = httpClient("server1", "foo", "WRONG");
        String data;
        try {
            data = jsonBuilder().startObject().field("blip", 1).endObject().string();
        } catch (IOException e) {
            throw new ElasticSearchException("", e);
        }
        HttpClientResponse resp = http.request("PUT", "/foo/bar/1", data.getBytes());
        assertThat(resp.errorCode(), equalTo(401));
    }
}
