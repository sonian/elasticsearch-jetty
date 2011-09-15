package org.elasticsearch.http.jetty.logging;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

/**
 * @author imotov
 */
public class RequestLoggerWrapper extends AbstractLifeCycle implements RequestLog {

    final private RequestLogger requestLogger;

    public RequestLoggerWrapper(RequestLogger requestLogger) {
        this.requestLogger = requestLogger;
    }

    @Override
    public void log(Request request, Response response) {
        requestLogger.log(request, response);
    }
}
