# Distributed Lock 指标说明

组件通过 `LockMetricsRecorder` 抽象记录指标。Spring Boot 环境下，starter 会在存在 Micrometer 时装配 `MicrometerLockMetricsRecorder`。

当前指标：

| 指标名 | 含义 |
|---|---|
| `iron.lock.acquire` | 加锁耗时和成功/失败结果 |
| `iron.lock.hold` | 持锁耗时 |
| `iron.lock.renew` | watchdog 续期结果 |
| `iron.lock.release` | 释放锁结果 |
| `iron.lock.lost` | 明确失锁次数 |

常用 tag：

| tag | 含义 |
|---|---|
| `provider` | provider 名称，例如 `redis` |
| `namespace` | 锁命名空间 |
| `lock` | 锁名 pattern，避免高基数 |
| `success` | 是否成功 |
| `status` | 对应状态 |

注意：不要把完整业务 id 直接作为指标 tag。组件会通过 `LockNamePatternResolver` 尽量把锁名归一化，避免 Prometheus 高基数问题。
