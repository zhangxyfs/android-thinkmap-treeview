/**
 * @Author: 怪兽N
 * @Time: 2021/5/8  19:06
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 * @Describe: Vertically down layout the tree view
 */
package com.gyso.treeview.layout;

import android.content.Context;
import android.view.View;

import com.gyso.treeview.TreeViewContainer;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.algorithm.table.Table;
import com.gyso.treeview.line.BaseLine;
import com.gyso.treeview.model.ITraversal;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.util.NodeCheck;
import com.gyso.treeview.util.ViewBox;

/**
 * 表格向下树形布局管理器，继承自TreeLayoutManager，用于实现垂直向下的表格布局
 *
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class TableDownTreeLayoutManager<T extends NodeItem> extends TreeLayoutManager<T> {
    protected MeasureListener<T> measureListener = null;

    /**
     * 构造函数，初始化表格向下布局管理器
     *
     * @param context            上下文
     * @param spaceParentToChild 父子节点之间的间距
     * @param spacePeerToPeer    同级节点之间的间距
     * @param baseline           基线设置
     */
    public TableDownTreeLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        super(context, spaceParentToChild, spacePeerToPeer, baseline);
    }

    @Override
    public int getTreeLayoutType() {
        return LAYOUT_TYPE_VERTICAL_DOWN;
    }

    @Override
    public void release() {
        measureListener = null;
    }

    @Override
    public void calculateByLayoutAlgorithm(TreeModel<T> mTreeModel) {
        new Table<T>().reconstruction(mTreeModel, Table.LOOSE_TABLE);
    }

    /**
     * 执行测量并监听测量过程
     *
     * @param treeViewContainer 树形视图容器
     * @param measureListener   测量监听器，用于监听测量过程中的事件
     */
    public void performMeasureAndListen(TreeViewContainer<T> treeViewContainer, MeasureListener<T> measureListener) {
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            mContentViewBox.clear();
            floorMax.clear();
            deepMax.clear();
            this.measureListener = measureListener;
            ITraversal<NodeModel<T>> traversal = new ITraversal<>() {
                @Override
                public void next(NodeModel<T> next) {
                    measure(next, treeViewContainer);
                    if (measureListener != null) {
                        measureListener.onMeasureChild(next);
                    }
                }

                @Override
                public void finish() {
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
                    mFixedDx = (fixedViewBox.getWidth() - mContentViewBox.getWidth()) / 2;
                    mFixedDy = (fixedViewBox.getHeight() - mContentViewBox.getHeight()) / 2;

                    //计算楼层起始位置
                    for (int i = 0; i <= floorMax.size(); i++) {
                        int fn = (i == floorMax.size()) ? floorMax.size() : floorMax.keyAt(i);
                        int preStart = floorStart.get(fn - 1, 0);
                        int preMax = floorMax.get(fn - 1, 0);
                        int startPos = (fn == 0 ? (mFixedDy + paddingBox.top) : spaceParentToChild) + preStart + preMax;
                        floorStart.put(fn, startPos);
                    }

                    //计算深度起始位置
                    for (int i = 0; i <= deepMax.size(); i++) {
                        int dn = (i == deepMax.size()) ? deepMax.size() : deepMax.keyAt(i);
                        int preStart = deepStart.get(dn - 1, 0);
                        int preMax = deepMax.get(dn - 1, 0);
                        int startPos = (dn == 0 ? (mFixedDx + paddingBox.left) : spacePeerToPeer) + preStart + preMax;
                        deepStart.put(dn, startPos);
                    }

                    if (measureListener != null) {
                        measureListener.onMeasureFinished();
                    }
                }
            };
            mTreeModel.doTraversalNodes(traversal);
        }
    }

    @Override
    public void performMeasure(TreeViewContainer<T> treeViewContainer) {
        performMeasureAndListen(treeViewContainer, null);
    }

    /**
     * 测量单个节点的尺寸
     *
     * @param node              要测量的节点
     * @param treeViewContainer 树形视图容器
     */
    public void measure(NodeModel<T> node, TreeViewContainer<T> treeViewContainer) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(node);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }
        int preMaxH = floorMax.get(node.floor);
        int curH = currentNodeView.getMeasuredHeight();
        if (preMaxH < curH) {
            floorMax.put(node.floor, curH);
            int delta = spaceParentToChild + curH - preMaxH;
            mContentViewBox.bottom += delta;
        }

        int preMaxW = deepMax.get(node.deep);
        int curW = currentNodeView.getMeasuredWidth();
        if (preMaxW < curW) {
            deepMax.put(node.deep, curW);
            int delta = spacePeerToPeer + curW - preMaxW;
            mContentViewBox.right += delta;
        }
    }

    @Override
    public void performLayout(final TreeViewContainer<T> treeViewContainer) {
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            mTreeModel.doTraversalNodes(new ITraversal<>() {
                @Override
                public void next(NodeModel<T> next) {
                    layoutNodes(next, treeViewContainer);
                }

                @Override
                public void finish() {
                    onManagerFinishLayoutAllNodes(treeViewContainer);
                }
            });
        }
    }


    @Override
    public ViewBox getTreeLayoutBox() {
        return fixedViewBox;
    }

    /**
     * 布局单个节点
     *
     * @param currentNode       当前节点
     * @param treeViewContainer 树形视图容器
     */
    private void layoutNodes(NodeModel<T> currentNode, TreeViewContainer<T> treeViewContainer) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        int deep = currentNode.deep;
        int floor = currentNode.floor;
        int leafCount = currentNode.leafCount;

        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }

        int currentWidth = currentNodeView.getMeasuredWidth();
        int currentHeight = currentNodeView.getMeasuredHeight();

        int verticalCenterFix = Math.abs(currentWidth - deepMax.get(deep)) / 2;

        int deltaWidth = 0;
        if (leafCount > 1) {
            deltaWidth = (deepStart.get(deep + leafCount) - deepStart.get(deep) - currentWidth) / 2 - verticalCenterFix;
            deltaWidth -= spacePeerToPeer / 2;
        }

        int top = extraDeltaY + (floor == 0 ? floorStart.get(0) : 0);
        if (currentNode.getParentNode() != null) {
            NodeModel<T> parentNode = currentNode.getParentNode();
            TreeViewHolder<?> parentHolder = treeViewContainer.getTreeViewHolder(parentNode);
            View parentNodeView = parentHolder == null ? null : parentHolder.getView();
            if (parentNodeView != null) {
                top += parentNodeView.getBottom() + floor * spaceParentToChild;
            } else {
                top += paddingBox.top;
            }
        }
        int left = deepStart.get(deep) + verticalCenterFix + deltaWidth + extraDeltaX;
        int bottom = top + currentHeight;
        int right = left + currentWidth;

        ViewBox finalLocation = new ViewBox(top, left, bottom, right);
        onManagerLayoutNode(currentNode, currentNodeView, finalLocation, treeViewContainer);

        // 控制隐藏被折叠的父节点下的子节点
        currentNodeView.setVisibility(NodeCheck.parentNodeContract(currentNode) ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onManagerLayoutNode(NodeModel<T> currentNode,
                                    View currentNodeView,
                                    ViewBox finalLocation,
                                    TreeViewContainer<T> treeViewContainer) {
        if (!layoutAnimatePrepare(currentNode, currentNodeView, finalLocation, treeViewContainer)) {
            currentNodeView.layout(finalLocation.left, finalLocation.top, finalLocation.right, finalLocation.bottom);
        }
    }

    @Override
    public void onManagerFinishLayoutAllNodes(TreeViewContainer<T> treeViewContainer) {
        layoutAnimate(treeViewContainer);
    }
}
