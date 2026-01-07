/**
 * Created by Z.Pan on 2016/10/9.
 */

package com.gyso.treeview.algorithm.force;

/**
 * FLink类表示连接两个FNode节点的链接
 */
public class FLink {

    FNode source;
    FNode target;
    private String text;

    /**
     * 构造函数，创建一个连接源节点和目标节点的链接
     * @param source 源节点
     * @param target 目标节点
     */
    public FLink(FNode source, FNode target) {
        this.source = source;
        this.target = target;
    }

    /**
     * 获取链接的文本内容
     * @return 链接的文本内容
     */
    public String getText() {
        return text;
    }

    /**
     * 设置链接的文本内容
     * @param text 链接的文本内容
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * 计算两个节点之间的欧几里得距离
     * @return 两个节点之间的距离
     */
    double getNodeDistance() {
        // 计算x坐标差值
        float dx = source.x - target.x;
        // 计算y坐标差值
        float dy = source.y - target.y;
        // 使用勾股定理计算两点间距离
        return Math.sqrt(dx * dx + dy * dy);
    }

}

