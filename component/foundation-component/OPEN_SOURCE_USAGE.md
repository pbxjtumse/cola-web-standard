# 开源基础库使用说明

## 1. Apache Commons Lang

用于字符串、异常链和反射辅助。Foundation 不全量转发 Commons Lang 的所有方法，只暴露项目中稳定高频的方法。

## 2. Apache Commons Collections

用于集合判空、分片等少量集合增强能力。复杂集合结构仍建议直接使用 JDK 或在具体组件中按需引入。

## 3. Commons Codec / JDK Codec

Base64、Hex 优先使用 JDK 17；摘要能力优先用 JDK MessageDigest。Commons Codec 保留为项目可选基础依赖，后续可用于更丰富的 Codec 场景。

## 4. Commons IO

V2 中资源读取核心采用 JDK 流式限长实现，避免依赖具体 Commons IO API 行为。GZIP 模块仍保留 Commons IO 作为薄封装依赖。

## 5. Jackson

Jackson 只出现在 `foundation-serialization-jackson` 模块。`foundation-serialization-api` 绝不依赖 Jackson。
