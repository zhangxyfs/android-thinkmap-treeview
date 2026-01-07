/**
 * manager the tree data nodes
 * guaishouN 674149099@qq.com
 */
package com.gyso.treeview.model;

import android.util.SparseArray;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * 树形数据模型类，用于管理树形结构的数据节点
 * 支持广度优先和深度优先的遍历方式，按层级组织节点数据
 *
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class TreeModel<T extends NodeItem> implements Serializable {
    /**
     * 类的标签，用于调试日志
     */
    private static final String TAG = TreeModel.class.getSimpleName();

    /**
     * 树的根节点
     */
    private NodeModel<T> rootNode;

    /**
     * 按层级分组的节点列表，key为层级索引，value为该层级的节点列表
     */
    private SparseArray<LinkedList<NodeModel<T>>> arrayByFloor = new SparseArray<>(10);

    /**
     * 遍历接口，用于节点遍历时的回调处理
     */
    private transient ITraversal<NodeModel<T>> iTraversal;

    /**
     * 最大深度值
     */
    private int maxDeep = 0;

    /**
     * 最小深度值
     */
    private int minDeep = 0;

    /**
     * 构造函数，创建树形数据模型
     *
     * @param rootNode 根节点
     */
    public TreeModel(NodeModel<T> rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * 遍历完成标志，用于控制遍历过程的中断
     */
    private boolean finishTraversal = false;

    /**
     * 向父节点添加子节点
     *
     * @param parent 父节点
     * @param childNodes 子节点数组
     */
    @SafeVarargs
    public final void addNode(@NotNull NodeModel<T> parent, @NotNull NodeModel<T>... childNodes) {
        // 检查子节点是否与父节点相同，避免自引用
        for (NodeModel<T> childNode : childNodes) {
            if (childNode == parent) return;
        }
        if (childNodes.length > 0) {
            // 设置父节点的树模型引用
            parent.treeModel = this;
            List<NodeModel<T>> nodeModels = new LinkedList<>();
            for (NodeModel<T> childNode : childNodes) {
                nodeModels.add(childNode);
                // 设置子节点的树模型引用
                childNode.treeModel = this;
            }
            // 向父节点添加子节点
            parent.addChildNodes(nodeModels);
            // 遍历所有子节点及其子树，更新层级和楼层列表
            for (NodeModel<T> child : childNodes) {
                child.traverseIncludeSelf(next -> {
                    // 更新节点层级
                    next.floor = next.parentNode.floor + 1;
                    // 将节点添加到对应楼层的列表中
                    List<NodeModel<T>> floorList = getFloorList(next.floor);
                    floorList.add(next);
                });
            }
        }
    }

    /**
     * 从父节点中移除子节点
     *
     * @param parent 父节点
     * @param childNode 要移除的子节点
     */
    public void removeNode(NodeModel<T> parent, NodeModel<T> childNode) {
        if (parent != null && childNode != null) {
            // 从父节点中移除子节点
            parent.removeChildNode(childNode);
            // 遍历被移除节点及其子树，从楼层列表中移除并重置层级
            childNode.traverseIncludeSelf(next -> {
                List<NodeModel<T>> nf = getFloorList(next.floor);
                nf.remove(next);
                next.floor = 0;
            });
        }
    }

    /**
     * 获取根节点
     *
     * @return 根节点
     */
    public NodeModel<T> getRootNode() {
        return rootNode;
    }

    /**
     * 按层级进行广度优先遍历
     * 广度遍历，按层级顺序访问节点，每一层的子节点按0到n的顺序排序
     * 节点将按层级顺序逐个显示
     */
    private void ergodicTreeByFloor() {
        Deque<NodeModel<T>> deque = new ArrayDeque<>();
        NodeModel<T> rootNode = getRootNode();
        deque.add(rootNode);
        while (!deque.isEmpty()) {
            rootNode = deque.poll();
            if (iTraversal != null) {
                iTraversal.next(rootNode);
            }
            if (this.finishTraversal) {
                break;
            }
            if (rootNode == null) {
                continue;
            }
            LinkedList<NodeModel<T>> childNodes = rootNode.getChildNodes();
            if (!childNodes.isEmpty()) {
                deque.addAll(childNodes);
            }
        }
        if (iTraversal != null) {
            iTraversal.finish();
            this.finishTraversal = false;
        }
    }

    /**
     * 获取按层级分组的节点列表
     *
     * @return 按层级分组的节点列表
     */
    public SparseArray<LinkedList<NodeModel<T>>> getArrayByFloor() {
        return arrayByFloor;
    }

    /**
     * 获取指定层级的所有节点
     *
     * @param floor 层级
     * @return 该层级的节点列表
     */
    public List<NodeModel<T>> getFloorList(int floor) {
        LinkedList<NodeModel<T>> nodeModels = arrayByFloor.get(floor);
        if (nodeModels == null) {
            nodeModels = new LinkedList<>();
            arrayByFloor.put(floor, nodeModels);
        }
        return nodeModels;
    }

    /**
     * 设置遍历完成标志
     *
     * @param finishTraversal 遍历完成标志
     */
    public void setFinishTraversal(boolean finishTraversal) {
        this.finishTraversal = finishTraversal;
    }

    /**
     * 遍历所有节点（默认按层级顺序）
     *
     * @param ITraversal 遍历接口
     */
    public void doTraversalNodes(ITraversal<NodeModel<T>> ITraversal) {
        doTraversalNodes(ITraversal, true);
    }

    /**
     * 遍历所有节点
     *
     * @param ITraversal 遍历接口
     * @param isOrderByFloor 是否按层级顺序遍历，true为广度优先，false为深度优先
     */
    public void doTraversalNodes(ITraversal<NodeModel<T>> ITraversal, boolean isOrderByFloor) {
        this.iTraversal = ITraversal;
        this.finishTraversal = false;
        if (isOrderByFloor) {
            ergodicTreeByFloor(); // 广度优先遍历
        } else {
            ergodicTreeByDeep();  // 深度优先遍历
        }
    }

    /**
     * 按深度进行深度优先遍历
     * 深度遍历，每层子节点列表按0到n的顺序排序
     * 节点将按深度顺序访问，先访问节点再访问其子节点，直到结束
     */
    private void ergodicTreeByDeep() {
        Stack<NodeModel<T>> stack = new Stack<>();
        NodeModel<T> rootNode = getRootNode();
        stack.add(rootNode);
        while (!stack.isEmpty()) {
            rootNode = stack.pop();
            if (iTraversal != null) {
                iTraversal.next(rootNode);
            }
            if (this.finishTraversal) {
                break;
            }
            if (rootNode == null) {
                continue;
            }
            LinkedList<NodeModel<T>> childNodes = rootNode.getChildNodes();
            if (!childNodes.isEmpty()) {
                stack.addAll(childNodes);
            }
        }
        if (iTraversal != null) {
            iTraversal.finish();
            this.finishTraversal = false;
        }
    }

    /**
     * 获取最大深度值
     *
     * @return 最大深度值
     */
    public int getMaxDeep() {
        return maxDeep;
    }

    /**
     * 设置最大深度值
     *
     * @param maxDeep 最大深度值
     */
    public void setMaxDeep(int maxDeep) {
        this.maxDeep = maxDeep;
    }

    /**
     * 获取最小深度值
     *
     * @return 最小深度值
     */
    public int getMinDeep() {
        return minDeep;
    }

    /**
     * 设置最小深度值
     *
     * @param minDeep 最小深度值
     */
    public void setMinDeep(int minDeep) {
        this.minDeep = minDeep;
    }
}

