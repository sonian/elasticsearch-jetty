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
package com.sonian.elasticsearch.http.jetty.error;

import com.sonian.elasticsearch.http.jetty.AbstractJettyHttpServerTests;
import com.sonian.elasticsearch.http.jetty.HttpClientResponse;
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
