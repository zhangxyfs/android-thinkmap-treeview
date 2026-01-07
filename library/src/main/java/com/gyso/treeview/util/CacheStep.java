package com.gyso.treeview.util;

import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * CacheStep类用于处理缓存步骤相关的操作
 * 该类是一个泛型类，限定类型参数E必须继承自NodeItem类
 *
 * @param <E> 泛型参数，表示节点项的类型，必须是NodeItem的子类
 */
public class CacheStep<E extends NodeItem> {
    public static final String STATUS_ADD = "ADD";
    public static final String STATUS_DELETE = "DELETE";
    public static final String STATUS_UPDATE = "UPDATE";
    public static final String STATUS_MOVE = "MOVE";


    /**
     * 后退队列（模拟栈）
     */
    private final Deque<CacheNode<E>> mBackwardStep = new ArrayDeque<>();

    /**
     * 前进队列（模拟栈）
     */
    private final Deque<CacheNode<E>> mForwardStep = new ArrayDeque<>();

    /**
     * 初始化方法，清空后退步骤和前进步骤的历史记录
     */
    public void init() {
        mBackwardStep.clear();
        mForwardStep.clear();
    }

    /**
     * 判断后退步骤是否为空
     *
     * @return 如果后退步骤为空则返回true，否则返回false
     */
    public boolean isBackwardEmpty() {
        return mBackwardStep.isEmpty();
    }

    /**
     * 判断前进步骤是否为空
     *
     * @return 如果前进步骤为空则返回true，否则返回false
     */
    public boolean isForwardEmpty() {
        return mForwardStep.isEmpty();
    }

    /**
     * 添加一个导航步骤到后退栈中，并清空前进步骤 ( 实现方案有点问题 )
     *
     * @param nodeModel 要添加的节点模型，不可为null
     */
    @Deprecated
    public void addStep(NodeModel<E> nodeModel, String status) {
        if (nodeModel == null) return;
        mBackwardStep.push(new CacheNode<>(nodeModel, status));
        mForwardStep.clear();
    }

    /**
     * 查看后退步骤中的最后一个缓存节点
     *
     * @return 如果后退步骤为空则返回null，否则返回最后一个缓存节点
     */
    public CacheNode<E> watchLastBackwardStep() {
        if (mBackwardStep.isEmpty()) {
            return null;
        }
        return mBackwardStep.getLast();
    }

    /**
     * 查看前进步骤中的最后一个缓存节点
     *
     * @return 如果前进步骤为空则返回null，否则返回最后一个缓存节点
     */
    public CacheNode<E> watchLastForwardStep() {
        if (mForwardStep.isEmpty()) {
            return null;
        }
        return mForwardStep.getLast();
    }


    /**
     * 获取上一步的节点模型，并将其移动至前进栈中
     *
     * @return 上一步的节点模型，如果不存在则返回null
     */
    public CacheNode<E> popBackwardStep() {
        if (mBackwardStep.isEmpty()) {
            return null;
        }
        CacheNode<E> cache = mBackwardStep.pop();
        mForwardStep.push(cache);
        return cache;
    }

    /**
     * 获取下一步的节点模型，并将其移动至后退栈中
     *
     * @return 下一步的节点模型，如果不存在则返回null
     */
    public CacheNode<E> popForwardStep() {
        if (mForwardStep.isEmpty()) {
            return null;
        }
        CacheNode<E> nodeModel = mForwardStep.pop();
        mBackwardStep.push(nodeModel);
        return nodeModel;
    }

    public static class CacheNode<E extends NodeItem> {
        public NodeModel<E> nodeModel;

        public String status;

        public CacheNode(NodeModel<E> nodeModel, String status) {
            this.nodeModel = nodeModel;
            this.status = status;
        }
    }
}
