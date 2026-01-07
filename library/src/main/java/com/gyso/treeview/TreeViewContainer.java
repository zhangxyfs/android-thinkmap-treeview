package com.gyso.treeview;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.customview.widget.ViewDragHelper;

import com.gyso.treeview.adapter.DrawInfo;
import com.gyso.treeview.adapter.TreeViewAdapter;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.cache_pool.HolderPool;
import com.gyso.treeview.cache_pool.PointPool;
import com.gyso.treeview.layout.TreeLayoutManager;
import com.gyso.treeview.line.BaseLine;
import com.gyso.treeview.listener.TreeViewControlListener;
import com.gyso.treeview.listener.TreeViewNotifier;
import com.gyso.treeview.model.ITraversal;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.Position;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.touch.DragBlock;
import com.gyso.treeview.touch.TouchEventHandler;
import com.gyso.treeview.util.CacheStep;
import com.gyso.treeview.util.ContainerAnimatorListener;
import com.gyso.treeview.util.NodeCheck;
import com.gyso.treeview.util.TreeViewLog;
import com.gyso.treeview.util.ViewBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * guaishouN 674149099@qq.com
 */

public class TreeViewContainer<T extends NodeItem> extends ViewGroup implements TreeViewNotifier<T>, DragBlock.DragBlockListener<T> {
    /**
     * TAG 用于日志输出的标识符，取自当前类的简单名称
     */
    private static final String TAG = TreeViewContainer.class.getSimpleName();

    /**
     * isDebug 调试开关，控制是否输出调试信息
     */
    private static boolean isDebug = false;

    /**
     * IS_EDIT_DRAGGING 编辑拖拽状态标识对象，用于标记当前处于编辑拖拽模式
     */
    public static final Object IS_EDIT_DRAGGING = new Object();

    /**
     * DRAG_HIT_SLOP 拖拽触发的最小距离阈值（像素），超过该距离认为是有效拖拽操作
     */
    public static final double DRAG_HIT_SLOP = 60;

    /**
     * Z_NOR 正常状态下节点视图的Z轴高度值
     */
    public static final float Z_NOR = 10f;

    /**
     * Z_SELECT 选中状态下节点视图的Z轴高度值
     */
    public static final float Z_SELECT = 20f;

    /**
     * DEFAULT_FOCUS_DURATION 默认焦点动画持续时间（毫秒）
     */
    public static final int DEFAULT_FOCUS_DURATION = 0;

    /**
     * DEFAULT_REMOVE_ANIMATOR_DES 默认移除动画的目标偏移量（像素）
     */
    public static final float DEFAULT_REMOVE_ANIMATOR_DES = 100;

    /**
     * mTreeModel 树形数据模型，存储和管理整个树结构的数据
     */
    public TreeModel<T> mTreeModel;

    /**
     * drawInfo 绘制信息对象，保存绘制相关的配置和状态
     */
    private DrawInfo drawInfo;

    /**
     * touchHandler 触摸事件处理器，处理各种触摸交互逻辑
     */
    private TouchEventHandler touchHandler;

    /**
     * scaleDetector 缩放手势检测器，用于识别双指缩放等手势
     */
    private ScaleGestureDetector scaleDetector;

    /**
     * gestureDetector 手势检测器，用于识别点击、滑动等基本手势
     */
    private GestureDetector gestureDetector;

    /**
     * scaleFactor 当前视图的缩放比例因子，默认为1.0表示原始大小
     */
    private float scaleFactor = 1f;

    /**
     * mTreeLayoutManager 树布局管理器，负责计算和排列各个节点的位置
     */
    private TreeLayoutManager<T> mTreeLayoutManager;

    /**
     * viewWidth 当前视图的宽度（像素）
     */
    private int viewWidth;

    /**
     * viewHeight 当前视图的高度（像素）
     */
    private int viewHeight;

    /**
     * winWidth 窗口或屏幕的宽度（像素）
     */
    private int winWidth;

    /**
     * winHeight 窗口或屏幕的高度（像素）
     */
    private int winHeight;

    /**
     * minScale 最小允许的缩放比例，防止过度缩小
     */
    private float minScale = 0.2f;

    /**
     * nodeViewMap 节点到视图持有者的映射表（已废弃或未使用）
     */
    private Map<NodeModel<T>, TreeViewHolder<T>> nodeViewMap = null;

    /**
     * mPaint 画笔对象，用于执行绘图操作
     */
    private Paint mPaint;

    /**
     * centerMatrix 中心变换矩阵，用于实现平移、缩放等图形变换
     */
    private Matrix centerMatrix;

    /**
     * adapter 树形视图适配器，提供节点视图创建和绑定的功能
     */
    private TreeViewAdapter<T> adapter;

    /**
     * dragBlock 拖拽阻断器，用于控制拖拽过程中的行为限制
     */
    private final DragBlock dragBlock;

    /**
     * isDraggingNodeMode 是否进入节点拖拽模式的标志位
     */
    private boolean isDraggingNodeMode;

    /**
     * 仅仅用于判断是否进入了编辑模式
     */
    private boolean isOnlyInEditMode = false;

    /**
     * isDraggingNode 当前是否正在拖动某个节点
     */
    private boolean isDraggingNode = false;

    /**
     * isDraggingMove 当前是否正在进行拖拽移动操作
     */
    private boolean isDraggingMove = false;

    /**
     * dragHelper 视图拖拽辅助工具，帮助处理复杂的拖拽逻辑
     */
    private final ViewDragHelper dragHelper;

    /**
     * holderPools 视图持有者池集合，按类型分类存储可复用的视图持有者
     */
    private final SparseArray<HolderPool> holderPools = new SparseArray<>();

    /**
     * viewConf 视图配置信息，包含系统定义的一些触摸和滚动参数
     */
    private final ViewConfiguration viewConf;

    /**
     * mLayoutTransition 布局过渡动画控制器，用于处理子视图添加/删除时的动画效果
     */
    private LayoutTransition mLayoutTransition;

    /**
     * isAnimateRemove 是否启用移除动画
     */
    private boolean isAnimateRemove;

    /**
     * isAnimateAdd 是否启用添加动画
     */
    private boolean isAnimateAdd;

    /**
     * isAnimateMove 是否启用移动动画
     */
    private boolean isAnimateMove;

    /**
     * lastTouchEvent 上一次记录的触摸事件对象
     */
    private MotionEvent lastTouchEvent;

    /**
     * lastTouchX 上一次触摸位置的X坐标
     */
    private float lastTouchX = 0f;

    /**
     * lastTouchY 上一次触摸位置的Y坐标
     */
    private float lastTouchY = 0f;

    /**
     * 标记复制操作是否成功的状态变量
     * <p>
     * 该变量用于跟踪当前或最近一次复制操作的执行结果，
     * true表示复制成功，false表示复制失败
     */
    private boolean mCopySuccess = true;

    /**
     * 缓存步骤处理器，用于处理缓存相关的操作流程
     * 该字段声明了一个泛型类型的缓存步骤对象，用于执行缓存相关的业务逻辑
     */
    private CacheStep<T> mCacheStep = new CacheStep<>();


    /**
     * 树形视图容器的控制监听器实例
     */
    private TreeViewControlListener<T> controlListener = null;


    /**
     * 构造函数，创建一个新的树形视图容器实例
     *
     * @param context 上下文环境，用于访问应用程序资源和服务
     */
    public TreeViewContainer(Context context) {
        this(context, null, 0);
    }

    /**
     * 构造函数，创建一个新的树形视图容器实例
     *
     * @param context 上下文环境，用于访问应用程序资源和服务
     * @param attrs   属性集合，包含在XML中定义的属性值
     */
    public TreeViewContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    public TreeViewContainer<T> container() {
        return this;
    }

    /**
     * 节点编辑监听器接口，用于处理节点编辑事件
     *
     * @param <T> 节点项类型，必须继承自NodeItem
     */
    public interface OnNodeEditListener<T extends NodeItem> {
        /**
         * 当节点被编辑时调用此方法
         *
         * @param node 被编辑的节点模型对象
         */
        void onEditNode(NodeModel<T> node);
    }

    /**
     * 节点编辑监听器实例，用于响应节点编辑操作
     */
    private OnNodeEditListener<T> nodeEditListener;


    /**
     * 设置节点编辑监听器
     *
     * @param listener 节点编辑监听器实例，用于监听节点的编辑事件
     */
    public void setOnNodeEditListener(OnNodeEditListener<T> listener) {
        this.nodeEditListener = listener;
    }

