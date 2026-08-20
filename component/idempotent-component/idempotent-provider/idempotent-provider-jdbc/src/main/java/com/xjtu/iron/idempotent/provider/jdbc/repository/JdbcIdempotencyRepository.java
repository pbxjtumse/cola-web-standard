package com.xjtu.iron.idempotent.provider.jdbc.repository;

import com.xjtu.iron.idempotent.provider.jdbc.execution.JdbcExecutionManager;
import com.xjtu.iron.idempotent.provider.jdbc.execution.DataSourceJdbcExecutionManager;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;
import com.xjtu.iron.idempotent.api.recovery.IdempotencyRecoveryMode;
import com.xjtu.iron.idempotent.api.state.IdempotencyStatus;
import com.xjtu.iron.idempotent.api.policy.IdempotencyWindowPolicy;
import com.xjtu.iron.idempotent.api.repository.*;
import com.xjtu.iron.idempotent.api.repository.acquire.*;
import com.xjtu.iron.idempotent.api.repository.recovery.*;
import com.xjtu.iron.idempotent.api.repository.write.*;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC 幂等状态仓储：DURABLE 的默认实现，也支持 WINDOWED。
 *
 * <p>这是幂等正确性的核心实现之一。阅读时建议按下面顺序理解：</p>
 * <ol>
 *     <li>首次请求：INSERT PROCESSING + UNIQUE(namespace,idempotency_key)，只能一个并发请求成功；</li>
 *     <li>记录已存在：短事务内 SELECT ... FOR UPDATE，再判断 SUCCESS / PROCESSING / FAILED / WINDOW EXPIRED；</li>
 *     <li>普通 tryAcquire 只判断，不会因为 PROCESSING_EXPIRED / FAILED_RETRYABLE 自动接管；</li>
 *     <li>显式 tryRecover 必须校验 expectedOwner + expectedVersion，成功后生成 newOwner + version+1；</li>
 *     <li>markSuccess / markFailed 都带 ownerToken + version 条件，旧 generation 无权覆盖新 generation；</li>
 *     <li>transaction-aware 模式下，tryAcquire/tryRecover 是 Tx-A，markSuccess 参加 Tx-B，markFailed 是 Tx-C。</li>
 * </ol>
 *
 * <p>DistributedLock 只在 Repository 外围减少热点竞争；即使完全关闭 Lock，本类仍必须独立保证并发正确性。</p>
 */
public final class JdbcIdempotencyRepository implements IdempotencyRepository, IdempotencyRecoveryRepository {

    public static final String PROVIDER_NAME = "jdbc";

    private final JdbcExecutionManager jdbc;
    private final String table;

    /**
     * 使用默认 {@link DataSourceJdbcExecutionManager} 的便捷构造。
     *
     * <p>该默认实现不会自动加入外层 Spring 事务。Starter 在 transaction integration 可用时，
     * 会改为使用接收 {@link JdbcExecutionManager} 的构造方式注入 transaction-aware 实现。</p>
     */
    public JdbcIdempotencyRepository(DataSource dataSource, String table) {
        this(new DataSourceJdbcExecutionManager(dataSource), table);
    }

    public JdbcIdempotencyRepository(JdbcExecutionManager jdbc, String table) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
        this.table = validateTableName(table);
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    /**
     * 显式声明 Provider 能力，Core 不通过 providerName 猜语义。
     */
    @Override
    public IdempotencyRepositoryCapabilities capabilities() {
        return IdempotencyRepositoryCapabilities.builder()
                .windowedSupported(true)
                .durableSupported(true)
                .resultPayloadSupported(true)
                .businessTransactionParticipationSupported(jdbc.supportsCurrentTransactionParticipation())
                .recoveryQuerySupported(true)
                .build();
    }

