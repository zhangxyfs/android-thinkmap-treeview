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
 * 水平左右布局管理器，继承自BoxRightTreeLayoutManager，用于实现树形结构的左右水平布局
 * 将根节点的子节点分为两部分，一部分保持在右侧，另一部分镜像到左侧
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class BoxHorizonLeftAndRightLayoutManager<T extends NodeItem> extends BoxRightTreeLayoutManager<T> {
    private static final String TAG = BoxHorizonLeftAndRightLayoutManager.class.getSimpleName();
    private boolean isJustCalculate;

    /**
     * 构造函数
     * @param context 上下文
     * @param spaceParentToChild 父子节点间距
     * @param spacePeerToPeer 同级节点间距
     * @param baseline 基线
     */
    public BoxHorizonLeftAndRightLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        super(context, spaceParentToChild, spacePeerToPeer, baseline);
    }

    @Override
    public int getTreeLayoutType() {
        return LAYOUT_TYPE_HORIZON_LEFT_AND_RIGHT;
    }


    @Override
    public void onManagerFinishMeasureAllNodes(TreeViewContainer<T> treeViewContainer) {
        getPadding(treeViewContainer);
        mContentViewBox.bottom += (paddingBox.bottom + paddingBox.top);
        extraDeltaX = mContentViewBox.right;
        mContentViewBox.right += (paddingBox.left + paddingBox.right + extraDeltaX);
        fixedViewBox.setValues(mContentViewBox);
        if (winHeight == 0 || winWidth == 0) {
            return;
        }
        // 计算缩放比例以适应屏幕
        float scale = 1f * winWidth / winHeight;
        float wr = 1f * mContentViewBox.getWidth() / winWidth;
        float hr = 1f * mContentViewBox.getHeight() / winHeight;
        if (wr >= hr) {
            // 宽度比例更大时，按宽度计算高度
            float bh = mContentViewBox.getWidth() / scale;
            fixedViewBox.bottom = (int) bh;
        } else {
            // 高度比例更大时，按高度计算宽度
            float bw = mContentViewBox.getHeight() * scale;
            fixedViewBox.right = (int) bw;
        }
        mFixedDx = fixedViewBox.getWidth() / 2;
        mFixedDy = (fixedViewBox.getHeight() - mContentViewBox.getHeight()) / 2;
    }

    @Override
    public void performLayout(final TreeViewContainer<T> treeViewContainer) {
        isJustCalculate = true;
        super.performLayout(treeViewContainer);
        isJustCalculate = false;
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            NodeModel<T> rootNode = mTreeModel.getRootNode();
            TreeViewHolder<?> rootNodeHolder = treeViewContainer.getTreeViewHolder(rootNode);
            View rootNodeView = rootNodeHolder == null ? null : rootNodeHolder.getView();
            if (rootNodeView == null) {
                throw new NullPointerException(" rootNodeView can not be null");
            }
            int rootCx = rootNodeView.getLeft() + rootNodeView.getMeasuredWidth() / 2;
            int rootCy = rootNodeView.getTop() + rootNodeView.getMeasuredHeight() / 2;
            //divide equally by two
            LinkedList<? extends NodeModel<T>> rootNodeChildNodes = rootNode.getChildNodes();
            Point divideDy = getDivideDy(rootNode, treeViewContainer);
            int centerAy = divideDy.x;
            int centerBy = divideDy.y;
            int divider = rootNodeChildNodes.size() / 2;
            int count = 0;
            for (NodeModel<T> node : rootNodeChildNodes) {
                if (count < divider) {
                    //move to mid
                    node.traverseIncludeSelf(n -> moveDy(n, treeViewContainer, (rootCy - centerAy)));
                } else {
                    //move to other side
                    node.traverseIncludeSelf(n -> mirrorByCxDy(n, treeViewContainer, rootCx, (rootCy - centerBy)));
                }
                count++;
            }
            onManagerFinishLayoutAllNodes(treeViewContainer);
        }
    }

    /**
     * 计算分割点的Y坐标，用于确定左右两部分的垂直中心位置
     * @param rootNode 根节点
     * @param treeViewContainer 树形视图容器
     * @return 包含左右两部分垂直中心坐标的Point对象
     */
    private Point getDivideDy(NodeModel<T> rootNode, TreeViewContainer<T> treeViewContainer) {
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
                minA = Math.min(minA, top);
                maxA = Math.max(maxA, top + currentHeight);
            } else {
                minB = Math.min(minB, top);
                maxB = Math.max(maxB, top + currentHeight);
            }
            count++;
        }
        return new Point((maxA + minA) / 2, (maxB + minB) / 2);
    }

    /**
     * 移动节点的Y坐标位置
     * @param currentNode 当前节点
     * @param treeViewContainer 树形视图容器
     * @param deltaY Y轴偏移量
     */
    private void moveDy(NodeModel<T> currentNode, TreeViewContainer<T> treeViewContainer, int deltaY) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }
        currentHolder.setHolderLayoutType(LAYOUT_TYPE_HORIZON_RIGHT);
        int currentWidth = currentNodeView.getMeasuredWidth();
        int currentHeight = currentNodeView.getMeasuredHeight();
        int left = currentNodeView.getLeft();
        int right = left + currentWidth;
        int top = deltaY + currentNodeView.getTop();
        int bottom = top + currentHeight;
        ViewBox finalLocation = new ViewBox(top, left, bottom, right);
        onManagerLayoutNode(currentNode, currentNodeView, finalLocation, treeViewContainer);
    }

    /**
     * 以中心点为轴心镜像节点位置
     * @param currentNode 当前节点
     * @param treeViewContainer 树形视图容器
     * @param centerX X轴中心点
     * @param deltaY Y轴偏移量
     */
    private void mirrorByCxDy(NodeModel<T> currentNode, TreeViewContainer<T> treeViewContainer, int centerX, int deltaY) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }
        currentHolder.setHolderLayoutType(LAYOUT_TYPE_HORIZON_LEFT);
        int right = centerX * 2 - currentNodeView.getLeft();
        int left = centerX * 2 - currentNodeView.getRight();
        int top = deltaY + currentNodeView.getTop();
        int bottom = deltaY + currentNodeView.getBottom();
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
