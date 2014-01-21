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
package com.sonian.elasticsearch.http.jetty;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.http.HttpRequest;
import org.elasticsearch.rest.support.RestUtils;

/**
 * @author imotov
 */
public class JettyHttpServerRestRequest extends HttpRequest {

    public static final String REQUEST_CONTENT_ATTRIBUTE = "com.sonian.elasticsearch.http.jetty.request-content";

    private final HttpServletRequest request;

    private final Method method;

    private final Map<String, String> params;

    private final BytesReference content;
    
    private final String opaqueId;

    public JettyHttpServerRestRequest(HttpServletRequest request) throws IOException {
        this.request = request;
        this.opaqueId = request.getHeader("X-Opaque-Id");
        this.method = Method.valueOf(request.getMethod());
        this.params = new HashMap<String, String>();

        if (request.getQueryString() != null) {
            RestUtils.decodeQueryString(request.getQueryString(), 0, params);
        }

        content = new BytesArray(Streams.copyToByteArray(request.getInputStream()));
        request.setAttribute(REQUEST_CONTENT_ATTRIBUTE, content);
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
        return content.length() > 0;
    }

    @Override public boolean contentUnsafe() {
        return false;
    }

    @Override
    public BytesReference content() {
        return content;  //To change body of implemented methods use File | Settings | File Templates.
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

    public String localAddr() {
        return this.request.getLocalAddr();
    }

    public long localPort() {
        return this.request.getLocalPort();
    }

    public String remoteAddr() {
        return this.request.getRemoteAddr();
    }

    public long remotePort() {
        return this.request.getRemotePort();
    }
    
    public String remoteUser() {
        return this.request.getRemoteUser();
    }
    
    public String scheme() {
        return this.request.getScheme();
    }
    
    public String contentType() {
        return this.request.getContentType();
    }

    public String opaqueId() {
        return this.opaqueId;
    }
    
    @Override
    public Iterable<Entry<String, String>> headers() {
        class HeadersWrapper implements Iterable<Entry<String, String>> {
            
            @Override
            public Iterator<Entry<String, String>> iterator() {
                return new HeadersIterator();
            }
            
            class HeadersIterator implements Iterator<Entry<String, String>> {
                
                private Enumeration<String> headerNames;
                
                public HeadersIterator() {
                    this.headerNames = request.getHeaderNames();
                }

                @Override
                public boolean hasNext() {
                    return headerNames.hasMoreElements();
                }

                @Override
                public Entry<String, String> next() {
                    String headerName = headerNames.nextElement();
                    return new AbstractMap.SimpleEntry<String, String>(headerName, request.getHeader(headerName));
                }

                @Override
                public void remove() {
                    return;
                }
                
            }
        }
        
        return new HeadersWrapper();
	}
}
