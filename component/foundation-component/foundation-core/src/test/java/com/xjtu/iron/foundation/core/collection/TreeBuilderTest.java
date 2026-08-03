package com.xjtu.iron.foundation.core.collection;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreeBuilderTest {

    @Test
    void shouldBuildForest() {
        Node root = new Node("root", null);
        Node child = new Node("child", "root");
        assertEquals(List.of(root), TreeBuilder.build(List.of(root, child)));
        assertEquals(List.of(child), root.getChildren());
    }

    @Test
    void shouldRejectParentCycle() {
        assertThrows(IllegalArgumentException.class,
                () -> TreeBuilder.build(List.of(new Node("a", "b"), new Node("b", "a"))));
    }

    private static final class Node implements TreeNode<String, Node> {
        private final String id;
        private final String parentId;
        private List<Node> children = List.of();
        private Node(String id, String parentId) { this.id = id; this.parentId = parentId; }
        public String getId() { return id; }
        public String getParentId() { return parentId; }
        public void setChildren(List<Node> children) { this.children = children; }
        public List<Node> getChildren() { return children; }
    }
}
