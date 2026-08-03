# Foundation V1 源码索引

本索引按 Maven 模块列出所有生产类型，便于代码审查和后续组件接入。

## `foundation-core`

- `com.xjtu.iron.foundation.core.collection.CollectionChecks`：提供集合和映射的空值检查。
- `com.xjtu.iron.foundation.core.collection.CollectionCopies`：创建保持顺序的防御性不可变副本。
- `com.xjtu.iron.foundation.core.collection.CollectionDiff`：计算集合成员差异，并保持输入顺序。
- `com.xjtu.iron.foundation.core.collection.CollectionDifference`：描述两个集合之间的新增、删除和保留元素。
- `com.xjtu.iron.foundation.core.collection.CollectionFilters`：提供返回不可变结果的集合过滤能力。
- `com.xjtu.iron.foundation.core.collection.CollectionIndexers`：将对象集合建立为保序索引。
- `com.xjtu.iron.foundation.core.collection.CollectionPartitions`：将集合拆分为独立且不可修改的批次。
- `com.xjtu.iron.foundation.core.collection.CollectionTransforms`：提供集合元素映射和扁平化能力。
- `com.xjtu.iron.foundation.core.collection.ListSupport`：提供列表安全访问能力。
- `com.xjtu.iron.foundation.core.collection.MapSupport`：提供映射合并和反转能力。
- `com.xjtu.iron.foundation.core.collection.SetSupport`：提供保序集合运算。
- `com.xjtu.iron.foundation.core.collection.TreeBuilder`：根据节点标识和父标识构建树结构。
- `com.xjtu.iron.foundation.core.collection.TreeNode`：描述树形节点的最小只读协议。
- `com.xjtu.iron.foundation.core.enumeration.CodeEnum`：定义具有稳定外部编码的枚举协议。
- `com.xjtu.iron.foundation.core.enumeration.EnumResolver`：提供按名称和稳定编码解析枚举的能力。
- `com.xjtu.iron.foundation.core.enumeration.UnknownEnumStrategy`：定义枚举解析失败时的处理策略。
- `com.xjtu.iron.foundation.core.exception.ExceptionSupport`：提供不包含具体组件语义的异常处理能力。
- `com.xjtu.iron.foundation.core.exception.StackTraceSupport`：提供面向日志和事件的堆栈文本生成能力。
- `com.xjtu.iron.foundation.core.exception.ThrowableChain`：提供异常原因链的循环安全遍历。
- `com.xjtu.iron.foundation.core.function.CheckedBiFunction`：可抛出受检异常的双参数函数。
- `com.xjtu.iron.foundation.core.function.CheckedConsumer`：可抛出受检异常的单参数消费者。
- `com.xjtu.iron.foundation.core.function.CheckedFunction`：可抛出受检异常的单参数函数。
- `com.xjtu.iron.foundation.core.function.CheckedRunnable`：可抛出受检异常的无参操作。
- `com.xjtu.iron.foundation.core.function.CheckedSupplier`：可抛出受检异常的值提供者。
- `com.xjtu.iron.foundation.core.number.DecimalSupport`：提供统一精度和舍入规则的十进制运算。
- `com.xjtu.iron.foundation.core.number.NumberChecks`：提供数字范围检查。
- `com.xjtu.iron.foundation.core.number.NumberConversions`：提供带溢出检查的数字转换。
- `com.xjtu.iron.foundation.core.number.Percentage`：表示百分比值，例如 `12.5%`。
- `com.xjtu.iron.foundation.core.object.ObjectChecks`：提供对象引用组合检查。
- `com.xjtu.iron.foundation.core.object.ObjectDefaults`：提供延迟计算的对象默认值能力。
- `com.xjtu.iron.foundation.core.object.ObjectEquality`：提供常见对象值比较能力。
- `com.xjtu.iron.foundation.core.text.CaseConverter`：在常用标识符命名格式之间转换。
- `com.xjtu.iron.foundation.core.text.CaseFormat`：定义组件工程支持的常用命名格式。
- `com.xjtu.iron.foundation.core.text.PlaceholderResolver`：解析形如 `${name}` 的简单占位符。
- `com.xjtu.iron.foundation.core.text.TextChecks`：提供字符串存在性和内容状态检查。
- `com.xjtu.iron.foundation.core.text.TextJoinerSupport`：提供集合文本安全拼接能力。
- `com.xjtu.iron.foundation.core.text.TextLength`：提供基于 Unicode 码点的文本长度计算。
- `com.xjtu.iron.foundation.core.text.TextMasker`：提供面向技术日志的基础脱敏能力。
- `com.xjtu.iron.foundation.core.text.TextNormalizer`：提供文本标准化能力。
- `com.xjtu.iron.foundation.core.text.TextSplitter`：提供可预测的文本拆分能力。
- `com.xjtu.iron.foundation.core.text.TextTruncator`：提供 Unicode 安全的文本截断能力。
- `com.xjtu.iron.foundation.core.validation.Arguments`：校验调用参数并抛出 IllegalArgumentException。
- `com.xjtu.iron.foundation.core.validation.StateChecks`：校验对象运行状态并抛出 IllegalStateException。
- `com.xjtu.iron.foundation.core.validation.ValidationResult`：聚合多个结构性校验结果。
- `com.xjtu.iron.foundation.core.validation.ValidationViolation`：描述一次结构性校验失败。

