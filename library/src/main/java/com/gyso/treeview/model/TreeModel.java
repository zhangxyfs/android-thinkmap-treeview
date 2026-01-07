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
 * manager the tree data nodes
 * guaishouN 674149099@qq.com
 */
public class TreeModel<T extends NodeItem> implements Serializable {
    private static final String TAG = TreeModel.class.getSimpleName();
    /**
     * the root for the tree
     */
    private NodeModel<T> rootNode;
    private SparseArray<LinkedList<NodeModel<T>>> arrayByFloor = new SparseArray<>(10);
    private transient ITraversal<NodeModel<T>> iTraversal;
    private int maxDeep = 0;
    private int minDeep = 0;

    public TreeModel(NodeModel<T> rootNode) {
        this.rootNode = rootNode;
    }

    private boolean finishTraversal = false;

    /**
     * add the node in some father node
     *
     * @param parent
     * @param childNodes
     */
    @SafeVarargs
    public final void addNode(@NotNull NodeModel<T> parent, @NotNull NodeModel<T>... childNodes) {
        for (NodeModel<T> childNode : childNodes) {
            if (childNode == parent) return;
        }
        if (childNodes.length > 0) {
            parent.treeModel = this;
            List<NodeModel<T>> nodeModels = new LinkedList<>();
            for (NodeModel<T> childNode : childNodes) {
                nodeModels.add(childNode);
                childNode.treeModel = this;
            }
            parent.addChildNodes(nodeModels);
            for (NodeModel<T> child : childNodes) {
                child.traverseIncludeSelf(next -> {
                    next.floor = next.parentNode.floor + 1;
                    List<NodeModel<T>> floorList = getFloorList(next.floor);
                    floorList.add(next);
                });
            }
        }
    }

    /**
     * remove
     *
     * @param parent    p node
     * @param childNode c node
     */
    public void removeNode(NodeModel<T> parent, NodeModel<T> childNode) {
        if (parent != null && childNode != null) {
            parent.removeChildNode(childNode);
            childNode.traverseIncludeSelf(next -> {
                List<NodeModel<T>> nf = getFloorList(next.floor);
                nf.remove(next);
                next.floor = 0;
            });
        }
    }

    public NodeModel<T> getRootNode() {
        return rootNode;
    }


    /**
     * child nodes will ergodic in the last
     * 广度遍历
     * breadth search
     * For every floor , child nodes  has been  sort 0--->n;
     * And node will been display one by one floor.
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

    public SparseArray<LinkedList<NodeModel<T>>> getArrayByFloor() {
        return arrayByFloor;
    }

    /**
     * @param floor level
     * @return all nodes in the same floor
     */
    public List<NodeModel<T>> getFloorList(int floor) {
        LinkedList<NodeModel<T>> nodeModels = arrayByFloor.get(floor);
        if (nodeModels == null) {
            nodeModels = new LinkedList<>();
            arrayByFloor.put(floor, nodeModels);
        }
        return nodeModels;
    }

    public void setFinishTraversal(boolean finishTraversal) {
        this.finishTraversal = finishTraversal;
    }

    public void doTraversalNodes(ITraversal<NodeModel<T>> ITraversal) {
        doTraversalNodes(ITraversal, true);
    }

    /**
     * when ergodic this tree, it will call back on {@link ITraversal)}
     *
     * @param ITraversal node
     */
    public void doTraversalNodes(ITraversal<NodeModel<T>> ITraversal, boolean isOrderByFloor) {
        this.iTraversal = ITraversal;
        this.finishTraversal = false;
        if (isOrderByFloor) {
            ergodicTreeByFloor();
        } else {
            ergodicTreeByDeep();
        }
    }

    /**
     * child nodes will ergodic by deep
     * 深度遍历
     * depth search
     * For every  child node list  has been  sort 0--->n;
     * And node will been display one then the children by deep until end.
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

    public int getMaxDeep() {
        return maxDeep;
    }

    public void setMaxDeep(int maxDeep) {
        this.maxDeep = maxDeep;
    }

    public int getMinDeep() {
        return minDeep;
    }

    public void setMinDeep(int minDeep) {
        this.minDeep = minDeep;
    }
}
