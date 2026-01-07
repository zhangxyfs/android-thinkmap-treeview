/**
 * @Author: 怪兽N
 * @Time: 2021/5/7  21:02
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 */
package com.gyso.treeview.line;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import com.gyso.treeview.adapter.DrawInfo;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.util.DensityUtils;

public class BaseLine {
    /**
     * 绘制连接线，该方法在树形视图的onDispatchDraw方法中被调用
     * 用于绘制从一个节点到另一个节点的连接线
     *
     * @param drawInfo 绘制信息对象，包含画布、画笔、路径等绘制所需的信息
     *                 以及起始节点和目标节点的ViewHolder信息
     */
    public void draw(DrawInfo drawInfo){
        Canvas canvas = drawInfo.getCanvas();
        TreeViewHolder<?> fromHolder = drawInfo.getFromHolder();
        TreeViewHolder<?> toHolder = drawInfo.getToHolder();
        Paint mPaint = drawInfo.getPaint();
        Path mPath = drawInfo.getPath();

        //获取视图和节点信息
        View fromView = fromHolder.getView();
        View toView = toHolder.getView();
        Context context = fromView.getContext();

        //重置画笔和路径，并设置绘制样式
        mPaint.reset();
        mPath.reset();
        mPaint.setColor(Color.MAGENTA);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(DensityUtils.dp2px(context,3));
        mPaint.setAntiAlias(true);

        //计算起始节点和目标节点的中心坐标
        int fromCenterX = (fromView.getLeft()+fromView.getRight())/2;
        int fromCenterY = (fromView.getTop()+fromView.getBottom())/2;
        int toCenterX = (toView.getLeft()+toView.getRight())/2;
        int toCenterY = (toView.getTop()+toView.getBottom())/2;

        //设置路径起点和终点
        mPath.moveTo(fromCenterX, fromCenterY);
        mPath.lineTo(toCenterX, toCenterY);

        //绘制连接线
        canvas.drawPath(mPath,mPaint);
    }
}
