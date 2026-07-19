package com.xjtu.iron.distributed.lock.provider.jdbc.fencing;

/** JDBC fencing token Provider 常量。 */
public final class JdbcFencingTokenConstants {

    public static final String PROVIDER_NAME = "jdbc-sequence";
    public static final String DEFAULT_TABLE_NAME = "iron_lock_fencing_token";
    public static final int DEFAULT_MAX_RETRIES = 5;
    public static final int MAX_NAMESPACE_LENGTH = 128;
    public static final int MAX_LOCK_NAME_LENGTH = 512;

    private JdbcFencingTokenConstants() {
    }
}
