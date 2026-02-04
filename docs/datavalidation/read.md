1. 整体架构方案：三层过滤模型
   我们将校验逻辑抽象为三个阶段，每一层解决不同维度的配置问题：

L1：结构与元数据校验 (Structural Layer)

目标：解决“必填项”、“数据类型”、“正则匹配”、“静态枚举值”等基础问题。

实现：基于 JSON Schema 规范（YAML 可转换为 JSON 处理）。

L2：逻辑与关联校验 (Logic Layer)

目标：解决“属性间依赖”。（例如：如果 type=LB，则 port 必填；或 A <= B）。

实现：基于 JEXL (Java Expression Language) 表达式引擎，配合全量上下文注入。

L3：动态准入校验 (Dynamic/External Layer)

目标：解决“外部数据依赖”。（例如：校验镜像是否存在于 Harbor，校验 VPC ID 是否真实存在于云平台）。

实现：插件化 Fetcher 机制，支持异步并行调用外部 API、数据库或 SDK


2. 核心领域模型设计 (Domain Model)
   我们要实现“配置驱动校验”，就需要定义一套完善的规则 DSL（领域专用语言）。

2.1 Rule 实体定义
```java
public class ValidationRule {
    private String id;           // 规则唯一标识
    private String path;         // JsonPath 路径，支持通配符如 $.clusters[*].nodes[*]
    private boolean forEach;     // 是否针对数组项循环校验
    
    // --- L1: 基础约束 ---
    private boolean required;    // 是否必填
    private List<Object> options;// 静态枚举列表
    
    // --- L2: 逻辑约束 ---
    private String condition;    // 触发校验的前置条件表达式
    private String expression;   // 判定逻辑表达式（如：cpu * 2 <= memory）
    
    // --- L3: 外部依赖 ---
    private FetcherConfig fetcher; // 外部数据抓取配置
    
    private String message;      // 校验失败时的友好提示
}

```
3. 核心执行流程设计
   校验引擎执行时，会按照以下逻辑闭环运行：

步骤一：上下文构建 (Context Building)
将原始 YAML 解析为 Map<String, Object>。

注入全局标识符 $ 代表根节点。

如果正在校验数组项，注入 item 代表当前迭代对象。

步骤二：外部数据预取 (Data Fetching)
扫描所有规则，识别出带有 fetcher 的规则。

性能优化：使用 CompletableFuture 并发调用所有外部接口。

将返回结果以 resultVar 存入上下文（如：context.put("vpcInfo", apiResponse)）。

步骤三：规则评估 (Rule Evaluation)
Condition 判定：计算 condition 表达式，结果为 false 则跳过该规则。

Expression 计算：利用 JEXL 执行复杂逻辑。表达式可以跨路径引用：

item.cpu <= $.global_config.max_cpu（局部 vs 全局）

item.vpc_id == vpcInfo.id（局部 vs 外部数据

4. 关键技术细节
   如何解决“属性间存在关系”？
   通过 JEXL 表达式的灵活性，我们可以轻松定义：

互斥关系：!(has(a) && has(b))

包含关系：type == 'GPU' ? gpu_driver != null : true

数值比例：memory / cpu >= 2

如何处理“静态列表”？
在 Rule 中定义 options 字段，校验器在执行时会自动进行 contains 判断。为了保持配置精简，这些静态列表也可以定义在 YAML 的 global_config 中，通过 $.global_config.available_regions 动态引用。

5. 错误定位与反馈
   对于用户来说，仅仅知道“校验失败”是不够的，必须知道“在哪一行”。

路径追踪：利用 JsonPath 报错时返回具体的路径（如 clusters[1].nodes[0].ip）。

行号映射：在 Java 加载 YAML 时，使用 SnakeYAML 的 Node 模式，将每个 Key 对应的 Mark（包含行号）存入一个影子 Map 中，报错时根据 Path 检索出行号。
