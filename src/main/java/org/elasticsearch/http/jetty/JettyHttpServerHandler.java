package org.elasticsearch.http.jetty;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author imotov
 */
public class JettyHttpServerHandler extends AbstractHandler {

    private final JettyHttpServerTransport serverTransport;

    public JettyHttpServerHandler(JettyHttpServerTransport serverTransport) {
        this.serverTransport = serverTransport;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JettyHttpServerRestRequest restRequest = new JettyHttpServerRestRequest(request);
        JettyHttpServerRestChannel restChannel = new JettyHttpServerRestChannel(restRequest, response);
        try {
            serverTransport.dispatchRequest(restRequest, restChannel);
            restChannel.await();
        } catch (InterruptedException e) {
            throw new ServletException("failed to dispatch request", e);
        } catch (Exception e) {
            throw new IOException("failed to dispatch request", e);
        }
    }
}
