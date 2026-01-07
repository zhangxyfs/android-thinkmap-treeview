package com.gyso.treeview.layout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.SparseIntArray;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.gyso.treeview.R;
import com.gyso.treeview.TreeViewContainer;
import com.gyso.treeview.adapter.DrawInfo;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.line.BaseLine;
import com.gyso.treeview.line.SmoothLine;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.util.DensityUtils;
import com.gyso.treeview.util.TreeViewLog;
import com.gyso.treeview.util.ViewBox;

import java.util.Map;

/**
 * guaishouN xw 674149099@qq.com
 */
/**
 * 树形布局管理器抽象基类，定义了树形结构布局的基本框架和通用功能
 *
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public abstract class TreeLayoutManager<T extends NodeItem> {
    /**
     * 类的标签，用于调试日志
     */
    public static final String TAG = TreeLayoutManager.class.getSimpleName();

    /**
     * 布局类型常量定义
     */
    public static final int LAYOUT_TYPE_NONE = -1;                   // 无布局类型
    public static final int LAYOUT_TYPE_HORIZON_RIGHT = 0;          // 水平向右布局
    public static final int LAYOUT_TYPE_VERTICAL_DOWN = 1;          // 垂直向下布局
    public static final int LAYOUT_TYPE_FORCE_DIRECTED = 2;         // 力导向布局
    public static final int LAYOUT_TYPE_HORIZON_LEFT = 3;           // 水平向左布局
    public static final int LAYOUT_TYPE_VERTICAL_UP = 4;            // 垂直向上布局
    public static final int LAYOUT_TYPE_RING = 5;                   // 环形布局
    public static final int LAYOUT_TYPE_HORIZON_LEFT_AND_RIGHT = 6; // 水平左右布局
    public static final int LAYOUT_TYPE_VERTICAL_DOWN_AND_UP = 7;   // 垂直上下布局

    /**
     * 默认内容边距，单位为dp
     */
    protected static final int DEFAULT_CONTENT_PADDING_DP = 50;
    /**
     * 默认父子节点间距，单位为dp
     */
    public static final int DEFAULT_SPACE_PARENT_CHILD_DP = 50;
    /**
     * 默认同级节点间距，单位为dp
     */
    public static final int DEFAULT_SPACE_PEER_PEER_DP = 20;
    /**
     * 默认连接线类型
     */
    public static final BaseLine DEFAULT_LINE = new SmoothLine();

    /**
     * 内容视图框，用于计算布局内容的边界
     */
    protected final ViewBox mContentViewBox;
    /**
     * 父子节点间距
     */
    protected int spaceParentToChild;
    /**
     * 同级节点间距
     */
    protected int spacePeerToPeer;

    /**
     * 固定视图框，其宽高与给定视口相同
     */
    protected final ViewBox fixedViewBox;
    /**
     * 固定偏移量X
     */
    protected int mFixedDx;
    /**
     * 固定偏移量Y
     */
    protected int mFixedDy;

    /**
     * 额外的X偏移量
     */
    protected int extraDeltaX, extraDeltaY;

    /**
     * 内容边距框
     */
    protected final ViewBox paddingBox;

    /**
     * 同一层级节点的最大值映射（楼层）
     */
    protected SparseIntArray floorMax = new SparseIntArray(200);

    /**
     * 同一深度节点的最大值映射（深度）
     */
    protected SparseIntArray deepMax = new SparseIntArray(200);

    /**
     * 同一层级节点的起始值映射（楼层）
     */
    protected SparseIntArray floorStart = new SparseIntArray(200);

    /**
     * 同一深度节点的起始值映射（深度）
     */
    protected SparseIntArray deepStart = new SparseIntArray(200);

    /**
     * 窗口高度
     */
    protected int winHeight;
    /**
     * 窗口宽度
     */
    protected int winWidth;

    /**
     * 连接线基类
     */
    private BaseLine baseline;

    /**
     * 构造函数，使用默认间距和连接线
     *
     * @param context 上下文
     */
    public TreeLayoutManager(Context context) {
        this(context, DEFAULT_SPACE_PARENT_CHILD_DP, DEFAULT_SPACE_PEER_PEER_DP, DEFAULT_LINE);
    }

    /**
     * 构造函数，使用默认连接线
     *
     * @param context 上下文
     * @param spacePeerToPeer 同级节点间距
     * @param spaceParentToChild 父子节点间距
     */
    public TreeLayoutManager(Context context, int spacePeerToPeer, int spaceParentToChild) {
        this(context, spacePeerToPeer, spaceParentToChild, DEFAULT_LINE);
    }

    /**
     * 构造函数，使用默认间距
     *
     * @param context 上下文
     * @param baseline 连接线类型
     */
    public TreeLayoutManager(Context context, BaseLine baseline) {
        this(context, DEFAULT_SPACE_PARENT_CHILD_DP, DEFAULT_SPACE_PEER_PEER_DP, baseline);
    }

    /**
     * 构造函数，指定所有参数
     *
     * @param context 上下文
     * @param spaceParentToChild 父子节点间距
     * @param spacePeerToPeer 同级节点间距
     * @param baseline 连接线类型
     */
    public TreeLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        mContentViewBox = new ViewBox();
        fixedViewBox = new ViewBox();
        paddingBox = new ViewBox();
        this.spaceParentToChild = DensityUtils.dp2px(context, spaceParentToChild);
        this.spacePeerToPeer = DensityUtils.dp2px(context, spacePeerToPeer);
        this.baseline = baseline;
    }

    /**
     * 获取父子节点间距
     *
     * @return 父子节点间距
     */
    public int getSpaceParentToChild() {
        return spaceParentToChild;
    }

    /**
     * 设置父子节点间距
     *
     * @param spaceParentToChild 父子节点间距
     */
    public void setSpaceParentToChild(int spaceParentToChild) {
        this.spaceParentToChild = spaceParentToChild;
    }

    /**
     * 获取同级节点间距
     *
     * @return 同级节点间距
     */
    public int getSpacePeerToPeer() {
        return spacePeerToPeer;
    }

    /**
     * 设置同级节点间距
     *
     * @param spacePeerToPeer 同级节点间距
     */
    public void setSpacePeerToPeer(int spacePeerToPeer) {
        this.spacePeerToPeer = spacePeerToPeer;
    }

    /**
     * 设置视口大小
     *
     * @param winHeight 窗口高度
     * @param winWidth 窗口宽度
     */
    public void setViewport(int winHeight, int winWidth) {
        this.winHeight = winHeight;
        this.winWidth = winWidth;
    }

    /**
     * 使用布局算法计算树模型
     *
     * @param mTreeModel 树模型
     */
    public abstract void calculateByLayoutAlgorithm(TreeModel<T> mTreeModel);

    /**
     * 执行测量
     *
     * @param treeViewContainer 树视图容器
     */
    public abstract void performMeasure(TreeViewContainer<T> treeViewContainer);

    /**
     * 执行布局
     *
     * @param treeViewContainer 树视图容器
     */
    public abstract void performLayout(TreeViewContainer<T> treeViewContainer);

    /**
     * 获取树形布局框
     *
     * @return 树形布局框
     */
    public abstract ViewBox getTreeLayoutBox();

    /**
     * 获取树形布局类型
     *
     * @return 布局类型常量
     */
    public abstract int getTreeLayoutType();

    /**
     * 释放资源
     */
    public abstract void release();

    /**
     * 获取边距
     *
     * @param treeViewContainer 树视图容器
     */
    public void getPadding(TreeViewContainer<T> treeViewContainer) {
        if (treeViewContainer.getPaddingStart() > 0) {
            paddingBox.setValues(
                    treeViewContainer.getPaddingTop(),
                    treeViewContainer.getPaddingLeft(),
                    treeViewContainer.getPaddingBottom(),
                    treeViewContainer.getPaddingRight());
        } else {
            int padding = DensityUtils.dp2px(treeViewContainer.getContext(), DEFAULT_CONTENT_PADDING_DP);
            paddingBox.setValues(padding, padding, padding, padding);
        }
    }

    /**
     * 执行绘制连接线
     *
     * @param drawInfo 绘制信息
     */
    public void performDrawLine(DrawInfo drawInfo) {
        if (baseline != null) {
            baseline.draw(drawInfo);
            return;
        }
        baseline = new BaseLine();
        baseline.draw(drawInfo);
    }

    /**
     * 准备布局动画数据
     *
     * @param currentNode 当前节点
     * @param currentNodeView 当前节点视图
     * @param finalLocation 节点视图最终位置
     * @param treeViewContainer 容器
     * @return true表示正在执行布局动画
     */
    protected boolean layoutAnimatePrepare(NodeModel<T> currentNode, View currentNodeView, ViewBox finalLocation, TreeViewContainer<T> treeViewContainer) {
        Object targetNodeTag = treeViewContainer.getTag(R.id.target_node);
        if (targetNodeTag instanceof NodeModel) {
            currentNodeView.setTag(R.id.node_final_location, finalLocation);
            if (targetNodeTag.equals(currentNode)) {
                TreeViewLog.e(TAG, "Get target location!");
                treeViewContainer.setTag(R.id.target_node_final_location, finalLocation);

                // 移除视图
                if (!animateRemoveNodes(treeViewContainer, finalLocation)) {
                    // TODO 直接移除节点
                    TreeViewLog.e(TAG, "Has remove nodes directly!");
                }

                Object targetLocationOnViewPortTag = treeViewContainer.getTag(R.id.target_location_on_viewport);
                if (targetLocationOnViewPortTag instanceof ViewBox) {
                    ViewBox targetLocationOnViewPort = (ViewBox) targetLocationOnViewPortTag;

                    // 修复预设大小和位置
                    float scale = targetLocationOnViewPort.getWidth() * 1f / finalLocation.getWidth();
                    treeViewContainer.setPivotX(0);
                    treeViewContainer.setPivotY(0);
                    treeViewContainer.setScaleX(scale);
                    treeViewContainer.setScaleY(scale);
                    TreeViewLog.d(TAG, "layoutAnimatePrepare: " + scale);
                    float dx = targetLocationOnViewPort.left - finalLocation.left * scale;
                    float dy = targetLocationOnViewPort.top - finalLocation.top * scale;
                    treeViewContainer.setTranslationX(dx);
                    treeViewContainer.setTranslationY(dy);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 执行布局动画
     *
     * @param treeViewContainer 容器
     */
    protected void layoutAnimate(TreeViewContainer<T> treeViewContainer) {
        TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        // 表示从预设位置平滑移动到当前位置
        Object nodeTag = treeViewContainer.getTag(R.id.target_node);
        Object targetNodeLocationTag = treeViewContainer.getTag(R.id.target_node_final_location);
        Object relativeLocationMapTag = treeViewContainer.getTag(R.id.relative_locations);
        Object animatorTag = treeViewContainer.getTag(R.id.node_trans_animator);
        if (animatorTag instanceof ValueAnimator) {
            ((ValueAnimator) animatorTag).end();
        }
        if (nodeTag instanceof NodeModel
                && targetNodeLocationTag instanceof ViewBox
                && relativeLocationMapTag instanceof Map) {
            ViewBox targetNodeLocation = (ViewBox) targetNodeLocationTag;
            Map<NodeModel<T>, ViewBox> relativeLocationMap = (Map<NodeModel<T>, ViewBox>) relativeLocationMapTag;

            AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
            valueAnimator.setDuration(TreeViewContainer.DEFAULT_FOCUS_DURATION);
            valueAnimator.setInterpolator(interpolator);
            valueAnimator.addUpdateListener(value -> {
                float ratio = (float) value.getAnimatedValue();
                TreeViewLog.e(TAG, "valueAnimator update ratio[" + ratio + "]");
                mTreeModel.doTraversalNodes(node -> {
                    TreeViewHolder<?> treeViewHolder = treeViewContainer.getTreeViewHolder(node);
                    if (treeViewHolder != null) {
                        View view = treeViewHolder.getView();
                        ViewBox preLocation = (ViewBox) view.getTag(R.id.node_pre_location);
                        ViewBox deltaLocation = (ViewBox) view.getTag(R.id.node_delta_location);
                        if (preLocation != null && deltaLocation != null) {
                            // 计算当前位置
                            ViewBox currentLocation = preLocation.add(deltaLocation.multiply(ratio));
                            view.layout(currentLocation.left,
                                    currentLocation.top,
                                    currentLocation.left + view.getMeasuredWidth(),
                                    currentLocation.top + view.getMeasuredHeight());
                        }
                    }
                });
            });

            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation, boolean isReverse) {
                    TreeViewLog.e(TAG, "onAnimationStart ");
                    // 计算并布局在预设位置
                    mTreeModel.doTraversalNodes(node -> {
                        TreeViewHolder<?> treeViewHolder = treeViewContainer.getTreeViewHolder(node);
                        if (treeViewHolder != null) {
                            View view = treeViewHolder.getView();
                            ViewBox relativeLocation = relativeLocationMap.get(treeViewHolder.getNode());

                            // 计算位置信息
                            ViewBox preLocation = targetNodeLocation.add(relativeLocation);
                            ViewBox finalLocation = (ViewBox) view.getTag(R.id.node_final_location);
                            if (preLocation == null || finalLocation == null) {
                                return;
                            }

                            ViewBox deltaLocation = finalLocation.subtract(preLocation);

                            // 保存为标签
                            view.setTag(R.id.node_pre_location, preLocation);
                            view.setTag(R.id.node_delta_location, deltaLocation);

                            // 布局在预设位置
                            view.layout(preLocation.left, preLocation.top, preLocation.left + view.getMeasuredWidth(), preLocation.top + view.getMeasuredHeight());
                        }
                    });

                }

                @Override
                public void onAnimationEnd(Animator animation, boolean isReverse) {

                    // 清除标签
                    treeViewContainer.setTag(R.id.target_location_on_viewport, null);
                    treeViewContainer.setTag(R.id.relative_locations, null);
                    treeViewContainer.setTag(R.id.target_node, null);
                    treeViewContainer.setTag(R.id.target_node_final_location, null);
                    treeViewContainer.setTag(R.id.node_trans_animator, null);

                    // 布局在最终位置
                    mTreeModel.doTraversalNodes(node -> {
                        TreeViewHolder<?> treeViewHolder = treeViewContainer.getTreeViewHolder(node);
                        if (treeViewHolder != null) {
                            View view = treeViewHolder.getView();
                            ViewBox finalLocation = (ViewBox) view.getTag(R.id.node_final_location);
                            if (finalLocation != null) {
                                view.layout(finalLocation.left, finalLocation.top, finalLocation.right, finalLocation.bottom);
                            }
                            view.setTag(R.id.node_pre_location, null);
                            view.setTag(R.id.node_delta_location, null);
                            view.setTag(R.id.node_final_location, null);
                            view.setElevation(TreeViewContainer.Z_NOR);
                        }
                    });
                }
            });
            treeViewContainer.setTag(R.id.node_trans_animator, valueAnimator);
            valueAnimator.start();
        }
    }

    /**
     * 执行视图移除动画
     *
     * @param treeViewContainer 容器
     * @param targetLocation 目标位置
     * @return 是否成功执行动画
     */
    private boolean animateRemoveNodes(TreeViewContainer<T> treeViewContainer, ViewBox targetLocation) {
        Object removedViewMapTag = treeViewContainer.getTag(R.id.mark_remove_views);
        Object relativeLocationMapTag = treeViewContainer.getTag(R.id.relative_locations);
        if (removedViewMapTag instanceof Map && relativeLocationMapTag instanceof Map) {

            final Map<NodeModel<T>, ViewBox> relativeLocationMap = (Map<NodeModel<T>, ViewBox>) relativeLocationMapTag;
            final Map<NodeModel<T>, View> removedViewMap = (Map) removedViewMapTag;
            int deltaDpMove = DensityUtils.dp2px(treeViewContainer.getContext(), TreeViewContainer.DEFAULT_REMOVE_ANIMATOR_DES);

            ValueAnimator removeAnimator = ValueAnimator.ofFloat(0f, 1f);
            removeAnimator.setDuration(TreeViewContainer.DEFAULT_FOCUS_DURATION);
            removeAnimator.addUpdateListener(value -> {
                for (NodeModel<T> nodeToRemove : removedViewMap.keySet()) {
                    View view = removedViewMap.get(nodeToRemove);
                    ViewBox relativeLocation = relativeLocationMap.get(nodeToRemove);
                    ViewBox location = targetLocation.add(relativeLocation);
                    float v = (float) value.getAnimatedValue();
                    if (getTreeLayoutType() == LAYOUT_TYPE_VERTICAL_DOWN) {
                        view.layout(location.left,
                                location.top + (int) (deltaDpMove * v),
                                location.left + view.getMeasuredWidth(),
                                location.top + view.getMeasuredHeight() + (int) (deltaDpMove * v));
                    } else if (getTreeLayoutType() == LAYOUT_TYPE_HORIZON_RIGHT) {
                        view.layout(location.left + (int) (deltaDpMove * v),
                                location.top,
                                location.left + view.getMeasuredWidth() + (int) (deltaDpMove * v),
                                location.top + view.getMeasuredHeight());
                    }
                    view.setAlpha(1 - v);
                }
            });

            removeAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    for (NodeModel<T> nodeToRemove : removedViewMap.keySet()) {
                        TreeViewLog.e(TAG, "removeAnimator onAnimationStart " + nodeToRemove);
                        View view = removedViewMap.get(nodeToRemove);
                        ViewBox relativeLocation = relativeLocationMap.get(nodeToRemove);
                        // 计算位置信息
                        ViewBox location = targetLocation.add(relativeLocation);
                        view.layout(location.left, location.top, location.right, location.bottom);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    for (NodeModel<T> nodeToRemove : removedViewMap.keySet()) {
                        TreeViewLog.e(TAG, "removeAnimator onAnimationEnd " + nodeToRemove);
                        View view = removedViewMap.get(nodeToRemove);
                        treeViewContainer.removeView(view);
                        view.setAlpha(1);
                        TreeViewHolder<T> holder = treeViewContainer.getTreeViewHolder(nodeToRemove);
                        if (holder != null) {
                            treeViewContainer.recycleHolder(holder);
                        }
                    }
                    removedViewMap.clear();
                    treeViewContainer.setTag(R.id.mark_remove_views, null);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            removeAnimator.start();
            return true;
        }
        return false;
    }

    /**
     * 直接布局节点（无动画）
     *
     * @param treeViewContainer 树视图容器
     */
    protected void layoutNodesDirectly(TreeViewContainer<T> treeViewContainer) {
        Object animatorTag = treeViewContainer.getTag(R.id.node_trans_animator);
        if (animatorTag instanceof ValueAnimator) {
            ((ValueAnimator) animatorTag).cancel();
        }
        treeViewContainer.setTag(R.id.node_trans_animator, null);

        TreeViewLog.d(TAG, "layoutNodesDirectly: 11.5 directly 1.1.8-11");
        TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        mTreeModel.doTraversalNodes(node -> {
            TreeViewHolder<?> holder = treeViewContainer.getTreeViewHolder(node);
            if (holder != null) {
                View view = holder.getView();
                ViewBox finalLocation = (ViewBox) view.getTag(R.id.node_final_location);
                if (finalLocation != null) {
                    view.layout(
                            finalLocation.left,
                            finalLocation.top,
                            finalLocation.right,
                            finalLocation.bottom
                    );
                }
                // 清除中间计算的标签
                view.setTag(R.id.node_pre_location, null);
                view.setTag(R.id.node_delta_location, null);
                view.setTag(R.id.node_final_location, null);
                view.setElevation(TreeViewContainer.Z_NOR);
            }
        });

        // 清理容器级标签（可选）
        treeViewContainer.setTag(R.id.target_location_on_viewport, null);
        treeViewContainer.setTag(R.id.relative_locations, null);
        treeViewContainer.setTag(R.id.target_node, null);
        treeViewContainer.setTag(R.id.target_node_final_location, null);
        treeViewContainer.setTag(R.id.node_trans_animator, null);
    }

    /**
     * 管理节点测量（可选实现）
     *
     * @param currentNode 当前节点
     * @param currentNodeView 当前节点视图
     * @param finalLocation 最终位置
     * @param treeViewContainer 树视图容器
     */
    public void onManagerMeasureNode(NodeModel<T> currentNode, View currentNodeView, ViewBox finalLocation, TreeViewContainer<T> treeViewContainer) {
    }

    /**
     * 管理所有节点测量完成（可选实现）
     *
     * @param treeViewContainer 树视图容器
     */
    public void onManagerFinishMeasureAllNodes(TreeViewContainer<T> treeViewContainer) {
    }

    /**
     * 管理节点布局（可选实现）
     *
     * @param currentNode 当前节点
     * @param currentNodeView 当前节点视图
     * @param finalLocation 最终位置
     * @param treeViewContainer 树视图容器
     */
    public void onManagerLayoutNode(NodeModel<T> currentNode, View currentNodeView, ViewBox finalLocation, TreeViewContainer<T> treeViewContainer) {
    }

    /**
     * 管理所有节点布局完成（可选实现）
     *
     * @param treeViewContainer 树视图容器
     */
    public void onManagerFinishLayoutAllNodes(TreeViewContainer<T> treeViewContainer) {
    }

    /**
     * 布局监听器接口
     *
     * @param <T> 节点项类型
     */
    public interface LayoutListener<T extends NodeItem> {
        /**
         * 布局子节点（默认空实现）
         *
         * @param next 下一个节点
         */
        default void onLayoutChild(NodeModel<T> next) {
        }

        /**
         * 布局完成
         */
        void onLayoutFinished();
    }

    /**
     * 测量监听器接口
     *
     * @param <T> 节点项类型
     */
    public interface MeasureListener<T extends NodeItem> {
        /**
         * 测量子节点（默认空实现）
         *
         * @param next 下一个节点
         */
        default void onMeasureChild(NodeModel<T> next) {
        }

        /**
         * 测量完成
         */
        void onMeasureFinished();
    }
}
