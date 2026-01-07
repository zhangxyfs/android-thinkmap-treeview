package com.gyso.treeview.touch;

import android.content.Context;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;

import com.gyso.treeview.TreeViewContainer;
import com.gyso.treeview.listener.TreeViewControlListener;
import com.gyso.treeview.util.TreeViewLog;
import com.gyso.treeview.util.ViewBox;

/**
 * handler the touch event and move or translate the view
 * guaishouN 674149099@qq.com
 */
/**
 * 触摸事件处理器，用于处理树形视图的触摸事件，包括拖拽、缩放和惯性滚动等操作
 */
public class TouchEventHandler {
    /**
     * 类的标签，用于调试日志
     */
    final static String TAG = TouchEventHandler.class.getSimpleName();

    /**
     * 最大缩放比例
     */
    public static float MAX_SCALE = 3f;

    /**
     * 最小缩放比例
     */
    public static float MIN_SCALE = 0.3f;

    /**
     * 触摸模式：未设置
     */
    private static final int TOUCH_MODE_UNSET = -1;

    /**
     * 触摸模式：释放
     */
    private static final int TOUCH_MODE_RELEASE = 0;

    /**
     * 触摸模式：单点触摸
     */
    private static final int TOUCH_MODE_SINGLE = 1;

    /**
     * 触摸模式：双点触摸
     */
    private static final int TOUCH_MODE_DOUBLE = 2;

    /**
     * 当前触摸模式
     */
    private int mode = 0;

    /**
     * 当前缩放因子
     */
    private float scaleFactor = 1.0f;

    /**
     * 缩放基准距离（双指距离）
     */
    private float scaleBaseR;

    /**
     * 手势检测器，用于检测滑动手势
     */
    private GestureDetector mGestureDetector;

    /**
     * 触摸阈值，用于判断是否开始拖拽
     */
    private float mTouchSlop;

    /**
     * 前一次移动触摸事件
     */
    private MotionEvent preMovingTouchEvent = null;

    /**
     * 前一次拦截触摸事件
     */
    private MotionEvent preInterceptTouchEvent = null;

    /**
     * 是否正在移动标志
     */
    private boolean mIsMoving;

    /**
     * 最小缩放比例
     */
    private float minScale = MIN_SCALE;

    /**
     * Y轴方向的惯性滚动动画
     */
    private FlingAnimation flingY = null;

    /**
     * X轴方向的惯性滚动动画
     */
    private FlingAnimation flingX = null;

    /**
     * 布局在父容器中的位置框
     */
    private ViewBox layoutLocationInParent = new ViewBox();

    /**
     * 视口框，用于边界限制
     */
    private final ViewBox viewportBox = new ViewBox();

    /**
     * 缩放前的焦点中心点
     */
    private PointF preFocusCenter = new PointF();

    /**
     * 缩放后的焦点中心点
     */
    private PointF postFocusCenter = new PointF();

    /**
     * 缩放前的平移量
     */
    private PointF preTranslate = new PointF();

    /**
     * 缩放前的缩放因子
     */
    private float preScaleFactor = 1f;

    /**
     * 惯性动画更新监听器，用于边界限制
     */
    private final DynamicAnimation.OnAnimationUpdateListener flingAnimateListener;

    /**
     * 是否保持在视口内
     */
    private boolean isKeepInViewport;

    /**
     * 树视图控制监听器
     */
    private TreeViewControlListener controlListener = null;

    /**
     * 触摸事件监听器
     */
    private TouchEventListener touchEventListener = null;

    /**
     * 用于控制监听器的缩放百分比，避免频繁回调
     */
    private int scalePercentOnlyForControlListener = 0;

    /**
     * 摩擦系数，影响惯性滚动的衰减速度
     */
    private float friction = 100f;

    /**
     * 触摸处理器监听器
     */
    private ToucheHandlerListener mToucheHandlerListener;

