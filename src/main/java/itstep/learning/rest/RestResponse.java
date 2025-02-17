package itstep.learning.rest;

import java.util.Map;

public class RestResponse {
    private int    status;    
    private String message;    
    private String resourceUrl;    // Resource identification in requests: Individual resources are identified in requests using URIs.
    private Map<String, String> meta;
    private long   cacheTime;      // seconds 
    private Object data;

    public String getResourceUrl() {
        return resourceUrl;
    }

    public RestResponse setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
        return this;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public RestResponse setMeta(Map<String, String> meta) {
        this.meta = meta;
        return this;
    }

    public long getCacheTime() {
        return cacheTime;
    }

    public RestResponse setCacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
        return this;
    }

    public Object getData() {
        return data;
    }

    public RestResponse setData(Object data) {
        this.data = data;
        return this;
    }
    
    public int getStatus() {
        return status;
    }

    public RestResponse setStatus(int status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public RestResponse setMessage(String message) {
        this.message = message;
        return this;
    }
}