## `foundation-time`

- `com.xjtu.iron.foundation.time.ClockProvider`：提供可替换的系统时钟。
- `com.xjtu.iron.foundation.time.DateRange`：表示左闭右闭的本地日期范围。
- `com.xjtu.iron.foundation.time.DateSupport`：提供不包含业务日历语义的自然日期计算。
- `com.xjtu.iron.foundation.time.Deadline`：表示一个绝对截止时间。
- `com.xjtu.iron.foundation.time.DurationParser`：解析 ISO-8601 或简写形式的持续时间。
- `com.xjtu.iron.foundation.time.DurationSupport`：提供持续时间边界运算。
- `com.xjtu.iron.foundation.time.Expiration`：描述一个对象的创建时间和存活时间。
- `com.xjtu.iron.foundation.time.InstantRange`：表示左闭右开的绝对时间范围。
- `com.xjtu.iron.foundation.time.InstantSupport`：提供绝对时间精度处理。
- `com.xjtu.iron.foundation.time.SystemClockProvider`：基于 JDK 系统时钟的默认实现。
- `com.xjtu.iron.foundation.time.TemporalPrecision`：定义组件工程常用的时间精度。
- `com.xjtu.iron.foundation.time.TimeFormats`：集中定义稳定、线程安全的时间格式器。
- `com.xjtu.iron.foundation.time.TimeParser`：提供带清晰异常信息的时间解析能力。
- `com.xjtu.iron.foundation.time.TimeRange`：表示同一自然日内左闭右开的本地时间范围。
- `com.xjtu.iron.foundation.time.TimeWindow`：表示以基准时间为中心的前后容差窗口。
- `com.xjtu.iron.foundation.time.ZoneSupport`：提供时区边界转换。

## `foundation-id`

- `com.xjtu.iron.foundation.id.CompactUuidIdGenerator`：生成不包含短横线的紧凑 UUID。
- `com.xjtu.iron.foundation.id.CompositeIdGenerator`：按顺序组合多个标识片段。
- `com.xjtu.iron.foundation.id.ContextualIdGenerator`：定义能够根据命名空间和属性生成标识的协议。
- `com.xjtu.iron.foundation.id.IdGenerationContext`：描述生成技术标识时的命名空间和附加属性。
- `com.xjtu.iron.foundation.id.IdGenerator`：定义通用技术标识生成协议。
- `com.xjtu.iron.foundation.id.IdGeneratorRegistry`：按名称管理不同用途的字符串标识生成器。
- `com.xjtu.iron.foundation.id.IdGenerators`：提供常用技术标识生成器工厂。
- `com.xjtu.iron.foundation.id.LongIdGenerator`：定义长整型技术标识生成器。
- `com.xjtu.iron.foundation.id.PrefixedIdGenerator`：为另一个字符串标识生成器增加固定前缀。
- `com.xjtu.iron.foundation.id.StringIdGenerator`：定义字符串技术标识生成器。
- `com.xjtu.iron.foundation.id.TimeSortableIdGenerator`：生成前缀按时间排序的字符串标识。
- `com.xjtu.iron.foundation.id.UuidIdGenerator`：生成标准带短横线 UUID。

## `foundation-codec`

