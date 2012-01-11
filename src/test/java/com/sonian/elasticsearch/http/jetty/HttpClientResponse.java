package com.sonian.elasticsearch.http.jetty;

import java.util.Map;

/**
 * @author imotov
 */
public class HttpClientResponse {
    private final Map<String, Object> response;

    private final int errorCode;

    private final Throwable e;

    public HttpClientResponse(Map<String, Object> response, int errorCode, Throwable e) {
        this.response = response;
        this.errorCode = errorCode;
        this.e = e;
    }

    public Map<String, Object> response() {
        return response;
    }

    public Object get(String key) {
        return response().get(key);
    }

    public String getString(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> map = response();
        for (int i = 0; i < keys.length - 1; i++) {
            map = (Map<String, Object>) map.get(keys[i]);
            if (map == null) {
                return null;
            }
        }

        return (String) map.get(keys[keys.length - 1]);
    }

    public int errorCode() {
        return errorCode;
    }

    public Throwable cause() {
        return e;
    }

}
