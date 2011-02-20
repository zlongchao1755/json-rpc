/*
 * Copyright (C) 2011 Develnix.com
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

package org.json.rpc.client;

import org.json.rpc.commons.JsonRpcClientException;

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

    public final void setHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public final String call(String requestData) throws Exception {
        String responseData = post(url, headers, requestData);
        return responseData;
    }

    private String post(URL url, Map<String, String> headers, String data)
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

        OutputStream out = null;

        try {
            out = connection.getOutputStream();

            out.write(data.getBytes());
            out.flush();
            out.close();

            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                throw new JsonRpcClientException("unexpected status code returned : " + statusCode);
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }

        BufferedReader in = null;

        try {
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response.toString();
        } finally {
            if (in != null) {
                in.close();
            }
        }
        }
    }
