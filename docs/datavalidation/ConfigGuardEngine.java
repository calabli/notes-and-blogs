package com.tutor.springjourney.datavalidation;

import com.jayway.jsonpath.JsonPath;
import org.apache.commons.jexl3.*;
import java.util.*;
import java.util.concurrent.*;

public class ConfigGuardEngine {
    private final JexlEngine jexl;
    private final Map<String, ExternalFetcher> fetchers = new ConcurrentHashMap<>();
    private final YamlPositionManager positionManager = new YamlPositionManager();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public ConfigGuardEngine() {
        // 注册 utils 命名空间
        Map<String, Object> ns = new HashMap<>();
        ns.put("utils", new ValidationUtils());
        this.jexl = new JexlBuilder().namespaces(ns)
                .strict(false)  // 关键：非严格模式，处理 null 更友好
                .silent(false)  // 调试阶段建议设为 false，方便看报错
                .create();
    }

    public void registerFetcher(String name, ExternalFetcher f) { fetchers.put(name, f); }

    public List<ValidationError> validate(String yamlStr, List<ValidationRule> rules) {
        Map<String, Object> root = positionManager.load(yamlStr);
        List<ValidationError> errors = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (ValidationRule rule : rules) {
            try {
                Object target = JsonPath.read(root, rule.getPath());
                if (rule.isForEach() && target instanceof List) {
                    List<?> list = (List<?>) target;
                    for (int i = 0; i < list.size(); i++) {
                        String subPath = buildPath(rule.getPath(), i);
                        tasks.add(runAsync(root, list.get(i), rule, subPath, errors));
                    }
                } else {
                    tasks.add(runAsync(root, target, rule, buildPath(rule.getPath(), -1), errors));
                }
            } catch (Exception e) { /* 路径不存在则跳过 */ }
        }

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
        return errors;
    }

    /**
     * 核心异步校验逻辑
     * * @param root        YAML 整个文档的根对象，对应表达式中的 $
     * @param item        当前正在校验的节点对象，对应表达式中的 _this
     * @param rule        校验规则定义
     * @param currentPath 当前节点的逻辑路径（如 spec.containers[0]）
     * @param errors      线程安全的错误收集列表
     */
    private CompletableFuture<Void> runAsync(Map<String, Object> root, Object item,
                                             ValidationRule rule, String currentPath, List<ValidationError> errors) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 1. 构造基础数据池 (Data Pool)
                // 在 JDK 8 中手动管理 Map，避免 JexlContext 类型不兼容问题
                Map<String, Object> dataPool = new HashMap<>();
                dataPool.put("$", root);
                dataPool.put("_this", item);