    /**
     * 普通 execute() 的原子状态抢占入口，也就是 Tx-A 的核心。
     *
     * <p>整个判断放在独立短事务中：PROCESSING 必须在真正业务 callback 开始前 COMMIT，让其他节点尽快看见
     * “当前 generation 已经有人处理”。这个方法返回 ACQUIRED 才代表当前调用真正获得执行权。</p>
     */
    @Override
    public IdempotencyAcquireResult tryAcquire(IdempotencyAcquireRequest request) {
        try {
            return jdbc.inNewTransaction(connection -> tryAcquireInTransaction(connection, request));
        } catch (Exception error) {
            return IdempotencyAcquireResult.providerError(error);
        }
    }

    private IdempotencyAcquireResult tryAcquireInTransaction(Connection connection, IdempotencyAcquireRequest request) throws Exception {

        // [A1 首次请求] 先走 INSERT 快路径。UNIQUE(namespace,idempotency_key) 是第一道数据库硬约束：
        // A/B 同时第一次进入时，只可能一个 INSERT 成功；INSERT 成功即本次 owner/version=1 generation 生效。
        if (tryInsertProcessing(connection, request)) {
            return IdempotencyAcquireResult.acquired(selectForUpdate(connection, request.getNamespace(), request.getKey()), false);
        }

        // [A2 已有记录] Duplicate Key 不是错误，而是进入“历史状态判定”分支。
        // SELECT ... FOR UPDATE 把读取 + 判断 + 必要更新锁在同一短事务，避免 Check-Then-Act 竞态。
        IdempotencyRecord current = selectForUpdate(connection, request.getNamespace(), request.getKey());
        if (current == null) {
            // 极端情况下唯一键冲突事务回滚后记录又被删除，交由上层重试。
            return IdempotencyAcquireResult.providerError(new IllegalStateException("idempotency record disappeared after duplicate key"));
        }

        // [A3 WINDOWED] 语义窗口结束后，旧物理记录可以因 retention 继续存在，但它已经不能阻止新 generation。
        // 因此这里不是等待物理 DELETE，而是在同一行上 version+1 并重置为新的 PROCESSING。
        if (isWindowExpired(current, request.getNow())) {
            IdempotencyRecord next = restartWindow(connection, current, request);
            return IdempotencyAcquireResult.acquired(next, true);
        }

        // [A4 身份保护] 同 key 但 requestHash 或 routeKey 改变，说明“同一个幂等身份”被错误复用，必须拒绝。
        if (routeConflict(current.getRouteKey(), request.getRouteKey())
                || hashConflict(current.getRequestHash(), request.getRequestHash())) {
            return IdempotencyAcquireResult.of(IdempotencyAcquireStatus.KEY_CONFLICT, current);
        }

        // [A5 状态判定] 持久状态只有 PROCESSING / SUCCESS / FAILED；ACTIVE/EXPIRED/RETRYABLE 是此刻计算出来的判定状态。
        return switch (current.getStatus()) {
            case SUCCESS -> {
                IdempotencyRecord touched = touchSlidingWindowIfNeeded(connection, current, request);
                yield IdempotencyAcquireResult.of(IdempotencyAcquireStatus.SUCCESS, touched);
            }
            case PROCESSING -> {
                // 与 Redis Lua 保持一致：已经超时的 PROCESSING 不能因为后来一个普通重复请求
                // 又把 WINDOWED 语义窗口向后续命。否则持续轮询一个已失效 generation
                // 可能让窗口永远不结束。
                if (isProcessingExpired(current, request.getNow())) {
                    yield IdempotencyAcquireResult.of(IdempotencyAcquireStatus.PROCESSING_EXPIRED, current);
                }
                IdempotencyRecord touched = touchSlidingWindowIfNeeded(connection, current, request);
                yield IdempotencyAcquireResult.of(IdempotencyAcquireStatus.PROCESSING_ACTIVE, touched);
            }
            case FAILED -> {
                IdempotencyRecord touched = touchSlidingWindowIfNeeded(connection, current, request);
                yield IdempotencyAcquireResult.of(
                        touched.isFailureRetryable()
                                ? IdempotencyAcquireStatus.FAILED_RETRYABLE
                                : IdempotencyAcquireStatus.FAILED_FINAL,
                        touched);
            }
        };
    }

