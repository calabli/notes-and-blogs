package com.tutor.springjourney.datavalidation;

import java.util.HashMap;
import java.util.Map;

public class ImageRepoFetcher implements ExternalFetcher {
    @Override
    public Object fetch(Map<String, Object> params, Map<String, Object> context) {
        // 1. 从 context 获取当前正在校验的镜像名
        // 因为 fetcher 是在 container 层触发的，'_this' 指向当前 container
        Map<?, ?> container = (Map<?, ?>) context.get("_this");
        String imageName = (String) container.get("image");
        String apiUrl = (String) params.get("apiUrl");

        System.out.println(">>> [Fetcher] 正在请求接口校验镜像: " + imageName);

        // 2. 模拟发送 HTTP 请求
        // 实际开发中这里可以使用 OkHttp 或 HttpClient
        Map<String, Object> response = new HashMap<>();
        
        // 模拟逻辑：如果是 nginx 开头的镜像就返回存在
        if (imageName != null && imageName.contains("nginx")) {
            response.put("exists", true);
            response.put("tag", "stable");
        } else {
            response.put("exists", false);
        }

        return response; // 这个 Map 会被注入为变量 'repoStatus'
    }
}