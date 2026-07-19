package com.xjtu.iron.distributed.lock.provider.jdbc.fencing;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * JDBC fencing token 表初始化器。
 *
 * <p>生产环境建议使用 Flyway/Liquibase 管理 DDL；该初始化器主要服务 Demo、测试和快速接入。</p>
 */
public final class JdbcFencingTokenSchemaInitializer {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final DataSource dataSource;
    private final String tableName;
    private final String platform;

    public JdbcFencingTokenSchemaInitializer(DataSource dataSource, String tableName, String platform) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        if (tableName == null || !SAFE_IDENTIFIER.matcher(tableName.trim()).matches()) {
            throw new IllegalArgumentException("tableName must be a simple SQL identifier: " + tableName);
        }
        this.tableName = tableName.trim();
        this.platform = platform == null ? "mysql" : platform.trim().toLowerCase(Locale.ROOT);
    }

    public void initialize() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(createTableSql());
        }
    }

    public String createTableSql() {
        String common = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "namespace VARCHAR(128) NOT NULL,"
                + "lock_name VARCHAR(512) NOT NULL,"
                + "current_token BIGINT NOT NULL,"
                + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (namespace, lock_name))";
        if ("mysql".equals(platform) || "mariadb".equals(platform)) {
            return common + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }
        if ("h2".equals(platform) || "postgresql".equals(platform) || "postgres".equals(platform)) {
            return common;
        }
        throw new IllegalArgumentException("unsupported JDBC fencing schema platform: " + platform);
    }
}
