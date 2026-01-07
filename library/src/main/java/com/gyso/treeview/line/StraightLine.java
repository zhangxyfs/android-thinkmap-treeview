/**
 * @Author: 怪兽N
 * @Time: 2021/5/8  9:40
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
 * 直线绘制类，继承自BaseLine，用于绘制连接树形结构节点的直线
 */
public class StraightLine extends BaseLine {
    /**
     * 默认线条宽度，单位为dp
     */
    public static final int DEFAULT_LINE_WIDTH_DP = 3;

    /**
     * 线条颜色，默认为#055287
     */
    private int lineColor = Color.parseColor("#055287");

    /**
     * 线条宽度，单位为dp
     */
    private int lineWidth = DEFAULT_LINE_WIDTH_DP;

    /**
     * 无参构造函数，调用父类构造函数初始化
     */
    public StraightLine() {
        super();
    }

    /**
     * 带参构造函数，用于初始化线条颜色和宽度
     *
     * @param lineColor    线条颜色值
     * @param lineWidth_dp 线条宽度，单位为dp
     */
    public StraightLine(int lineColor, int lineWidth_dp) {
        this();
        this.lineColor = lineColor;
        this.lineWidth = lineWidth_dp;
    }

    /**
     * 设置线条颜色
     *
     * @param lineColor 线条颜色值
     */
    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * 设置线条宽度
     *
     * @param lineWidth 线条宽度，单位为dp
     */
    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * 绘制直线连接线
     * 根据不同的布局类型计算起始点和结束点坐标，然后绘制直线
     *
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
        //get view and node
        View fromView = fromHolder.getView();
        NodeModel<?> fromNode = fromHolder.getNode();
        View toView = toHolder.getView();
        NodeModel<?> toNode = toHolder.getNode();
        Context context = fromView.getContext();

        // modify by zxy 2025/12/10 16:19 判断父节点是否contract, 如果为收缩状态那么不再绘制线条
        if (fromNode.isContract() || NodeCheck.parentNodeContract(fromNode)) {
            return;
        }


        PointF startPoint, endPoint;
        if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_HORIZON_RIGHT) {
            startPoint = PointPool.obtain(fromView.getRight(), (fromView.getTop() + fromView.getBottom()) / 2f);
            endPoint = PointPool.obtain(toView.getLeft(), (toView.getTop() + toView.getBottom()) / 2f);

        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_HORIZON_LEFT) {
            startPoint = PointPool.obtain(fromView.getLeft(), (fromView.getTop() + fromView.getBottom()) / 2f);
            endPoint = PointPool.obtain(toView.getRight(), (toView.getTop() + toView.getBottom()) / 2f);

        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_VERTICAL_UP) {
            startPoint = PointPool.obtain((fromView.getLeft() + fromView.getRight()) / 2f, fromView.getTop());
            endPoint = PointPool.obtain((toView.getLeft() + toView.getRight()) / 2f, toView.getBottom());

        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_VERTICAL_DOWN) {
            startPoint = PointPool.obtain((fromView.getLeft() + fromView.getRight()) / 2f, fromView.getBottom());
            endPoint = PointPool.obtain((toView.getLeft() + toView.getRight()) / 2f, toView.getTop());

        } else {
            super.draw(drawInfo);
            return;
        }

        //设置画笔属性
        mPaint.reset();
        mPath.reset();
        mPaint.setColor(lineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(DensityUtils.dp2px(context, lineWidth));
        mPaint.setAntiAlias(true);

        mPath.moveTo(startPoint.x, startPoint.y);
        mPath.lineTo(endPoint.x, endPoint.y);
        //释放点对象到对象池
        PointPool.free(startPoint);
        PointPool.free(endPoint);
        //draw
        canvas.drawPath(mPath, mPaint);
    }
}