    /**
     * 显式 Recovery 的二次 CAS 入口。
     *
     * <p>扫描 candidate 只能说明“当时看起来需要恢复”，不能直接授予执行权。本方法会在新的 Tx-A 中重新锁行，
     * 校验 expectedOwner / expectedVersion / recovery policy / 当前状态；只有全部仍成立才生成 newOwner + version+1。</p>
     */
    @Override
    public IdempotencyRecoveryResult tryRecover(IdempotencyRecoveryAcquireRequest request) {
        try {
            return jdbc.inNewTransaction(connection -> tryRecoverInTransaction(connection, request));
        } catch (Exception error) {
            return IdempotencyRecoveryResult.providerError(error);
        }
    }

    private IdempotencyRecoveryResult tryRecoverInTransaction(Connection connection,
                                                                 IdempotencyRecoveryAcquireRequest request) throws Exception {

        // [R1] 重新锁住“当前”记录。扫描时的 candidate 只是快照，不能替代真正执行时的实时状态。
        IdempotencyRecord current = selectForUpdate(connection, request.getNamespace(), request.getKey());
        if (current == null) {
            return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.NOT_FOUND, null);
        }

        if (current.getRecoveryMode() != IdempotencyRecoveryMode.EXTERNAL_TASK) {
            return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.NOT_RECOVERABLE, current);
        }

        if (isWindowExpired(current, request.getNow())) {
            return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.NOT_RECOVERABLE, current);
        }

        // [R2] 扫描和真正任务执行之间存在时间差；version/owner 任一变化都说明 candidate 已过时。
        if (request.getExpectedVersion() != null && request.getExpectedVersion().longValue() != current.getVersion()) {
            return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.STALE_CANDIDATE, current);
        }
        if (request.getExpectedOwnerToken() != null && !Objects.equals(request.getExpectedOwnerToken(), current.getOwnerToken())) {
            return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.STALE_CANDIDATE, current);
        }

        if (routeConflict(current.getRouteKey(), request.getRouteKey())
                || hashConflict(current.getRequestHash(), request.getRequestHash())) {
            return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.KEY_CONFLICT, current);
        }

        if (current.getStatus() == IdempotencyStatus.SUCCESS) {
            return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.SUCCESS, current);
        }

        if (current.getStatus() == IdempotencyStatus.PROCESSING) {
            if (!isProcessingExpired(current, request.getNow())) {
                return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.PROCESSING_ACTIVE, current);
            }
            if (!request.isRecoverProcessingTimeout()) {
                return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.NOT_RECOVERABLE, current);
            }
            // [R3] 真正接管：newOwner + version+1，旧 owner 从这一刻起成为 stale generation。
            return IdempotencyRecoveryResult.acquired(reacquire(connection, current, request), "PROCESSING_TIMEOUT");
        }

        // FAILED
        if (!current.isFailureRetryable() || !request.isRecoverFailed()) {
            return IdempotencyRecoveryResult.of(IdempotencyRecoveryStatus.FAILED_FINAL, current);
        }
        return IdempotencyRecoveryResult.acquired(reacquire(connection, current, request),
                current.getFailureCode() == null ? "FAILED_RETRY" : current.getFailureCode());
    }

    /**
     * 当前 generation 成功完成后的 PROCESSING -> SUCCESS 条件写。
     *
     * <p>WHERE 条件必须同时包含 status=PROCESSING、ownerToken、version。
     * 这使得 A 已过期、B 已接管以后，A 即使恢复也无法把 B 的新状态覆盖掉。</p>
     *
     * <p><strong>事务边界：</strong>固定调用 inCurrentTransaction(...)。transaction-aware JdbcExecutionManager
     * 会复用 Tx-B 当前 Connection，实现“业务写 + SUCCESS”同事务提交；未接入事务组件时不会声称具备该原子性。</p>
     */
    @Override
    public IdempotencyWriteResult markSuccess(IdempotencySuccessRequest request) {
        try {
            return jdbc.inCurrentTransaction(connection -> {
                WindowTimes times = completionWindowTimes(
                        connection, request.getNamespace(), request.getKey(),
                        request.getMode(), request.getWindowPolicy(),
                        request.getIdempotencyWindow(), request.getRecordRetentionTtl(), request.getNow());

                String sql = "UPDATE " + table
                        + " SET status=?,result_payload=?,failure_code=NULL,failure_message=NULL,"
                        + "failure_retryable=FALSE,processing_expire_at=NULL,completed_at=?,updated_at=?,"
                        + "window_expire_at=?,retention_expire_at=?"
                        + " WHERE namespace=? AND idempotency_key=? AND status=? AND owner_token=? AND version=?";

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, IdempotencyStatus.SUCCESS.name());
                    statement.setString(2, request.getResultPayload());
                    setTimestamp(statement, 3, request.getNow());
                    setTimestamp(statement, 4, request.getNow());
                    setTimestamp(statement, 5, times.windowExpireAt);
                    setTimestamp(statement, 6, times.retentionExpireAt);
                    statement.setString(7, request.getNamespace());
                    statement.setString(8, request.getKey());
                    statement.setString(9, IdempotencyStatus.PROCESSING.name());
                    statement.setString(10, request.getOwnerToken());
                    statement.setLong(11, request.getVersion());

                    // affectedRows=1 才表示当前 owner/version 仍有完成资格；0 行要进一步区分 stale owner / 已终态 / 不存在。
                    return classifyWrite(connection, statement.executeUpdate(), request.getNamespace(), request.getKey(),
                            request.getOwnerToken(), request.getVersion());
                }
            });
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    /**
     * 当前 generation 失败后的 PROCESSING -> FAILED 条件写。
     *
     * <p>业务事务如果已经回滚，FAILED 必须使用独立新事务提交，否则失败状态也会跟着回滚。
     * 该语义固定为 Tx-C = inNewTransaction(...)。</p>
     */
    @Override
    public IdempotencyWriteResult markFailed(IdempotencyFailureRequest request) {
        try {
            // Tx-C：业务事务失败/回滚以后，FAILED 必须在独立事务中持久化；
            // 如果继续复用已经回滚的 Tx-B，FAILED 自己也会被一起回滚。
            return jdbc.inNewTransaction(connection -> {
                WindowTimes times = completionWindowTimes(
                        connection, request.getNamespace(), request.getKey(),
                        request.getMode(), request.getWindowPolicy(),
                        request.getIdempotencyWindow(), request.getRecordRetentionTtl(), request.getNow());

                String sql = "UPDATE " + table
                        + " SET status=?,failure_code=?,failure_message=?,failure_retryable=?,"
                        + "processing_expire_at=NULL,updated_at=?,window_expire_at=?,retention_expire_at=?"
                        + " WHERE namespace=? AND idempotency_key=? AND status=? AND owner_token=? AND version=?";

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, IdempotencyStatus.FAILED.name());
                    statement.setString(2, request.getFailure().getCode());
                    statement.setString(3, request.getFailure().getMessage());
                    statement.setBoolean(4, request.getFailure().isRetryable());
                    setTimestamp(statement, 5, request.getNow());
                    setTimestamp(statement, 6, times.windowExpireAt);
                    setTimestamp(statement, 7, times.retentionExpireAt);
                    statement.setString(8, request.getNamespace());
                    statement.setString(9, request.getKey());
                    statement.setString(10, IdempotencyStatus.PROCESSING.name());
                    statement.setString(11, request.getOwnerToken());
                    statement.setLong(12, request.getVersion());

                    return classifyWrite(connection, statement.executeUpdate(), request.getNamespace(), request.getKey(),
                            request.getOwnerToken(), request.getVersion());
                }
            });
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    @Override
    public Optional<IdempotencyRecord> find(String namespace, String key) {
        try {
            return jdbc.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(selectSql(false))) {
                    statement.setString(1, namespace);
                    statement.setString(2, key);
                    try (ResultSet rs = statement.executeQuery()) {
                        return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                    }
                }
            });
        } catch (Exception error) {
            throw new IllegalStateException("query idempotency record failed", error);
        }
    }

    /**
     * 为未来 Reliable Task 提供“只查询候选”的能力。
     *
     * <p>这里只返回候选快照，不负责扫描调度、抢任务、MQ 投递，也不直接修改状态。
     * 真正接管必须再次调用 tryRecover()，用 expectedOwner/version 做二次确认。</p>
     */
    @Override
    public List<IdempotencyRecoveryCandidate> findRecoveryCandidates(IdempotencyRecoveryQuery query) {
        try {
            return jdbc.withConnection(connection -> queryRecoveryCandidates(connection, query));
        } catch (Exception error) {
            throw new IllegalStateException("query recovery candidates failed", error);
        }
    }

    private List<IdempotencyRecoveryCandidate> queryRecoveryCandidates(
            Connection connection,
            IdempotencyRecoveryQuery query) throws SQLException {

        StringBuilder sql = new StringBuilder()
                .append("SELECT namespace,idempotency_key,route_key,request_hash,status,owner_token,version,")
                .append("processing_expire_at,failure_code FROM ").append(table)
                .append(" WHERE recovery_mode=? AND namespace=? AND (window_expire_at IS NULL OR window_expire_at>?) AND (")
                .append("(status=? AND processing_expire_at IS NOT NULL AND processing_expire_at<=?)")
                .append(" OR (status=? AND failure_retryable=TRUE))");
        if (query.getRouteKey() != null && !query.getRouteKey().isBlank()) {
            sql.append(" AND route_key=?");
        }
        sql.append(" ORDER BY updated_at ASC LIMIT ?");

        List<IdempotencyRecoveryCandidate> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, IdempotencyRecoveryMode.EXTERNAL_TASK.name());
            statement.setString(index++, query.getNamespace());
            setTimestamp(statement, index++, query.getNow());
            statement.setString(index++, IdempotencyStatus.PROCESSING.name());
            setTimestamp(statement, index++, query.getNow());
            statement.setString(index++, IdempotencyStatus.FAILED.name());
            if (query.getRouteKey() != null && !query.getRouteKey().isBlank()) {
                statement.setString(index++, query.getRouteKey());
            }
            statement.setInt(index, Math.max(1, query.getLimit()));

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new IdempotencyRecoveryCandidate(
                            rs.getString("namespace"),
                            rs.getString("idempotency_key"),
                            rs.getString("route_key"),
                            rs.getString("request_hash"),
                            IdempotencyStatus.valueOf(rs.getString("status")),
                            rs.getString("owner_token"),
                            rs.getLong("version"),
                            toInstant(rs.getTimestamp("processing_expire_at")),
                            rs.getString("failure_code")));
                }
            }
        }
        return result;
    }

    /**
     * 首次请求快路径：通过唯一键插入 PROCESSING。
     *
     * <p>发生唯一键冲突不视为 ProviderError，而是返回 false，让调用方转入“已有记录”判断。</p>
     */
    private boolean tryInsertProcessing(Connection connection, IdempotencyAcquireRequest request) throws SQLException {

        WindowTimes times = initialWindowTimes(request);
        String sql = "INSERT INTO " + table
                + " (namespace,idempotency_key,route_key,request_hash,status,owner_token,version,"
                + "result_payload,failure_code,failure_message,failure_retryable,recovery_mode,window_policy,"
                + "processing_expire_at,window_expire_at,retention_expire_at,created_at,updated_at,completed_at)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            statement.setString(i++, request.getNamespace());
            statement.setString(i++, request.getKey());
            statement.setString(i++, request.getRouteKey());
            statement.setString(i++, request.getRequestHash());
            statement.setString(i++, IdempotencyStatus.PROCESSING.name());
            statement.setString(i++, request.getOwnerToken());
            statement.setLong(i++, 1L);
            statement.setString(i++, null);
            statement.setString(i++, null);
            statement.setString(i++, null);
            statement.setBoolean(i++, false);
            statement.setString(i++, request.getRecoveryMode().name());
            statement.setString(i++, request.getWindowPolicy().name());
            setTimestamp(statement, i++, request.getNow().plus(request.getProcessingTimeout()));
            setTimestamp(statement, i++, times.windowExpireAt);
            setTimestamp(statement, i++, times.retentionExpireAt);
            setTimestamp(statement, i++, request.getNow());
            setTimestamp(statement, i++, request.getNow());
            statement.setTimestamp(i, null);
            statement.executeUpdate();
            return true;
        } catch (SQLException error) {
            if (isConstraintViolation(error)) {
                return false;
            }
            throw error;
        }
    }

    /**
     * WINDOWED 幂等窗口已经结束后开启新的 generation。
     *
     * <p>旧物理记录可能因为 retention 仍存在，因此这里不是 INSERT，而是在 version 条件保护下
     * 重置业务字段并把 version + 1。新的 generation 与旧 generation 在语义上已经是两次独立执行。</p>
     */
    private IdempotencyRecord restartWindow(Connection connection, IdempotencyRecord current,
                                              IdempotencyAcquireRequest request) throws SQLException {

        WindowTimes times = initialWindowTimes(request);
        String sql = "UPDATE " + table
                + " SET route_key=?,request_hash=?,status=?,owner_token=?,version=?,"
                + "result_payload=NULL,failure_code=NULL,failure_message=NULL,failure_retryable=FALSE,"
                + "recovery_mode=?,window_policy=?,processing_expire_at=?,window_expire_at=?,retention_expire_at=?,"
                + "created_at=?,updated_at=?,completed_at=NULL"
                + " WHERE namespace=? AND idempotency_key=? AND version=?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            statement.setString(i++, request.getRouteKey());
            statement.setString(i++, request.getRequestHash());
            statement.setString(i++, IdempotencyStatus.PROCESSING.name());
            statement.setString(i++, request.getOwnerToken());
            statement.setLong(i++, current.getVersion() + 1);
            statement.setString(i++, request.getRecoveryMode().name());
            statement.setString(i++, request.getWindowPolicy().name());
            setTimestamp(statement, i++, request.getNow().plus(request.getProcessingTimeout()));
            setTimestamp(statement, i++, times.windowExpireAt);
            setTimestamp(statement, i++, times.retentionExpireAt);
            setTimestamp(statement, i++, request.getNow());
            setTimestamp(statement, i++, request.getNow());
            statement.setString(i++, request.getNamespace());
            statement.setString(i++, request.getKey());
            statement.setLong(i, current.getVersion());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("failed to restart expired idempotency window");
            }
        }
        return selectForUpdate(connection, request.getNamespace(), request.getKey());
    }

    /**
     * 显式恢复时开启下一代 PROCESSING。
     *
     * <p>只允许在已经持有行锁、且旧 version 仍匹配时更新；newOwner + version+1
     * 是后续拒绝旧执行者的核心。</p>
     */
    private IdempotencyRecord reacquire(Connection connection, IdempotencyRecord previous,
                                          IdempotencyRecoveryAcquireRequest request) throws SQLException {

        String sql = "UPDATE " + table
                + " SET status=?,owner_token=?,version=?,processing_expire_at=?,"
                + "failure_code=NULL,failure_message=NULL,failure_retryable=FALSE,result_payload=NULL,"
                + "completed_at=NULL,updated_at=?"
                + " WHERE namespace=? AND idempotency_key=? AND version=?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, IdempotencyStatus.PROCESSING.name());
            statement.setString(2, request.getNewOwnerToken());
            statement.setLong(3, previous.getVersion() + 1);
            setTimestamp(statement, 4, request.getNow().plus(request.getProcessingTimeout()));
            setTimestamp(statement, 5, request.getNow());
            statement.setString(6, previous.getNamespace());
            statement.setString(7, previous.getKey());
            statement.setLong(8, previous.getVersion());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("failed to reacquire idempotency record");
            }
        }
        return selectForUpdate(connection, previous.getNamespace(), previous.getKey());
    }

    /**
     * SLIDING_ON_ACCESS 模式下推进语义窗口。
     *
     * <p>FIXED_FROM_FIRST_ACQUIRE 不会进入这里，因此普通重复访问不会无限延长窗口。</p>
     */
    private IdempotencyRecord touchSlidingWindowIfNeeded(Connection connection, IdempotencyRecord current,
                                                            IdempotencyAcquireRequest request) throws SQLException {
        if (!request.getMode().isWindowed() || request.getWindowPolicy() != IdempotencyWindowPolicy.SLIDING_ON_ACCESS
                || request.getIdempotencyWindow() == null) {
            return current;
        }
        WindowTimes next = new WindowTimes(
                request.getNow().plus(request.getIdempotencyWindow()),
                request.getNow().plus(request.getIdempotencyWindow()).plus(request.getRecordRetentionTtl()));
        String sql = "UPDATE " + table
                + " SET window_expire_at=?,retention_expire_at=?,updated_at=?"
                + " WHERE namespace=? AND idempotency_key=? AND version=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setTimestamp(statement, 1, next.windowExpireAt);
            setTimestamp(statement, 2, next.retentionExpireAt);
            setTimestamp(statement, 3, request.getNow());
            statement.setString(4, current.getNamespace());
            statement.setString(5, current.getKey());
            statement.setLong(6, current.getVersion());
            statement.executeUpdate();
        }
        return selectForUpdate(connection, current.getNamespace(), current.getKey());
    }

    /**
     * 计算 SUCCESS/FAILED 完成时应保留的 window/retention 时间。
     *
     * <p>FIXED 模式沿用首次抢占得到的绝对时间；SLIDING 模式才使用 now + window 重新推进。</p>
     */
    private WindowTimes completionWindowTimes(
            Connection connection,
            String namespace,
            String key,
            IdempotencyMode mode,
            IdempotencyWindowPolicy policy,
            Duration window,
            Duration retention,
            Instant now) throws SQLException {

        if (!mode.isWindowed()) {
            return WindowTimes.none();
        }
        if (policy == IdempotencyWindowPolicy.SLIDING_ON_ACCESS) {
            Instant windowAt = now.plus(window);
            return new WindowTimes(windowAt, windowAt.plus(retention));
        }
        IdempotencyRecord current = select(connection, namespace, key, false);
        return current == null ? WindowTimes.none() : new WindowTimes(current.getWindowExpireAt(), current.getRetentionExpireAt());
    }

    private WindowTimes initialWindowTimes(IdempotencyAcquireRequest request) {
        if (!request.getMode().isWindowed()) {
            return WindowTimes.none();
        }
        Instant windowAt = request.getNow().plus(request.getIdempotencyWindow());
        return new WindowTimes(windowAt, windowAt.plus(request.getRecordRetentionTtl()));
    }

    /**
     * 把 SQL affectedRows=0 翻译成稳定的领域语义。
     *
     * <p>不能只返回 false，因为 0 行可能分别意味着记录不存在、已经终态、owner/version 失效，
     * 三种情况对上层处理完全不同。尤其 STALE_OWNER 会让事务路径强制 rollback，防止旧 generation 的业务 SQL 泄漏提交。</p>
     */
    private IdempotencyWriteResult classifyWrite(
            Connection connection,
            int updated,
            String namespace,
            String key,
            String ownerToken,
            long version) throws SQLException {

        IdempotencyRecord current = select(connection, namespace, key, false);
        if (updated == 1) {
            return IdempotencyWriteResult.of(IdempotencyWriteStatus.UPDATED, current);
        }
        if (current == null) {
            return IdempotencyWriteResult.of(IdempotencyWriteStatus.NOT_FOUND, null);
        }
        if (current.getStatus() != IdempotencyStatus.PROCESSING) {
            return IdempotencyWriteResult.of(IdempotencyWriteStatus.ALREADY_FINAL, current);
        }
        if (!Objects.equals(ownerToken, current.getOwnerToken()) || version != current.getVersion()) {
            return IdempotencyWriteResult.of(IdempotencyWriteStatus.STALE_OWNER, current);
        }
        return IdempotencyWriteResult.providerError(
                new IllegalStateException("conditional write returned 0 but owner still appears current"));
    }

    private IdempotencyRecord selectForUpdate(Connection connection, String namespace, String key)
            throws SQLException {
        return select(connection, namespace, key, true);
    }

    private IdempotencyRecord select(Connection connection, String namespace, String key, boolean forUpdate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(selectSql(forUpdate))) {
            statement.setString(1, namespace);
            statement.setString(2, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private String selectSql(boolean forUpdate) {
        return "SELECT namespace,idempotency_key,route_key,request_hash,status,owner_token,version,"
                + "result_payload,failure_code,failure_message,failure_retryable,recovery_mode,window_policy,"
                + "processing_expire_at,window_expire_at,retention_expire_at,created_at,updated_at,completed_at"
                + " FROM " + table + " WHERE namespace=? AND idempotency_key=?"
                + (forUpdate ? " FOR UPDATE" : "");
    }

    private IdempotencyRecord map(ResultSet rs) throws SQLException {
        return IdempotencyRecord.builder()
                .namespace(rs.getString("namespace"))
                .key(rs.getString("idempotency_key"))
                .routeKey(rs.getString("route_key"))
                .requestHash(rs.getString("request_hash"))
                .status(IdempotencyStatus.valueOf(rs.getString("status")))
                .ownerToken(rs.getString("owner_token"))
                .version(rs.getLong("version"))
                .resultPayload(rs.getString("result_payload"))
                .failureCode(rs.getString("failure_code"))
                .failureMessage(rs.getString("failure_message"))
                .failureRetryable(rs.getBoolean("failure_retryable"))
                .recoveryMode(enumValue(IdempotencyRecoveryMode.class, rs.getString("recovery_mode"), IdempotencyRecoveryMode.NONE))
                .windowPolicy(enumValue(IdempotencyWindowPolicy.class,
                        rs.getString("window_policy"), IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE))
                .processingExpireAt(toInstant(rs.getTimestamp("processing_expire_at")))
                .windowExpireAt(toInstant(rs.getTimestamp("window_expire_at")))
                .retentionExpireAt(toInstant(rs.getTimestamp("retention_expire_at")))
                .createdAt(toInstant(rs.getTimestamp("created_at")))
                .updatedAt(toInstant(rs.getTimestamp("updated_at")))
                .completedAt(toInstant(rs.getTimestamp("completed_at")))
                .build();
    }

    private boolean isWindowExpired(IdempotencyRecord record, Instant now) {
        return record.getWindowExpireAt() != null && !record.getWindowExpireAt().isAfter(now);
    }

    private boolean isProcessingExpired(IdempotencyRecord record, Instant now) {
        return record.getProcessingExpireAt() != null && !record.getProcessingExpireAt().isAfter(now);
    }

    private boolean hashConflict(String oldHash, String newHash) {
        return oldHash != null && !oldHash.isBlank()
                && newHash != null && !newHash.isBlank()
                && !oldHash.equals(newHash);
    }

    private boolean routeConflict(String oldRoute, String newRoute) {
        if ((oldRoute == null || oldRoute.isBlank()) && (newRoute == null || newRoute.isBlank())) {
            return false;
        }
        return !Objects.equals(normalize(oldRoute), normalize(newRoute));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean isConstraintViolation(SQLException error) {
        String state = error.getSQLState();
        return state != null && state.startsWith("23");
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E defaultValue) {
        return value == null || value.isBlank() ? defaultValue : Enum.valueOf(type, value);
    }

    private static Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static void setTimestamp(PreparedStatement statement, int index, Instant value)
            throws SQLException {
        statement.setTimestamp(index, value == null ? null : Timestamp.from(value));
    }

    private static String validateTableName(String value) {
        String table = value == null || value.isBlank() ? "iron_idempotency_record" : value;
        if (!table.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("invalid table name: " + table);
        }
        return table;
    }

    private static final class WindowTimes {
        private final Instant windowExpireAt;
        private final Instant retentionExpireAt;

        private WindowTimes(Instant windowExpireAt, Instant retentionExpireAt) {
            this.windowExpireAt = windowExpireAt;
            this.retentionExpireAt = retentionExpireAt;
        }

        private static WindowTimes none() {
            return new WindowTimes(null, null);
        }
    }
}
