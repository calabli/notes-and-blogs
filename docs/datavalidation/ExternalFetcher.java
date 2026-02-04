package com.tutor.springjourney.datavalidation;

import java.util.Map;

public interface ExternalFetcher {
    /**
     * @param params 规则配置的静态参数
     * @param context 整个 YAML 数据的上下文（用于动态获取参数）
     */
    Object fetch(Map<String, Object> params, Map<String, Object> context);
}