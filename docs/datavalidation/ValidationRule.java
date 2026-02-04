package com.tutor.springjourney.datavalidation;

import lombok.Data;

@Data
public class ValidationRule {
    private String id;              // 规则 ID
    private String path;            // JsonPath 路径 (如 $.clusters[*].nodes[*])
    private boolean forEach = false;// 是否遍历数组
    private String condition;       // 触发条件 (JEXL 表达式)
    private String expression;      // 校验逻辑 (JEXL 表达式)
    private String message;         // 报错信息模板
    private FetcherConfig fetcher;  // 外部依赖配置
    private  String level; // FATAL, ERROR, WARN, INFO
}