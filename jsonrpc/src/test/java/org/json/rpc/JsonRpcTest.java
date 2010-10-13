package org.json.rpc;

import org.json.rpc.client.*;
import org.json.rpc.server.JsonRpcExecutor;
import org.json.rpc.server.JsonRpcServerTransport;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import static org.testng.Assert.*;

public class JsonRpcTest {

    private JsonRpcInvoker invoker;
    private JsonRpcExecutor executor;
    private RpcIntroSpection system;

    @BeforeTest
    public void setupTest() {
        invoker = new JsonRpcInvoker();
        executor = new JsonRpcExecutor();
        system = getInstance("system", RpcIntroSpection.class);
    }

    @AfterTest
    public void teardownTest() {
        invoker = null;
        executor = null;
        system = null;
    }

    @Test
    public void testListMethods() throws Exception {
        String[] methods = system.listMethods();
        assertEquals(methods, new String[]{"system.listMethods", "system.methodHelp", "system.methodSignature"});
    }

    @Test
    public void testMethodSignature() throws Exception {
        String[] sig = system.methodSignature("system.listMethods");
        assertEquals(sig, new String[]{"array"});
    }


    private <T> T getInstance(String handleName, Class<T> ...classes) {
         return invoker.get(new JsonRpcClientTransport(){
             public String call(final String requestData) throws Exception {
                 final StringBuilder resultData = new StringBuilder();
                JsonRpcServerTransport serverTransport = new JsonRpcServerTransport() {

                    public String readRequest() throws Exception {
                        return requestData;
                    }

                    public void writeResponse(String responseData) throws Exception {
                        resultData.append(responseData);
                    }
                };
                executor.execute(serverTransport);
                return resultData.toString();
             }
         }, handleName, classes);
    }

    //@Test
    public void testRemote() throws Exception {
        String url = "http://127.0.0.1:8888/rpc";

        HttpJsonRpcClientTransport transport = new HttpJsonRpcClientTransport(new URL(url));

        JsonRpcInvoker invoker = new JsonRpcInvoker();

        RpcIntroSpection system = invoker.get(transport, "system", RpcIntroSpection.class);
        System.out.println(Arrays.toString(system.listMethods()));
    }
}
