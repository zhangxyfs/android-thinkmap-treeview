/**
 * @Author: 怪兽N
 * @Time: 2021/5/8  9:47
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 */
package com.gyso.treeview.line;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.View;

import com.gyso.treeview.adapter.DrawInfo;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.cache_pool.PointPool;
import com.gyso.treeview.layout.TreeLayoutManager;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.util.DensityUtils;
import com.gyso.treeview.util.NodeCheck;
/**
 * 平滑线条类，继承自BaseLine
 * 用于绘制具有平滑效果的线条图形元素
 */
public class SmoothLine extends BaseLine {
    /**
     * 默认线条宽度，单位为dp（密度无关像素），值为3dp
     */
    public static final int DEFAULT_LINE_WIDTH_DP = 3;

    /**
     * 线条颜色，使用Color.parseColor解析的颜色值#055287（深蓝色）
     */
    private int lineColor = Color.parseColor("#055287");

    /**
     * 线条宽度，初始化为默认线条宽度
     */
    private int lineWidth = DEFAULT_LINE_WIDTH_DP;


    /**
     * 默认构造函数，初始化一个平滑连线对象。
     */
    public SmoothLine() {
        super();
    }

    /**
     * 带颜色和宽度参数的构造函数。
     *
     * @param lineColor     连线的颜色（ARGB格式）
     * @param lineWidth_dp  连线的宽度（单位：dp）
     */
    public SmoothLine(int lineColor, int lineWidth_dp) {
        this();
        this.lineColor = lineColor;
        this.lineWidth = lineWidth_dp;
    }

    /**
     * 设置连线颜色。
     *
     * @param lineColor 连线的新颜色（ARGB格式）
     */
    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * 设置连线宽度。
     *
     * @param lineWidth 连线的新宽度（单位：dp）
     */
    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * 绘制两个节点之间的平滑曲线连接线。
     * <p>
     * 根据布局方向（水平向右、水平向左、垂直向下、垂直向上）选择不同的控制点计算方式，
     * 并使用三次贝塞尔曲线进行绘制。若起始节点处于收缩状态，则不执行绘制操作。
     *
     * @param drawInfo 包含绘图上下文信息的对象，包括画布、起始与目标视图持有者、画笔、路径等
     */
    @Override
    public void draw(DrawInfo drawInfo) {
        Canvas canvas = drawInfo.getCanvas();
        TreeViewHolder<?> fromHolder = drawInfo.getFromHolder();
        TreeViewHolder<?> toHolder = drawInfo.getToHolder();
        Paint mPaint = drawInfo.getPaint();
        Path mPath = drawInfo.getPath();
        int holderLayoutType = toHolder.getHolderLayoutType();
        int spacePeerToPeer = drawInfo.getSpacePeerToPeer();
        int spaceParentToChild = drawInfo.getSpaceParentToChild();

        // 获取起始和结束视图及其对应的节点模型
        View fromView = fromHolder.getView();
        NodeModel<?> fromNode = fromHolder.getNode();
        View toView = toHolder.getView();
        NodeModel<?> toNode = toHolder.getNode();
        Context context = fromView.getContext();

        // modify by zxy 2025/12/10 16:19 判断父节点是否contract, 如果为收缩状态那么不再绘制线条
        if (fromNode.isContract() || NodeCheck.parentNodeContract(fromNode)) {
            return;
        }

        PointF startPoint, point1, endPoint, point2;

        /**
         * 根据不同的布局类型，计算连接两个视图（fromView 和 toView）所需的路径点坐标。
         * 该方法主要用于树形结构布局中节点之间的连线绘制逻辑。
         *
         * @param holderLayoutType 布局类型，决定连线的方向和样式，
         *                         取值来自 TreeLayoutManager 的 LAYOUT_TYPE_* 常量。
         * @param fromView         连线起始的视图。
         * @param toView           连线终点的视图。
         * @param context          上下文对象，用于进行 dp 到 px 的转换。
         */
        if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_HORIZON_RIGHT) {
            // 水平向右布局：从 fromView 的右侧中心出发，先向右延伸一段距离，再转向 toView 左侧中心
            startPoint = PointPool.obtain(fromView.getRight(), (fromView.getTop() + fromView.getBottom()) / 2f);
            point1 = PointPool.obtain(startPoint.x + DensityUtils.dp2px(context, 15), startPoint.y);
            endPoint = PointPool.obtain(toView.getLeft(), (toView.getTop() + toView.getBottom()) / 2f);
            point2 = PointPool.obtain(startPoint.x, endPoint.y);

        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_HORIZON_LEFT) {
            // 水平向左布局：从 fromView 的左侧中心出发，先向左延伸一段距离，再转向 toView 右侧中心
            startPoint = PointPool.obtain(fromView.getLeft(), (fromView.getTop() + fromView.getBottom()) / 2f);
            point1 = PointPool.obtain(startPoint.x - DensityUtils.dp2px(context, 15), startPoint.y);
            endPoint = PointPool.obtain(toView.getRight(), (toView.getTop() + toView.getBottom()) / 2f);
            point2 = PointPool.obtain(startPoint.x, endPoint.y);

        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_VERTICAL_DOWN) {
            // 垂直向下布局：从 fromView 的底部中心出发，先向下延伸一段距离，再转向 toView 顶部中心
            startPoint = PointPool.obtain((fromView.getLeft() + fromView.getRight()) / 2f, fromView.getBottom());
            point1 = PointPool.obtain(startPoint.x, startPoint.y + DensityUtils.dp2px(context, 15));
            endPoint = PointPool.obtain((toView.getLeft() + toView.getRight()) / 2f, toView.getTop());
            point2 = PointPool.obtain(endPoint.x, startPoint.y);

        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_VERTICAL_UP) {
            // 垂直向上布局：从 fromView 的顶部中心出发，先向上延伸一段距离，再转向 toView 底部中心
            startPoint = PointPool.obtain((fromView.getLeft() + fromView.getRight()) / 2f, fromView.getTop());
            point1 = PointPool.obtain(startPoint.x, startPoint.y - DensityUtils.dp2px(context, 15));
            endPoint = PointPool.obtain((toView.getLeft() + toView.getRight()) / 2f, toView.getBottom());
            point2 = PointPool.obtain(endPoint.x, startPoint.y);

        } else {
            // 不支持的布局类型时调用父类默认绘制逻辑
            super.draw(drawInfo);
            return;
        }

        mPaint.reset();
        mPath.reset();

        // 配置画笔属性并开始绘制路径
        mPaint.setColor(lineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(DensityUtils.dp2px(context, lineWidth));
        mPaint.setAntiAlias(true);
        mPath.moveTo(startPoint.x, startPoint.y);
        mPath.cubicTo(point1.x, point1.y, point2.x, point2.y, endPoint.x, endPoint.y);

        // 回收临时使用的PointF对象以减少内存分配开销
        PointPool.free(startPoint);
        PointPool.free(point1);
        PointPool.free(point2);
        PointPool.free(endPoint);

        // 执行实际绘制操作
        canvas.drawPath(mPath, mPaint);
    }

}
