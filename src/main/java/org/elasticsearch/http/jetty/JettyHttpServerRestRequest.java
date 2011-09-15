package org.elasticsearch.http.jetty;

import org.elasticsearch.common.Unicode;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.http.HttpRequest;
import org.elasticsearch.rest.support.AbstractRestRequest;
import org.elasticsearch.rest.support.RestUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author imotov
 */
public class JettyHttpServerRestRequest extends AbstractRestRequest implements HttpRequest {

    private final HttpServletRequest request;

    private final Method method;

    private final Map<String, String> params;

    private final byte[] content;

    public JettyHttpServerRestRequest(HttpServletRequest request) throws IOException {
        this.request = request;
        this.method = Method.valueOf(request.getMethod());
        this.params = new HashMap<String, String>();

        if (request.getQueryString() != null) {
            RestUtils.decodeQueryString(request.getQueryString(), 0, params);
        }

        content = Streams.copyToByteArray(request.getInputStream());
        request.setAttribute("org.elasticsearch.http.jetty.request-content", content);
    }

    @Override public Method method() {
        return this.method;
    }

    @Override public String uri() {
        int prefixLength = 0;
        if (request.getContextPath() != null ) {
            prefixLength += request.getContextPath().length();
        }
        if (request.getServletPath() != null ) {
            prefixLength += request.getServletPath().length();
        }
        if (prefixLength > 0) {
            return request.getRequestURI().substring(prefixLength);
        } else {
            return request.getRequestURI();
        }
    }

    @Override public String rawPath() {
        return uri();
    }

    @Override public boolean hasContent() {
        return content.length > 0;
    }

    @Override public boolean contentUnsafe() {
        return false;
    }

    @Override public byte[] contentByteArray() {
        return content;
    }

    @Override public int contentByteArrayOffset() {
        return 0;
    }

    @Override public int contentLength() {
        return content.length;
    }

    @Override public String contentAsString() {
        return Unicode.fromBytes(contentByteArray(), contentByteArrayOffset(), contentLength());
    }

    @Override public String header(String name) {
        return request.getHeader(name);
    }

    @Override public Map<String, String> params() {
        return params;
    }

    @Override public boolean hasParam(String key) {
        return params.containsKey(key);
    }

    @Override public String param(String key) {
        return params.get(key);
    }

    @Override public String param(String key, String defaultValue) {
        String value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
}
