package com.gyso.treeview.util;

import androidx.annotation.Nullable;

import com.gyso.treeview.model.NodeModel;

/**
 * 节点检测
 */
public class NodeCheck {
    /**
     * 检测当前节点的父节点是否被contract
     *
     * @param currentNode 当前节点
     * @return true 父节点被contract
     */
    public static boolean parentNodeContract(@Nullable NodeModel<?> currentNode) {
        if (currentNode == null) return false;
        NodeModel<?> parentNode = currentNode.parentNode;
        while (parentNode != null) {
            if (parentNode.isContract()) {
                return true;
            }
            parentNode = parentNode.parentNode;
        }
        return false;
    }
}
