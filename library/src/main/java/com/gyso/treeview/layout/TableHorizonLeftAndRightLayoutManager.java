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
 * 表格型水平左右布局管理器，继承自TableRightTreeLayoutManager
 * 该布局管理器将根节点的子节点分为两部分，一部分保持在右侧，另一部分镜像到左侧
 *
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class TableHorizonLeftAndRightLayoutManager<T extends NodeItem> extends TableRightTreeLayoutManager<T> {
    /**
     * 类的标签，用于调试日志
     */
    private static final String TAG = TableHorizonLeftAndRightLayoutManager.class.getSimpleName();

    /**
     * 标识是否仅进行计算的标志位
     * 在执行左右分布前先进行计算布局，此时该标志为true
     */
    private boolean isJustCalculate;

    /**
     * 构造函数，初始化表格型水平左右布局管理器
     *
     * @param context 上下文对象
     * @param spaceParentToChild 父子节点之间的间距
     * @param spacePeerToPeer 同级节点之间的间距
     * @param baseline 基线对齐方式
     */
    public TableHorizonLeftAndRightLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        super(context, spaceParentToChild, spacePeerToPeer, baseline);
    }

    /**
     * 所有节点测量完成后的处理
     * 计算布局框、缩放比例和位置偏移，为左右布局调整内容视图框
     *
     * @param treeViewContainer 树形视图容器
     */
    @Override
    public void onManagerFinishMeasureAllNodes(TreeViewContainer<T> treeViewContainer) {
        // 获取边距
        getPadding(treeViewContainer);
        // 更新内容视图框底部
        mContentViewBox.bottom += (paddingBox.bottom + paddingBox.top);
        // 计算额外的X偏移量（为左侧布局预留空间）
        extraDeltaX = mContentViewBox.right;
        // 更新内容视图框右侧，包含左右两部分的空间
        mContentViewBox.right += (paddingBox.left + paddingBox.right + extraDeltaX);
        // 设置固定视图框
        fixedViewBox.setValues(mContentViewBox);

        if (winHeight == 0 || winWidth == 0) {
            return;
        }

        // 计算缩放比例
        float scale = 1f * winWidth / winHeight;
        float wr = 1f * mContentViewBox.getWidth() / winWidth;
        float hr = 1f * mContentViewBox.getHeight() / winHeight;

        // 根据宽高比调整固定视图框
        if (wr >= hr) {
            float bh = mContentViewBox.getWidth() / scale;
            fixedViewBox.bottom = (int) bh;
        } else {
            float bw = mContentViewBox.getHeight() * scale;
            fixedViewBox.right = (int) bw;
        }

        // 计算固定偏移量
        mFixedDx = (fixedViewBox.getWidth() - mContentViewBox.getWidth()) / 2;
        mFixedDy = (fixedViewBox.getHeight() - mContentViewBox.getHeight()) / 2;

        // 计算楼层起始位置（注意这里调整了参数顺序，floor对应x方向）
        for (int i = 0; i <= floorMax.size(); i++) {
            int fn = (i == floorMax.size()) ? floorMax.size() : floorMax.keyAt(i);
            int preStart = floorStart.get(fn - 1, 0);
            int preMax = floorMax.get(fn - 1, 0);
            // 计算起始位置，根节点使用固定偏移量，其他节点使用父子间距
            int startPos = (fn == 0 ? (mFixedDx + paddingBox.left) : spaceParentToChild) + preStart + preMax;
            floorStart.put(fn, startPos);
        }

        // 计算深度起始位置（注意这里调整了参数顺序，deep对应y方向）
        for (int i = 0; i <= deepMax.size(); i++) {
            int dn = (i == deepMax.size()) ? deepMax.size() : deepMax.keyAt(i);
            int preStart = deepStart.get(dn - 1, 0);
            int preMax = deepMax.get(dn - 1, 0);
            // 计算起始位置，根节点使用固定偏移量，其他节点使用同级间距
            int startPos = (dn == 0 ? (mFixedDy + paddingBox.top) : spacePeerToPeer) + preStart + preMax;
            deepStart.put(dn, startPos);
        }
    }

    /**
     * 获取树形布局类型
     *
     * @return 返回水平左右布局类型常量
     */
    @Override
    public int getTreeLayoutType() {
        return LAYOUT_TYPE_HORIZON_LEFT_AND_RIGHT;
    }

    /**
     * 执行布局操作
     * 先执行父类的右侧布局计算，然后将一半子节点镜像到左侧
     *
     * @param treeViewContainer 树形视图容器
     */
    @Override
    public void performLayout(final TreeViewContainer<T> treeViewContainer) {
        // 设置标志为仅计算，避免在计算阶段执行动画
        isJustCalculate = true;
        // 执行父类的布局计算（右侧布局）
        super.performLayout(treeViewContainer);
        // 恢复标志，准备进行左右分布
        isJustCalculate = false;

        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            NodeModel<T> rootNode = mTreeModel.getRootNode();
            TreeViewHolder<?> rootNodeHolder = treeViewContainer.getTreeViewHolder(rootNode);
            View rootNodeView = rootNodeHolder == null ? null : rootNodeHolder.getView();
            if (rootNodeView == null) {
                throw new NullPointerException(" rootNodeView can not be null");
            }
            // 计算根节点中心坐标
            int rootCx = rootNodeView.getLeft() + rootNodeView.getMeasuredWidth() / 2;
            int rootCy = rootNodeView.getTop() + rootNodeView.getMeasuredHeight() / 2;

            // 将根节点的子节点平分为两部分
            LinkedList<? extends NodeModel<T>> rootNodeChildNodes = rootNode.getChildNodes();
            Point divideDy = getDivideDy(rootNode, treeViewContainer);
            int centerAy = divideDy.x; // 第一部分的中心Y坐标
            int centerBy = divideDy.y; // 第二部分的中心Y坐标
            int divider = rootNodeChildNodes.size() / 2;
            int count = 0;

            // 遍历子节点，前一半保持在右侧，后一半镜像到左侧
            for (NodeModel<T> node : rootNodeChildNodes) {
                if (count < divider) {
                    // 前一半节点移动到中心位置
                    node.traverseIncludeSelf(n -> moveDy(n, treeViewContainer, (rootCy - centerAy)));
                } else {
                    // 后一半节点镜像到左侧
                    node.traverseIncludeSelf(n -> mirrorByCxDy(n, treeViewContainer, rootCx, (rootCy - centerBy)));
                }
                count++;
            }
            onManagerFinishLayoutAllNodes(treeViewContainer);
        }
    }

    /**
     * 计算分割点的Y坐标，用于确定左右两部分的垂直中心位置
     *
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

        // 遍历子节点，计算前一半和后一半的边界
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
                // 前一半节点的边界
                minA = Math.min(minA, top);
                maxA = Math.max(maxA, top + currentHeight);
            } else {
                // 后一半节点的边界
                minB = Math.min(minB, top);
                maxB = Math.max(maxB, top + currentHeight);
            }
            count++;
        }
        // 返回两部分的中心Y坐标
        return new Point((maxA + minA) / 2, (maxB + minB) / 2);
    }

    /**
     * 移动节点的Y坐标位置
     *
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
        // 设置布局类型为右侧布局
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
     * 以中心点为轴心镜像节点到左侧
     *
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
        // 设置布局类型为左侧布局
        currentHolder.setHolderLayoutType(LAYOUT_TYPE_HORIZON_LEFT);
        int currentWidth = currentNodeView.getMeasuredWidth();
        int currentHeight = currentNodeView.getMeasuredHeight();
        // 计算镜像后的左侧位置，考虑父子间距和宽度的一半
        int left = centerX * 2 - currentNodeView.getLeft() - spaceParentToChild - currentWidth / 2;
        int right = left + currentWidth;
        int top = deltaY + currentNodeView.getTop();
        int bottom = top + currentHeight;
        ViewBox finalLocation = new ViewBox(top, left, bottom, right);
        onManagerLayoutNode(currentNode, currentNodeView, finalLocation, treeViewContainer);
    }

    /**
     * 布局单个节点
     * 根据isJustCalculate标志决定是否执行动画准备
     *
     * @param currentNode 当前节点模型
     * @param currentNodeView 当前节点视图
     * @param finalLocation 最终位置信息
     * @param treeViewContainer 树形视图容器
     */
    @Override
    public void onManagerLayoutNode(NodeModel<T> currentNode, View currentNodeView, ViewBox finalLocation, TreeViewContainer<T> treeViewContainer) {
        // 如果仅进行计算，则直接执行布局，不执行动画
        if (isJustCalculate) {
            currentNodeView.layout(finalLocation.left, finalLocation.top, finalLocation.right, finalLocation.bottom);
            return;
        }
        // 否则执行动画准备，如果动画准备失败则直接布局
        if (!layoutAnimatePrepare(currentNode, currentNodeView, finalLocation, treeViewContainer)) {
            currentNodeView.layout(finalLocation.left, finalLocation.top, finalLocation.right, finalLocation.bottom);
        }
    }

    /**
     * 完成所有节点布局后的处理
     * 只有在非计算模式下才调用父类的完成处理方法
     *
     * @param treeViewContainer 树形视图容器
     */
    @Override
    public void onManagerFinishLayoutAllNodes(TreeViewContainer<T> treeViewContainer) {
        // 只有在非计算模式下才执行父类的完成处理
        if (!isJustCalculate) {
            super.onManagerFinishLayoutAllNodes(treeViewContainer);
        }
    }
}
