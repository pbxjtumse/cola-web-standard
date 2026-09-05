package com.xjtu.iron.relational.spi;

import com.xjtu.iron.relational.api.exception.RelationalAccessException;

import java.sql.SQLException;

/**
 * 将数据库厂商相关 SQLException / SQLState / vendorCode 翻译为稳定的
 * RelationalAccessException + RelationalFailureType。
 */
public interface SqlExceptionTranslator {

    RelationalAccessException translate(
            SqlExecutionContext context,
            SQLException exception
    );
}