    /**
     * 构造函数，初始化触摸事件处理器
     *
     * @param context 上下文
     * @param listener 触摸处理器监听器
     */
    public TouchEventHandler(Context context, ToucheHandlerListener listener) {
        mToucheHandlerListener = listener;
        View mView = listener.needView();
        // 创建惯性动画更新监听器，用于边界限制
        flingAnimateListener = (animation, value, velocity) -> keepWithinBoundaries();
        // 初始化手势检测器，用于处理滑动手势
        mGestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        // 创建X轴方向的惯性滚动动画
                        flingX = new FlingAnimation(mView, DynamicAnimation.TRANSLATION_X);
                        flingX.setStartVelocity(velocityX)
                                .setFriction(friction)
                                .addUpdateListener(flingAnimateListener)
                                .start();

                        // 创建Y轴方向的惯性滚动动画
                        flingY = new FlingAnimation(mView, DynamicAnimation.TRANSLATION_Y);
                        flingY.setStartVelocity(velocityY)
                                .setFriction(friction)
                                .addUpdateListener(flingAnimateListener)
                                .start();
                        return false;
                    }
                });
        // 获取触摸阈值
        ViewConfiguration vc = ViewConfiguration.get(mView.getContext());
        mTouchSlop = vc.getScaledTouchSlop() * 0.8f;
    }

    /**
     * 设置视口大小
     * 注意：这里的窗口是指显示树形视图的视口，不是手机窗口
     *
     * @param winWidth 视口宽度
     * @param winHeight 视口高度
     */
    public void setViewport(int winWidth, int winHeight) {
        viewportBox.setValues(0, 0, winHeight, winWidth);
    }

    /**
     * 设置缩放比例限制
     *
     * @param maxScale 最大缩放比例
     * @param minScale 最小缩放比例
     */
    public void setScale(float maxScale, float minScale) {
        if (maxScale > 0) {
            MAX_SCALE = maxScale;
        }
        if (minScale > 0) {
            MIN_SCALE = minScale;
        }
    }

    /**
     * 获取当前缩放比例
     *
     * @return 当前缩放比例
     */
    public float getNowScale() {
        return scaleFactor;
    }

    /**
     * 检测是否应该拦截触摸事件
     *
     * @param event 触摸事件
     * @return true表示拦截
     */
    public boolean detectInterceptTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        onTouchEvent(event);
        if (action == MotionEvent.ACTION_DOWN) {
            preInterceptTouchEvent = MotionEvent.obtain(event);
            mIsMoving = false;
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mIsMoving = false;
        }
        if (action == MotionEvent.ACTION_MOVE && mTouchSlop < calculateMoveDistance(event, preInterceptTouchEvent)) {
            mIsMoving = true;
        }
        return mIsMoving;
    }

    /**
     * 处理触摸事件，包括拖拽和缩放
     *
     * @param event 触摸事件
     * @return true表示已消费该事件
     */
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        if (mToucheHandlerListener == null) return true;
        View mView = mToucheHandlerListener.needView();

        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mode = TOUCH_MODE_SINGLE;
                preMovingTouchEvent = MotionEvent.obtain(event);
                // 如果是树形视图容器，获取其最小缩放比例
                if (mView instanceof TreeViewContainer) {
                    minScale = ((TreeViewContainer) mView).getMinScale();
                }
                // 取消之前的惯性滚动动画
                if (flingX != null) {
                    flingX.cancel();
                }
                if (flingY != null) {
                    flingY.cancel();
                }
                // 通知控制监听器触摸移动事件
                if (controlListener != null) {
                    controlListener.onTouchMove(action);
                }
                break;
            case MotionEvent.ACTION_UP:
                mode = TOUCH_MODE_RELEASE;
                // 通知控制监听器触摸移动事件
                if (controlListener != null) {
                    controlListener.onTouchMove(action);
                }
                // 通知触摸事件监听器最后的触摸事件
                if (touchEventListener != null) {
                    touchEventListener.lastTouchEvent(event);
                }
                // 保持视图在边界内
                keepWithinBoundaries();
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                mode = TOUCH_MODE_UNSET;
                // 通知控制监听器触摸移动事件
                if (controlListener != null) {
                    controlListener.onTouchMove(action);
                }
                // 通知触摸事件监听器最后的触摸事件
                if (touchEventListener != null) {
                    touchEventListener.lastTouchEvent(event);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mode++;
                // 当进入双指模式时，记录初始状态
                if (mode >= TOUCH_MODE_DOUBLE) {
                    scaleFactor = preScaleFactor = mView.getScaleX();
                    preTranslate.set(mView.getTranslationX(), mView.getTranslationY());
                    scaleBaseR = (float) distanceBetweenFingers(event);
                    centerPointBetweenFingers(event, preFocusCenter);
                    centerPointBetweenFingers(event, postFocusCenter);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // 双指缩放处理
                if (mode >= TOUCH_MODE_DOUBLE) {
                    float scaleNewR = (float) distanceBetweenFingers(event);
                    centerPointBetweenFingers(event, postFocusCenter);
                    if (scaleBaseR <= 0) {
                        break;
                    }
                    // 使用平滑插值计算新的缩放比例
                    scaleFactor = (scaleNewR / scaleBaseR) * preScaleFactor * 0.15f + scaleFactor * 0.85f;
                    int scaleState = TreeViewControlListener.FREE_SCALE;
                    // 计算实际的最小缩放比例
                    float finalMinScale = isKeepInViewport ? minScale : minScale * 0.8f;
                    if (scaleFactor >= MAX_SCALE) {
                        scaleFactor = MAX_SCALE;
                        scaleState = TreeViewControlListener.MAX_SCALE;
                    } else if (scaleFactor <= finalMinScale) {
                        scaleFactor = finalMinScale;
                        scaleState = TreeViewControlListener.MIN_SCALE;
                    }
                    // 通知控制监听器缩放状态
                    if (controlListener != null) {
                        int current = (int) (scaleFactor * 100);
                        // 避免频繁回调
                        if (scalePercentOnlyForControlListener != current) {
                            scalePercentOnlyForControlListener = current;
                            controlListener.onScaling(scaleState, scalePercentOnlyForControlListener);
                        }
                    }
                    // 设置缩放中心点和缩放比例
                    mView.setPivotX(0);
                    mView.setPivotY(0);
                    mView.setScaleX(scaleFactor);
                    mView.setScaleY(scaleFactor);
                    // 计算并设置新的平移量
                    float tx = postFocusCenter.x - (preFocusCenter.x - preTranslate.x) * scaleFactor / preScaleFactor;
                    float ty = postFocusCenter.y - (preFocusCenter.y - preTranslate.y) * scaleFactor / preScaleFactor;
                    mView.setTranslationX(tx);
                    mView.setTranslationY(ty);

                } else if (mode == TOUCH_MODE_SINGLE) {
                    // 单指拖拽处理
                    float deltaX = event.getRawX() - preMovingTouchEvent.getRawX();
                    float deltaY = event.getRawY() - preMovingTouchEvent.getRawY();
                    float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    if (distance > mTouchSlop) {
                        onSinglePointMoving(deltaX, deltaY);
                    }
                }
                break;
            case MotionEvent.ACTION_OUTSIDE:
                TreeViewLog.e(TAG, "onTouchEvent: touch out side");
                break;
        }
        preMovingTouchEvent = MotionEvent.obtain(event);
        return true;
    }

    /**
     * 计算两个事件之间的移动距离
     *
     * @param event1 第一个事件
     * @param event2 第二个事件
     * @return 移动距离
     */
    private float calculateMoveDistance(MotionEvent event1, MotionEvent event2) {
        if (event1 == null || event2 == null) {
            return 0f;
        }
        float disX = Math.abs(event1.getRawX() - event2.getRawX());
        float disY = Math.abs(event1.getRawY() - event2.getRawY());
        return (float) Math.sqrt(disX * disX + disY * disY);
    }

    /**
     * 单点移动处理，即平移
     *
     * @param deltaX X轴移动距离
     * @param deltaY Y轴移动距离
     */
    private void onSinglePointMoving(float deltaX, float deltaY) {
        if (mToucheHandlerListener == null) return;
        View mView = mToucheHandlerListener.needView();
        float translationX = mView.getTranslationX() + deltaX;
        mView.setTranslationX(translationX);
        float translationY = mView.getTranslationY() + deltaY;
        mView.setTranslationY(translationY);
    }

    /**
     * 保持视图在边界内
     */
    private void keepWithinBoundaries() {
        if (!isKeepInViewport) {
            return;
        }
        if (mToucheHandlerListener == null) return;
        View mView = mToucheHandlerListener.needView();

        calculateBound();
        int dBottom = layoutLocationInParent.bottom - viewportBox.bottom;
        int dTop = layoutLocationInParent.top - viewportBox.top;
        int dLeft = layoutLocationInParent.left - viewportBox.left;
        int dRight = layoutLocationInParent.right - viewportBox.right;
        float translationX = mView.getTranslationX();
        float translationY = mView.getTranslationY();
        if (dLeft > 0) {
            mView.setTranslationX(translationX - dLeft);
        }
        if (dRight < 0) {
            mView.setTranslationX(translationX - dRight);
        }
        if (dBottom < 0) {
            mView.setTranslationY(translationY - dBottom);
        }
        if (dTop > 0) {
            mView.setTranslationY(translationY - dTop);
        }
    }

    /**
     * 计算移动时的边界
     * 当添加 (dX,dY) 时，视图相对于视口的位置
     */
    private void calculateBound() {
        if (mToucheHandlerListener == null) return;
        View v = mToucheHandlerListener.needView();
        float left = v.getLeft() * v.getScaleX() + v.getTranslationX();
        float top = v.getTop() * v.getScaleY() + v.getTranslationY();
        float right = v.getRight() * v.getScaleX() + v.getTranslationX();
        float bottom = v.getBottom() * v.getScaleY() + v.getTranslationY();
        layoutLocationInParent.setValues((int) top, (int) left, (int) bottom, (int) right);
    }

    /**
     * 计算两个手指之间的距离
     *
     * @param event 触摸事件
     * @return 两个手指之间的距离
     */
    private double distanceBetweenFingers(MotionEvent event) {
        if (event.getPointerCount() > 1) {
            float disX = Math.abs(event.getX(0) - event.getX(1));
            float disY = Math.abs(event.getY(0) - event.getY(1));
            return Math.sqrt(disX * disX + disY * disY);
        }
        return 1;
    }

    /**
     * 计算两个手指的中心点
     *
     * @param event 触摸事件
     * @param point 存储中心点的PointF对象
     */
    private void centerPointBetweenFingers(MotionEvent event, PointF point) {
        float xPoint0 = event.getX(0);
        float yPoint0 = event.getY(0);
        float xPoint1 = event.getX(1);
        float yPoint1 = event.getY(1);
        point.set((xPoint0 + xPoint1) / 2f, (yPoint0 + yPoint1) / 2f);
    }

    /**
     * 设置是否保持视图在视口内
     *
     * @param keepInViewport true表示保持在视口内
     */
    public void setKeepInViewport(boolean keepInViewport) {
        isKeepInViewport = keepInViewport;
    }

    /**
     * 设置控制监听器
     *
     * @param controlListener 控制监听器
     */
    public void setControlListener(TreeViewControlListener controlListener) {
        this.controlListener = controlListener;
    }

    /**
     * 设置触摸事件监听器
     *
     * @param touchEventListener 触摸事件监听器
     */
    public void setTouchEventListener(TouchEventListener touchEventListener) {
        this.touchEventListener = touchEventListener;
    }

    /**
     * 设置摩擦系数
     *
     * @param friction 摩擦系数
     */
    public void setFriction(float friction) {
        this.friction = friction;
        if (flingX != null) {
            flingX.setFriction(friction);
        }
        if (flingY != null) {
            flingY.setFriction(friction);
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        controlListener = null;
        touchEventListener = null;
        mToucheHandlerListener = null;
    }

    /**
     * 触摸事件监听器接口
     */
    public interface TouchEventListener {
        /**
         * 最后的触摸事件
         *
         * @param event 触摸事件
         */
        void lastTouchEvent(MotionEvent event);
    }

    /**
     * 触摸处理器监听器接口
     */
    public interface ToucheHandlerListener {
        /**
         * 获取需要处理触摸事件的视图
         *
         * @return 需要处理触摸事件的视图
         */
        View needView();
    }
}
