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
import com.gyso.treeview.util.ViewBox;

/**
 * 左向树形布局管理器，继承自右向树形布局管理器，实现镜像布局效果
 * 该布局管理器将树形结构按照从左到右的水平布局方式进行排列
 *
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class BoxLeftTreeLayoutManager<T extends NodeItem> extends BoxRightTreeLayoutManager<T> {
    private static final String TAG = BoxLeftTreeLayoutManager.class.getSimpleName();
    private boolean isJustCalculate;

    /**
     * 构造函数，初始化左向树形布局管理器
     *
     * @param context 上下文对象
     * @param spaceParentToChild 父子节点之间的间距
     * @param spacePeerToPeer 同级节点之间的间距
     * @param baseline 基线对齐方式
     */
    public BoxLeftTreeLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        super(context, spaceParentToChild, spacePeerToPeer, baseline);
    }

    /**
     * 获取树形布局类型
     *
     * @return 返回水平向左布局类型常量
     */
    @Override
    public int getTreeLayoutType() {
        return LAYOUT_TYPE_HORIZON_LEFT;
    }

    /**
     * 执行布局操作，先进行计算布局，然后对所有节点进行镜像翻转
     *
     * @param treeViewContainer 树形视图容器
     */
    @Override
    public void performLayout(final TreeViewContainer<T> treeViewContainer) {
        isJustCalculate = true;
        super.performLayout(treeViewContainer);
        isJustCalculate = false;
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            final int cx = fixedViewBox.getWidth() / 2;
            // 遍历所有节点并进行镜像翻转
            mTreeModel.doTraversalNodes(new ITraversal<>() {
                @Override
                public void next(NodeModel<T> next) {
                    mirrorByCx(next, treeViewContainer, cx);
                }

                @Override
                public void finish() {
                    onManagerFinishLayoutAllNodes(treeViewContainer);
                }
            });
        }
    }

    /**
     * 以中心线为轴对节点进行镜像翻转
     *
     * @param currentNode 当前节点模型
     * @param treeViewContainer 树形视图容器
     * @param centerX 中心线X坐标
     */
    private void mirrorByCx(NodeModel<T> currentNode, TreeViewContainer<T> treeViewContainer, int centerX) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }
        // 计算镜像后的左右边界位置
        int left = centerX * 2 - currentNodeView.getRight();
        int right = centerX * 2 - currentNodeView.getLeft();
        int top = currentNodeView.getTop();
        int bottom = currentNodeView.getBottom();
        ViewBox finalLocation = new ViewBox(top, left, bottom, right);
        onManagerLayoutNode(currentNode, currentNodeView, finalLocation, treeViewContainer);
    }

    /**
     * 布局单个节点，根据是否仅计算标志决定是否执行动画
     *
     * @param currentNode 当前节点模型
     * @param currentNodeView 当前节点视图
     * @param finalLocation 最终位置信息
     * @param treeViewContainer 树形视图容器
     */
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

    /**
     * 完成所有节点布局后的处理，仅在非计算模式下调用父类方法
     *
     * @param treeViewContainer 树形视图容器
     */
    @Override
    public void onManagerFinishLayoutAllNodes(TreeViewContainer<T> treeViewContainer) {
        if (!isJustCalculate) {
            super.onManagerFinishLayoutAllNodes(treeViewContainer);
        }
    }
}
