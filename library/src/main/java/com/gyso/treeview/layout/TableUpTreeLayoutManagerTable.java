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
 * 表格型向上树形布局管理器，继承自TableDownTreeLayoutManager
 * 该布局管理器将树形结构按照从下到上的垂直方向进行布局
 * 实现方式是先按向下布局计算位置，然后以垂直中心线为轴进行镜像翻转
 *
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class TableUpTreeLayoutManagerTable<T extends NodeItem> extends TableDownTreeLayoutManager<T> {
    /**
     * 类的标签，用于调试日志
     */
    private static final String TAG = TableUpTreeLayoutManagerTable.class.getSimpleName();

    /**
     * 标识是否仅进行计算的标志位
     * 在执行镜像翻转前先进行计算布局，此时该标志为true
     */
    private boolean isJustCalculate;

    /**
     * 构造函数，初始化表格型向上树形布局管理器
     *
     * @param context 上下文对象
     * @param spaceParentToChild 父子节点之间的间距
     * @param spacePeerToPeer 同级节点之间的间距
     * @param baseline 基线对齐方式
     */
    public TableUpTreeLayoutManagerTable(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        super(context, spaceParentToChild, spacePeerToPeer, baseline);
    }

    /**
     * 获取树形布局类型
     *
     * @return 返回垂直向上布局类型常量
     */
    @Override
    public int getTreeLayoutType() {
        return LAYOUT_TYPE_VERTICAL_UP;
    }

    /**
     * 执行布局操作
     * 先执行父类的向下布局计算，然后对所有节点进行垂直镜像翻转
     *
     * @param treeViewContainer 树形视图容器
     */
    @Override
    public void performLayout(final TreeViewContainer<T> treeViewContainer) {
        // 设置标志为仅计算，避免在计算阶段执行动画
        isJustCalculate = true;
        // 执行父类的布局计算（向下布局）
        super.performLayout(treeViewContainer);
        // 恢复标志，准备进行镜像翻转
        isJustCalculate = false;

        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            // 计算垂直中心线位置
            final int cy = fixedViewBox.getHeight() / 2;
            // 遍历所有节点，对每个节点执行垂直镜像翻转
            mTreeModel.doTraversalNodes(new ITraversal<>() {
                @Override
                public void next(NodeModel<T> next) {
                    // 对当前节点执行垂直镜像翻转
                    mirrorByCy(next, treeViewContainer, cy);
                }

                @Override
                public void finish() {
                    // 完成所有节点布局后的处理
                    onManagerFinishLayoutAllNodes(treeViewContainer);
                }
            });
        }
    }

    /**
     * 以垂直中心线为轴对节点进行镜像翻转
     * 通过计算新位置实现垂直方向的翻转效果
     *
     * @param currentNode 当前节点模型
     * @param treeViewContainer 树形视图容器
     * @param centerY 垂直中心线Y坐标
     */
    private void mirrorByCy(NodeModel<T> currentNode, TreeViewContainer<T> treeViewContainer, int centerY) {
        // 获取当前节点的ViewHolder
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
        // 获取当前节点的视图
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }
        // 保持左右位置不变
        int left = currentNodeView.getLeft();
        int right = currentNodeView.getRight();
        // 计算镜像后的上下位置（垂直翻转）
        int bottom = centerY * 2 - currentNodeView.getTop();
        int top = centerY * 2 - currentNodeView.getBottom();
        // 创建新的位置信息
        ViewBox finalLocation = new ViewBox(top, left, bottom, right);
        // 执行节点布局
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
