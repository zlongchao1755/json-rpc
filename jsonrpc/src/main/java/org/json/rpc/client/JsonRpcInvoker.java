package org.json.rpc.client;

import com.google.gson.*;
import org.json.rpc.JsonRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Random;

public final class JsonRpcInvoker {

    private static Logger LOG = LoggerFactory.getLogger(JsonRpcInvoker.class);

    private static Random RAND = new Random();

    public <T> T get(final JsonRpcClientTransport transport, final String handle, final Class<T>... classes) {
        return (T) Proxy.newProxyInstance(JsonRpcInvoker.class.getClassLoader(), classes, new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return JsonRpcInvoker.this.invoke(handle, transport, proxy, method, args);
            }
        });
    }

    private Object invoke(String handleName,
                          JsonRpcClientTransport transport, Object proxy, Method method,
                          Object[] args) throws Throwable {
        int id = RAND.nextInt(Integer.MAX_VALUE);
        String methodName = handleName + "." + method.getName();

        Gson gson = new Gson();

        JsonObject req = new JsonObject();
        req.addProperty("id", id);
        req.addProperty("method", methodName);

        JsonArray params = new JsonArray();
        if (args != null) {
            for (Object o : args) {
                params.add(gson.toJsonTree(o));
            }
        }
        req.add("params", params);

        String requestData = req.toString();
        LOG.debug("JSON-RPC >>  {}", requestData);
        String responseData = transport.call(requestData);
        LOG.debug("JSON-RPC <<  {}", responseData);

        JsonParser parser = new JsonParser();
        JsonObject resp = (JsonObject) parser.parse(new StringReader(responseData));

        JsonElement respId = resp.get("id");
        JsonElement result = resp.get("result");
        JsonElement error = resp.get("error");

        if (error != null) {
            if (error.isJsonPrimitive()) {
                throw new JsonRpcException(error.getAsString());
            } else {
                throw new JsonRpcException("error occured, check payload", error.toString());
            }
        }

        if (method.getReturnType() == Void.class) {
            return null;
        }

        return gson.fromJson(result.toString(), method.getReturnType());
    }
}
