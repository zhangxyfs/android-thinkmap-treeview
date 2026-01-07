/**
 * @Author: 怪兽N
 * @Time: 2021/6/7  17:56
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 * @Describe: drag block
 */
package com.gyso.treeview.touch;

import android.graphics.PointF;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

import com.gyso.treeview.R;
import com.gyso.treeview.TreeViewContainer;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.cache_pool.PointPool;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.util.ViewBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拖拽块类，用于处理树形视图中节点的拖拽操作
 * 支持批量拖拽和动画恢复功能
 *
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class DragBlock<T extends NodeItem> {
    /**
     * 临时存储拖拽的视图列表
     */
    private final List<View> tmp;

    /**
     * 拖拽状态标志，volatile确保线程安全
     */
    private volatile boolean isDragging;

    /**
     * 存储视图与其原始位置的映射关系
     */
    private final Map<View, ViewBox> originPositionMap;

    /**
     * 用于平滑滚动的滚动器
     */
    private final OverScroller mScroller;

    /**
     * 前一个位置点，用于计算移动距离
     */
    private PointF prePointF = null;

    /**
     * 拖拽块监听器
     */
    private DragBlockListener<T> mListener = null;

    /**
     * 用于mScroller的动画曲线插值器
     * 实现五次方缓动效果，开始缓慢加速，结束时缓慢减速
     */
    private static final Interpolator sInterpolator = t -> {
        t -= 1.0f;
        return t * t * t * t * t + 1.0f;
    };

    /**
     * 构造函数，初始化拖拽块
     *
     * @param listener 拖拽块监听器
     */
    public DragBlock(DragBlockListener<T> listener) {
        tmp = new ArrayList<>();
        mListener = listener;
        this.mScroller = new OverScroller(listener.container().getContext(), sInterpolator);
        originPositionMap = new HashMap<>();
    }

    /**
     * 加载视图到拖拽块中
     *
     * @param view 要加载的视图
     * @return 成功加载返回true，否则返回false
     */
    public boolean load(View view) {
        if (originPositionMap.isEmpty() && tmp.isEmpty()) {
            tmp.add(view);
            originPositionMap.put(view, new ViewBox(view));
            addItem(view);
            return true;
        }
        return false;
    }

    /**
     * 递归添加视图到拖拽块中，包括其子节点视图
     *
     * @param view 要添加的视图
     */
    private void addItem(View view) {
        if (mListener == null) return;
        Object tag = view.getTag(R.id.item_holder);
        if (tag instanceof TreeViewHolder) {
            TreeViewHolder<NodeItem> holder = (TreeViewHolder<NodeItem>) tag;
            NodeModel<NodeItem> node = holder.getNode();
            // 遍历当前节点的所有子节点
            for (NodeModel n : node.getChildNodes()) {
                TreeViewHolder h = mListener.container().getTreeViewHolder(n);
                tmp.add(h.getView());
                originPositionMap.put(h.getView(), new ViewBox(h.getView()));
                addItem(h.getView());
            }
        }
    }

    /**
     * 拖拽视图
     *
     * @param dx X轴移动距离
     * @param dy Y轴移动距离
     */
    public void drag(int dx, int dy) {
        if (!mScroller.isFinished()) {
            return;
        }
        this.isDragging = true;
        // 对所有拖拽的视图进行位置偏移
        for (int i = 0; i < tmp.size(); i++) {
            View view = tmp.get(i);
            view.offsetLeftAndRight(dx);
            view.offsetTopAndBottom(dy);
        }
    }

    /**
     * 设置拖拽状态
     *
     * @param dragging 拖拽状态
     */
    public void setDragging(boolean dragging) {
        isDragging = dragging;
    }

    /**
     * 自动释放资源
     * 当滚动完成且不在拖拽状态时清理资源
     */
    private void autoRelease() {
        if (mScroller.isFinished() && !isDragging) {
            originPositionMap.clear();
            tmp.clear();
            System.gc();
        }
    }

    /**
     * 平滑恢复视图到原始位置
     *
     * @param referenceView 参考视图，用于确定恢复位置
     */
    public void smoothRecover(View referenceView) {
        if (!mScroller.isFinished()) {
            return;
        }
        ViewBox rBox = originPositionMap.get(referenceView);
        if (rBox == null) {
            return;
        }
        prePointF = PointPool.obtain(0f, 0f);
        // 开始滚动动画，从当前位置恢复到原始位置
        mScroller.startScroll(0, 0, referenceView.getLeft() - rBox.left, referenceView.getTop() - rBox.top);
    }

    /**
     * 计算滚动偏移，用于动画执行
     *
     * @return 如果滚动未完成返回true，否则返回false
     */
    public boolean computeScroll() {
        boolean isSuc = mScroller.computeScrollOffset();
        if (isSuc) {
            PointF curPointF = PointPool.obtain(mScroller.getCurrX(), mScroller.getCurrY());
            mScroller.getCurrY();
            // 对所有拖拽的视图进行位置调整
            for (int i = 0; i < tmp.size(); i++) {
                View view = tmp.get(i);
                int dx = (int) (curPointF.x - prePointF.x);
                int dy = (int) (curPointF.y - prePointF.y);
                view.offsetLeftAndRight(-dx);
                view.offsetTopAndBottom(-dy);
            }
            if (prePointF != null) {
                prePointF.set(curPointF);
            }
            PointPool.free(curPointF);
            return true;
        }
        autoRelease();
        return false;
    }

    /**
     * 拖拽块监听器接口
     *
     * @param <T> 节点项类型
     */
    public interface DragBlockListener<T extends NodeItem> {
        /**
         * 获取树形视图容器
         *
         * @return 树形视图容器
         */
        TreeViewContainer<T> container();
    }

    /**
     * 释放资源
     */
    public void release() {
        if (originPositionMap != null) {
            originPositionMap.clear();
        }
        mListener = null;
    }
}
