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
/**
 * 树形视图通知器接口，用于处理树形结构数据的变化通知
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public interface TreeViewNotifier<T extends NodeItem> {

    /**
     * 当数据集发生变化时调用
     */
    void onDataSetChange();

    /**
     * 当节点被移除时调用
     * @param nodeModel 被移除的节点模型
     */
    void onRemoveNode(NodeModel<T> nodeModel);

    /**
     * 当父节点的子节点被移除时调用
     * @param parentNode 父节点模型
     */
    void onRemoveChildNodes(NodeModel<T> parentNode);

    /**
     * 当节点项视图发生变化时调用
     * @param nodeModel 发生变化的节点模型
     */
    void onItemViewChange(NodeModel<T> nodeModel);

    /**
     * 当向父节点添加子节点时调用
     * @param parent 父节点模型
     * @param childNodes 子节点模型数组
     */
    void onAddNodes(NodeModel<T> parent, NodeModel<T>... childNodes);

    /**
     * 复制子树操作的通知
     * @param rootNode 根节点模型
     * @param sourceNode 源节点模型
     * @param handler 用于处理复制操作的处理器
     * @param delay 延迟执行时间（毫秒）
     * @param start 是否开始执行复制操作
     */
    void onCopySubtree(@NotNull NodeModel<T> rootNode, @NotNull NodeModel<T> sourceNode, @NotNull Handler handler, long delay, boolean start);

    /**
     * 根据节点ID定位节点位置
     * @param nodeId 节点唯一标识符
     * @return 对应的节点模型，如果未找到则返回null
     */
    NodeModel<T> locateNodePosition(String nodeId);
}
