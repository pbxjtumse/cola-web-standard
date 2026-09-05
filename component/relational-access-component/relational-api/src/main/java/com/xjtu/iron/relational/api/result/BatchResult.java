package com.xjtu.iron.relational.api.result;

/**
 * JDBC batch 的标准结果。
 *
 * @param updateCounts 与 PreparedStatement.executeBatch() 顺序对应的更新计数
 */
public record BatchResult(int[] updateCounts) {

    public BatchResult {
        updateCounts = updateCounts == null ? new int[0] : updateCounts.clone();
    }

    @Override
    public int[] updateCounts() {
        return updateCounts.clone();
    }

    public int size() {
        return updateCounts.length;
    }
}
