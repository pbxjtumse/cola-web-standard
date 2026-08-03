package com.xjtu.iron.foundation.core.collection;

import java.util.List;

/**
 * 可构造成树的节点最小协议。
 *
 * <p>该接口用于配置树、菜单树、分类树等纯技术组织结构。业务实体不一定需要直接实现它，
 * 也可以在调用 TreeUtils 时通过函数式参数完成 id、parentId 和 children 的映射。</p>
 *
 * @param <K> 节点 id 类型
 * @param <N> 节点类型
 */
public interface TreeNode<K, N> {

    K getId();

    K getParentId();

    void setChildren(List<N> children);
}
