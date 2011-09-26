package com.sonian.elasticsearch.http.jetty.handler.logging;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.elasticsearch.common.Unicode;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerRestRequest;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerTransport;
import com.sonian.elasticsearch.http.jetty.handler.AbstractJettyHttpServerTransportHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author imotov
 */
public class RequestLogHandler extends AbstractJettyHttpServerTransportHandler {

    private volatile boolean logRequests;

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if(logRequests && logger.isInfoEnabled()) {
            Response baseResponse = (Response) response;
            long latency = System.currentTimeMillis() - baseRequest.getTimeStamp();
            String content = Unicode.fromBytes((byte[]) request.getAttribute(JettyHttpServerRestRequest.REQUEST_CONTENT_ATTRIBUTE));
            logger.info("{} {} {} {} {} {} [{}]",
                    request.getMethod(),
                    request.getPathInfo(),
                    request.getQueryString(),
                    baseResponse.getStatus(),
                    baseResponse.getContentCount() >= 0 ? baseResponse.getContentCount() : "-",
                    latency,
                    content
                    );
        }
    }

    @Override
    public void setTransport(JettyHttpServerTransport transport) {
        super.setTransport(transport);
        logRequests = getTransport().componentSettings().getAsBoolean("request_log.enabled", false);
    }

}
