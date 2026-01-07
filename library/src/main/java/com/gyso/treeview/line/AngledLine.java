/**
 * @Author: 怪兽N
 * @Time: 2021/5/18  16:47
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 * @Describe:
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
 * 带角度的连接线类，继承自BaseLine，用于绘制树形结构中节点间的连接线
 * 支持水平左右、垂直上下等多种布局类型的连接线绘制
 * 90°直角
 */
public class AngledLine extends BaseLine {
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
     * 无参构造函数，使用默认的颜色和线条宽度
     */
    public AngledLine() {
        super();
    }

    /**
     * 带参构造函数，可自定义线条颜色和宽度
     *
     * @param lineColor    线条颜色值
     * @param lineWidth_dp 线条宽度，单位为dp
     */
    public AngledLine(int lineColor, int lineWidth_dp) {
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
     * 绘制连接线，根据不同的布局类型绘制带角度的连接线
     * 连接线采用四点路径：起点 -> 中间点1 -> 中间点2 -> 终点
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
        if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_HORIZON_RIGHT) {
            // 水平向右布局：从节点右侧中点到目标节点左侧中点的连接线
            startPoint = PointPool.obtain(fromView.getRight(), (fromView.getTop() + fromView.getBottom()) / 2f);
            point1 = PointPool.obtain(startPoint.x + spaceParentToChild / 3f, startPoint.y);
            endPoint = PointPool.obtain(toView.getLeft(), (toView.getTop() + toView.getBottom()) / 2f);
            point2 = PointPool.obtain(startPoint.x + spaceParentToChild / 3f, endPoint.y);

        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_HORIZON_LEFT) {
            // 水平向左布局：从节点左侧中点到目标节点右侧中点的连接线
            startPoint = PointPool.obtain(fromView.getLeft(), (fromView.getTop() + fromView.getBottom()) / 2f);
            point1 = PointPool.obtain(startPoint.x - spaceParentToChild / 3f, startPoint.y);
            endPoint = PointPool.obtain(toView.getRight(), (toView.getTop() + toView.getBottom()) / 2f);
            point2 = PointPool.obtain(startPoint.x - spaceParentToChild / 3f, endPoint.y);

        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_VERTICAL_DOWN) {
            // 垂直向下布局：从节点底部中点到目标节点顶部中点的连接线
            startPoint = PointPool.obtain((fromView.getLeft() + fromView.getRight()) / 2f, fromView.getBottom());
            point1 = PointPool.obtain(startPoint.x, startPoint.y + spaceParentToChild / 3f);
            endPoint = PointPool.obtain((toView.getLeft() + toView.getRight()) / 2f, toView.getTop());
            point2 = PointPool.obtain(endPoint.x, startPoint.y + spaceParentToChild / 3f);

        } else if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_VERTICAL_UP) {
            // 垂直向上布局：从节点顶部中点到目标节点底部中点的连接线
            startPoint = PointPool.obtain((fromView.getLeft() + fromView.getRight()) / 2f, fromView.getTop());
            point1 = PointPool.obtain(startPoint.x, startPoint.y - spaceParentToChild / 3f);
            endPoint = PointPool.obtain((toView.getLeft() + toView.getRight()) / 2f, toView.getBottom());
            point2 = PointPool.obtain(endPoint.x, startPoint.y - spaceParentToChild / 3f);

        } else {
            super.draw(drawInfo);
            return;
        }

        //设置画笔属性
        mPath.reset();
        mPaint.reset();
        mPaint.setColor(lineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(DensityUtils.dp2px(context, lineWidth));
        mPaint.setAntiAlias(true);

        //构建连接线路径：起点 -> 中间点1 -> 中间点2 -> 终点
        mPath.moveTo(startPoint.x, startPoint.y);
        mPath.lineTo(point1.x, point1.y);
        mPath.lineTo(point2.x, point2.y);
        mPath.lineTo(endPoint.x, endPoint.y);

        //释放点对象到对象池
        PointPool.free(startPoint);
        PointPool.free(point1);
        PointPool.free(point2);
        PointPool.free(endPoint);
        //绘制路径
        canvas.drawPath(mPath, mPaint);
    }
}
