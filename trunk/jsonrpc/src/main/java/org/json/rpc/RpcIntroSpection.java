package org.json.rpc;

public interface RpcIntroSpection {

    String[] listMethods();

    String[] methodSignature(String method);

    String methodHelp(String method);

}
