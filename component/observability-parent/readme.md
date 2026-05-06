# 组件边界
## 组件负责什么
- 日志规范
- MDC 上下文规范
- Trace / Request 上下文初始化与透传
- Micrometer 指标统一接入
- Spring Boot Actuator 统一接入
- 统一异常观测
- HTTP 入站观测
- 异步任务观测
- 消息头 trace 注入/提取规范
- 给其他组件提供埋点 SPI
## 组件不负责什么
- 不替代 ELK / Loki / SkyWalking / Tempo / Jaeger 这些平台
- 不做 BI 数据分析
- 不做业务报表
- 不负责权限系统
- 不负责业务审计全量系统
- 不直接承载复杂业务逻辑

## V1 版本的能力设计
### 统一 Trace / Request 上下文
#### 定义标准上下文字段： 
- traceId
- requestId
- appName
- env
- module
- tenantId
- userId
- clientIp
- uri
- method
- bizCode
- bizNo

- 注意： tenantId/userId/bizNo 不一定每次都有
但 traceId/requestId/appName 必须有
### HTTP 入站统一观测
#### 在 web 入站时做一层 filter：

##### 动作
- 从请求头读取 traceId
- 没有就生成新的
- 初始化 ObservationContext
- 写入 MDC
- 记录开始时间
- 请求结束后打印 access log
- 上报请求耗时和结果指标
##### 输出
- access log
- 请求总量指标
- 请求耗时指标
- 状态码分布
- 异常计数
### 统一异常观测
做一个统一异常处理器，不是为了替代业务异常，而是为了统一观测。

#### 做的事
- 统一记录异常日志
- 统一计数
- 标记异常类型
- 记录接口、业务码、耗时、traceId
- 结果

#### 你以后能直接看：
- 哪个接口 5xx 多
- 哪类异常最多
- 哪个业务码异常高发

### 线程池 / 异步上下文透传

你现有 TaskWrapper 的方向是对的，但应该纳入 observability。

#### 标准动作
- 提交任务时复制上下文
- 执行线程恢复上下文
- 执行完成清理上下文
- 记录任务耗时
- 记录异步异常
- 注意 这里不能只是复制 MDC，最好复制统一的 ObservationContext，MDC 只是日志层镜像。

### 统一指标输出

V1 指标先统一这几类：

#### 基础请求指标
- 请求总量
- 请求耗时
- 请求异常数
#### 线程池指标
- active
- pool size
- queue size
- reject total
#### 治理指标
- bulkhead in use
- bulkhead reject total
- rate limit reject total
- timeout total
#### 任务指标
- task total
- task success total
- task fail total
- task duration
#### 消息指标（先预留）
- send total
- send fail total
- consume total
- consume fail total
- retry total
- dead total

Spring Boot 的指标能力是基于 Micrometer 的，Actuator 提供生产可观测入口；Prometheus 端点也走这条链路。

### 日志标准化

日志一定要标准化，不然后面 Grafana/Loki/ELK 很难用。

日志字段建议
通用字段
timestamp
level
app
env
traceId
requestId
thread
logger
message
请求日志额外字段
method
uri
status
costMs
clientIp
业务日志额外字段
bizCode
bizNo
tenantId
userId
result
异常日志额外字段
exception
errorCode
rootCause
stack





Metrics 指标
Logs 日志采集
Traces 链路追踪
Alerting 告警
Dashboard 大盘
Collector 数据转发


HTTP 请求进入
↓
ObservabilityWebFilter
- web-mdc-enabled=true 时写请求级 MDC
  ↓
  DemoController.template()
  ↓
  TraceAspect
- method-tracing-enabled=true，所以创建 demo.template.api Span
- method-mdc-enabled=true，所以临时写方法级 MDC
  ↓
  log.info("template api called")
  ↓
  TraceTemplate.execute("demo.template.inner")
- 创建代码块级 Span
- template-mdc-enabled=true，所以临时写模板级 MDC
  ↓
  log.info("inside trace template")
  ↓
  TraceTemplate finally
- close inner span
- 恢复方法级 MDC
  ↓
  TraceAspect finally
- close method span
- 恢复请求级 MDC
  ↓
  ObservabilityWebFilter finally
- 恢复请求进入前 MDC

阶段 1：Tracing + MDC 本地闭环

你现在正在这里。

1. @Trace 方法级 Span
2. TraceTemplate 代码块级 Span
3. TraceErrorResolver 异常扩展解析
4. 请求级 MDC Filter
5. 方法级 MDC 开关
6. 模板级 MDC 开关
7. demo-app 本地验证

当前还差：第 4、5、6 步真正接入代码。

阶段 2：接入 OpenTelemetry Java Agent

目标：

1. 让 traceId / spanId 不再为空
2. 自动采集 HTTP / JDBC / Redis / MQ
3. 你的 @Trace / TraceTemplate 作为业务 Span 补充进去

现在你日志里 traceId= 为空，主要就是还没到这个阶段。

阶段 3：OpenTelemetry Collector

目标：

应用 / Java Agent
↓
OTel Collector
↓
Tempo / SkyWalking / Jaeger

Collector 负责解耦后端，未来换后端不用动业务服务。

阶段 4：Trace 后端

你可以选一个先跑通：

Tempo + Grafana
或者
SkyWalking OAP + UI

长期来说，你的 SDK 不应该绑定后端。

阶段 5：Metrics 指标

这块你现在只是占了一个 metrics 包，还没真正做。

后面要补：

1. MetricsService 抽象
2. Micrometer 适配
3. JVM / HTTP / 业务指标
4. Prometheus scrape
5. Grafana dashboard
   阶段 6：Logs 日志体系

当前只是 MDC 打 traceId/spanId，还不算完整日志体系。

后面要补：

1. JSON 日志格式
2. traceId/spanId 标准字段
3. Loki / ELK / OpenSearch 接入
4. 日志和 Trace 联动
   阶段 7：告警与大盘

最后才做：

1. PrometheusRule
2. Alertmanager
3. Grafana dashboard
4. 慢接口 / 错误率 / JVM / 业务异常告警
5. 
curl http://localhost:8080/hello

curl http://localhost:8080/template

curl -i http://localhost:8080/biz-error

curl -i http://localhost:8080/system-error

-javaagent:/Users/xy/IdeaProjects/cola-web-standard/component/observability-parent/tools/otel/opentelemetry-javaagent.jar -Dotel.service.name=observability-demo-app -Dotel.traces.exporter=console -Dotel.metrics.exporter=none -Dotel.logs.exporter=none -Dotel.javaagent.debug=false