package com.tutor.springjourney.datavalidation;

import lombok.Data;

import java.util.Map;

@Data
public class FetcherConfig {
    private String name;            // 注册的 Fetcher 实现名
    private String resultVar;       // 存入上下文的变量名
    private Map<String, Object> params; // 传递给接口的参数

    public static FetcherConfig fromMap(Map<String, Object> map) {
        if (map == null) return null;
        FetcherConfig config = new FetcherConfig();
        config.setName((String) map.get("name"));
        config.setResultVar((String) map.get("resultVar"));
        config.setParams((Map<String, Object>) map.get("params"));
        return config;
    }
    
    // Getters & Setters...
}