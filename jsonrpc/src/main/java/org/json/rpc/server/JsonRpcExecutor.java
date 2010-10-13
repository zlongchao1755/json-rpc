package org.json.rpc.server;

import com.google.gson.*;
import org.json.rpc.RpcIntroSpection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.json.rpc.server.SupportedTypes.getTypeName;
import static org.json.rpc.server.SupportedTypes.isAllowed;

public final class JsonRpcExecutor implements RpcIntroSpection {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcExecutor.class);

    private static final Pattern METHOD_PATTERN = Pattern
            .compile("([_a-zA-Z][_a-zA-Z0-9]*)\\.([_a-zA-Z][_a-zA-Z0-9]*)");

    private final Map<String, Entry<?>> handlers;

    @SuppressWarnings("unchecked")
    public JsonRpcExecutor() {
        this.handlers = new HashMap<String, Entry<?>>();
        addHandler("system", this, RpcIntroSpection.class);
    }

    public <T> void addHandler(String name, T handler, Class<T>... classes) {
        Entry<T> entry = new Entry<T>(handler, classes);
        if (this.handlers.containsKey(name)) {
            throw new IllegalArgumentException("handler already exists");
        }
        this.handlers.put(name, entry);
    }

    private static void verifyInterface(Class<?> clazz) {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("clazz is not an interface");
        }

        for (Method method : clazz.getMethods()) {
            isAllowed(method);
        }
    }

    public final void execute(JsonRpcServerTransport transport) {
        String methodName = null;
        JsonArray params = null;

        JsonObject resp = new JsonObject();
        String error = null;

        try {
            String requestData = transport.readRequest();
            LOG.debug("JSON-RPC >>  {}", requestData);
            JsonParser parser = new JsonParser();
            JsonObject req = (JsonObject) parser.parse(new StringReader(requestData));

            resp.add("id", req.get("id"));

            methodName = req.getAsJsonPrimitive("method").getAsString();
            params = (JsonArray) req.get("params");
            if (params == null) {
                params = new JsonArray();
            }
        } catch (Exception e) {
            LOG.warn("unable to read request", e);
            error = "unable to read request";
        }

        if (error == null) {
            try {
                JsonElement result = executeMethod(methodName, params);
                resp.add("result", result);
            } catch (Throwable t) {
                LOG.warn("exception occured while executing : " + methodName, t);
                error = t.getMessage();
            }
        }

        try {
            if (error != null) {
                resp.addProperty("error", error);
            }
            String responseData = resp.toString();
            LOG.debug("JSON-RPC <<  {}", responseData);
            transport.writeResponse(responseData);
        } catch (Exception e) {
            LOG.warn("unable to write response : " + resp, e);
        }
    }

    private JsonElement executeMethod(String methodName, JsonArray params) throws Throwable {
        try {
            Matcher mat = METHOD_PATTERN.matcher(methodName);
            if (!mat.find()) {
                throw new IllegalArgumentException("invalid method name");
            }

            String handleName = mat.group(1);
            methodName = mat.group(2);

            Entry<?> entry = handlers.get(handleName);
            if (entry == null) {
                throw new IllegalArgumentException("no such method exists");
            }

            Method executableMethod = null;
            for (Method m : entry.methods) {
                if (!m.getName().equals(methodName)) {
                    continue;
                }

                if (canExecute(m, params)) {
                    executableMethod = m;
                    break;
                }
            }

            if (executableMethod == null) {
                throw new IllegalArgumentException("no such method exists");
            }

            Object result = executableMethod.invoke(
                    entry.handler, getParameters(executableMethod, params));

            return new Gson().toJsonTree(result);
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            }
            throw t;
        }
    }

    public boolean canExecute(Method method, JsonArray params) {
        if (method.getParameterTypes().length != params.size()) {
            return false;
        }

        return true;
    }

    public Object[] getParameters(Method method, JsonArray params) {
        List<Object> list = new ArrayList<Object>();
        Gson gson = new Gson();
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            JsonElement p = params.get(i);
            Object o = gson.fromJson(p.toString(), types[i]);
            list.add(o);
        }
        return list.toArray();
    }


    private static class Entry<T> {

        public final Class<T>[] classes;
        public final T handler;
        public final Map<String, String[]> signatures;
        public final Set<Method> methods;

        public Entry(T handler, Class<T>... classes) {
            if (handler == null) {
                throw new NullPointerException("handler");
            }

            if (classes.length == 0) {
                throw new IllegalArgumentException(
                        "at least one interface has to be mentioned");
            }

            this.handler = handler;
            this.classes = classes.clone();

            Map<String, List<String>> map = new HashMap<String, List<String>>();
            Set<Method> set = new HashSet<Method>();

            for (Class<?> clazz : classes) {
                verifyInterface(clazz);

                if (!clazz.isInterface()) {
                    throw new IllegalArgumentException(
                            "class should be an interface : " + clazz);
                }

                for (Method m : clazz.getMethods()) {
                    set.add(m);
                    Class<?>[] params = m.getParameterTypes();

                    List<String> list = map.get(m.getName());
                    if (list == null) {
                        list = new ArrayList<String>();
                    }
                    StringBuffer buff = new StringBuffer(getTypeName(m
                            .getReturnType()));
                    for (int i = 0; i < params.length; i++) {
                        buff.append(",").append(getTypeName(params[i]));
                    }
                    list.add(buff.toString());
                    map.put(m.getName(), list);
                }

            }

            Map<String, String[]> signs = new TreeMap<String, String[]>();
            for (String key : map.keySet()) {
                signs.put(key, map.get(key).toArray(new String[0]));
            }

            this.methods = Collections.unmodifiableSet(set);
            this.signatures = Collections.unmodifiableMap(signs);
        }
    }

    public String[] listMethods() {
        Set<String> methods = new TreeSet<String>();
        for (String name : this.handlers.keySet()) {
            Entry<?> entry = this.handlers.get(name);
            for (String method : entry.signatures.keySet()) {
                methods.add(name + "." + method);
            }
        }
        return methods.toArray(new String[0]);
    }

    public String methodHelp(String method) {
        // FIXME: implement this
        return "";
    }

    public String[] methodSignature(String method) {
        if (method == null) {
            throw new NullPointerException("method");
        }

        Matcher mat = METHOD_PATTERN.matcher(method);
        if (!mat.find()) {
            throw new IllegalArgumentException("invalid method name");
        }

        String handleName = mat.group(1);
        String methodName = mat.group(2);

        Set<String> signatures = new TreeSet<String>();

        Entry<?> entry = handlers.get(handleName);
        if (entry == null) {
            throw new IllegalArgumentException("no such method exists");
        }

        for (Method m : entry.methods) {
            if (!m.getName().equals(methodName)) {
                continue;
            }

            String[] sign = entry.signatures.get(m.getName());

            StringBuffer buff = new StringBuffer(sign[0]);
            for (int i = 1; i < sign.length; i++) {
                buff.append(",").append(sign[i]);
            }

            signatures.add(buff.toString());
        }

        if (signatures.size() == 0) {
            throw new IllegalArgumentException("no such method exists");
        }

        return signatures.toArray(new String[0]);
    }


}
