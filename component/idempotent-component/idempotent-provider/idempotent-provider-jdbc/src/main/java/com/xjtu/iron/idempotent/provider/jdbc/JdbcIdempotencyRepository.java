package com.xjtu.iron.idempotent.provider.jdbc;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.IdempotencyStatus;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireResult;
import com.xjtu.iron.idempotent.api.repository.IdempotencyAcquireStatus;
import com.xjtu.iron.idempotent.api.repository.IdempotencyFailureRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRecord;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;
import com.xjtu.iron.idempotent.api.repository.IdempotencySuccessRequest;
import com.xjtu.iron.idempotent.api.repository.IdempotencyWriteResult;
import com.xjtu.iron.idempotent.api.repository.IdempotencyWriteStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * DURABLE 模式的 JDBC 幂等状态仓储。
 *
 * <p>正确性依赖三层约束：</p>
 * <ol>
 *     <li>首次抢占依赖 UNIQUE(namespace, idempotency_key)；</li>
 *     <li>已有记录使用 SELECT ... FOR UPDATE 串行判断超时/失败是否可接管；</li>
 *     <li>最终 SUCCESS / FAILED 写入再次比较 ownerToken + version，拒绝旧执行者。</li>
 * </ol>
 *
 * <p>注意：本仓储只管理“幂等状态”。业务数据与幂等 SUCCESS 如何进入同一事务，
 * 属于后续事务集成能力；V1 不假装解决跨事务双写问题。</p>
 */
public final class JdbcIdempotencyRepository implements IdempotencyRepository {

    public static final String PROVIDER_NAME = "jdbc";

    private static final Pattern SAFE_TABLE = Pattern.compile("[A-Za-z0-9_]+");

    private final DataSource dataSource;
    private final String table;

    public JdbcIdempotencyRepository(DataSource dataSource, String table) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        if (table == null || !SAFE_TABLE.matcher(table).matches()) {
            throw new IllegalArgumentException("unsafe tableName: " + table);
        }
        this.table = table;
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(IdempotencyMode mode) {
        return mode == IdempotencyMode.DURABLE;
    }

    /**
     * 尝试获得执行权。
     *
     * <p>先做 INSERT 快路径；唯一键冲突后才进入已有记录的行锁状态判断。
     * 这样第一次请求不需要额外 SELECT。</p>
     */
    @Override
    public IdempotencyAcquireResult tryAcquire(IdempotencyAcquireRequest request) {
        if (request.getMode() != IdempotencyMode.DURABLE) {
            return IdempotencyAcquireResult.providerError(
                    new IllegalArgumentException("jdbc supports DURABLE only"));
        }

        try {
            IdempotencyRecord created = tryInsert(request);
            if (created != null) {
                return IdempotencyAcquireResult.acquired(created, false);
            }
            return inspectExistingRecord(request);
        } catch (Exception error) {
            return IdempotencyAcquireResult.providerError(error);
        }
    }

    /**
     * 首次请求快路径。
     *
     * @return 新建的 PROCESSING 记录；如果是唯一键冲突则返回 null。
     */
    private IdempotencyRecord tryInsert(IdempotencyAcquireRequest request) throws SQLException {
        String sql = "INSERT INTO " + table
                + " (namespace,idempotency_key,request_hash,status,owner_token,version,"
                + "processing_expire_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?)";

        Instant now = request.getNow();
        Instant expireAt = now.plus(request.getProcessingTimeout());

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, request.getNamespace());
            statement.setString(2, request.getKey());
            statement.setString(3, request.getRequestHash());
            statement.setString(4, IdempotencyStatus.PROCESSING.name());
            statement.setString(5, request.getOwnerToken());
            statement.setLong(6, 1L);
            statement.setTimestamp(7, Timestamp.from(expireAt));
            statement.setTimestamp(8, Timestamp.from(now));
            statement.setTimestamp(9, Timestamp.from(now));
            statement.executeUpdate();

