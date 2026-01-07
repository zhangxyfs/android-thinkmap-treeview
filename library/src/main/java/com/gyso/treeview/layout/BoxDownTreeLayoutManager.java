package com.gyso.treeview.layout;

import android.content.Context;
import android.view.View;

import com.gyso.treeview.TreeViewContainer;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.line.BaseLine;
import com.gyso.treeview.model.ITraversal;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.util.NodeCheck;
import com.gyso.treeview.util.TreeViewLog;
import com.gyso.treeview.util.ViewBox;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 垂直向下布局的树形管理器，用于实现从上到下的树形结构布局
 *
 * @param <T> 节点项的泛型类型，必须继承自NodeItem
 */
public class BoxDownTreeLayoutManager<T extends NodeItem> extends TreeLayoutManager<T> {
    private static final String TAG = BoxDownTreeLayoutManager.class.getSimpleName();

    /**
     * 用于存储父节点的栈结构，用于布局计算时的节点遍历
     */
    private final Deque<NodeModel<T>> parentsStack = new ArrayDeque<>();

    /**
     * 用于存储子节点的集合，用于布局计算时的节点管理
     */
    private final Set<NodeModel<T>> childrenSet = new HashSet<>();

    /**
     * 节点与视图盒子的映射关系，用于存储每个节点对应的布局位置信息
     */
    private final Map<NodeModel<T>, ViewBox> nodeToBoxMap = new HashMap<>();

