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

import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.Base64;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Map;

/**
 * @author imotov
 */
public class HttpClient {

    private final URL baseUrl;

    private final String encodedAuthorization;

    public HttpClient(TransportAddress transportAddress) {
        this(transportAddress, null, null);
    }

    public HttpClient(TransportAddress transportAddress, String username, String password) {
        InetSocketAddress address = ((InetSocketTransportAddress) transportAddress).address();
        try {
            baseUrl = new URL("http", address.getHostName(), address.getPort(), "/");
        } catch (MalformedURLException e) {
            throw new ElasticSearchException("", e);
        }
        if (username != null) {
            String userPassword = username + ":" + password;
            encodedAuthorization =  Base64.encodeBytes(userPassword.getBytes());
        } else {
            encodedAuthorization = null;
        }
    }

    public Map<String, Object> request(String path) {
        return request("GET", path, null);

    }

    public Map<String, Object> request(String method, String path) {
        return request(method, path, null);
    }

    @SuppressWarnings({"unchecked"})
    public Map<String, Object> request(String method, String path, Map<String, Object> data) {
        ObjectMapper mapper = new ObjectMapper();

        URL url;
        try {
            url = new URL(baseUrl, path);
        } catch (MalformedURLException e) {
            throw new ElasticSearchException("Cannot parse " + path, e);
        }

        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(method);
            if (data != null) {
                urlConnection.setDoOutput(true);
            }
            if (encodedAuthorization != null) {
                urlConnection.setRequestProperty("Authorization", "Basic " +
                        encodedAuthorization);
            }

            urlConnection.connect();
        } catch (IOException e) {
            throw new ElasticSearchException("", e);
        }

        if (data != null) {
            OutputStream outputStream = null;
            try {
                outputStream = urlConnection.getOutputStream();
                mapper.writeValue(outputStream, data);
            } catch (IOException e) {
                throw new ElasticSearchException("", e);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        throw new ElasticSearchException("", e);
                    }
                }
            }
        }

        int errorCode = -1;
        try {
            errorCode = urlConnection.getResponseCode();
            InputStream inputStream = urlConnection.getInputStream();
            return mapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
            throw new ElasticSearchException("HTTP " + errorCode, e);
        } finally {
            urlConnection.disconnect();
        }
    }
}
