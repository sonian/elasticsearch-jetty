package com.sonian.elasticsearch.http.jetty.error;

import com.sonian.elasticsearch.http.jetty.AbstractJettyHttpServerTests;
import com.sonian.elasticsearch.http.jetty.HttpClientResponse;
import junit.framework.TestCase;
import org.elasticsearch.ElasticSearchException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author drewr
 */
public class JettyHttpServerErrorHandlerTest extends AbstractJettyHttpServerTests {
    @BeforeMethod
    public void startNodes() {
        startNode("server1");
    }

    @AfterMethod
    public void closeNodes() {
        closeAllNodes();
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testErrorHandler() throws Exception {
        HttpClientResponse resp = httpClient("server1").request("PUT", "foo/bar/bizzle");
        assertThat(resp.errorCode(), equalTo(HttpURLConnection.HTTP_UNAUTHORIZED));
        assertThat("response body should be error message",
                   resp.get("body").equals("401 Unauthorized /foo/bar/bizzle"));
    }

}
