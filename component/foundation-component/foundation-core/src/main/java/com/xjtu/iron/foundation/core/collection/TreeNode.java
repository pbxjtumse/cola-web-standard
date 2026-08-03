package com.xjtu.iron.foundation.core.collection;

import java.util.List;

/**
 * 描述树形节点的最小只读协议。
 */
public interface TreeNode<ID, T extends TreeNode<ID, T>> {

    /** 返回当前节点标识。 */
    ID getId();

    /** 返回父节点标识；根节点可返回 {@code null}。 */
    ID getParentId();

    /**
     * 接收构建完成的直接子节点。
     *
     * @param children 不可修改的子节点列表
     */
    void setChildren(List<T> children);
}
