package org.json.rpc;

public class JsonRpcException extends Exception {

    private String payload;

    public JsonRpcException(String message) {
        super(message);
    }

    public JsonRpcException(String message, String payload) {
        this(message);
        this.payload = payload;
    }

    public JsonRpcException(String message, Throwable cause) {
        super(message, cause);
    }    
}
