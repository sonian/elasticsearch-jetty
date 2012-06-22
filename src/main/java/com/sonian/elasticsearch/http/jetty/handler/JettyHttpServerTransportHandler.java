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
package com.sonian.elasticsearch.http.jetty.handler;

import com.sonian.elasticsearch.http.jetty.JettyHttpServerRestChannel;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerRestRequest;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.elasticsearch.common.Classes;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerTransport;
import org.elasticsearch.http.HttpServerAdapter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author imotov
 */
public class JettyHttpServerTransportHandler extends AbstractHandler {

    private volatile JettyHttpServerTransport transport;

    protected volatile ESLogger logger;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        Server server = getServer();
        // JettyHttpServerTransport can be either set explicitly in jetty.xml or obtained from server
        if (transport == null) {
            JettyHttpServerTransport transport = (JettyHttpServerTransport) server.getAttribute(JettyHttpServerTransport.TRANSPORT_ATTRIBUTE);
            if (transport == null) {
                throw new IllegalArgumentException("Transport is not specified");
            }
            setTransport(transport);
        }
    }

    @Override
    protected void doStop() throws Exception {
        this.transport = null;
        this.logger = null;
        super.doStop();
    }

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpServerAdapter adapter = getTransport().httpServerAdapter();
        JettyHttpServerRestRequest restRequest = new JettyHttpServerRestRequest(request);
        JettyHttpServerRestChannel restChannel = new JettyHttpServerRestChannel(restRequest, response);
        try {
            adapter.dispatchRequest(restRequest, restChannel);
            restChannel.await();
        } catch (InterruptedException e) {
            throw new ServletException("failed to dispatch request", e);
        } catch (Exception e) {
            throw new IOException("failed to dispatch request", e);
        }
        if (restChannel.sendFailure() != null) {
            throw restChannel.sendFailure();
        }
    }


    public JettyHttpServerTransport getTransport() {
        return transport;
    }

    public void setTransport(JettyHttpServerTransport transport) {
        this.transport = transport;
        this.logger = Loggers.getLogger(buildClassLoggerName(getClass()), transport.settings());
    }

    private static String buildClassLoggerName(Class clazz) {
        return Classes.getPackageName(clazz);
    }
}
