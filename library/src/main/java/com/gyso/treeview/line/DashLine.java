/**
 * @Author: 怪兽N
 * @Time: 2021/5/8  17:41
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 * @Describe: DashPathEffect line
 */
package com.gyso.treeview.line;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
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
 * 虚线绘制类，继承自BaseLine，用于绘制虚线连接线
 */
public class DashLine extends BaseLine {
    /**
     * 默认线宽（dp单位）
     */
    public static final int DEFAULT_LINE_WIDTH_DP = 3;

    /**
     * 线条颜色，默认为#E57373
     */
    private int lineColor = Color.parseColor("#E57373");

    /**
     * 线条宽度（dp单位）
     */
    private int lineWidth = DEFAULT_LINE_WIDTH_DP;

    /**
     * 虚线路径效果，数组{20,10}表示实线段长度20，间隔10，偏移量10
     */
    private final static DashPathEffect effect = new DashPathEffect(new float[]{20, 10}, 10);

    /**
     * 无参构造函数
     */
    public DashLine() {
        super();
    }

    /**
     * 带参构造函数
     * @param lineColor 线条颜色
     * @param lineWidth_dp 线条宽度（dp单位）
     */
    public DashLine(int lineColor, int lineWidth_dp) {
        this();
        this.lineColor = lineColor;
        this.lineWidth = lineWidth_dp;
    }

    /**
     * 设置线条颜色
     * @param lineColor 线条颜色
     */
    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * 设置线条宽度
     * @param lineWidth 线条宽度
     */
    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * 绘制虚线连接线
     * @param drawInfo 绘制信息对象，包含画布、画笔、路径等绘制所需信息
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

        //获取视图和节点信息
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
        //根据布局类型计算不同方向的起始点、控制点和终点
        if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_HORIZON_RIGHT) {
            startPoint = PointPool.obtain(fromView.getRight(), (fromView.getTop() + fromView.getBottom()) / 2f);
            point1 = PointPool.obtain(startPoint.x + DensityUtils.dp2px(context, 15), startPoint.y);
            endPoint = PointPool.obtain(toView.getLeft(), (toView.getTop() + toView.getBottom()) / 2f);
            point2 = PointPool.obtain(startPoint.x, endPoint.y);
        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_HORIZON_LEFT) {
            startPoint = PointPool.obtain(fromView.getLeft(), (fromView.getTop() + fromView.getBottom()) / 2f);
            point1 = PointPool.obtain(startPoint.x - DensityUtils.dp2px(context, 15), startPoint.y);
            endPoint = PointPool.obtain(toView.getRight(), (toView.getTop() + toView.getBottom()) / 2f);
            point2 = PointPool.obtain(startPoint.x, endPoint.y);
        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_VERTICAL_DOWN) {
            startPoint = PointPool.obtain((fromView.getLeft() + fromView.getRight()) / 2f, fromView.getBottom());
            point1 = PointPool.obtain(startPoint.x, startPoint.y + DensityUtils.dp2px(context, 15));
            endPoint = PointPool.obtain((toView.getLeft() + toView.getRight()) / 2f, toView.getTop());
            point2 = PointPool.obtain(endPoint.x, startPoint.y);
        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_VERTICAL_UP) {
            startPoint = PointPool.obtain((fromView.getLeft() + fromView.getRight()) / 2f, fromView.getTop());
            point1 = PointPool.obtain(startPoint.x, startPoint.y - DensityUtils.dp2px(context, 15));
            endPoint = PointPool.obtain((toView.getLeft() + toView.getRight()) / 2f, toView.getBottom());
            point2 = PointPool.obtain(endPoint.x, startPoint.y);
        } else {
            super.draw(drawInfo);
            return;
        }

        mPaint.reset();
        mPath.reset();
        //设置画笔属性
        mPaint.setColor(lineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(DensityUtils.dp2px(context, lineWidth));
        mPaint.setAntiAlias(true);
        mPaint.setPathEffect(effect);

        mPath.moveTo(startPoint.x, startPoint.y);
        mPath.cubicTo(
                point1.x, point1.y,
                point2.x, point2.y,
                endPoint.x, endPoint.y);

        //释放点对象到对象池
        PointPool.free(startPoint);
        PointPool.free(point1);
        PointPool.free(point2);
        PointPool.free(endPoint);
        //绘制路径
        canvas.drawPath(mPath, mPaint);
    }
}