            // 这里 connection 默认 autoCommit=true，INSERT 已提交，可安全通过普通查询读取快照。
            return find(request.getNamespace(), request.getKey()).orElse(null);
        } catch (SQLException error) {
            if (isDuplicate(error)) {
                return null;
            }
            throw error;
        }
    }

    /**
     * 唯一键已存在时，在短事务中锁定当前幂等记录并完成状态决策。
     */
    private IdempotencyAcquireResult inspectExistingRecord(IdempotencyAcquireRequest request)
            throws SQLException {

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                IdempotencyRecord current = selectForUpdate(
                        connection, request.getNamespace(), request.getKey());

                // 理论上唯一键冲突后记录应存在。极端情况下记录被外部维护删除，可重新走一次首次抢占。
                if (current == null) {
                    connection.rollback();
                    return tryAcquire(request);
                }

                if (hashConflict(current.getRequestHash(), request.getRequestHash())) {
                    connection.commit();
                    return IdempotencyAcquireResult.of(IdempotencyAcquireStatus.KEY_CONFLICT, current);
                }

                if (current.getStatus() == IdempotencyStatus.SUCCESS) {
                    connection.commit();
                    return IdempotencyAcquireResult.of(IdempotencyAcquireStatus.SUCCESS, current);
                }

                if (current.getStatus() == IdempotencyStatus.PROCESSING) {
                    return handleProcessing(connection, current, request);
                }

                if (current.getStatus() == IdempotencyStatus.FAILED) {
                    return handleFailed(connection, current, request);
                }

                throw new IllegalStateException("unsupported status: " + current.getStatus());
            } catch (Exception error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private IdempotencyAcquireResult handleProcessing(
            Connection connection,
            IdempotencyRecord current,
            IdempotencyAcquireRequest request) throws SQLException {

        boolean expired = current.getProcessingExpireAt() != null
                && !current.getProcessingExpireAt().isAfter(request.getNow());

        if (!expired) {
            connection.commit();
            return IdempotencyAcquireResult.of(IdempotencyAcquireStatus.PROCESSING, current);
        }

        // 先显式落成 FAILED(PROCESSING_TIMEOUT)，保留清晰状态语义。
        markTimeoutFailed(connection, current, request.getNow());
        IdempotencyRecord failed = selectForUpdate(
                connection, request.getNamespace(), request.getKey());

        if (!request.isRetryOnProcessingTimeout()) {
            connection.commit();
            return IdempotencyAcquireResult.of(IdempotencyAcquireStatus.FAILED, failed);
        }

        // 同一事务内立即重新抢占，version + 1。
        IdempotencyRecord next = reacquire(connection, failed, request);
        connection.commit();
        return IdempotencyAcquireResult.acquired(next, true);
    }

    private IdempotencyAcquireResult handleFailed(
            Connection connection,
            IdempotencyRecord current,
            IdempotencyAcquireRequest request) throws SQLException {

        boolean processingTimeoutFailure = "PROCESSING_TIMEOUT".equals(current.getFailureCode());
        boolean retryAllowed = processingTimeoutFailure
                ? request.isRetryOnProcessingTimeout()
                : request.isRetryFailed() && current.isFailureRetryable();

        if (!retryAllowed) {
            connection.commit();
            return IdempotencyAcquireResult.of(IdempotencyAcquireStatus.FAILED, current);
        }

        IdempotencyRecord next = reacquire(connection, current, request);
        connection.commit();
        return IdempotencyAcquireResult.acquired(next, true);
    }

    private void markTimeoutFailed(Connection connection, IdempotencyRecord current, Instant now)
            throws SQLException {
        String sql = "UPDATE " + table
                + " SET status=?,failure_code=?,failure_message=?,failure_retryable=?,"
                + "processing_expire_at=NULL,updated_at=?"
                + " WHERE namespace=? AND idempotency_key=? AND status=? AND owner_token=? AND version=?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, IdempotencyStatus.FAILED.name());
            statement.setString(2, "PROCESSING_TIMEOUT");
            statement.setString(3, "processing owner lease expired");
            statement.setBoolean(4, true);
            statement.setTimestamp(5, Timestamp.from(now));
            statement.setString(6, current.getNamespace());
            statement.setString(7, current.getKey());
            statement.setString(8, IdempotencyStatus.PROCESSING.name());
            statement.setString(9, current.getOwnerToken());
            statement.setLong(10, current.getVersion());

            if (statement.executeUpdate() != 1) {
                throw new SQLException("PROCESSING timeout state changed concurrently");
            }
        }
    }

    /**
     * FAILED -> PROCESSING 重新抢占。
     *
     * <p>version 必须递增，这个值同时可以向业务侧暴露为 fencingVersion。</p>
     */
    private IdempotencyRecord reacquire(
            Connection connection,
            IdempotencyRecord previous,
            IdempotencyAcquireRequest request) throws SQLException {

        String sql = "UPDATE " + table
                + " SET status=?,owner_token=?,version=?,request_hash=?,processing_expire_at=?,"
                + "result_payload=NULL,completed_at=NULL,updated_at=?"
                + " WHERE namespace=? AND idempotency_key=? AND status=? AND version=?";

        long nextVersion = previous.getVersion() + 1;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, IdempotencyStatus.PROCESSING.name());
            statement.setString(2, request.getOwnerToken());
            statement.setLong(3, nextVersion);
            statement.setString(4, request.getRequestHash() == null
                    ? previous.getRequestHash() : request.getRequestHash());
            statement.setTimestamp(5, Timestamp.from(
                    request.getNow().plus(request.getProcessingTimeout())));
            statement.setTimestamp(6, Timestamp.from(request.getNow()));
            statement.setString(7, previous.getNamespace());
            statement.setString(8, previous.getKey());
            statement.setString(9, IdempotencyStatus.FAILED.name());
            statement.setLong(10, previous.getVersion());

            if (statement.executeUpdate() != 1) {
                throw new SQLException("failed to reacquire FAILED record");
            }
        }

        return selectForUpdate(connection, previous.getNamespace(), previous.getKey());
    }

    @Override
    public IdempotencyWriteResult markSuccess(IdempotencySuccessRequest request) {
        String sql = "UPDATE " + table
                + " SET status=?,result_payload=?,failure_code=NULL,failure_message=NULL,"
                + "failure_retryable=FALSE,processing_expire_at=NULL,completed_at=?,updated_at=?"
                + " WHERE namespace=? AND idempotency_key=? AND status=? AND owner_token=? AND version=?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, IdempotencyStatus.SUCCESS.name());
            statement.setString(2, request.getResultPayload());
            statement.setTimestamp(3, Timestamp.from(request.getNow()));
            statement.setTimestamp(4, Timestamp.from(request.getNow()));
            statement.setString(5, request.getNamespace());
            statement.setString(6, request.getKey());
            statement.setString(7, IdempotencyStatus.PROCESSING.name());
            statement.setString(8, request.getOwnerToken());
            statement.setLong(9, request.getVersion());

            return classifyWrite(
                    statement.executeUpdate(),
                    request.getNamespace(),
                    request.getKey(),
                    request.getOwnerToken(),
                    request.getVersion());
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    @Override
    public IdempotencyWriteResult markFailed(IdempotencyFailureRequest request) {
        String sql = "UPDATE " + table
                + " SET status=?,failure_code=?,failure_message=?,failure_retryable=?,"
                + "processing_expire_at=NULL,updated_at=?"
                + " WHERE namespace=? AND idempotency_key=? AND status=? AND owner_token=? AND version=?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, IdempotencyStatus.FAILED.name());
            statement.setString(2, request.getFailure().getCode());
            statement.setString(3, request.getFailure().getMessage());
            statement.setBoolean(4, request.getFailure().isRetryable());
            statement.setTimestamp(5, Timestamp.from(request.getNow()));
            statement.setString(6, request.getNamespace());
            statement.setString(7, request.getKey());
            statement.setString(8, IdempotencyStatus.PROCESSING.name());
            statement.setString(9, request.getOwnerToken());
            statement.setLong(10, request.getVersion());

            return classifyWrite(
                    statement.executeUpdate(),
                    request.getNamespace(),
                    request.getKey(),
                    request.getOwnerToken(),
                    request.getVersion());
        } catch (Exception error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    /**
     * 条件更新 0 行时，进一步区分“旧 owner”“已经 final”“记录消失”和真正 Provider 异常。
     */
    private IdempotencyWriteResult classifyWrite(
            int updated,
            String namespace,
            String key,
            String ownerToken,
            long version) {

        try {
            Optional<IdempotencyRecord> current = find(namespace, key);
            if (updated == 1) {
                return IdempotencyWriteResult.of(IdempotencyWriteStatus.UPDATED, current.orElse(null));
            }
            if (current.isEmpty()) {
                return IdempotencyWriteResult.of(IdempotencyWriteStatus.NOT_FOUND, null);
            }

            IdempotencyRecord record = current.get();
            if (record.getStatus() != IdempotencyStatus.PROCESSING) {
                return IdempotencyWriteResult.of(IdempotencyWriteStatus.ALREADY_FINAL, record);
            }
            if (!Objects.equals(ownerToken, record.getOwnerToken()) || version != record.getVersion()) {
                return IdempotencyWriteResult.of(IdempotencyWriteStatus.STALE_OWNER, record);
            }

            return IdempotencyWriteResult.providerError(
                    new IllegalStateException(
                            "conditional write returned 0 but owner still appears current"));
        } catch (RuntimeException error) {
            return IdempotencyWriteResult.providerError(error);
        }
    }

    @Override
    public Optional<IdempotencyRecord> find(String namespace, String key) {
        String sql = selectSql(false);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, namespace);
            statement.setString(2, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("query idempotency record failed", error);
        }
    }

    private IdempotencyRecord selectForUpdate(Connection connection, String namespace, String key)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(selectSql(true))) {
            statement.setString(1, namespace);
            statement.setString(2, key);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private String selectSql(boolean forUpdate) {
        return "SELECT namespace,idempotency_key,request_hash,status,owner_token,version,"
                + "result_payload,failure_code,failure_message,failure_retryable,processing_expire_at,"
                + "created_at,updated_at,completed_at FROM " + table
                + " WHERE namespace=? AND idempotency_key=?"
                + (forUpdate ? " FOR UPDATE" : "");
    }

    private IdempotencyRecord map(ResultSet rs) throws SQLException {
        return IdempotencyRecord.builder()
                .namespace(rs.getString("namespace"))
                .key(rs.getString("idempotency_key"))
                .requestHash(rs.getString("request_hash"))
                .status(IdempotencyStatus.valueOf(rs.getString("status")))
                .ownerToken(rs.getString("owner_token"))
                .version(rs.getLong("version"))
                .resultPayload(rs.getString("result_payload"))
                .failureCode(rs.getString("failure_code"))
                .failureMessage(rs.getString("failure_message"))
                .failureRetryable(rs.getBoolean("failure_retryable"))
                .processingExpireAt(instant(rs.getTimestamp("processing_expire_at")))
                .createdAt(instant(rs.getTimestamp("created_at")))
                .updatedAt(instant(rs.getTimestamp("updated_at")))
                .completedAt(instant(rs.getTimestamp("completed_at")))
                .build();
    }

    private Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private boolean hashConflict(String storedHash, String requestHash) {
        return storedHash != null
                && !storedHash.isBlank()
                && requestHash != null
                && !requestHash.isBlank()
                && !storedHash.equals(requestHash);
    }

    /**
     * 只把真正的唯一键冲突视为“已有幂等记录”。
     *
     * <p>不能简单把所有 SQLState 23xxx 都当成 DuplicateKey，因为 NOT NULL、FK 等完整性约束
     * 失败同样属于 23 类。如果误判，会把真正的数据库故障伪装成并发抢占失败。</p>
     */
    private boolean isDuplicate(SQLException error) {
        for (SQLException current = error; current != null; current = current.getNextException()) {
            String state = current.getSQLState();
            int code = current.getErrorCode();
            String message = current.getMessage() == null
                    ? ""
                    : current.getMessage().toLowerCase(Locale.ROOT);

            // MySQL duplicate key.
            if (code == 1062) {
                return true;
            }
            // H2 / PostgreSQL unique violation.
            if ("23505".equals(state)) {
                return true;
            }
            // 某些驱动只给通用 23000，需要结合错误文本，避免误吞其他完整性错误。
            if ("23000".equals(state)
                    && (message.contains("duplicate") || message.contains("unique"))) {
                return true;
            }
        }
        return false;
    }
}