- `com.xjtu.iron.foundation.codec.Base64Support`：提供标准 Base64 和 URL Safe Base64 编解码能力。
- `com.xjtu.iron.foundation.codec.ByteSupport`：提供基本类型和字节数组转换能力。
- `com.xjtu.iron.foundation.codec.CharsetSupport`：提供严格字符集转换，遇到非法字节时拒绝静默替换。
- `com.xjtu.iron.foundation.codec.ChecksumSupport`：提供快速完整性校验值计算。
- `com.xjtu.iron.foundation.codec.CompressionSupport`：提供 GZIP 压缩和受限解压能力。
- `com.xjtu.iron.foundation.codec.ContentFingerprint`：表示由内容计算得到的稳定指纹。
- `com.xjtu.iron.foundation.codec.DigestAlgorithm`：定义基础组件允许使用的安全摘要算法。
- `com.xjtu.iron.foundation.codec.DigestSupport`：提供不可逆内容摘要和常量时间比较。
- `com.xjtu.iron.foundation.codec.HexSupport`：提供十六进制编解码能力。
- `com.xjtu.iron.foundation.codec.UrlCodec`：提供 URL 查询参数编解码能力。

## `foundation-context`

- `com.xjtu.iron.foundation.context.ContextCarrier`：定义跨边界传递字符串上下文的载体。
- `com.xjtu.iron.foundation.context.ContextCodec`：定义执行上下文和字符串载体之间的编解码协议。
- `com.xjtu.iron.foundation.context.ContextEntry`：表示一个类型安全的上下文条目。
- `com.xjtu.iron.foundation.context.ContextKey`：描述具有名称和运行时类型的上下文键。
- `com.xjtu.iron.foundation.context.ContextPropagationPolicy`：决定某个上下文键是否允许跨边界传播。
- `com.xjtu.iron.foundation.context.ContextReader`：从外部载体读取执行上下文。
- `com.xjtu.iron.foundation.context.ContextSnapshot`：表示某个时刻捕获的执行上下文快照。
- `com.xjtu.iron.foundation.context.ContextValueConverter`：将上下文值转换为可跨进程传输的字符串。
- `com.xjtu.iron.foundation.context.ContextWriter`：将执行上下文写入外部载体。
- `com.xjtu.iron.foundation.context.ExecutionContext`：表示一次逻辑执行过程的不可变技术上下文。
- `com.xjtu.iron.foundation.context.ExecutionContextBuilder`：构建不可变执行上下文。
- `com.xjtu.iron.foundation.context.MapContextCarrier`：基于 Map 的上下文载体实现。
- `com.xjtu.iron.foundation.context.StandardContextCodec`：编解码标准字符串上下文键。
- `com.xjtu.iron.foundation.context.StandardContextKeys`：定义多个技术组件共同理解的低基数标准上下文键。
- `com.xjtu.iron.foundation.context.StringContextValueConverter`：字符串上下文值转换器。

## `foundation-reflection`

- `com.xjtu.iron.foundation.reflection.AnnotationSupport`：提供注解查找能力。
- `com.xjtu.iron.foundation.reflection.ClassSupport`：提供类型加载和继承层次检查。
- `com.xjtu.iron.foundation.reflection.ConstructorSupport`：提供明确失败语义的构造器调用。
- `com.xjtu.iron.foundation.reflection.FieldSupport`：提供字段查找和安全读取能力。
- `com.xjtu.iron.foundation.reflection.GenericType`：通过匿名子类保留泛型类型信息。
- `com.xjtu.iron.foundation.reflection.GenericTypeResolver`：解析类实现接口时声明的泛型实参。
- `com.xjtu.iron.foundation.reflection.MethodSupport`：提供方法查找和调用能力。
- `com.xjtu.iron.foundation.reflection.PropertyDescriptor`：描述可读写 JavaBean 属性。
- `com.xjtu.iron.foundation.reflection.PropertySupport`：提供基于 JavaBeans 规范的属性描述和读取。
- `com.xjtu.iron.foundation.reflection.ReflectionException`：表示基础反射操作失败。
- `com.xjtu.iron.foundation.reflection.TypeSupport`：提供 java.lang.reflect.Type 分类和原始类型解析。

## `foundation-resource`

