package org.elasticsearch.http.jetty.logging;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.elasticsearch.common.Unicode;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

/**
 * @author imotov
 */
public class RequestLogger extends AbstractComponent {

    @Inject
    public RequestLogger(Settings settings) {
        super(settings);
    }

    public void log(Request request, Response response) {

        if(logger.isInfoEnabled()) {
            long latency = System.currentTimeMillis() - request.getTimeStamp();
            String content = Unicode.fromBytes((byte[])request.getAttribute("org.elasticsearch.http.jetty.request-content"));
            logger.info("{} {} {} {} {} {} [{}]",
                    request.getMethod(),
                    request.getPathInfo(),
                    request.getQueryString(),
                    response.getStatus(),
                    response.getContentCount() >= 0 ? response.getContentCount() : "-",
                    latency,
                    content
                    );
        }

    }
}
