package com.tutor.springjourney.datavalidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class GenericHttpFetcher implements ExternalFetcher {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object fetch(Map<String, Object> params, Map<String, Object> context) {
        String urlStr = (String) params.get("url");
        String method = (String) params.getOrDefault("method", "GET");
        Map<String, String> headers = (Map<String, String>) params.get("headers");
        
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);

            // 1. 设置 Header
            if (headers != null) {
                headers.forEach(conn::setRequestProperty);
            }

            // 2. 发送 Body (如果是 POST/PUT)
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                conn.setDoOutput(true);
                // 模拟根据 bodyFields 从当前上下文中提取数据并发送
                Map<String, Object> body = new HashMap<>();
                List<String> fields = (List<String>) params.get("bodyFields");
                Map<?, ?> currentData = (Map<?, ?>) context.get("_this");
                if (fields != null && currentData != null) {
                    fields.forEach(f -> body.put(f, currentData.get(f)));
                }
                
                try (OutputStream os = conn.getOutputStream()) {
                    mapper.writeValue(os, body);
                }
            }

            // 3. 读取响应
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (InputStream is = conn.getInputStream()) {
                    return mapper.readValue(is, Map.class);
                }
            } else {
                // 非 200 情况返回错误 Map 供校验引擎判定
                Map<String, Object> errorRes = new HashMap<>();
                errorRes.put("code", code);
                errorRes.put("error", "HTTP_ERROR");
                return errorRes;
            }

        } catch (Exception e) {
            Map<String, Object> fail = new HashMap<>();
            fail.put("code", 500);
            fail.put("exception", e.getMessage());
            return fail;
        }
    }
}