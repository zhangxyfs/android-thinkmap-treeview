package com.gyso.treeview.layout;

import android.content.Context;
import android.graphics.PointF;
import android.view.View;

import com.gyso.treeview.TreeViewContainer;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.algorithm.ring.RingForSimple;
import com.gyso.treeview.line.BaseLine;
import com.gyso.treeview.model.ITraversal;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.util.NodeCheck;
import com.gyso.treeview.util.TreeViewLog;
import com.gyso.treeview.util.ViewBox;

import java.util.Map;
/**
 * 简单环形树形布局管理器，继承自TreeLayoutManager
 * 用于实现树形结构的简单环形布局，节点围绕中心点呈环形排列
 *
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class SimpleRingTreeLayoutManager<T extends NodeItem> extends TreeLayoutManager<T> {
    /**
     * 类的标签，用于调试日志
     * 注意：这里引用的是CompactRingTreeLayoutManager，可能是复制时的错误
     */
    private static final String TAG = CompactRingTreeLayoutManager.class.getSimpleName();

    /**
     * 环形布局计算器，用于计算节点在环形中的位置
     */
    private RingForSimple<T> ring = null;

    /**
     * 存储节点模型与其在环形布局中对应坐标的映射关系
     */
    private Map<NodeModel<T>, PointF> ringPositions = null;

    /**
     * 构造函数，初始化简单环形树形布局管理器
     *
     * @param context 上下文对象
     * @param spaceParentToChild 父子节点间距
     * @param spacePeerToPeer 同级节点间距
     * @param baseline 基线对齐方式
     */
    public SimpleRingTreeLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        super(context, spaceParentToChild, spacePeerToPeer, baseline);
    }

    /**
     * 获取树形布局类型
     *
     * @return 返回环形布局类型常量
     */
    @Override
    public int getTreeLayoutType() {
        return LAYOUT_TYPE_RING;
    }

    /**
     * 释放资源
     */
    @Override
    public void release() {
        // 当前实现不需要特殊释放操作
    }

    /**
     * 使用布局算法计算树模型
     * 使用RingForSimple算法进行简单环形布局重构
     *
     * @param mTreeModel 树模型
     */
    @Override
    public void calculateByLayoutAlgorithm(TreeModel<T> mTreeModel) {
        RingForSimple.getInstance(mTreeModel).reconstruction(mTreeModel);
    }

    /**
     * 执行测量并监听测量过程
     * 计算节点尺寸信息并生成环形布局位置
     *
     * @param treeViewContainer 树视图容器
     * @param measureListener 测量监听器
     */
    public void performMeasureAndListen(TreeViewContainer<T> treeViewContainer, MeasureListener<T> measureListener) {
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            // 清空楼层起始位置映射
            floorStart.clear();
            // 清空内容视图框
            mContentViewBox.clear();
            // 清空楼层最大值映射
            floorMax.clear();
            // 清空深度最大值映射
            deepMax.clear();

            // 创建遍历器，用于遍历所有节点进行测量
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
                    // 添加边距
                    getPadding(treeViewContainer);
                    mContentViewBox.bottom += (paddingBox.bottom + paddingBox.top);
                    mContentViewBox.right += (paddingBox.left + paddingBox.right);
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

                    // 计算根节点中心坐标
                    int rootCenterX = mFixedDx + fixedViewBox.getWidth() / 2;
                    int rootCenterY = mFixedDx + fixedViewBox.getHeight() / 2;

                    // 初始化环形布局计算器并设置中心点和楼层起始位置
                    ring = RingForSimple.getInstance(mTreeModel).setCenter(rootCenterX, rootCenterY).setFloorStart(floorStart);
                    // 生成环形布局中的节点位置
                    ringPositions = ring.genPositions();

                    if (measureListener != null) {
                        measureListener.onMeasureFinished();
                    }
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
     * 测量节点尺寸
     * 计算节点在环形布局中的半径需求
     *
     * @param node 节点
     * @param treeViewContainer 树视图容器
     */
    private void measure(NodeModel<T> node, TreeViewContainer<T> treeViewContainer) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(node);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();
        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }

        int curH = currentNodeView.getMeasuredHeight();
        int curW = currentNodeView.getMeasuredWidth();
        // 计算节点的对角线长度作为半径需求，并加上同级间距的一半
        int maxR = (int) Math.hypot(curH, curW) + (spacePeerToPeer / 2);
        int preMaxR = floorMax.get(node.floor);
        if (preMaxR < maxR) {
            floorMax.put(node.floor, maxR);
        }

        int maxChildFl = floorMax.get(node.floor);
        NodeModel<T> parentNode = node.getParentNode();
        int contentMax = 0;
        if (parentNode != null) {
            // 计算父节点的半径需求
            int maxParentFl = floorMax.get(parentNode.floor);
            int parentStart = floorStart.get(parentNode.floor);
            // 计算当前节点的起始半径位置
            int start = parentStart + maxParentFl / 2 + spaceParentToChild + maxChildFl / 2;
            floorStart.put(node.floor, start);
            contentMax = start + maxChildFl;
        } else {
            // 只有根节点会执行此逻辑
            floorStart.put(node.floor, 0);
        }

        TreeViewLog.e(TAG, floorStart + "");
        // 更新内容视图框大小
        mContentViewBox.bottom = mContentViewBox.right = Math.max(mContentViewBox.right, 2 * contentMax);
        TreeViewLog.e(TAG, "measure--" + mContentViewBox);
    }

    /**
     * 执行布局
     * 遍历所有节点并根据环形位置进行布局
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
                    TreeViewLog.e(TAG, "performLayout next: " + next);
                    layoutNodes(next, treeViewContainer);
                }

                @Override
                public void finish() {
                    // 执行布局动画
                    layoutAnimate(treeViewContainer);
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
     * 根据环形布局计算的位置设置节点坐标
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

        // 默认使用楼层和深度起始位置
        int top = floorStart.get(floor);
        int left = deepStart.get(deep);

        // 如果环形位置映射存在，使用计算的环形位置
        if (ringPositions != null) {
            PointF position = ringPositions.get(currentNode);
            if (position != null) {
                top = (int) position.x;
                left = (int) position.y;
            } else {
                TreeViewLog.e(TAG, "layout " + currentNode + " error!! Position is null");
            }
        } else {
            TreeViewLog.e(TAG, "layout " + currentNode + " error!! PingPositions is null!!!");
            return;
        }

        int bottom = top + currentHeight;
        int right = left + currentWidth;

        TreeViewLog.e(TAG, "top[" + top + "]left[" + left + "]bottom[" + bottom + "]right[" + right + "]");

        ViewBox finalLocation = new ViewBox(top, left, bottom, right);

        if (!layoutAnimatePrepare(currentNode, currentNodeView, finalLocation, treeViewContainer)) {
            // 如果不需要动画，直接进行布局
            currentNodeView.layout(left, top, right, bottom);
        }

        // 控制隐藏被折叠的父节点下的子节点
        currentNodeView.setVisibility(NodeCheck.parentNodeContract(currentNode) ? View.GONE : View.VISIBLE);
    }
}
