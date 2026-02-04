package com.tutor.springjourney.datavalidation;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        // 1. 初始化校验引擎
        ConfigGuardEngine engine = new ConfigGuardEngine();
        // --- 关键步骤：注册 Fetcher ---
        engine.registerFetcher("imageRepoFetcher", new ImageRepoFetcher());

        // 2. 指定文件路径
        String rulesPath = "D:\\MySoftware\\ideawork\\boot-playground\\spring-journey\\src\\main\\java\\com\\tutor\\springjourney\\datavalidation\\rule.yaml";
        String dataPath = "D:\\MySoftware\\ideawork\\boot-playground\\spring-journey\\src\\main\\java\\com\\tutor\\springjourney\\datavalidation\\data.yaml";

        try {
            // 3. 读取规则文件并解析
            System.out.println(">>> 正在加载规则文件: " + rulesPath);
            String rulesYamlContent = readFile(rulesPath);
            List<ValidationRule> rules = parseRules(rulesYamlContent);

            // 4. 读取用户数据文件 (无需在 Main 转换，直接传 String 给引擎)
            System.out.println(">>> 正在读取待校验数据: " + dataPath);
            String dataYamlContent = readFile(dataPath);

            // 5. 执行校验逻辑
            System.out.println(">>> 校验引擎启动中...");
            List<ValidationError> errors = engine.validate(dataYamlContent, rules);

            // 6. 结果输出
            System.out.println("--------------------------------------");
            if (errors.isEmpty()) {
                System.out.println("✅ 校验结果：配置完全符合规范！");
            } else {
                // 在 Main 中对错误按级别排序，FATAL 排最前
                errors.sort((a, b) -> {
                    Map<String, Integer> priority = new HashMap<>();
                    priority.put("FATAL", 0);
                    priority.put("ERROR", 1);
                    priority.put("WARN", 2);
                    return priority.getOrDefault(a.getLevel(), 9) - priority.getOrDefault(b.getLevel(), 9);
                });
                System.err.println("❌ 校验失败：发现以下 " + errors.size() + " 个违规项：");
                for (ValidationError error : errors) {
                    System.err.println(error.toString());
                }
            }
            System.out.println("--------------------------------------");

        } catch (IOException e) {
            System.err.println("致命错误：无法读取配置文件 - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("校验流程执行异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            // 7. 优雅关闭线程池
            engine.shutdown();
        }
    }

    /**
     * JDK 8 风格读取文件到 String
     */
    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    /**
     * 将 YAML 解析为 Rule 列表
     */
    private static List<ValidationRule> parseRules(String content) {
        Yaml yaml = new Yaml();
        List<Map<String, Object>> rawRules = yaml.load(content);
        List<ValidationRule> rules = new ArrayList<>();

        for (Map<String, Object> map : rawRules) {
            ValidationRule rule = new ValidationRule();
            rule.setId((String) map.get("id"));
            rule.setPath((String) map.get("path"));
            rule.setForEach(map.getOrDefault("forEach", false).equals(true));
            rule.setExpression((String) map.get("expression"));
            rule.setMessage((String) map.get("message"));
            rule.setCondition(map.get("condition") != null ? (String) map.get("condition") : null);
            // 如果有 fetcher 配置也可以在这里解析
            rule.setFetcher(FetcherConfig.fromMap((Map<String, Object>) map.get("fetcher")));
            rule.setLevel((String) map.getOrDefault("level", "ERROR"));
            rules.add(rule);
        }
        return rules;
    }
}