    /**
     * 构造函数，初始化垂直向下树形布局管理器
     *
     * @param context            上下文对象
     * @param spaceParentToChild 父子节点之间的间距
     * @param spacePeerToPeer    同级节点之间的间距
     * @param baseline           基线对齐方式
     */
    public BoxDownTreeLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        super(context, spaceParentToChild, spacePeerToPeer, baseline);
    }

    @Override
    public void calculateByLayoutAlgorithm(TreeModel<T> mTreeModel) {

    }

    @Override
    public int getTreeLayoutType() {
        return LAYOUT_TYPE_VERTICAL_DOWN;
    }

    @Override
    public void release() {
        parentsStack.clear();
        childrenSet.clear();
        nodeToBoxMap.clear();
    }

    /**
     * 执行测量操作，计算所有节点的尺寸和位置
     * 遍历树形结构，先测量叶子节点，然后逐层向上测量父节点
     *
     * @param treeViewContainer 树形视图容器
     */
    @Override
    public void performMeasure(TreeViewContainer<T> treeViewContainer) {
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            mContentViewBox.clear();
            nodeToBoxMap.clear();
            parentsStack.clear();
            childrenSet.clear();
            ITraversal<NodeModel<T>> traversal = new ITraversal<>() {
                @Override
                public void next(NodeModel<T> next) {
                    TreeViewLog.d(TAG, "performMeasure:" + next);
                    if (!next.childNodes.isEmpty()) {
                        parentsStack.add(next);
                    } else {
                        childrenSet.add(next);
                    }
                    measure(next, treeViewContainer);
                }

                @Override
                public void finish() {
                    // 从栈中取出父节点，当其所有子节点都已测量完成时进行布局计算
                    while (!parentsStack.isEmpty()) {
                        NodeModel<T> oneParent = parentsStack.pollFirst();
                        if (childrenSet.containsAll(oneParent.childNodes)) {
                            layoutByBox(oneParent, treeViewContainer);
                            childrenSet.add(oneParent);
                            childrenSet.removeAll(oneParent.childNodes);
                        } else {
                            parentsStack.addLast(oneParent);
                        }
                    }
                    ViewBox rootBox = nodeToBoxMap.get(mTreeModel.getRootNode());
                    mContentViewBox.setValues(rootBox);
                    onManagerFinishMeasureAllNodes(treeViewContainer);
                }
            };
            //深度遍历
            mTreeModel.doTraversalNodes(traversal, false);
        }
    }

    /**
     * 所有节点测量完成后，计算整体布局的缩放和居中调整
     * 根据窗口尺寸和内容尺寸计算合适的缩放比例和偏移量
     *
     * @param treeViewContainer 树形视图容器
     */
    @Override
    public void onManagerFinishMeasureAllNodes(TreeViewContainer<T> treeViewContainer) {
        getPadding(treeViewContainer);
        mContentViewBox.bottom += (paddingBox.bottom + paddingBox.top);
        mContentViewBox.right += (paddingBox.left + paddingBox.right);
        fixedViewBox.setValues(mContentViewBox);
        if (winHeight == 0 || winWidth == 0) {
            return;
        }

        float scale = 1f * winWidth / winHeight;
        float wr = 1f * mContentViewBox.getWidth() / winWidth;
        float hr = 1f * mContentViewBox.getHeight() / winHeight;
        if (wr >= hr) {
            float bh = mContentViewBox.getWidth() / scale;
            fixedViewBox.bottom = (int) bh;
        } else {
            float bw = mContentViewBox.getHeight() * scale;
            fixedViewBox.right = (int) bw;
        }
        mFixedDx = paddingBox.top + (fixedViewBox.getWidth() - mContentViewBox.getWidth()) / 2;
        mFixedDy = paddingBox.left + (fixedViewBox.getHeight() - mContentViewBox.getHeight()) / 2;
    }

    /**
     * 测量单个节点的尺寸并记录到映射表中
     *
     * @param node              要测量的节点
     * @param treeViewContainer 树形视图容器
     */
    private void measure(NodeModel<T> node, TreeViewContainer<T> treeViewContainer) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(node);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }
        int curW = currentNodeView.getMeasuredWidth();
        int curH = currentNodeView.getMeasuredHeight();
        ViewBox viewBox = nodeToBoxMap.get(node);
        if (viewBox == null) {
            viewBox = new ViewBox(0, 0, curH, curW);
            nodeToBoxMap.put(node, viewBox);
        }
        viewBox.clear();
        viewBox.right = curW;
        viewBox.bottom = curH;
    }

    /**
     * 执行布局操作，为所有节点设置最终的位置
     * 遍历树形结构，为每个节点计算并设置布局位置
     *
     * @param treeViewContainer 树形视图容器
     */
    @Override
    public void performLayout(final TreeViewContainer<T> treeViewContainer) {

        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            ITraversal<NodeModel<T>> traversal = new ITraversal<NodeModel<T>>() {
                @Override
                public void next(NodeModel<T> next) {
                    layoutNodes(next, treeViewContainer);
                }

                @Override
                public void finish() {
                    onManagerFinishLayoutAllNodes(treeViewContainer);
                }
            };
            mTreeModel.doTraversalNodes(traversal, false);
        }
    }

    @Override
    public ViewBox getTreeLayoutBox() {
        return fixedViewBox;
    }

    /**
     * 根据父节点和子节点的尺寸信息计算子节点的布局位置
     * 将子节点水平排列在父节点下方，并进行居中对齐
     *
     * @param parentNode        父节点
     * @param treeViewContainer 树形视图容器
     */
    private void layoutByBox(NodeModel<T> parentNode, TreeViewContainer<T> treeViewContainer) {
        TreeViewHolder<?> parentHolder = treeViewContainer.getTreeViewHolder(parentNode);
        View parentNodeView = parentHolder == null ? null : parentHolder.getView();
        if (parentNodeView == null) {
            throw new NullPointerException(" parentNodeView can not be null");
        }
        ViewBox parentLocationBox = nodeToBoxMap.get(parentNode);
        int maxChildHeight = 0;
        int sumWidth = 0;
        for (NodeModel<T> childNode : parentNode.childNodes) {
            ViewBox childLocationBox = nodeToBoxMap.get(childNode);
            maxChildHeight = Math.max(maxChildHeight, childLocationBox.getHeight());
            int childWidth = childLocationBox.getWidth();
            int childHeight = childLocationBox.getHeight();
            childLocationBox.left = sumWidth;
            childLocationBox.top = spaceParentToChild * 2 + parentLocationBox.getHeight();
            childLocationBox.bottom = childLocationBox.top + childHeight;
            childLocationBox.right = childLocationBox.left + childWidth;
            nodeToBoxMap.put(childNode, childLocationBox);
            sumWidth += childLocationBox.getWidth() + spacePeerToPeer;
        }
        sumWidth -= spacePeerToPeer;
        int delta = (parentLocationBox.getWidth() - sumWidth) / 2;
        if (delta > 0) {
            ViewBox deltaBox = new ViewBox(0, delta, 0, delta);
            for (NodeModel<T> childNode : parentNode.childNodes) {
                ViewBox childLocationBox = nodeToBoxMap.get(childNode);
                ViewBox newLocation = childLocationBox.add(deltaBox);
                nodeToBoxMap.put(childNode, newLocation);
            }
        }
        parentLocationBox.bottom = spaceParentToChild * 2 + parentLocationBox.getHeight() + maxChildHeight;
        parentLocationBox.right = Math.max(parentLocationBox.getWidth(), sumWidth);
        nodeToBoxMap.put(parentNode, parentLocationBox);
    }

    /**
     * 为单个节点设置布局位置，计算节点在整体布局中的绝对坐标
     *
     * @param currentNode       当前节点
     * @param treeViewContainer 树形视图容器
     */
    private void layoutNodes(NodeModel<T> currentNode, TreeViewContainer<T> treeViewContainer) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();

        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }

        int currentWidth = currentNodeView.getMeasuredWidth();
        int currentHeight = currentNodeView.getMeasuredHeight();

        ViewBox viewBox = nodeToBoxMap.get(currentNode);
        int centerFix = Math.max(viewBox.getWidth(), currentWidth) - currentWidth;
        int top = mFixedDy + viewBox.top;
        int left = mFixedDx + viewBox.left + centerFix / 2;
        NodeModel<T> pNode = currentNode.parentNode;
        // 递归计算所有父节点的偏移量，得到绝对位置
        while (pNode != null) {
            ViewBox upViewBox = nodeToBoxMap.get(pNode);
            top += upViewBox.top;
            left += upViewBox.left;
            pNode = pNode.parentNode;
        }

        int bottom = top + currentHeight;
        int right = left + currentWidth;

        ViewBox finalLocation = new ViewBox(top, left, bottom, right);
        onManagerLayoutNode(currentNode, currentNodeView, finalLocation, treeViewContainer);

        // 控制隐藏被折叠的父节点下的子节点
        currentNodeView.setVisibility(NodeCheck.parentNodeContract(currentNode) ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onManagerLayoutNode(NodeModel<T> currentNode, View currentNodeView, ViewBox finalLocation, TreeViewContainer<T> treeViewContainer) {
        if (!layoutAnimatePrepare(currentNode, currentNodeView, finalLocation, treeViewContainer)) {
            currentNodeView.layout(finalLocation.left, finalLocation.top, finalLocation.right, finalLocation.bottom);
        }
    }

    @Override
    public void onManagerFinishLayoutAllNodes(TreeViewContainer<T> treeViewContainer) {
        layoutAnimate(treeViewContainer);
    }
}
