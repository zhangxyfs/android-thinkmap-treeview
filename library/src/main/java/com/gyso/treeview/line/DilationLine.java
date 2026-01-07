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

/**
 * 扩张的线 仿xmind
 */
public class DilationLine extends BaseLine {
    public static final int DEFAULT_LINE_WIDTH_DP = 3;
    private int lineColor = Color.parseColor("#ff000000");
    private int lineWidth = DEFAULT_LINE_WIDTH_DP;

    public DilationLine() {
        super();
    }

    public DilationLine(int lineColor, int lineWidth_dp) {
        this();
        this.lineColor = lineColor;
        this.lineWidth = lineWidth_dp;
    }

    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    @Override
    public void draw(DrawInfo drawInfo) {
        Canvas canvas = drawInfo.getCanvas();
        TreeViewHolder<?> fromHolder = drawInfo.getFromHolder();
        TreeViewHolder<?> toHolder = drawInfo.getToHolder();
        Paint mPaint = drawInfo.getPaint();
        Path mPath = drawInfo.getPath();
        int holderLayoutType = toHolder.getHolderLayoutType();

        //get view and node
        View fromView = fromHolder.getView();
        NodeModel<?> fromNode = fromHolder.getNode();
        View toView = toHolder.getView();
        NodeModel<?> toNode = toHolder.getNode();
        Context context = fromView.getContext();

        float toViewWidth = toView.getRight() - toView.getLeft();
        float toViewHeight = toView.getBottom() - toView.getTop();

        if (holderLayoutType == TreeLayoutManager.LAYOUT_TYPE_HORIZON_RIGHT) {
            if (toView.getTop() + toViewHeight / 2f > fromView.getTop()) {
                PointF startPoint = PointPool.obtain((fromView.getLeft() + fromView.getRight()) / 2f, fromView.getTop());
                PointF controlPoint = PointPool.obtain(fromView.getLeft() + fromView.getWidth() * 3 / 4f, toView.getTop() - toViewHeight / 2f);
                PointF endPoint = PointPool.obtain(toView.getLeft(), (toView.getTop() + toView.getBottom()) / 2f);
                dealQuadTo(context, canvas, mPaint, mPath, startPoint, controlPoint, endPoint);
            } else if (toView.getTop() + toViewHeight / 2f < fromView.getBottom()) {
                PointF startPoint = PointPool.obtain((fromView.getLeft() + fromView.getRight()) / 2f, fromView.getBottom());
                PointF controlPoint = PointPool.obtain(fromView.getLeft() + fromView.getWidth() * 3 / 4f, toView.getTop() + toViewHeight / 2f);
                PointF endPoint = PointPool.obtain(toView.getLeft(), (toView.getTop() + toView.getBottom()) / 2f);
                dealQuadTo(context, canvas, mPaint, mPath, startPoint, controlPoint, endPoint);
            } else {
                PointF startPoint, point1, endPoint, point2;
                startPoint = PointPool.obtain(fromView.getRight(), (fromView.getTop() + fromView.getBottom()) / 2f);
                point1 = PointPool.obtain(startPoint.x + DensityUtils.dp2px(context, 15), startPoint.y);
                endPoint = PointPool.obtain(toView.getLeft(), (toView.getTop() + toView.getBottom()) / 2f);
                point2 = PointPool.obtain(startPoint.x, endPoint.y);
                dealCubicTo(context, canvas, mPaint, mPath, startPoint, point1, point2, endPoint);
            }

        }  else {
            super.draw(drawInfo);
            return;
        }
    }

    private void dealQuadTo(Context context, Canvas canvas, Paint mPaint, Path mPath, PointF startPoint, PointF controlPoint, PointF endPoint) {
        mPaint.reset();
        mPath.reset();
        //set paint
        mPaint.setColor(lineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(DensityUtils.dp2px(context, lineWidth));
        mPaint.setAntiAlias(true);
        mPath.moveTo(startPoint.x, startPoint.y);
        mPath.quadTo(controlPoint.x, controlPoint.y, endPoint.x, endPoint.y);
        //do not forget release
        PointPool.free(startPoint);
        PointPool.free(controlPoint);
        PointPool.free(endPoint);
        //draw
        canvas.drawPath(mPath, mPaint);
    }

    private void dealCubicTo(Context context, Canvas canvas, Paint mPaint, Path mPath, PointF startPoint, PointF point1, PointF point2, PointF endPoint){
        mPaint.reset();
        mPath.reset();
        //set paint
        mPaint.setColor(lineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(DensityUtils.dp2px(context, lineWidth));
        mPaint.setAntiAlias(true);
        mPath.moveTo(startPoint.x, startPoint.y);
        mPath.cubicTo(point1.x, point1.y, point2.x, point2.y, endPoint.x, endPoint.y);
        //do not forget release
        PointPool.free(startPoint);
        PointPool.free(point1);
        PointPool.free(point2);
        PointPool.free(endPoint);
        //draw
        canvas.drawPath(mPath, mPaint);
    }
}
