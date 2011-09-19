package org.elasticsearch.http.jetty.handler.timeout;

import org.eclipse.jetty.server.Request;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.http.jetty.JettyHttpServerTransport;
import org.elasticsearch.http.jetty.handler.AbstractJettyHttpServerTransportHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author imotov
 */
public class SimulatedTimeoutHandler extends AbstractJettyHttpServerTransportHandler {

    private static final String SLEEP_PARAM = "sleep";

    private volatile boolean emulateTimeout;

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if(emulateTimeout) {
            if (request.getParameter(SLEEP_PARAM) != null) {
                long sleep = 0;
                try {
                    TimeValue timeValue = TimeValue.parseTimeValue(request.getParameter(SLEEP_PARAM), null);
                    if (timeValue != null) {
                        sleep = timeValue.millis();
                    }
                } catch (ElasticSearchParseException ex) {
                    logger.error("Invalid sleep parameter [{}]", request.getParameter(SLEEP_PARAM));
                }
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void setTransport(JettyHttpServerTransport transport) {
        super.setTransport(transport);
        emulateTimeout = getTransport().componentSettings().getAsBoolean("emulate_timeout.enabled", false);

    }
}
