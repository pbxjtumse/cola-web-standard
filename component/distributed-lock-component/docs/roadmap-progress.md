# Distributed Lock Roadmap Progress

## Phase 1 - Redis production baseline

Implemented:

- Redis `LockProvider` with acquire/renew/check/release.
- owner token generated per successful lease attempt.
- Lua release and renew owner-token verification.
- Lua check result mapping (`HELD` / `NOT_FOUND` / `NOT_OWNER`).
- `NO_WAIT` and `BACKOFF` wait strategies.
- manual `LockHandle` API and `execute` template API.
- watchdog renewal with `maxRenewTime` protection.
- events, Micrometer metrics and Actuator health indicator.
- Spring Boot auto-configuration and demo.
- unit tests and Redis Testcontainers integration test.

Remaining release checks:

- run full `mvn clean test` in a Maven + Docker environment;
- run the demo against a real Redis instance;
- verify metric and health output in the final `start` application;
- finish operator/user documentation.

## Phase 2 - fencing token

Implemented:

- Redis `INCR` fencing token inside `acquire.lua`.
- Redis Cluster hash-tag alignment for lock and fencing keys.
- fencing token carried by `LockLease`, `LockHandle` and `LockResult`.
- Provider capability and unit/integration tests for increasing tokens.

Remaining:

- independent `FencingTokenProvider` registry integration;
- JDBC/DB sequence fencing provider;
- database conditional-write example;
- test proving a stale owner token is rejected;
- fencing token retention/cleanup and operational documentation.

## Phase 3 - strong coordination providers

Not started:

- ZooKeeper provider;
- Etcd provider;
- fair waiting semantics;
- session/lease-loss mapping;
- provider capability matrix and compatibility tests.


## v26 Redisson / POM 收口补充

- Redis Lua Provider：保留，作为轻量、自研、Core-managed watchdog Provider。
- Redisson Provider：已加入基础互斥、Provider-native wait、Provider-managed watchdog、RFencedLock native fencing。
- JDBC fencing：继续作为 external fencing Provider，与 redis/redisson 均可组合。
- POM：组件根导入 component-bom；Provider 由二级 aggregator 聚合；Redisson 第三方版本由最外层根 POM 管理。
- 待完整仓库同步：component-bom 增加 `distributed-lock-provider-redisson`；root dependencyManagement 增加 `org.redisson:redisson` 版本。
- 生产迁移：redis 与 redisson 不属于同一协调域，切换 Provider 必须按 `provider-migration-safety.md` 执行。