    /**
     * 节点点击事件监听器接口
     * 定义了节点相关的各种点击事件回调方法
     *
     * @param <T> 节点数据类型，必须继承自NodeItem
     */
    public interface OnNodeClickListener<T extends NodeItem> {
        /**
         * 节点单击事件回调
         *
         * @param node          被点击的节点模型
         * @param view          被点击的视图对象
         * @param clickPosition 点击位置信息
         */
        void onNodeClicked(NodeModel<T> node, View view, Position clickPosition);

        /**
         * 节点双击事件回调
         *
         * @param node          被双击的节点模型
         * @param view          被双击的视图对象
         * @param clickPosition 双击位置信息
         */
        void onNodeDoubleClicked(NodeModel<T> node, View view, Position clickPosition);

        /**
         * 节点外部区域点击事件回调
         * 当用户点击节点以外的空白区域时触发
         */
        void onOutsideNodeClicked();
    }

    // 节点点击监听器实例
    private OnNodeClickListener<T> onNodeClickListener;

    /**
     * 设置节点点击监听器
     *
     * @param listener 节点点击监听器实例，用于处理节点的各种点击事件
     */
    public void setOnNodeClickListener(OnNodeClickListener<T> listener) {
        this.onNodeClickListener = listener;
    }

    public interface OnAnimationEndListener {
        void onAnimationEnd();
    }


    /**
     * 构造函数，用于初始化 TreeViewContainer 实例。
     *
     * @param context      上下文环境，通常用于访问当前应用的资源或系统服务。
     * @param attrs        属性集合，包含在 XML 中定义的自定义属性。
     * @param defStyleAttr 默认样式属性，用于指定该控件的默认主题样式。
     */
    public TreeViewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        dragBlock = new DragBlock(this);
        dragHelper = ViewDragHelper.create(this, dragCallback);
        touchHandler = new TouchEventHandler(getContext(), () -> this);
        viewConf = ViewConfiguration.get(context);
        TreeViewLog.e(TAG, "TreeViewContainer constructor");

