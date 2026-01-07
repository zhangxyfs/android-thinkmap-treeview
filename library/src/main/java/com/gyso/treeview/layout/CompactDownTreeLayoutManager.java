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
import com.gyso.treeview.util.TreeViewLog;
import com.gyso.treeview.util.ViewBox;
/**
 * 紧凑型垂直向下布局管理器，继承自TreeLayoutManager
 * 用于实现树形结构的紧凑垂直向下布局
 *
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class CompactDownTreeLayoutManager<T extends NodeItem> extends TreeLayoutManager<T> {
    /**
     * 类的标签，用于调试日志
     */
    private static final String TAG = CompactDownTreeLayoutManager.class.getSimpleName();

    /**
     * 测量监听器，用于监听测量过程
     */
    protected MeasureListener measureListener = null;

    /**
     * 构造函数
     *
     * @param context 上下文
     * @param spaceParentToChild 父子节点间距
     * @param spacePeerToPeer 同级节点间距
     * @param baseline 基线
     */
    public CompactDownTreeLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        super(context, spaceParentToChild, spacePeerToPeer, baseline);
    }

    /**
     * 使用布局算法计算树模型
     * 使用Table.COMPACT_TABLE算法进行紧凑布局重构
     *
     * @param mTreeModel 树模型
     */
    @Override
    public void calculateByLayoutAlgorithm(TreeModel<T> mTreeModel) {
        new Table<T>().reconstruction(mTreeModel, Table.COMPACT_TABLE);
    }

    /**
     * 获取树形布局类型
     *
     * @return 垂直向下布局类型常量
     */
    @Override
    public int getTreeLayoutType() {
        return LAYOUT_TYPE_VERTICAL_DOWN;
    }

    /**
     * 释放资源
     */
    @Override
    public void release() {
        // 当前实现不需要特殊释放操作
    }

    /**
     * 执行测量并监听测量过程
     *
     * @param treeViewContainer 树视图容器
     * @param measureListener 测量监听器
     */
    public void performMeasureAndListen(TreeViewContainer<T> treeViewContainer, MeasureListener<T> measureListener) {
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            // 清空内容视图框
            mContentViewBox.clear();
            // 清空楼层最大值映射
            floorMax.clear();
            // 清空深度最大值映射
            deepMax.clear();
            // 设置测量监听器
            this.measureListener = measureListener;

            // 创建遍历器，用于遍历所有节点进行测量
            ITraversal<NodeModel<T>> traversal = new ITraversal<>() {
                @Override
                public void next(NodeModel<T> next) {
                    // 测量当前节点
                    measure(next, treeViewContainer);
                    // 如果有监听器，通知子节点测量完成
                    if (measureListener != null) {
                        measureListener.onMeasureChild(next);
                    }
                }

                @Override
                public void finish() {
                    // 所有节点测量完成后的处理
                    onManagerFinishMeasureAllNodes(treeViewContainer);
                }
            };
            // 执行节点遍历
            mTreeModel.doTraversalNodes(traversal);
        }
    }

    /**
     * 执行测量（无监听器版本）
     *
     * @param treeViewContainer 树视图容器
     */
    @Override
    public void performMeasure(TreeViewContainer<T> treeViewContainer) {
        performMeasureAndListen(treeViewContainer, null);
    }

    /**
     * 所有节点测量完成后的处理
     * 计算布局框、缩放比例和位置偏移
     *
     * @param treeViewContainer 树视图容器
     */
    @Override
    public void onManagerFinishMeasureAllNodes(TreeViewContainer<T> treeViewContainer) {
        // 获取边距
        getPadding(treeViewContainer);
        // 更新内容视图框底部
        mContentViewBox.bottom += (paddingBox.bottom + paddingBox.top);
        // 更新内容视图框右侧
        mContentViewBox.right += (paddingBox.left + paddingBox.right);
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

        // 计算楼层起始位置
        for (int i = 0; i <= floorMax.size(); i++) {
            int fn = (i == floorMax.size()) ? floorMax.size() : floorMax.keyAt(i);
            int preStart = floorStart.get(fn - 1, 0);
            int preMax = floorMax.get(fn - 1, 0);
            // 计算起始位置，根节点使用固定偏移量，其他节点使用父子间距
            int startPos = (fn == 0 ? (mFixedDy + paddingBox.top) : spaceParentToChild) + preStart + preMax;
            floorStart.put(fn, startPos);
        }

        // 计算深度起始位置
        for (int i = 0; i <= deepMax.size(); i++) {
            int dn = (i == deepMax.size()) ? deepMax.size() : deepMax.keyAt(i);
            int preStart = deepStart.get(dn - 1, 0);
            int preMax = deepMax.get(dn - 1, 0);
            // 计算起始位置，根节点使用固定偏移量，其他节点使用同级间距
            int startPos = (dn == 0 ? (mFixedDx + paddingBox.left) : spacePeerToPeer) + preStart + preMax;
            deepStart.put(dn, startPos);
        }

        // 通知测量完成
        if (measureListener != null) {
            measureListener.onMeasureFinished();
        }
    }

    /**
     * 测量节点尺寸
     * 更新楼层和深度的最大值
     *
     * @param node 节点
     * @param treeViewContainer 树视图容器
     */
    private void measure(NodeModel<T> node, TreeViewContainer<T> treeViewContainer) {
        // 输出调试日志
        TreeViewLog.e(TAG, node + "gyso-----");
        // 获取节点的ViewHolder
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(node);
        // 获取节点视图
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }

        // 处理高度
        int preMaxH = floorMax.get(node.floor);
        int curH = currentNodeView.getMeasuredHeight();
        if (preMaxH < curH) {
            floorMax.put(node.floor, curH);
            int delta = spaceParentToChild + curH - preMaxH;
            mContentViewBox.bottom += delta;
        }

        // 处理宽度
        int preMaxW = deepMax.get(node.deep);
        int curW = currentNodeView.getMeasuredWidth();
        if (preMaxW < curW) {
            deepMax.put(node.deep, curW);
            int delta = spacePeerToPeer + curW - preMaxW;
            mContentViewBox.right += delta;
        }
    }

    /**
     * 执行布局
     * 遍历所有节点并进行布局
     *
     * @param treeViewContainer 树视图容器
     */
    @Override
    public void performLayout(final TreeViewContainer<T> treeViewContainer) {
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            // 遍历所有节点进行布局
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

    /**
     * 获取树形布局框
     *
     * @return 固定视图框
     */
    @Override
    public ViewBox getTreeLayoutBox() {
        return fixedViewBox;
    }

    /**
     * 布局单个节点
     * 根据节点的楼层和深度计算位置
     *
     * @param currentNode 当前节点
     * @param treeViewContainer 树视图容器
     */
    private void layoutNodes(NodeModel<T> currentNode, TreeViewContainer<T> treeViewContainer) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        int deep = currentNode.deep;
        int floor = currentNode.floor;

        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }

        int currentWidth = currentNodeView.getMeasuredWidth();
        int currentHeight = currentNodeView.getMeasuredHeight();

        // 计算垂直居中调整值
        int verticalCenterFix = Math.abs(currentWidth - deepMax.get(deep)) / 2;

        // 计算节点位置
        int top = floorStart.get(floor) + extraDeltaY;
        int left = deepStart.get(deep) + verticalCenterFix + extraDeltaX;
        int bottom = top + currentHeight;
        int right = left + currentWidth;

        ViewBox finalLocation = new ViewBox(top, left, bottom, right);
        onManagerLayoutNode(currentNode, currentNodeView, finalLocation, treeViewContainer);

        // 控制隐藏被折叠的父节点下的子节点
        currentNodeView.setVisibility(NodeCheck.parentNodeContract(currentNode) ? View.GONE : View.VISIBLE);
    }

    /**
     * 布局单个节点（管理器接口实现）
     *
     * @param currentNode 当前节点
     * @param currentNodeView 当前节点视图
     * @param finalLocation 最终位置
     * @param treeViewContainer 树视图容器
     */
    @Override
    public void onManagerLayoutNode(NodeModel<T> currentNode,
                                    View currentNodeView,
                                    ViewBox finalLocation,
                                    TreeViewContainer<T> treeViewContainer) {
        if (!layoutAnimatePrepare(currentNode, currentNodeView, finalLocation, treeViewContainer)) {
            // 如果不需要动画，直接进行布局
            currentNodeView.layout(finalLocation.left, finalLocation.top, finalLocation.right, finalLocation.bottom);
        }
    }

    /**
     * 所有节点布局完成后的处理
     * 执行布局动画
     *
     * @param treeViewContainer 树视图容器
     */
    @Override
    public void onManagerFinishLayoutAllNodes(TreeViewContainer<T> treeViewContainer) {
        layoutAnimate(treeViewContainer);
    }
}
