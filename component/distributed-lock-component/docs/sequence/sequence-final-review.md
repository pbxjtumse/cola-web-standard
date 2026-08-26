# Distributed Lock Sequence UML Final Review

## Final rule

All sequence diagrams are organized by real call direction:

`API -> Core -> SPI -> Provider -> Resource`

## Module ownership

| Module | Allowed participants |
|---|---|
| distributed-lock-api | `DistributedLockClient`, `LockCallback`, `LockOptions`, `FencingTokenGuard` |
| distributed-lock-core | `DefaultDistributedLockClient`, `LockExecutionTemplate`, `LockAcquisitionService`, `DefaultLockHandle`, `LockRuntimeState`, `FencingTokenCoordinator`, `FencingTokenProvider`, `LockWatchdog`, `LockMetricsFacade`, `LockEventPublisher` |
| distributed-lock-spi | `LockProvider`, `LockProviderRegistry`, `LockProviderCapabilities`, SPI protocol request/response/status objects |
| distributed-lock-provider-redis | `RedisLockProvider`, Redis key/script helpers |
| distributed-lock-provider-redisson | `RedissonLockProvider`, Redisson ownership/watchdog integration |
| distributed-lock-fencing-provider-jdbc | `JdbcSequenceFencingTokenProvider`, JDBC token storage/schema helpers |
| Resource | Redis, DB, business repository/resource |

## Important correction

In the current codebase, `FencingTokenProvider` is under `distributed-lock-core`, not under `distributed-lock-spi`.
Therefore, sequence diagrams place `FencingTokenProvider` in Core and `JdbcSequenceFencingTokenProvider` in the JDBC provider module.

## File count

This package contains all sequence diagrams under:

- `docs/sequence/L0-overview`
- `docs/sequence/L1-main-flow`
- `docs/sequence/L2-scenario-flow`
- `docs/sequence/L3-internal-flow`

