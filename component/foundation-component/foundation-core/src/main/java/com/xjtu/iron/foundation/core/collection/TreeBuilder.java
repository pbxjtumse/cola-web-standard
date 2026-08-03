package com.xjtu.iron.foundation.core.collection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 根据节点标识和父标识构建树结构。
 */
public final class TreeBuilder {

    private TreeBuilder() {
    }

    /**
     * 构建森林；父节点不存在的节点按根节点处理。
     */
    public static <ID, T extends TreeNode<ID, T>> List<T> build(List<T> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }

        Map<ID, T> index = new LinkedHashMap<>();
        Map<ID, List<T>> children = new LinkedHashMap<>();
        for (T node : nodes) {
            Objects.requireNonNull(node, "tree node must not be null");
            if (index.putIfAbsent(node.getId(), node) != null) {
                throw new IllegalArgumentException("duplicate tree node id: " + node.getId());
            }
            children.computeIfAbsent(node.getId(), ignored -> new ArrayList<>());
        }

        validateNoCycles(index);

        List<T> roots = new ArrayList<>();
        for (T node : nodes) {
            T parent = index.get(node.getParentId());
            if (parent == null || Objects.equals(node.getId(), node.getParentId())) {
                roots.add(node);
            } else {
                children.get(parent.getId()).add(node);
            }
        }
        nodes.forEach(node -> node.setChildren(List.copyOf(children.get(node.getId()))));
        return List.copyOf(roots);
    }

    /**
     * 沿父节点链检查循环引用。
     */
    private static <ID, T extends TreeNode<ID, T>> void validateNoCycles(Map<ID, T> index) {
        for (T start : index.values()) {
            java.util.LinkedHashSet<ID> path = new java.util.LinkedHashSet<>();
            T current = start;
            while (current != null && current.getParentId() != null
                    && !Objects.equals(current.getId(), current.getParentId())) {
                if (!path.add(current.getId())) {
                    throw new IllegalArgumentException("tree contains a parent cycle: " + path);
                }
                current = index.get(current.getParentId());
            }
        }
    }
}