        // 初始化缩放手势检测器，处理双指缩放操作
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                lastTouchX = detector.getFocusX();
                lastTouchY = detector.getFocusY();
                scaleFactor *= scale;
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 3.0f));
                setScaleX(scaleFactor);
                setScaleY(scaleFactor);

                TreeViewLog.d(TAG, "onScale: scaleFactor=" + scaleFactor);
                return true;
            }
        });

        // 初始化手势检测器，处理单击与双击事件
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // 查找被点击的子视图
                View child = dragHelper.findTopChildUnder((int) e.getX(), (int) e.getY());
                if (child != null) {
                    TreeViewLog.d(TAG, "onDoubleTap: " + child.getTag());
                    NodeModel<T> node = (NodeModel<T>) child.getTag();

                    // 判断节点是否展开，并触发相应的点击事件
                    if (!NodeCheck.parentNodeContract(node)) {
                        if (onNodeClickListener != null) {
                            onNodeClickListener.onNodeClicked(node, child, new Position(e.getX(), e.getY(), viewWidth, viewHeight));
                            TreeViewLog.d(TAG, "onDoubleTap: onNodeClickListener");
                            return true;
                        }
                        if (node != null && nodeEditListener != null) {
                            TreeViewLog.d(TAG, "onDoubleTap: nodeEditListener");
                            nodeEditListener.onEditNode(node);
                        }
                    }
                    return true;
                } else {
                    onNodeClickListener.onOutsideNodeClicked();
                }
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 处理双击事件，查找目标节点并回调监听器
                View child = dragHelper.findTopChildUnder((int) e.getX(), (int) e.getY());
                if (child != null) {
                    NodeModel<T> node = (NodeModel<T>) child.getTag();

                    if (!NodeCheck.parentNodeContract(node)) {
                        if (onNodeClickListener != null) {
                            TreeViewLog.d(TAG, child.getLeft() + " " + child.getRight() + " " + child.getTop() + " " + child.getBottom());
                            Position clickPosition = new Position(e.getX(), e.getY(), viewWidth, viewHeight);
                            onNodeClickListener.onNodeDoubleClicked(node, child, clickPosition);
                            TreeViewLog.d(TAG, "onDoubleTap: " + clickPosition);

                            return true;
                        }
                    }
                }
                return super.onDoubleTap(e);
            }
        });
    }


    public void setTreeViewControlListener(TreeViewControlListener listener) {
        if (touchHandler != null) {
            touchHandler.setControlListener(listener);
        }
    }

    /**
     * 初始化方法
     * <p>
     * 该方法用于初始化绘图相关的组件和属性，包括设置视图的裁剪属性、
     * 创建画笔对象并设置抗锯齿、创建路径对象以及初始化绘制信息对象。
     */
    private void init() {
        // 设置视图组是否裁剪子视图
        setClipChildren(false);
        // 设置是否裁剪到padding区域
        setClipToPadding(false);

        // 初始化画笔并设置抗锯齿
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        // 创建并重置路径对象
        Path mPath = new Path();
        mPath.reset();

        // 初始化绘制信息对象，并设置画笔和路径
        drawInfo = new DrawInfo();
        drawInfo.setPaint(mPaint);
        drawInfo.setPath(mPath);
    }


    public void setIsDebug(boolean b) {
        isDebug = b;
        if (isDebug) {
            setBackgroundColor(getResources().getColor(R.color.debug_container_color));
        } else {
            setBackgroundColor(Color.TRANSPARENT);
        }
    }

    /**
     * 当视图的可见性聚合状态发生变化时调用此方法
     *
     * @param isVisible 表示视图是否可见的布尔值
     */
    @SuppressLint("ObjectAnimatorBinding")
    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        TreeViewLog.e(TAG, "onVisibilityAggregated");

        // 初始化布局过渡动画器
        if (mLayoutTransition == null) {
            mLayoutTransition = new LayoutTransition();
            // 禁用默认的过渡动画效果
            mLayoutTransition.setAnimator(LayoutTransition.CHANGE_APPEARING, null);
            mLayoutTransition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING, null);
            mLayoutTransition.setAnimator(LayoutTransition.CHANGING, null);
            mLayoutTransition.setAnimator(LayoutTransition.APPEARING, null);

            // 设置消失动画的持续时间和动画效果
            mLayoutTransition.setDuration(LayoutTransition.DISAPPEARING, DEFAULT_FOCUS_DURATION);
            ObjectAnimator disappearAnimator = ObjectAnimator.ofFloat(null, "alpha", 1f, 0f);
            mLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, disappearAnimator);
        }
        //setLayoutTransition(mLayoutTransition);
    }


    /**
     * 测量视图及其子视图的大小
     *
     * @param widthMeasureSpec  宽度测量规范，包含宽度尺寸和测量模式
     * @param heightMeasureSpec 高度测量规范，包含高度尺寸和测量模式
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        TreeViewLog.e(TAG, "onMeasure");
        // 测量所有子视图
        final int size = getChildCount();
        for (int i = 0; i < size; i++) {
            measureChild(getChildAt(i), widthMeasureSpec, heightMeasureSpec);
        }

        // 更新窗口宽高信息
        if (MeasureSpec.getSize(widthMeasureSpec) > 0 && MeasureSpec.getSize(heightMeasureSpec) > 0) {
            winWidth = MeasureSpec.getSize(widthMeasureSpec);
            winHeight = MeasureSpec.getSize(heightMeasureSpec);
        }

        // 使用树形布局管理器进行布局测量
        if (mTreeLayoutManager != null && mTreeModel != null) {
            mTreeLayoutManager.setViewport(winHeight, winWidth);
            mTreeLayoutManager.performMeasure(this);
            ViewBox viewBox = mTreeLayoutManager.getTreeLayoutBox();
            drawInfo.setSpace(mTreeLayoutManager.getSpacePeerToPeer(), mTreeLayoutManager.getSpaceParentToChild());

            // 根据测量结果设置最终的视图尺寸
            int specWidth = MeasureSpec.makeMeasureSpec(Math.max(winWidth, viewBox.getWidth()), MeasureSpec.EXACTLY);
            int specHeight = MeasureSpec.makeMeasureSpec(Math.max(winHeight, viewBox.getHeight()), MeasureSpec.EXACTLY);
            setMeasuredDimension(specWidth, specHeight);
        } else {
            // 如果没有布局管理器，则使用父类的测量逻辑
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }


    /**
     * 当View尺寸发生变化时回调此方法
     *
     * @param w    新的宽度
     * @param h    新的高度
     * @param oldw 原来的宽度
     * @param oldh 原来的高度
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        TreeViewLog.e(TAG, "onSizeChanged w[" + w + "]h[" + h + "]oldw[" + oldw + "]oldh[" + oldh + "]");
        viewWidth = w;
        viewHeight = h;
        drawInfo.setWindowWidth(w);
        drawInfo.setWindowHeight(h);
        TreeViewLog.d(TAG, "onSizeChanged: 123");

        // 根据是否处于编辑模式来决定如何处理窗口变化
        if (!isOnlyInEditMode) {
            fixWindow();
        } else {
            // 如果 View 已经初始化且用户已进行缩放，此时只需要更新尺寸信息，不重置用户的缩放和平移。
            // 只需要确保 centerMatrix 重新计算 确保 centerMatrix 实例存在，不为空即可
            if (centerMatrix == null) {
                centerMatrix = new Matrix();
                Log.d(TAG, "onSizeChanged: centerMatrix ");
            }
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        TreeViewLog.e(TAG, "onLayout");
        if (mTreeLayoutManager != null && mTreeModel != null) {
            mTreeLayoutManager.performLayout(this);
        }
    }

    /**
     * 调整视图窗口以适配显示区域，确保内容能够正确展示。
     * <p>
     * 此方法主要用于处理视图与窗口之间的尺寸差异，通过计算缩放比例来调整视图的显示效果，
     * 并设置相应的变换矩阵和触摸代理。同时保留原有的变换状态以防图像异常消失。
     */
    private void fixWindow() {
        TreeViewLog.d(TAG, "fixWindow: single fix location");

        TreeViewLog.d(TAG, "计算视图与窗口的高度和宽度比值，并确定最小缩放比例");
        float scale;
        float hr = 1f * viewHeight / winHeight;
        float wr = 1f * viewWidth / winWidth;
        scale = Math.max(hr, wr);
        minScale = 1f / scale;

        // 保存当前的变换属性（用于后续比较）
        float oldPivotX = getPivotX();
        float oldPivotY = getPivotY();
        float oldTranslateX = getTranslationX();
        float oldTranslateY = getTranslationY();
        float oldScaleX = getScaleX();
        float oldScaleY = getScaleY();

        // 如果需要进行缩放调整
        if (Math.abs(scale - 1) > 0.01f) {
            TreeViewLog.d(TAG, "设置缩放中心点为(0,0)，并应用反向缩放以适应窗口大小");
            setPivotX(0);
            setPivotY(0);
            setScaleX(1f / scale);
            setScaleY(1f / scale);
        }

        // 初始化或更新居中变换矩阵，去除平移分量以便于后续操作
        if (centerMatrix == null) {
            centerMatrix = new Matrix();
        }
        centerMatrix.set(getMatrix());
        float[] values = new float[9];
        centerMatrix.getValues(values);
        values[Matrix.MTRANS_X] = 0f;
        values[Matrix.MTRANS_Y] = 0f;
        centerMatrix.setValues(values);

        // 设置触摸代理以支持交互
        setTouchDelegate();

        // 防止因缩放变化导致图像突然消失的问题：
        // 若其他变换参数未变而仅缩放发生变化，则恢复原始缩放值
        if (oldPivotX == getPivotX() && oldPivotY == getPivotY() && oldTranslateX == getTranslationX() && oldTranslateY == getTranslationY()) {
            if (oldScaleX != getScaleX()) {
                setScaleX(oldScaleX);
            }
            if (oldScaleY != getScaleY()) {
                setScaleY(oldScaleY);
            }
        }
    }


    /**
     * 设置触摸委托区域，使树形视图能够接收超出其边界范围的触摸事件
     * <p>
     * 该方法通过遍历所有树节点，计算出包含所有节点的最小矩形区域，
     * 然后将当前视图的触摸委托区域扩展到该区域，确保用户点击节点附近
     * 区域时也能正确触发节点的触摸事件。
     */
    private void setTouchDelegate() {
        post(() -> {
            if (mTreeModel == null) return;

            // 计算包含所有树节点的边界矩形
            final float[] bounds = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
            // bounds[0] = minX, bounds[1] = minY, bounds[2] = maxX, bounds[3] = maxY

            mTreeModel.doTraversalNodes(node -> {
                TreeViewHolder<?> holder = getTreeViewHolder(node);
                if (holder != null) {
                    View v = holder.getView();
                    bounds[0] = Math.min(bounds[0], v.getLeft());
                    bounds[1] = Math.min(bounds[1], v.getTop());
                    bounds[2] = Math.max(bounds[2], v.getRight());
                    bounds[3] = Math.max(bounds[3], v.getBottom());
                }
            });

            // 获取当前ViewGroup的原始边界
            Rect delegateArea = new Rect();
            getHitRect(delegateArea);

            // 将触摸委托区域扩展到包含所有节点的最小矩形
            delegateArea.left = (int) Math.min(delegateArea.left, bounds[0]);
            delegateArea.top = (int) Math.min(delegateArea.top, bounds[1]);
            delegateArea.right = (int) Math.max(delegateArea.right, bounds[2]);
            delegateArea.bottom = (int) Math.max(delegateArea.bottom, bounds[3]);

            // 设置触摸委托
            TouchDelegate touchDelegate = new TouchDelegate(delegateArea, this);
            if (getParent() instanceof View) {
                ((View) getParent()).setTouchDelegate(touchDelegate);
            }
        });
    }

    /**
     * 这个有问题 大概需要重置中心点
     * <p>
     * 通过根节点进行聚焦操作
     * <p>
     * 该方法用于根据中心矩阵调整当前视图的缩放和平移，使视图聚焦到根节点位置。
     * 主要功能包括：
     * 1. 计算所需的缩放比例
     * 2. 获取当前矩阵状态并进行边界检查
     * 3. 设置初始的缩放和平移值
     * 4. 根据焦点位置计算新的平移量以保持视觉焦点不变
     * 5. 应用新的缩放和平移变换
     * </p>
     */
    @Deprecated
    public void focusByRootNode() {
        TreeViewHolder<T> holder = nodeViewMap.get(mTreeModel.getRootNode());
        if (holder == null) return;
        View rootView = holder.getView();
        if (rootView == null) return;

        float[] centerM = new float[9];
        centerMatrix.getValues(centerM);
        float[] now = new float[9];
        getMatrix().getValues(now);

        if (now[Matrix.MSCALE_X] > 0 && now[Matrix.MSCALE_Y] > 0) {
            setScaleX(centerM[Matrix.MSCALE_X]);
            setScaleY(centerM[Matrix.MSCALE_Y]);
            setTranslationX(centerM[Matrix.MTRANS_X]);
            setTranslationY(centerM[Matrix.MTRANS_Y]);
        }


        float[] nowM = new float[9];
        Matrix matrix = getMatrix();
        matrix.getValues(nowM);
        float oldZoomX = nowM[Matrix.MSCALE_X];
        float oldZoomY = nowM[Matrix.MSCALE_Y];
        float translationX = nowM[Matrix.MTRANS_X];
        float translationY = nowM[Matrix.MTRANS_Y];

        // 计算目标缩放比例
        float needZoomX = 1f;
        float needZoomY = 1f;
        TreeViewLog.d(TAG, "toScaleTree: " + needZoomX);

        // 确定缩放中心点坐标（默认为中心位置）
        float focusX = rootView.getRight();
        float focusY = viewHeight / 2f;

        // 根据当前变换计算实际焦点在画布中的位置，并据此调整平移量以保持视觉焦点不变
        float currentFocusX = focusX * oldZoomX + translationX;
        float currentFocusY = focusY * oldZoomY + translationY;
        float needTranslationX = currentFocusX - focusX * needZoomX;
        float needTranslationY = currentFocusY - focusY * needZoomY;

        setScaleX(needZoomX);
        setScaleY(needZoomY);
        setTranslationX(needTranslationX);
        setTranslationY(needTranslationY);
    }

    public void focusByRootNodeP1() {
        // 固定放大倍数
        float scaleFactor = 1.0f;

        View parentView = ((View) getParent());
        int[] parentLocation = new int[2];
        parentView.getLocationOnScreen(parentLocation);


        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // 屏幕中心偏左的位置（固定值）
        float targetX = parentView.getWidth() * 0.35f; // 偏左 35%
        float targetY = screenHeight / 2f;   // 垂直居中


        targetY -= (parentLocation[1] - (screenHeight - parentLocation[1] - parentView.getHeight()));

        NodeModel<T> rootNode = mTreeModel.getRootNode();
        TreeViewHolder<T> holder = nodeViewMap.get(rootNode);
        if (holder == null) return;

        View rootView = holder.getView();
        // 根节点中心相对于树视图的坐标
        float nodeCenterX = rootView.getX() + rootView.getWidth() / 2f;
        float nodeCenterY = rootView.getY() + rootView.getHeight() / 2f;

        // 计算平移值，使根节点中心到达目标位置
        float newTransX = targetX - nodeCenterX * scaleFactor;
        float newTransY = targetY - nodeCenterY * scaleFactor;

        // 设置缩放和平移
        setScaleX(scaleFactor);
        setScaleY(scaleFactor);
        setTranslationX(newTransX);
        setTranslationY(newTransY);

        Log.d(TAG, "focusByRootNode: nodeCenterX=" + nodeCenterX + ", nodeCenterY=" + nodeCenterY);
        Log.d(TAG, "focusByRootNode: newTransX=" + newTransX + ", newTransY=" + newTransY);
    }

    /**
     * 根据节点模型获取对应的视图对象
     *
     * @param nodeModel 节点模型对象，用于查找对应的视图
     * @return 返回与节点模型关联的视图对象，如果未找到则返回null
     */
    @WorkerThread
    public @Nullable View getNodeView(NodeModel<T> nodeModel) {
        // 检查节点视图映射表是否为空
        if (nodeViewMap == null) return null;
        View needView = null;

        // 首先直接通过节点模型作为键来查找对应的视图持有者
        TreeViewHolder<T> holder = nodeViewMap.get(nodeModel);
        if (holder != null) {
            return holder.getView();
        }

        // 如果直接查找失败，则通过节点ID进行匹配查找
        NodeItem inputItem = nodeModel.value;
        for (Map.Entry<NodeModel<T>, TreeViewHolder<T>> entry : nodeViewMap.entrySet()) {
            NodeModel<T> key = entry.getKey();
            TreeViewHolder<T> value = entry.getValue();

            NodeItem mapItem = key.value;
            // 检查节点模型是否相同，或者节点ID相同且不为空
            if (key == nodeModel || TextUtils.equals(inputItem.itemId, mapItem.itemId) && !TextUtils.isEmpty(mapItem.itemId)) {
                needView = value.getView();
                break;
            }
        }
        return needView;
    }

    /**
     * 中点对焦
     * 将视图聚焦到预设的中心位置，通过动画或直接设置的方式调整缩放比例和位移
     *
     * @param useAnimation 是否使用动画效果进行聚焦
     */
    public void focusMidLocation(boolean useAnimation) {
        focusMidLocation(useAnimation, null);
    }

    public void focusMidLocation(boolean useAnimation, OnAnimationEndListener listener) {
        TreeViewLog.e(TAG, "focusMidLocation: " + getMatrix());
        float[] centerM = new float[9];
        // 检查中心矩阵是否存在
        if (centerMatrix == null) {
            TreeViewLog.e(TAG, "no centerMatrix!!!");
            return;
        }
        centerMatrix.getValues(centerM);
        float[] now = new float[9];
        getMatrix().getValues(now);
        TreeViewLog.e(TAG, "focusMidLocation: \n"
                + Arrays.toString(centerM) + "\n"
                + Arrays.toString(now));
        // 确保当前缩放值有效后再执行动画
        if (now[Matrix.MSCALE_X] > 0 && now[Matrix.MSCALE_Y] > 0) {
            animate().scaleX(centerM[Matrix.MSCALE_X])
                    .translationX(centerM[Matrix.MTRANS_X])
                    .scaleY(centerM[Matrix.MSCALE_Y])
                    .translationY(centerM[Matrix.MTRANS_Y])
                    .setDuration(useAnimation ? DEFAULT_FOCUS_DURATION : 0)
                    .setListener(new ContainerAnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            // 动画结束时通知控制监听器更新缩放状态
                            if (listener != null) {
                                listener.onAnimationEnd();
                            } else if (controlListener != null) {
                                controlListener.onScaling(TreeViewControlListener.FREE_SCALE, (int) (centerM[Matrix.MSCALE_X] * 100 + 1));
                            }
                        }
                    }).start();
        }
    }

    /**
     * 对树形视图进行缩放操作。
     *
     * @param scale 缩放的比例增量，用于增加或减少当前的缩放级别
     * @param isBig 指示是放大（true）还是缩小（false）
     * @return 如果成功执行了缩放动画则返回 true，否则返回 false（例如超出缩放范围限制时）
     */
    public boolean toScaleTree(float scale, boolean isBig) {
        float[] centerM = new float[9];
        if (centerMatrix != null) {
            centerMatrix.getValues(centerM);
        }

        float[] nowM = new float[9];
        Matrix matrix = getMatrix();
        matrix.getValues(nowM);
        float oldZoomX = nowM[Matrix.MSCALE_X];
        float oldZoomY = nowM[Matrix.MSCALE_Y];
        float translationX = nowM[Matrix.MTRANS_X];
        float translationY = nowM[Matrix.MTRANS_Y];

        // 计算目标缩放比例
        float needZoomX = isBig ? oldZoomX + scale : oldZoomX - scale;
        float needZoomY = isBig ? oldZoomY + scale : oldZoomY - scale;
        TreeViewLog.d(TAG, "toScaleTree: " + needZoomX);

        // 判断是否低于最小允许的缩放比例
        if (centerMatrix != null) {
            if (needZoomX <= centerM[Matrix.MSCALE_X] || needZoomY <= centerM[Matrix.MSCALE_Y]) {
//                focusMidLocation(true);
                return false;
            }
        } else {
            if (needZoomX <= TouchEventHandler.MIN_SCALE || needZoomY <= TouchEventHandler.MIN_SCALE) {
//                focusMidLocation(true);
                return false;
            }
        }

        // 判断是否超过最大允许的缩放比例
        if (needZoomX > TouchEventHandler.MAX_SCALE || needZoomY > TouchEventHandler.MAX_SCALE) {
            return false;
        }

        // 确定缩放中心点坐标（默认为中心位置）
        float focusX = viewWidth / 2f;
        float focusY = viewHeight / 2f;

        // 根据当前变换计算实际焦点在画布中的位置，并据此调整平移量以保持视觉焦点不变
        float currentFocusX = focusX * oldZoomX + translationX;
        float currentFocusY = focusY * oldZoomY + translationY;
        float needTranslationX = currentFocusX - focusX * needZoomX;
        float needTranslationY = currentFocusY - focusY * needZoomY;

        // 执行属性动画实现平滑缩放和平移效果
        animate().scaleX(needZoomX).scaleY(needZoomY)
                .translationX(needTranslationX).translationY(needTranslationY)
                .setDuration(DEFAULT_FOCUS_DURATION)
                .setListener(new ContainerAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (controlListener != null) {
                            controlListener.onScaling(TreeViewControlListener.FREE_SCALE, (int) (needZoomX * 100 + 1));
                        }
                    }
                }).start();

        return true;
    }


    public float getMinScale() {
        return minScale;
    }

    public void setLastTouchEvent(MotionEvent event) {
        lastTouchEvent = event;
    }

    private float focusNodeX = -1;
    private float focusNodeY = -1;

    /**
     * 更新焦点节点到视图中心最近的节点
     * <p>
     * 该方法会遍历所有可见节点，计算每个节点中心点到视图中心的距离，
     * 找到距离最短的节点，并将焦点位置更新为该节点的中心坐标。
     * <p>
     * 注意：该方法不会直接修改焦点节点引用，只是更新焦点坐标位置
     */
    private void updateFocusNodeToNearestCenter() {
        // 检查树模型是否为空，为空则直接返回
        if (mTreeModel == null) return;

        // 计算视图中心坐标
        float centerX = viewWidth / 2f;
        float centerY = viewHeight / 2f;

        // 初始化最小距离和最近节点坐标
        float minDist = Float.MAX_VALUE;
        float nearestX = -1, nearestY = -1;

        // 获取所有可见节点列表
        List<NodeModel<T>> visibleNodes = new ArrayList<>();
        mTreeModel.doTraversalNodes(visibleNodes::add);

        // 遍历所有可见节点，找到距离视图中心最近的节点
        for (NodeModel<T> node : visibleNodes) {
            TreeViewHolder<?> holder = getTreeViewHolder(node);
            if (holder != null) {
                View v = holder.getView();
                // 计算当前节点的中心坐标
                float nodeCenterX = v.getX() + v.getWidth() / 2f;
                float nodeCenterY = v.getY() + v.getHeight() / 2f;

                // 计算节点中心到视图中心的距离平方
                float dx = nodeCenterX - centerX;
                float dy = nodeCenterY - centerY;
                float dist = dx * dx + dy * dy;

                // 更新最小距离和最近节点坐标
                if (dist < minDist) {
                    minDist = dist;
                    nearestX = nodeCenterX;
                    nearestY = nodeCenterY;
                }
            }
        }

        // 如果找到了最近的节点，则更新焦点坐标
        if (nearestX >= 0 && nearestY >= 0) {
            focusNodeX = nearestX;
            focusNodeY = nearestY;
        }
    }


    /**
     * 拦截触摸事件处理方法
     *
     * @param event 触摸事件对象，包含触摸点坐标和动作类型等信息
     * @return true表示拦截该事件并交由当前View处理，false表示不拦截让子View处理
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {

        // 如果不是拖拽节点模式，直接返回false不拦截事件
        if (!isDraggingNodeMode) return false;

        int action = event.getActionMasked();

        // 处理按下事件
        if (action == MotionEvent.ACTION_DOWN) {
            // 查找点击位置下的顶层子View
            View child = dragHelper.findTopChildUnder((int) event.getX(), (int) event.getY());
            if (child != null) {
                // 如果找到了子View，设置拖拽状态并请求父View不拦截后续事件
                isDraggingNode = true;
                isDraggingMove = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            } else {
                // 如果没有找到子View，设置移动状态并记录触摸坐标
                isDraggingNode = false;
                isDraggingMove = true;
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            }
        }

        // 处理取消和抬起事件，重置拖拽状态
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            isDraggingNode = false;
            isDraggingMove = false;
        }

        // 返回当前是否处于拖拽节点状态
        return isDraggingNode;
    }


    /**
     * 处理触摸事件，包括拖拽和手势检测
     *
     * @param event 触摸事件对象，包含触摸点坐标、动作类型等信息
     * @return boolean 返回true表示事件已被处理，false表示事件未被处理
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
//        scaleDetector.onTouchEvent(event) ;  //手势缩放
        int action = event.getActionMasked();

        // 处理节点拖拽逻辑
        if (isDraggingNode) {
            TreeViewLog.d(TAG, "onTouchEvent: draghelper");
            dragHelper.processTouchEvent(event);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                isDraggingNode = false;
                isDraggingMove = false;
                lastTouchX = 0;
                lastTouchY = 0;
                TreeViewLog.d(TAG, "onTouchEvent: draghelper up");
                getParent().requestDisallowInterceptTouchEvent(false);
//                updateFocusNodeToNearestCenter();
            }
            return true;
        } else {
//            updateFocusNodeToNearestCenter();
            TreeViewLog.d(TAG, "onTouchEvent: draghelper area");
            return touchHandler.onTouchEvent(event);
        }
    }


    /**
     * 绘制子视图时的回调方法，用于绘制树形结构的连接线
     *
     * @param canvas 画布对象，用于绘制图形
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        // 如果树模型不为空，则进行树形连接线的绘制
        if (mTreeModel != null) {
            drawInfo.setCanvas(canvas);
            drawTreeLine(mTreeModel.getRootNode());
        }
        // 调用父类的绘制方法继续绘制其他内容
        super.dispatchDraw(canvas);
    }


    /**
     * 绘制树形的连线
     *
     * @param parent parent node
     */
    private void drawTreeLine(NodeModel<T> parent) {
        LinkedList<? extends NodeModel<T>> childNodes = parent.getChildNodes();
        for (NodeModel<T> node : childNodes) {
            TreeViewHolder<T> parentHolder = getTreeViewHolder(parent);
            // 设置父节点holder的布局类型
            if (parentHolder.getHolderLayoutType() == TreeLayoutManager.LAYOUT_TYPE_NONE) {
                parentHolder.setHolderLayoutType(mTreeLayoutManager.getTreeLayoutType());
            }
            drawInfo.setFromHolder(parentHolder);
            TreeViewHolder<?> childHolder = getTreeViewHolder(node);
            // 设置子节点holder的布局类型
            if (childHolder.getHolderLayoutType() == TreeLayoutManager.LAYOUT_TYPE_NONE) {
                childHolder.setHolderLayoutType(mTreeLayoutManager.getTreeLayoutType());
            }
            drawInfo.setToHolder(childHolder);
            drawDragBackGround(childHolder.getView());
            // 检查是否处于编辑拖拽模式，如果是则跳过绘制连线
            if (isDraggingNodeMode && childHolder.getView().getTag(R.id.edit_and_dragging) == IS_EDIT_DRAGGING) {
                //Is editing and dragging, so not draw line.
                drawTreeLine(node);
                continue;
            }
            // 优先使用适配器自定义绘制连线，否则使用默认绘制方式
            BaseLine adapterDrawLine = adapter.onDrawLine(drawInfo);
            if (adapterDrawLine != null) {
                adapterDrawLine.draw(drawInfo);
            } else {
                mTreeLayoutManager.performDrawLine(drawInfo);
            }
            // 递归绘制子节点的连线
            drawTreeLine(node);
        }
    }


    public TreeModel<T> getTreeModel() {
        return adapter.getTreeModel();
    }

    /**
     * 添加所有的NoteView
     */
    private void addNoteViews() {
        if (mTreeModel != null) {
            mTreeModel.doTraversalNodes(this::addNodeViewToGroup);
        }
    }

    /**
     * 将节点视图添加到组中
     *
     * @param node 要添加的节点模型对象
     */
    private void addNodeViewToGroup(NodeModel<T> node) {
        // 创建节点对应的ViewHolder
        TreeViewHolder<T> treeViewHolder = createHolder(node);
        // 绑定ViewHolder数据
        adapter.bindViewHolder(treeViewHolder);
        // 获取ViewHolder的视图并设置默认高度
        View view = treeViewHolder.getView();
        view.setElevation(Z_NOR);
        // 将视图添加到当前容器中
        this.addView(view);
        // 为视图设置tag标识，存储对应的ViewHolder
        view.setTag(R.id.item_holder, treeViewHolder);
        // 如果节点视图映射表不为空，则将节点与ViewHolder的映射关系存入map中
        if (nodeViewMap != null) {
            nodeViewMap.put(node, treeViewHolder);
        }
    }


    private final ViewDragHelper.Callback dragCallback = new ViewDragHelper.Callback() {
        /**
         * 尝试捕获视图用于拖拽操作
         *
         * @param child 需要捕获的子视图
         * @param pointerId 触摸点的标识符
         * @return 如果成功捕获视图返回true，否则返回false
         */
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            TreeViewLog.d(TAG, "tryCaptureView: ");
            // modify by zxy 当节点为隐藏状态时候返回 false
            if (child.getVisibility() == View.GONE) return false;

            // 检查是否处于拖拽节点模式，并且当前视图可以被拖拽
            if (isDraggingNodeMode && dragBlock.load(child)) {
                child.setTag(R.id.edit_and_dragging, IS_EDIT_DRAGGING);
                child.setElevation(Z_SELECT);
                return true;
            }
            return false;
        }

        /**
         * 获取视图水平拖拽范围
         *
         * @param child 需要获取拖拽范围的子视图
         * @return 水平方向的最大拖拽范围值
         */
        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            TreeViewLog.d(TAG, "getViewHorizontalDragRange: ");
            return Integer.MAX_VALUE;
        }

        /**
         * 获取视图垂直拖拽范围
         *
         * @param child 需要获取拖拽范围的子视图
         * @return 垂直方向的最大拖拽范围值
         */
        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            TreeViewLog.d(TAG, "getViewVerticalDragRange: ");
            return Integer.MAX_VALUE;
        }


        /**
         * 限制视图在水平方向上的拖拽位置
         *
         * @param child 被拖拽的子视图
         * @param left 视图尝试移动到的左侧位置
         * @param dx 水平方向上的位移增量
         * @return 实际允许的水平位置
         */
        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            TreeViewLog.d(TAG, "clampViewPositionHorizontal: ");
            // 当处于拖拽状态时，处理水平拖拽逻辑
            if (dragHelper.getViewDragState() == ViewDragHelper.STATE_DRAGGING) {
                final int oldLeft = child.getLeft();
                dragBlock.drag(dx, 0);
                estimateToHitTarget(child);
                invalidate();
                return oldLeft;
            } else {
                return left;
            }
        }

        /**
         * 限制视图在垂直方向上的拖拽位置
         *
         * @param child 被拖拽的子视图
         * @param top 视图尝试移动到的顶部位置
         * @param dy 垂直方向上的位移增量
         * @return 实际允许的垂直位置
         */
        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            TreeViewLog.d(TAG, "clampViewPositionVertical: ");
            // 当处于拖拽状态时，处理垂直拖拽逻辑
            if (dragHelper.getViewDragState() == ViewDragHelper.STATE_DRAGGING) {
                final int oldTop = child.getTop();
                dragBlock.drag(0, dy);
                estimateToHitTarget(child);
                invalidate();
                return oldTop;
            } else {
                return top;
            }
        }


        /**
         * 当拖拽的视图被释放时调用此方法，处理节点的重新排列逻辑
         *
         * @param releasedChild 被释放的子视图，即正在被拖拽的视图
         * @param xvel X轴方向的速度
         * @param yvel Y轴方向的速度
         */
        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            TreeViewLog.d(TAG, "onViewReleased: ");
            Object fTag = releasedChild.getTag(R.id.the_hit_target);
            boolean getHit = fTag != null;
            if (getHit) {
                // 获取目标位置的ViewHolder和节点信息
                TreeViewHolder<T> targetHolder = getTreeViewHolder((NodeModel<T>) fTag);
                NodeModel<T> targetHolderNode = targetHolder.getNode();
                // 获取被释放视图的ViewHolder和节点信息
                TreeViewHolder<T> releasedChildHolder = (TreeViewHolder<T>) releasedChild.getTag(R.id.item_holder);
                NodeModel<T> releasedChildHolderNode = releasedChildHolder.getNode();

                // 从原父节点中移除该节点，然后添加到新的目标节点下
                if (releasedChildHolderNode.getParentNode() != null) {
                    mTreeModel.removeNode(releasedChildHolderNode.getParentNode(), releasedChildHolderNode);
                }
                mTreeModel.addNode(targetHolderNode, releasedChildHolderNode);

                TreeViewLog.d("TreeViewContainer", "onViewReleased: [" + releasedChildHolderNode.getValue() + "] " + " to [" + targetHolderNode.getValue() + "]");
                if (controlListener != null) {
                    controlListener.onDragMoveNodeComplete(releasedChildHolderNode, targetHolderNode);
                }

                // 重新计算布局并请求重新绘制
                mTreeLayoutManager.calculateByLayoutAlgorithm(mTreeModel);
                if (isAnimateMove()) {
                    recordAnchorLocationOnViewPort(false, false, targetHolderNode);
                }
                requestLayout();
            } else {
                // 没有找到有效目标位置，恢复到原来的位置
                dragBlock.smoothRecover(releasedChild);
            }

            // 重置拖拽状态和相关标记
            dragBlock.setDragging(false);
            releasedChild.setElevation(Z_NOR);
            releasedChild.setTag(R.id.edit_and_dragging, null);
            releasedChild.setTag(R.id.the_hit_target, null);
            TreeViewLog.d(TAG, "onViewReleased: 123");
            invalidate();
        }
    };

    /**
     * 计算并处理滚动逻辑
     * <p>
     * 此方法用于处理视图的滚动计算，通过调用dragBlock的computeScroll方法来执行具体的滚动逻辑，
     * 如果滚动状态发生变化则触发视图重绘。
     */
    @Override
    public void computeScroll() {
        // 调用dragBlock计算滚动，并根据返回结果决定是否需要重绘视图
        if (dragBlock.computeScroll()) {
            invalidate();
        }
    }


    /**
     * find the hit node
     *
     * @param srcView src view
     */
    private static final int DRAG_HIT_MARGIN = 10;

    /**
     * 估算当前拖拽的视图是否命中了某个目标节点视图，用于实现拖拽放置逻辑。
     * <p>
     * 此方法首先检查当前视图是否已经命中某个目标节点。如果已命中但当前位置不再包含在目标区域，
     * 则取消之前的命中状态，并通知监听器。如果没有命中目标或之前命中的目标已失效，
     * 则遍历所有节点视图，判断当前中心点是否落在某一个目标视图的扩展区域内。
     * 若命中，则记录该目标节点并回调监听器。
     *
     * @param srcView 当前正在被拖拽的源视图
     * @return 是否命中了有效目标节点
     */
    private boolean estimateToHitTarget(View srcView) {
        PointF src = getCenterPoint(srcView);

        // 检查当前是否已有命中的目标节点
        Object tag = srcView.getTag(R.id.the_hit_target);
        if (tag instanceof NodeModel) {
            TreeViewHolder<?> holder = getTreeViewHolder((NodeModel) tag);
            Rect targetRect = new Rect();
            holder.getView().getHitRect(targetRect); // 获取目标视图的边界矩形
            targetRect.inset(-DRAG_HIT_MARGIN, -DRAG_HIT_MARGIN); // 扩展边界以增加容错范围

            // 如果当前中心点不在目标范围内，清除命中状态并通知监听器
            if (!targetRect.contains((int) src.x, (int) src.y)) {
                srcView.setTag(R.id.the_hit_target, null); // 取消命中
                if (controlListener != null) {
                    Object srcViewHolderTag = srcView.getTag(R.id.item_holder);
                    if (srcViewHolderTag instanceof TreeViewHolder) {
                        Log.d(TAG, "estimateToHitTarget: cancel");
                        controlListener.onDragMoveNodesHit(
                                ((TreeViewHolder<T>) srcViewHolderTag).getNode(),
                                null,
                                srcView,
                                null
                        );
                    }
                }
            }
        }

        // 如果尚未命中任何目标节点，则进行全节点遍历检测
        if (srcView.getTag(R.id.the_hit_target) == null) {
            mTreeModel.doTraversalNodes((ITraversal<NodeModel<T>>) next -> {
                TreeViewHolder<?> holder = getTreeViewHolder(next);
                if (holder.getView() == srcView) { // 跳过自身视图
                    return;
                }
                Rect targetRect = new Rect();
                holder.getView().getHitRect(targetRect);
                targetRect.inset(-DRAG_HIT_MARGIN, -DRAG_HIT_MARGIN); // 扩展检测区域

                // 如果当前中心点位于目标视图范围内，标记为命中并通知监听器
                if (targetRect.contains((int) src.x, (int) src.y)) {
                    mTreeModel.setFinishTraversal(true);
                    srcView.setTag(R.id.the_hit_target, holder.getNode());

                    // 回调命中监听
                    if (controlListener != null) {
                        Log.d(TAG, "estimateToHitTarget: hit");
                        Object srcViewHolderTag = srcView.getTag(R.id.item_holder);
                        if (srcViewHolderTag instanceof TreeViewHolder) {
                            controlListener.onDragMoveNodesHit(
                                    ((TreeViewHolder<T>) srcViewHolderTag).getNode(),
                                    next,
                                    srcView,
                                    holder.getView()
                            );
                        }
                    }
                }
            });
        }

        PointPool.free(src);
        return srcView.getTag(R.id.the_hit_target) != null;
    }


    /**
     * 绘制拖拽背景效果
     * <p>
     * 当视图被标记为目标命中对象时，绘制一个圆角矩形边框来表示拖拽目标区域。
     * 该方法会计算目标视图的位置和大小，并根据布局管理器的间距设置来确定绘制参数。
     *
     * @param view 需要绘制拖拽背景的视图对象
     */
    private void drawDragBackGround(View view) {
        Object fTag = view.getTag(R.id.the_hit_target);
        boolean getHit = fTag != null;
        if (getHit) {
            // 计算源视图和目标视图的对角线长度，用于确定绘制半径
            double srcR = Math.hypot(view.getWidth(), view.getHeight());
            TreeViewHolder<T> holder = getTreeViewHolder((NodeModel) fTag);
            View targetView = holder.getView();
            double tarR = Math.hypot(targetView.getWidth(), targetView.getHeight());
            float minGap = Math.min(mTreeLayoutManager.getSpacePeerToPeer(), mTreeLayoutManager.getSpaceParentToChild());
            double fR = minGap / getScaleX() + Math.max(srcR, tarR) / 2;

            // 获取目标视图的边界坐标
            int left = targetView.getLeft();
            int top = targetView.getTop();
            int right = targetView.getRight();
            int bottom = targetView.getBottom();

            // 设置画笔属性并绘制圆角矩形边框
            mPaint.reset();
            mPaint.setStyle(Paint.Style.STROKE); // 只画边框
            mPaint.setStrokeWidth(10); // 边框粗细
            mPaint.setColor(Color.BLACK); // 边框颜色
            mPaint.setAntiAlias(true);
            float cornerRadius = 20f;
            drawInfo.getCanvas().drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, mPaint);
        }
    }


    private PointF getCenterPoint(View view) {
        return PointPool.obtain(view.getX() + view.getWidth() / 2f, view.getY() + view.getHeight() / 2f);
    }

    protected void requestMoveNodeByDragging(boolean isEditMode) {
        this.isDraggingNodeMode = isEditMode;
        this.isOnlyInEditMode = isEditMode;
        ViewParent parent = getParent();
        if (parent instanceof View) {
            parent.requestDisallowInterceptTouchEvent(isEditMode);
        }
    }

    protected void draggingNodeMode(boolean isOpen) {
        this.isDraggingNodeMode = isOpen;
    }

    /**
     * 是否开启编辑模式
     *
     * @return
     */
    public boolean isEditMode() {
        return isDraggingNodeMode;
    }

    public void setTreeLayoutManager(TreeLayoutManager TreeLayoutManager) {
        mTreeLayoutManager = TreeLayoutManager;
    }

    public TreeViewAdapter<T> getAdapter() {
        return adapter;
    }

    public void setAdapter(TreeViewAdapter<T> adapter) {
        this.adapter = adapter;
        this.adapter.setNotifier(this);
    }

    public TreeViewHolder<T> getTreeViewHolder(NodeModel<T> nodeModel) {
        if (nodeModel == null || nodeViewMap == null) {
            return null;
        }
        return nodeViewMap.get(nodeModel);
    }

    /**
     * 数据集变化时的回调方法
     * 当数据源发生改变时，重新构建树形视图结构
     */
    @Override
    public void onDataSetChange() {
        // 获取更新后的树模型数据
        mTreeModel = adapter.getTreeModel();
        TreeViewLog.d(TAG, "onDataSetChange: 111");

        // 清除所有现有视图并重新构建
        removeAllViews();
        if (mTreeModel != null) {
            // 初始化节点视图映射表
            nodeViewMap = nodeViewMap == null ? new HashMap<>() : nodeViewMap;
            nodeViewMap.clear();

            // 添加节点视图并计算布局
            addNoteViews();
            mTreeLayoutManager.calculateByLayoutAlgorithm(mTreeModel);
        }

        // 初始化缓存步骤
        mCacheStep.init();
    }
    /**
     * 检查新添加的节点是否超出右边界，如果是，则自动向左平移视口。
     */
    public void autoScrollForNewNode(NodeModel<T>... childNodes) {
        if (childNodes == null || childNodes.length == 0) return;
        //暂时忽略这个方法
        NodeModel<T> lastChild = childNodes[childNodes.length - 1];

        // 获取到节点的最新位置
        post(() -> {
            TreeViewHolder<T> holder = getTreeViewHolder(lastChild);
            if (holder == null) return;
            View childView = holder.getView();

            float currentScale = getScaleX();
            float currentTransX = getTranslationX();

            // 计算节点右边缘在屏幕上的绝对位置
            // 节点左坐标 + 节点宽度 = 节点右边缘
            float nodeRightInScreen = (childView.getLeft() + childView.getWidth()) * currentScale + currentTransX;

            float padding = 150f;
            float rightBoundary = winWidth - padding;

            if (nodeRightInScreen > rightBoundary) {
                float offset = nodeRightInScreen - rightBoundary;
                animate().translationX(currentTransX - offset)
                        .setDuration(300)
                        .start();
            }
        });
    }
    /**
     * 将视角直接定位到指定节点
     * @param targetNode 目标节点
     * @param useAnimation 是否使用动画
     */
    public void focusOnNode(NodeModel<T> targetNode, boolean useAnimation) {
        post(() -> {
            TreeViewHolder<T> holder = getTreeViewHolder(targetNode);
            if (holder == null) return;
            View nodeView = holder.getView();

            //获取当前缩放比例
            float scale = getScaleX();

            // 固定新节点在屏幕上的位置 考虑偏左一点
            float targetScreenX = winWidth * 0.3f;
            float targetScreenY = winHeight * 0.5f;


            float relativeNodeCenterX = nodeView.getLeft() + nodeView.getWidth() / 2f;
            float relativeNodeCenterY = nodeView.getTop() + nodeView.getHeight() / 2f;
            //屏幕目标位置 - (节点原始坐标 * 当前缩放)
            float destTransX = targetScreenX - (relativeNodeCenterX * scale);
            float destTransY = targetScreenY - (relativeNodeCenterY * scale);

            if (useAnimation) {
                animate()
                        .translationX(destTransX)
                        .translationY(destTransY)
                        .setDuration(400)
                        .start();
            } else {
                setTranslationX(destTransX);
                setTranslationY(destTransY);
            }
        });
    }
    /**
     * 当添加节点时的回调方法
     *
     * @param parent     父节点模型
     * @param childNodes 要添加的子节点数组
     */
    @Override
    public void onAddNodes(NodeModel<T> parent, NodeModel<T>... childNodes) {
        // 检查适配器是否为空
        if (adapter != null) {
            // 如果启用添加动画，则记录锚点位置
            if (isAnimateAdd()) {
                recordAnchorLocationOnViewPort(false, false, parent);
            }
            // 将子节点添加到树模型中
            mTreeModel.addNode(parent, childNodes);
            // 重新计算树布局
            mTreeLayoutManager.calculateByLayoutAlgorithm(mTreeModel);
            // 为每个新添加的节点创建并添加视图
            for (NodeModel<T> node : childNodes) {
                addNodeViewToGroup(node);
            }
//            autoScrollForNewNode(childNodes);   //暂时不用，只加了判断右边框方法
            if (childNodes.length > 0) {  //直接定位到新添加的节点上
                focusOnNode(childNodes[childNodes.length - 1], false);
            }
        }
    }


    /**
     * 复制子树结构到指定根节点下
     *
     * @param rootNode   目标根节点，新复制的子树将作为其子节点
     * @param sourceNode 源节点，以此节点为根的整个子树将被复制
     * @param handler    用于处理延迟执行的处理器
     * @param delay      延迟时间，单位为毫秒，用于控制递归复制的时间间隔
     */
    @Override
    public void onCopySubtree(@NonNull NodeModel<T> rootNode, @NonNull NodeModel<T> sourceNode, @NonNull Handler handler, long delay, boolean start) {

        // 创建新的根节点，基于源节点的值构建
        NodeModel<T> newRoot = new NodeModel(sourceNode.getValue().neuBuild());

        // 将新创建的节点添加到目标根节点的子节点中
        onAddNodes(rootNode, newRoot);

        if (start) {
//            mCacheStep.addStep(newRoot, CacheStep.STATUS_ADD);
        }

        // 延迟递归复制源节点的所有子节点
        handler.postDelayed(() -> {
            for (NodeModel<T> child : sourceNode.childNodes) {
                onCopySubtree(newRoot, child, handler, delay, false);
            }
        }, delay);
    }

    /**
     * 根据节点ID定位节点位置
     *
     * @param nodeId 要查找的节点ID
     * @return 返回找到的节点模型，如果未找到则返回null
     */
    @Override
    public NodeModel<T> locateNodePosition(String nodeId) {
        // 获取树模型和根节点
        TreeModel<T> treeModel = adapter.getTreeModel();
        NodeModel<T> root = treeModel.getRootNode();

        // 首先检查根节点是否匹配
        if (TextUtils.equals(root.value.itemId, nodeId)) {
            return root;
        }

        // 递归查找子节点
        return findChildNode(root, nodeId);
    }


    public void copySubtree(@NonNull NodeModel<T> rootNode, @NonNull NodeModel<T> sourceNode, @NonNull Handler handler, long delay) {
        mCopySuccess = false;
        onCopySubtree(rootNode, sourceNode, handler, delay, true);
        mCopySuccess = true;
    }

    /**
     * 获取复制操作是否成功的状态
     *
     * @return 复制成功返回true，否则返回false
     */
    public boolean isCopySuccess() {
        return mCopySuccess;
    }

    /**
     * 当节点被移除时的回调方法
     *
     * @param nodeModel 被移除的节点模型，不能为空
     */
    @Override
    public void onRemoveNode(NodeModel<T> nodeModel) {
        innerRemoveNode(nodeModel, false);
    }

    /**
     * 当子节点被批量移除时的回调方法
     *
     * @param parentNode 父节点模型，其下的子节点将被移除，不能为空
     */
    @Override
    public void onRemoveChildNodes(NodeModel<T> parentNode) {
        innerRemoveNode(parentNode, true);
    }

    /**
     * 当节点视图需要更新时的回调方法
     *
     * @param nodeModel 需要更新视图的节点模型，不能为空
     */
    @Override
    public void onItemViewChange(NodeModel<T> nodeModel) {
        if (adapter == null || nodeModel == null) {
            return;
        }
        // 获取当前节点的 ViewHolder
        TreeViewHolder<T> holder = getTreeViewHolder(nodeModel);
        if (holder != null) {
            // 重新绑定数据到现有 View
            TreeViewLog.d(TAG, "onItemViewChange: 111");
            adapter.bindViewHolder(holder);
        } else {
            TreeViewLog.d(TAG, "onItemViewChange: 112");
            // 当 ViewHolder 不存在时，重新计算布局并请求重新布局
            mTreeLayoutManager.calculateByLayoutAlgorithm(mTreeModel);
            requestLayout();
        }
    }

    /**
     * 查找指定节点下的子节点
     *
     * @param parent 父节点对象，用于搜索其子节点
     * @param nodeId 要查找的节点ID
     * @return 找到的子节点对象，如果未找到则返回null
     */
    private NodeModel<T> findChildNode(NodeModel<T> parent, String nodeId) {
        // 遍历父节点的所有直接子节点
        for (NodeModel<T> child : parent.childNodes) {
            // 检查当前子节点是否匹配目标节点ID
            if (TextUtils.equals(child.value.itemId, nodeId)) {
                return child;
            }
            // 递归搜索当前子节点的子树
            return findChildNode(child, nodeId);
        }
        // 未找到匹配的子节点
        return null;
    }


    /**
     * 内部移除节点的方法
     *
     * @param nodeModel              要移除的节点模型
     * @param isRemoveChildNodesOnly 是否仅移除子节点，true表示只移除子节点，false表示移除整个节点及其子节点
     */
    private void innerRemoveNode(NodeModel<T> nodeModel, boolean isRemoveChildNodesOnly) {
        if (adapter != null) {
            // 如果需要执行动画移除
            if (isAnimateRemove()) {
                // 记录锚点位置
                recordAnchorLocationOnViewPort(true, isRemoveChildNodesOnly, nodeModel);
                if (isRemoveChildNodesOnly) {
                    // 遍历直接子节点并从树模型中移除
                    nodeModel.traverseDirectChildren(next -> adapter.getTreeModel().removeNode(nodeModel, next));
                } else {
                    // 从父节点中移除当前节点
                    adapter.getTreeModel().removeNode(nodeModel.getParentNode(), nodeModel);
                }
            } else {
                // 不执行动画的移除操作
                if (isRemoveChildNodesOnly) {
                    // 遍历除自身外的所有节点并移除视图，然后遍历直接子节点并从树模型中移除
                    nodeModel.traverseExcludeSelf(this::removeViewByNode);
                    nodeModel.traverseDirectChildren(next -> adapter.getTreeModel().removeNode(nodeModel, next));
                } else {
                    // 遍历包含自身在内的所有节点并移除视图，然后从树模型中移除当前节点
                    nodeModel.traverseIncludeSelf(this::removeViewByNode);
                    adapter.getTreeModel().removeNode(nodeModel.getParentNode(), nodeModel);
                }
            }
            // 重新计算布局并请求重新布局
            mTreeLayoutManager.calculateByLayoutAlgorithm(mTreeModel);
            requestLayout();
        }
    }


    /**
     * Remove View By Node
     *
     * @param nodeModel node to remove the view
     */
    private void removeViewByNode(NodeModel<T> nodeModel) {
        //remove view
        TreeViewHolder<T> holder = getTreeViewHolder(nodeModel);
        if (holder != null) {
            removeView(holder.getView());
            recycleHolder(holder);
        }
    }

    /**
     * 准备移动、添加或删除节点的操作，记录视口中的最后一个节点作为锚点节点，以使界面变化看起来更平滑。
     *
     * @param isRemove             是否是删除操作
     * @param isRemoveChildrenOnly 是否仅删除子节点（当 isRemove 为 true 时有效）
     * @param targetNode           目标节点
     */
    private void recordAnchorLocationOnViewPort(boolean isRemove, boolean isRemoveChildrenOnly, NodeModel<T> targetNode) {
        if (targetNode == null) {
            return;
        }

        // 如果是删除操作，则收集将要被删除的节点及其对应的视图，并保存到 tag 中
        if (isRemove) {
            Map<NodeModel<T>, View> removeNodeMap = new HashMap<>();
            if (isRemoveChildrenOnly) {
                // 只遍历目标节点的子节点（不包括自身）
                targetNode.traverseExcludeSelf(node -> {
                    removeNodeMap.put(node, getTreeViewHolder(node).getView());
                });
            } else {
                // 遍历目标节点及其所有子节点（包括自身）
                targetNode.traverseIncludeSelf(node -> {
                    removeNodeMap.put(node, getTreeViewHolder(node).getView());
                });
                // 删除后，使用父节点作为新的目标节点进行后续处理
                targetNode = targetNode.getParentNode();
            }
            setTag(R.id.mark_remove_views, removeNodeMap);
        }

        // 记录目标节点在视口中的位置信息以及其它节点相对于该目标节点的位置信息
        if (targetNode != null) {
            TreeViewHolder<T> targetHolder = getTreeViewHolder(targetNode);
            if (targetHolder != null) {
                View targetHolderView = targetHolder.getView();
                // 设置选中状态下的 Z 轴高度
                targetHolderView.setElevation(Z_SELECT);

                ViewBox targetBox = ViewBox.getViewBox(targetHolderView);
                // 获取目标节点相对于当前视图矩阵的视口坐标
                ViewBox targetBoxOnViewport = targetBox.convert(getMatrix());

                // 保存目标节点及它在视口中的位置
                setTag(R.id.target_node, targetNode);
                setTag(R.id.target_location_on_viewport, targetBoxOnViewport);

                // 收集其他所有节点相对于目标节点的位置偏移量
                Map<NodeModel<T>, ViewBox> relativeLocationMap = new HashMap<>();
                mTreeModel.doTraversalNodes(node -> {
                    TreeViewHolder<T> oneHolder = getTreeViewHolder(node);
                    ViewBox relativeBox =
                            oneHolder != null ?
                                    ViewBox.getViewBox(oneHolder.getView()).subtract(targetBox) :
                                    new ViewBox();
                    relativeLocationMap.put(node, relativeBox);
                });

                // 将相对位置映射关系存储到 tag 中供后续使用
                setTag(R.id.relative_locations, relativeLocationMap);
            }
        }
    }

    /**
     * 创建或复用TreeViewHolder实例
     *
     * @param node 节点数据模型，用于创建或配置ViewHolder
     * @return 返回配置好的TreeViewHolder实例
     */
    private TreeViewHolder<T> createHolder(NodeModel<T> node) {
        // 获取节点对应的holder类型
        int type = adapter.getHolderType(node);
        HolderPool holderPool = holderPools.get(type);

        // 检查对应类型的holder池是否存在
        if (holderPool == null) {
            // 如果holder池不存在，则创建新的holder池并存储
            holderPool = new HolderPool();
            holderPools.put(type, holderPool);
        } else {
            // 尝试从holder池中获取可复用的holder实例
            TreeViewHolder<T> holder = (TreeViewHolder<T>) holderPool.obtain();
            if (holder != null) {
                // 复用成功，设置节点数据并返回
                holder.setNode(node);
                return holder;
            }
        }
        // holder池中无可用实例，通过adapter创建新的ViewHolder
        return adapter.onCreateViewHolder(this, node);
    }


    /**
     * 回收指定的TreeViewHolder到对应的缓存池中
     *
     * @param holder 需要被回收的TreeViewHolder实例，不能为空
     */
    public void recycleHolder(TreeViewHolder<T> holder) {
        // 根据holder关联的节点获取对应的holder类型
        int type = adapter.getHolderType(holder.getNode());

        // 从holderPools中获取对应类型的HolderPool，如果不存在则创建新的
        HolderPool holderPool = holderPools.get(type);
        if (holderPool == null) {
            holderPool = new HolderPool();
            holderPools.put(type, holderPool);
        }

        // 将holder放入对应的缓存池中进行复用管理
        holderPool.free(holder);
    }


    public void setAnimateRemove(boolean animateRemove) {
        isAnimateRemove = animateRemove;
    }

    public void setAnimateAdd(boolean animateAdd) {
        isAnimateAdd = animateAdd;
    }

    public void setAnimateMove(boolean animateMove) {
        isAnimateMove = animateMove;
    }

    public boolean isAnimateRemove() {
        return isAnimateRemove;
    }

    public boolean isAnimateAdd() {
        return isAnimateAdd;
    }

    public boolean isAnimateMove() {
        return isAnimateMove;
    }

    public void setControlListener(TreeViewControlListener<T> controlListener) {
        this.controlListener = controlListener;
    }

    public void release() {
        holderPools.clear();
        nodeViewMap.clear();
        mCacheStep.init();
        adapter = null;
        dragBlock.release();
        controlListener = null;
        onNodeClickListener = null;
    }
}
