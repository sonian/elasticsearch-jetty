package com.sonian.elasticsearch.http.jetty.handler.timeout;

import org.eclipse.jetty.server.Request;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.unit.TimeValue;
import com.sonian.elasticsearch.http.jetty.JettyHttpServerTransport;
import com.sonian.elasticsearch.http.jetty.handler.AbstractJettyHttpServerTransportHandler;
import org.elasticsearch.rest.support.RestUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.common.collect.Maps.newHashMap;

/**
 * @author imotov
 */
public class SimulatedTimeoutHandler extends AbstractJettyHttpServerTransportHandler {

    private static final String SLEEP_PARAM = "sleep";

    private volatile boolean emulateTimeout;

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if(emulateTimeout && baseRequest.getQueryString() != null) {
            Map<String, String> params = newHashMap();
            // We have to decode query string ourselves because request.getParameter method will read
            // request content and make input stream unavailable to other handlers.
            RestUtils.decodeQueryString(baseRequest.getQueryString(), 0, params);
            if (params.containsKey(SLEEP_PARAM)) {
                long sleep = 0;
                try {
                    TimeValue timeValue = TimeValue.parseTimeValue(params.get(SLEEP_PARAM), null);
                    if (timeValue != null) {
                        sleep = timeValue.millis();
                    }
                } catch (ElasticSearchParseException ex) {
                    logger.error("Invalid sleep parameter [{}]", params.get(SLEEP_PARAM));
                }
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        // Ignore
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
