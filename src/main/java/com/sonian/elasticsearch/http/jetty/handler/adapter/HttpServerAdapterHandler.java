package com.sonian.elasticsearch.http.jetty.handler.adapter;

import org.eclipse.jetty.server.Request;
import org.elasticsearch.http.HttpServerAdapter;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerRestChannel;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerRestRequest;
import com.sonian.elasticsearch.http.jetty.handler.AbstractJettyHttpServerTransportHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author imotov
 */
public class HttpServerAdapterHandler extends AbstractJettyHttpServerTransportHandler {

    @Override
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

}
