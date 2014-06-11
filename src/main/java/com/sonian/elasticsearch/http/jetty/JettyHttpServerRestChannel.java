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

import org.elasticsearch.http.HttpChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * @author imotov
 */
public class JettyHttpServerRestChannel extends HttpChannel {
    private final RestRequest restRequest;

    private final HttpServletResponse resp;

    private IOException sendFailure;

    private final CountDownLatch latch;

    public JettyHttpServerRestChannel(RestRequest restRequest, HttpServletResponse resp) {
        super(restRequest);
        this.restRequest = restRequest;
        this.resp = resp;
        this.latch = new CountDownLatch(1);
    }

    public void await() throws InterruptedException {
        latch.await();
    }

    public IOException sendFailure() {
        return sendFailure;
    }

    @Override
    public void sendResponse(RestResponse response) {
        resp.setContentType(response.contentType());
        resp.addHeader("Access-Control-Allow-Origin", "*");
        if (response.status() != null) {
            resp.setStatus(response.status().getStatus());
        } else {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        if (restRequest.method() == RestRequest.Method.OPTIONS) {
            // also add more access control parameters
            resp.addHeader("Access-Control-Max-Age", "1728000");
            resp.addHeader("Access-Control-Allow-Methods", "OPTIONS, HEAD, GET, POST, PUT, DELETE");
            resp.addHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Content-Length");
        }
        try {
            int contentLength = response.content().length();
            resp.setContentLength(contentLength);
            ServletOutputStream out = resp.getOutputStream();
            response.content().writeTo(out);
            // TODO: close in a finally?
            out.close();
        } catch (IOException e) {
            sendFailure = e;
        } finally {
            latch.countDown();
        }
     }
}
