package com.xjtu.iron.relational.api.result;

/**
 * INSERT / UPDATE / DELETE 的标准结果。
 *
 * @param affectedRows JDBC 返回的受影响行数
 */
public record UpdateResult(long affectedRows) {
}