                // 2. 属性平铺 (Property Flattening)
                // 将 item 中的 key 直接暴露给 JEXL 上下文，方便写简短表达式（如 cpu > 2）
                if (item instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) item;
                    for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                        String key = String.valueOf(entry.getKey());
                        // 保护保留关键字不被平铺数据覆盖
                        if (!"$".equals(key) && !"_this".equals(key)) {
                            dataPool.put(key, entry.getValue());
                        }
                    }
                }

                // 4. 初始化 JEXL 上下文
                // dataPool 包含了：根节点、当前节点、平铺字段、Fetcher 返回值
                JexlContext context = new MapContext(dataPool);

                // 3. 执行外部数据获取 (Fetcher)
                // 如果规则定义了需要从远程接口拿数据，先执行 Fetcher
                if (rule.getFetcher() != null && rule.getFetcher().getName() != null) {
                    boolean shouldFetch = true; // 默认直接调接口

                    // 如果写了条件，先判断条件
                    if (rule.getCondition() != null && !rule.getCondition().trim().isEmpty()) {
                        try {
                            Object trigger = jexl.createExpression(rule.getCondition()).evaluate(context);
                            // 只有显式返回 true 才会触发调接口
                            shouldFetch = (trigger instanceof Boolean && (Boolean) trigger);
                        } catch (Exception e) {
                            // 如果条件表达式执行失败，默认不调接口，并记录异常（可选）
                            shouldFetch = false;
                        }
                    }

                    // 满足条件（或没写条件）才调接口
                    if (shouldFetch) {
                        ExternalFetcher fetcher = fetchers.get(rule.getFetcher().getName());
                        if (fetcher != null) {
                            Map<String, Object> readOnlyContext = Collections.unmodifiableMap(new HashMap<>(dataPool));
                            Object fetchedData = fetcher.fetch(rule.getFetcher().getParams(), readOnlyContext);

                            // 将结果同时注入数据池和 context，供后续 Expression 使用
                            dataPool.put(rule.getFetcher().getResultVar(), fetchedData);
                            context.set(rule.getFetcher().getResultVar(), fetchedData);
                        }
                    } else {
                        // 如果是因为有条件且不满足而跳过，且规则后续不需要执行 expression，
                        // 或者你希望这种情况直接算“校验通过”，可以在这里 return。
                        // 如果后面还有 expression 要跑，就继续向下执行。
                        if (rule.getExpression() == null) return;
                    }
                }

                // 5. 执行前置条件评估 (Condition)
                // 如果 condition 为 false，则跳过本次规则校验（不记为错误）
                if (rule.getCondition() != null && !rule.getCondition().trim().isEmpty()) {
                    Object trigger = jexl.createExpression(rule.getCondition()).evaluate(context);
                    if (!(trigger instanceof Boolean && (Boolean) trigger)) {
                        return; // 条件不满足，退出本次异步任务
                    }
                }

                // 6. 执行核心逻辑校验 (Expression)
                if (rule.getExpression() != null) {
                    JexlExpression expression = jexl.createExpression(rule.getExpression());
                    Object pass = expression.evaluate(context);

                    // 确定错误级别，默认为 ERROR
                    String severity = rule.getLevel() != null ? rule.getLevel().toUpperCase() : "ERROR";

                    // 判断校验结果：必须显式返回 Boolean.TRUE 才算通过
                    if (!(pass instanceof Boolean && (Boolean) pass)) {
                        int line = positionManager.getLine(currentPath);
                        errors.add(new ValidationError(
                                rule.getId(),
                                severity,
                                rule.getMessage(),
                                line,
                                currentPath,
                                item
                        ));
                    }
                }
            } catch (JexlException.Parsing pex) {
                // 语法错误：表达式写错了
                errors.add(new ValidationError(rule.getId(), "FATAL",
                        "表达式语法错误: " + pex.getMessage(), 0, currentPath, item));
            } catch (Exception ex) {
                // 运行错误：如类型转换异常等
                int line = positionManager.getLine(currentPath);
                errors.add(new ValidationError(rule.getId(), "FATAL",
                        "校验执行异常: " + ex.getMessage(), line, currentPath, item));
            }
        }, executor); // 使用自定义线程池，防止阻塞 commonPool
    }

    public void checkBlacklist(Object node, String blacklist, List<ValidationError> errors, String currentPath) {
        if (node instanceof Map) {
            ((Map<?, ?>) node).forEach((k, v) ->
                    checkBlacklist(v, blacklist, errors, currentPath + "." + k));
        } else if (node instanceof List) {
            List<?> list = (List<?>) node;
            for (int i = 0; i < list.size(); i++) {
                checkBlacklist(list.get(i), blacklist, errors, currentPath + "[" + i + "]");
            }
        } else if (node instanceof String) {
            String val = (String) node;
            for (char c : blacklist.toCharArray()) {
                if (val.indexOf(c) >= 0) {
                    errors.add(new ValidationError("GLOBAL_LIMIT", "ERROR",
                            "包含非法字符: " + c, positionManager.getLine(currentPath), currentPath, val));
                    break;
                }
            }
        }
    }

    private String buildPath(String path, int index) {
        String p = path.replace("$.", "");
        if (index >= 0) return p.replace("[*]", "[" + index + "]");
        return p;
    }

    public void shutdown() { executor.shutdown(); }
}