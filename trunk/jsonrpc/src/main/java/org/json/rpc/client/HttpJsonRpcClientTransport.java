package org.json.rpc.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HttpJsonRpcClientTransport implements JsonRpcClientTransport {

    private URL url;
    private final Map<String, String> headers;

    public HttpJsonRpcClientTransport(URL url) {
        this.url = url;
        this.headers = new HashMap<String, String>();
    }

    public void setHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public String call(String requestData) throws Exception {
        String responseData = post(url, headers, requestData);
        return responseData;
    }

    public String post(URL url, Map<String, String> headers, String data)
            throws IOException {

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.addRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.connect();

        OutputStream out = connection.getOutputStream();
        out.write(data.getBytes());
        out.flush();
        out.close();

        int statusCode = connection.getResponseCode();
        if (statusCode != 200) {
            throw new IOException("unexpected status code returned : " + statusCode);
        }


        BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }
}