- `com.xjtu.iron.foundation.resource.ByteArrayResource`：基于防御性字节数组副本的内存资源。
- `com.xjtu.iron.foundation.resource.ClassPathResource`：读取类路径资源。
- `com.xjtu.iron.foundation.resource.DefaultResourceLoader`：支持 classpath 和本地文件的默认资源加载器。
- `com.xjtu.iron.foundation.resource.FileSystemResource`：读取本地文件系统资源。
- `com.xjtu.iron.foundation.resource.Resource`：描述可以读取的二进制资源。
- `com.xjtu.iron.foundation.resource.ResourceLimitExceededException`：表示读取资源时超过配置的最大字节数。
- `com.xjtu.iron.foundation.resource.ResourceLoader`：根据资源位置创建资源对象。
- `com.xjtu.iron.foundation.resource.ResourceLocation`：解析 classpath、file 和普通文件路径资源位置。
- `com.xjtu.iron.foundation.resource.ResourceNotFoundException`：表示资源不存在或者无法解析。
- `com.xjtu.iron.foundation.resource.ResourceReader`：以受限方式读取资源内容。

## `foundation-serialization/foundation-serialization-api`

- `com.xjtu.iron.foundation.serialization.SerializationContext`：描述一次序列化调用的技术上下文。
- `com.xjtu.iron.foundation.serialization.SerializationException`：表示序列化技术能力执行失败。
- `com.xjtu.iron.foundation.serialization.SerializationFormat`：定义序列化数据格式。
- `com.xjtu.iron.foundation.serialization.SerializationOperation`：定义序列化异常发生的操作阶段。
- `com.xjtu.iron.foundation.serialization.SerializationOptions`：定义单次序列化调用的通用选项。
- `com.xjtu.iron.foundation.serialization.SerializedPayload`：表示可跨消息、缓存或 Outbox 边界传输的序列化载荷。
- `com.xjtu.iron.foundation.serialization.Serializer`：定义对象和二进制载荷之间的序列化协议。
- `com.xjtu.iron.foundation.serialization.SerializerNotFoundException`：表示注册表中不存在请求的序列化器。
- `com.xjtu.iron.foundation.serialization.SerializerRegistry`：按数据格式管理序列化器。
- `com.xjtu.iron.foundation.serialization.StringSerializer`：定义对象和文本之间的序列化协议。
- `com.xjtu.iron.foundation.serialization.TypeDescriptor`：描述反序列化目标类型，并支持泛型类型。

## `foundation-serialization/foundation-serialization-jackson`

- `com.xjtu.iron.foundation.serialization.jackson.JacksonConfiguration`：描述 Jackson 序列化器的稳定基础配置。
- `com.xjtu.iron.foundation.serialization.jackson.JacksonJsonSerializer`：基于 Jackson 2.x 的 JSON 序列化实现。
- `com.xjtu.iron.foundation.serialization.jackson.JacksonModuleProvider`：向 Foundation Jackson Mapper 提供显式模块。
- `com.xjtu.iron.foundation.serialization.jackson.JacksonObjectMapperFactory`：创建与 Web MVC 配置隔离的 Jackson ObjectMapper。
- `com.xjtu.iron.foundation.serialization.jackson.JacksonSerializationOptionsMapper`：将单次通用选项映射为 Jackson Reader 和 Writer。
- `com.xjtu.iron.foundation.serialization.jackson.JacksonSerializerCustomizer`：在 ObjectMapper 冻结给序列化器之前执行受控定制。
- `com.xjtu.iron.foundation.serialization.jackson.JacksonSerializerFactory`：创建 Jackson JSON 序列化器。
- `com.xjtu.iron.foundation.serialization.jackson.JacksonTypeFactorySupport`：将 Foundation 类型描述转换为 Jackson JavaType。

## `foundation-test-support`

- `com.xjtu.iron.foundation.test.context.InMemoryContextCarrier`：用于上下文传播测试的内存载体。
- `com.xjtu.iron.foundation.test.context.TestExecutionContexts`：创建常用测试执行上下文。
- `com.xjtu.iron.foundation.test.exception.ExceptionAssertions`：提供不绑定测试框架的异常断言。
- `com.xjtu.iron.foundation.test.id.FixedStringIdGenerator`：始终返回固定标识的测试生成器。
- `com.xjtu.iron.foundation.test.id.SequentialStringIdGenerator`：按顺序生成可预测标识的测试生成器。
- `com.xjtu.iron.foundation.test.resource.ResourceFixtures`：创建测试资源。
- `com.xjtu.iron.foundation.test.serialization.SerializationAssertions`：提供不绑定 JUnit 的序列化往返断言。
- `com.xjtu.iron.foundation.test.time.MutableClockProvider`：可在测试中手动推进的线程安全时钟。
