package org.elasticsearch.http.jetty;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.log.Log;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.PortsRange;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.http.BindHttpException;
import org.elasticsearch.http.HttpServerAdapter;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.http.HttpStats;
import org.elasticsearch.transport.BindTransportException;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author imotov
 */
public class JettyHttpServerTransport extends AbstractLifecycleComponent<HttpServerTransport> implements HttpServerTransport {

    private static final String SLEEP_PARAM = "sleep";

    private final NetworkService networkService;

    private final String port;

    private final String bindHost;

    private final String publishHost;

    private final Environment environment;

    private final ClusterName clusterName;

    private final boolean requestLogEnabled;

    private final String requestLogFilename;

    private final int requestLogRetainDays;

    private final boolean requestLogExtended;

    private final boolean emulateTimeOut;

    private final String requestLogTimeZone;

    private final ESLoggerLog esLoggerLog;

    private volatile BoundTransportAddress boundAddress;

    private volatile InetSocketAddress jettyBoundAddress;

    private volatile Server jettyServer;

    private volatile HttpServerAdapter httpServerAdapter;


    @Inject
    public JettyHttpServerTransport(Settings settings, Environment environment, ClusterName clusterName, NetworkService networkService) {
        super(settings);
        this.networkService = networkService;
        this.environment = environment;
        this.clusterName = clusterName;
        this.port = componentSettings.get("port", settings.get("http.port", "9200-9300"));
        this.bindHost = componentSettings.get("bind_host", settings.get("http.bind_host", settings.get("http.host")));
        this.publishHost = componentSettings.get("publish_host", settings.get("http.publish_host", settings.get("http.host")));
        this.requestLogEnabled = componentSettings.getAsBoolean("request_log.enabled", false);
        this.requestLogFilename = componentSettings.get("request_log.filename", "request.log.yyyy_mm_dd");
        this.requestLogRetainDays = componentSettings.getAsInt("request_log.retain_days", 90);
        this.requestLogExtended = componentSettings.getAsBoolean("request_log.extended", false);
        this.requestLogTimeZone = componentSettings.get("request_log.timezone", "GMT");
        this.emulateTimeOut = componentSettings.getAsBoolean("emulate_timeout.enabled", false);
        this.esLoggerLog = new ESLoggerLog(settings);
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        InetAddress hostAddressX;
        try {
            hostAddressX = networkService.resolveBindHostAddress(bindHost);
        } catch (IOException e) {
            throw new BindHttpException("Failed to resolve host [" + bindHost + "]", e);
        }
        final InetAddress hostAddress = hostAddressX;

        PortsRange portsRange = new PortsRange(port);
        final AtomicReference<Exception> lastException = new AtomicReference<Exception>();

        Log.setLog(esLoggerLog);

        portsRange.iterate(new PortsRange.PortCallback() {
            @Override
            public boolean onPortNumber(int portNumber) {
                jettyBoundAddress = new InetSocketAddress(hostAddress, portNumber);
                Server server = new Server(jettyBoundAddress);
                server.setHandler(createJettyHandler());
                try {
                    server.start();
                    jettyServer = server;
                    lastException.set(null);
                } catch (BindException e) {
                    lastException.set(e);
                    return false;
                } catch (Exception e) {
                    lastException.set(e);
                    return true;
                }
                return true;
            }
        });
        if (lastException.get() != null) {
            throw new BindHttpException("Failed to bind to [" + port + "]", lastException.get());
        }
        InetSocketAddress publishAddress;
        try {
            publishAddress = new InetSocketAddress(networkService.resolvePublishHostAddress(publishHost), jettyBoundAddress.getPort());
        } catch (Exception e) {
            throw new BindTransportException("Failed to resolve publish address", e);
        }
        this.boundAddress = new BoundTransportAddress(new InetSocketTransportAddress(jettyBoundAddress), new InetSocketTransportAddress(publishAddress));

    }


    private Handler createJettyHandler() {
        JettyHttpServerHandler handler = new JettyHttpServerHandler(this);
        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(handler);

        if (requestLogEnabled) {
            File logs = environment.logsFile();
            if (!logs.exists()) {
                logs.mkdirs();
            }
            RequestLogHandler requestLogHandler = new RequestLogHandler();
            String filename = new File(environment.logsFile(), clusterName.value() + "." + requestLogFilename).getAbsolutePath();
            NCSARequestLog requestLog = new NCSARequestLog(filename);
            requestLog.setRetainDays(requestLogRetainDays);
            requestLog.setAppend(true);
            requestLog.setExtended(requestLogExtended);
            requestLog.setLogTimeZone(requestLogTimeZone);
            requestLogHandler.setRequestLog(requestLog);
            handlers.addHandler(requestLogHandler);
        }
        return handlers;
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        if (jettyServer != null) {
            try {
                jettyServer.stop();
            } catch (Exception ex) {
                throw new ElasticSearchException("Cannot stop jetty server", ex);
            }
            jettyServer = null;
        }
    }

    @Override
    protected void doClose() throws ElasticSearchException {
    }

    @Override
    public BoundTransportAddress boundAddress() {
        return this.boundAddress;
    }

    @Override
    public HttpStats stats() {
        return new HttpStats(0);
    }

    @Override
    public void httpServerAdapter(HttpServerAdapter httpServerAdapter) {
        this.httpServerAdapter = httpServerAdapter;
    }

    void dispatchRequest(JettyHttpServerRestRequest request, JettyHttpServerRestChannel channel) {
        if (emulateTimeOut) {
            if (request.hasParam(SLEEP_PARAM)) {
                long sleep = 0;
                try {
                    TimeValue timeValue = TimeValue.parseTimeValue(request.param(SLEEP_PARAM), null);
                    if (timeValue != null) {
                        sleep = timeValue.millis();
                    }
                } catch (ElasticSearchParseException ex) {
                    logger.error("Invalid sleep parameter [{}]", request.param(SLEEP_PARAM));
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
        httpServerAdapter.dispatchRequest(request, channel);
    }
}
