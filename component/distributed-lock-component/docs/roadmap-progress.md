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
