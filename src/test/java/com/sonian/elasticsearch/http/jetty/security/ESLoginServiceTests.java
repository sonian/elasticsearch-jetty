package com.sonian.elasticsearch.http.jetty.security;

import com.sonian.elasticsearch.http.jetty.AbstractJettyHttpServerTests;
import com.sonian.elasticsearch.http.jetty.HttpClient;
import com.sonian.elasticsearch.http.jetty.HttpClientResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author drewr
 */
public class ESLoginServiceTests extends AbstractJettyHttpServerTests {
    @BeforeMethod
    public void setup() {
        startNode("server1", ImmutableSettings
                .settingsBuilder()
                .put("sonian.elasticsearch.http.jetty.config", "jetty.xml,jetty-es-auth.xml,jetty-restrict-writes.xml"));
    }

    @AfterMethod
    public void stop() {
        closeAllNodes();
    }

    @Test
    public void testSuccess() throws Exception {
        publishAuth("server1", "foo", "MD5:37b51d194a7513e45b56f6524f2d51f2", "pray:readwrite:love"); // password bar
        HttpClient http = httpClient("server1", "foo", "bar");
        String data;
        data = jsonBuilder().startObject().field("blip", 1).endObject().string();
        HttpClientResponse resp = http.request("PUT", "/foo/bar/1", data.getBytes());
        assertThat(resp.errorCode(), equalTo(201));
    }

    @Test
    public void testFail()  throws Exception {
        publishAuth("server1", "foo", "MD5:37b51d194a7513e45b56f6524f2d51f2", "readwrite"); // password bar
        HttpClient http = httpClient("server1", "foo", "WRONG");
        String data;
        data = jsonBuilder().startObject().field("blip", 1).endObject().string();
        HttpClientResponse resp = http.request("PUT", "/foo/bar/1", data.getBytes());
        assertThat(resp.errorCode(), equalTo(401));
    }

    @Test
    public void testTwoUsers()  throws Exception {
        publishAuth("server1", "john", "password1", "readwrite");
        publishAuth("server1", "jane", "password2", "readwrite");
        String data;
        data = jsonBuilder().startObject().field("blip", 1).endObject().string();

        HttpClient http = httpClient("server1", "john", "password1");
        HttpClientResponse resp = http.request("PUT", "/foo/bar/1", data.getBytes());
        assertThat(resp.errorCode(), equalTo(201));

        http = httpClient("server1", "jane", "password2");
        resp = http.request("PUT", "/foo/bar/2", data.getBytes());
        assertThat(resp.errorCode(), equalTo(201));

        http = httpClient("server1", "john", "password2");
        resp = http.request("PUT", "/foo/bar/3", data.getBytes());
        assertThat(resp.errorCode(), equalTo(401));

        http = httpClient("server1", "jane", "password1");
        resp = http.request("PUT", "/foo/bar/4", data.getBytes());
        assertThat(resp.errorCode(), equalTo(401));

        http = httpClient("server1", "JaNe", "password2");
        resp = http.request("PUT", "/foo/bar/4", data.getBytes());
        assertThat(resp.errorCode(), equalTo(401));
    }

    @Test
    public void testEmptyPassword()  throws Exception {
        publishAuth("server1", "foo", null, "readwrite");
        String data;
        data = jsonBuilder().startObject().field("blip", 1).endObject().string();

        HttpClient http = httpClient("server1");
        HttpClientResponse resp = http.request("PUT", "/foo/bar/1", data.getBytes());
        assertThat(resp.errorCode(), equalTo(401));

        http = httpClient("server1", "foo", "");
        resp = http.request("PUT", "/foo/bar/1", data.getBytes());
        assertThat(resp.errorCode(), equalTo(401));
    }

    @Test
    public void testEmptyRoles()  throws Exception {
        publishAuth("server1", "foo", "bar", null);
        String data;
        data = jsonBuilder().startObject().field("blip", 1).endObject().string();

        HttpClient http = httpClient("server1", "foo", "bar");
        HttpClientResponse resp = http.request("PUT", "/foo/bar/1", data.getBytes());
        assertThat(resp.errorCode(), equalTo(403));

        http = httpClient("server1", "foo", "bar");
        resp = http.request("PUT", "/foo/bar/1", data.getBytes());
        assertThat(resp.errorCode(), equalTo(403));
    }

    protected void publishAuth(String server, String user, String pass, String roles) throws IOException {
        final String idx = "auth";
        XContentBuilder contentBuilder =  XContentFactory.jsonBuilder().startObject();
        if (pass != null) {
            contentBuilder.field("password", pass);
        }
        if (roles != null) {
            contentBuilder.field("roles", roles.split(":"));
        }
        contentBuilder.endObject();

        client(server).prepareIndex().setIndex(idx).setType("user").setId(user)
                .setSource(contentBuilder)
                .execute().actionGet();
        client(server).admin().indices().prepareRefresh(idx).execute().actionGet();
    }
}
