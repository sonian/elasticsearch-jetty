package org.elasticsearch.http.jetty;

import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.ElasticSearchException;
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

    public HttpClient(TransportAddress transportAddress) {
        InetSocketAddress address = ((InetSocketTransportAddress)transportAddress).address();
        try {
            baseUrl = new URL("http", address.getHostName(), address.getPort(), "/");
        } catch (MalformedURLException e) {
            throw new ElasticSearchException("", e);
        }
    }

    public Map<String, Object> request(String path) {
        return request("GET", path, null);

    }
    public Map<String, Object> request(String method, String path) {
        return request(method, path, null);
    }

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
            urlConnection =  (HttpURLConnection)url.openConnection();
            urlConnection.setRequestMethod(method);
            if(data != null) {
                urlConnection.setDoOutput(true);
            }
            urlConnection.connect();
        } catch (IOException e) {
            throw new ElasticSearchException("", e);
        }

        if(data != null) {
            OutputStream outputStream = null;
            try {
                outputStream = urlConnection.getOutputStream();
                mapper.writeValue(outputStream, data);
            } catch (IOException e) {
                throw new ElasticSearchException("", e);
            } finally {
                if(outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        throw new ElasticSearchException("", e);
                    }
                }
            }
        }

        try {
            InputStream inputStream = urlConnection.getInputStream();
            return mapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
            throw new ElasticSearchException("", e);
        } finally {
            urlConnection.disconnect();
        }
    }
}
