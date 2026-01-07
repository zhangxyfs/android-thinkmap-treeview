package com.gyso.treeview.layout;

import android.content.Context;
import android.graphics.Point;
import android.view.View;

import com.gyso.treeview.TreeViewContainer;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.line.BaseLine;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.util.ViewBox;

import java.util.LinkedList;

/**
 * 表格垂直上下布局管理器，继承自TableDownTreeLayoutManager
 * 实现垂直向下和向上的混合布局方式
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class TableVerticalUpAndTableDownLayoutManager<T extends NodeItem> extends TableDownTreeLayoutManager<T> {
    private boolean isJustCalculate;

    /**
     * 构造函数
     * @param context 上下文
     * @param spaceParentToChild 父子节点间距
     * @param spacePeerToPeer 同级节点间距
     * @param baseline 基线
     */
    public TableVerticalUpAndTableDownLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        super(context, spaceParentToChild, spacePeerToPeer, baseline);
    }

    @Override
    public int getTreeLayoutType() {
        return LAYOUT_TYPE_VERTICAL_DOWN_AND_UP;
    }

    @Override
    public void onManagerFinishMeasureAllNodes(TreeViewContainer<T> treeViewContainer) {
        getPadding(treeViewContainer);
        extraDeltaY = mContentViewBox.bottom;
        mContentViewBox.bottom += (paddingBox.bottom + paddingBox.top) + extraDeltaY;
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


    @Override
    public void performLayout(final TreeViewContainer<T> treeViewContainer) {
        isJustCalculate = true;
        super.performLayout(treeViewContainer);
        isJustCalculate = false;
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            NodeModel<T> rootNode = mTreeModel.getRootNode();
            TreeViewHolder<T> rootNodeHolder = treeViewContainer.getTreeViewHolder(rootNode);
            View rootNodeView = rootNodeHolder == null ? null : rootNodeHolder.getView();
            if (rootNodeView == null) {
                throw new NullPointerException(" rootNodeView can not be null");
            }
            int rootCx = rootNodeView.getLeft() + rootNodeView.getMeasuredWidth() / 2;
            int rootCy = rootNodeView.getTop() + rootNodeView.getMeasuredHeight() / 2;
            //divide equally by two
            LinkedList<? extends NodeModel<T>> rootNodeChildNodes = rootNode.getChildNodes();
            Point divideDx = getDivideDx(rootNode, treeViewContainer);
            int centerAx = divideDx.x;
            int centerBx = divideDx.y;
            int divider = rootNodeChildNodes.size() / 2;
            int count = 0;
            for (NodeModel<T> node : rootNodeChildNodes) {
                if (count < divider) {
                    //move to mid
                    node.traverseIncludeSelf(n -> moveDx(n, treeViewContainer, (rootCx - centerAx)));
                } else {
                    //move to other side
                    node.traverseIncludeSelf(n -> mirrorByCxDy(n, treeViewContainer, rootCy, (rootCx - centerBx)));
                }
                count++;
            }
            onManagerFinishLayoutAllNodes(treeViewContainer);
        }
    }

    /**
     * 获取分割点的X坐标
     * @param rootNode 根节点
     * @param treeViewContainer 树视图容器
     * @return 分割点的X坐标
     */
    private Point getDivideDx(NodeModel<T> rootNode, TreeViewContainer<T> treeViewContainer) {
        LinkedList<? extends NodeModel<T>> rootNodeChildNodes = rootNode.getChildNodes();
        int divider = rootNodeChildNodes.size() / 2;
        int count = 0;
        int minA, maxA, minB, maxB;
        minA = minB = Integer.MAX_VALUE;
        maxA = maxB = Integer.MIN_VALUE;
        for (NodeModel<T> currentNode : rootNodeChildNodes) {
            TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
            View currentNodeView = currentHolder == null ? null : currentHolder.getView();
            if (currentNodeView == null) {
                throw new NullPointerException(" currentNodeView can not be null");
            }
            int left = currentNodeView.getLeft();
            int top = currentNodeView.getTop();
            int currentHeight = currentNodeView.getMeasuredHeight();
            int currentWidth = currentNodeView.getMeasuredWidth();
            if (count < divider) {
                minA = Math.min(minA, left);
                maxA = Math.max(maxA, left + currentWidth);
            } else {
                minB = Math.min(minB, left);
                maxB = Math.max(maxB, left + currentWidth);
            }
            count++;
        }
        return new Point((maxA + minA) / 2, (maxB + minB) / 2);
    }

    /**
     * 移动节点的X坐标
     * @param currentNode 当前节点
     * @param treeViewContainer 树视图容器
     * @param deltaX X轴偏移量
     */
    private void moveDx(NodeModel<T> currentNode, TreeViewContainer<T> treeViewContainer, int deltaX) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }
        currentHolder.setHolderLayoutType(LAYOUT_TYPE_VERTICAL_DOWN);
        int currentWidth = currentNodeView.getMeasuredWidth();
        int currentHeight = currentNodeView.getMeasuredHeight();
        int left = deltaX + currentNodeView.getLeft();
        int right = left + currentWidth;
        int top = currentNodeView.getTop();
        int bottom = top + currentHeight;
        ViewBox finalLocation = new ViewBox(top, left, bottom, right);
        onManagerLayoutNode(currentNode, currentNodeView, finalLocation, treeViewContainer);
    }

    /**
     * 以中心点镜像翻转节点
     * @param currentNode 当前节点
     * @param treeViewContainer 树视图容器
     * @param centerY Y轴中心点
     * @param deltaX X轴偏移量
     */
    private void mirrorByCxDy(NodeModel<T> currentNode, TreeViewContainer<T> treeViewContainer, int centerY, int deltaX) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }
        currentHolder.setHolderLayoutType(LAYOUT_TYPE_VERTICAL_UP);
        int currentWidth = currentNodeView.getMeasuredWidth();
        int currentHeight = currentNodeView.getMeasuredHeight();
        int left = deltaX + currentNodeView.getLeft();
        int right = left + currentWidth;
        int top = centerY * 2 - currentNodeView.getTop() - spaceParentToChild - currentHeight / 2;
        int bottom = top + currentHeight;
        ViewBox finalLocation = new ViewBox(top, left, bottom, right);
        onManagerLayoutNode(currentNode, currentNodeView, finalLocation, treeViewContainer);
    }

    @Override
    public void onManagerLayoutNode(NodeModel<T> currentNode, View currentNodeView, ViewBox finalLocation, TreeViewContainer<T> treeViewContainer) {
        if (isJustCalculate) {
            currentNodeView.layout(finalLocation.left, finalLocation.top, finalLocation.right, finalLocation.bottom);
            return;
        }
        if (!layoutAnimatePrepare(currentNode, currentNodeView, finalLocation, treeViewContainer)) {
            currentNodeView.layout(finalLocation.left, finalLocation.top, finalLocation.right, finalLocation.bottom);
        }
    }

    @Override
    public void onManagerFinishLayoutAllNodes(TreeViewContainer<T> treeViewContainer) {
        if (!isJustCalculate) {
            super.onManagerFinishLayoutAllNodes(treeViewContainer);
        }
    }
}
