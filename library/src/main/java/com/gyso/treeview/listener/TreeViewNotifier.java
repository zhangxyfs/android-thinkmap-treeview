package com.gyso.treeview.listener;


import android.os.Handler;

import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;

import org.jetbrains.annotations.NotNull;

/**
 * @Author: 怪兽N
 * @Time: 2021/4/23
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 * @Describe:
 */
public interface TreeViewNotifier<T extends NodeItem> {
    void onDataSetChange();

    void onRemoveNode(NodeModel<T> nodeModel);

    void onRemoveChildNodes(NodeModel<T> parentNode);

    void onItemViewChange(NodeModel<T> nodeModel);

    void onAddNodes(NodeModel<T> parent, NodeModel<T>... childNodes);

    void onCopySubtree(@NotNull NodeModel<T> rootNode, @NotNull NodeModel<T> sourceNode, @NotNull Handler handler, long delay, boolean start);

    NodeModel<T> locateNodePosition(String nodeId);
}