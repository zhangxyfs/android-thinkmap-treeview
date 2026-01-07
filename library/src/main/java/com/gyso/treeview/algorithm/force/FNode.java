/**
 * 力导向图中显示的节点。
 *
 * Created by Z.Pan on 2016/10/8.
 */
package com.gyso.treeview.algorithm.force;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public class FNode {

    /** 根节点级别 */
    public static final int ROOT_NODE_LEVEL = 0;

    /** 拖动开始状态标识 */
    static final short DRAG_START = 2;
    /** 拖动中状态标识 */
    static final short DRAG = 4;
    /** 拖动结束状态标识 */
    static final short DRAG_END = 6;

    /** 节点显示的内容 */
    private String text;
    /** 用来携带其他数据，如：该节点对应的数据实体 Bean，或数据库中的 _id */
    private Object obj;
    /** 节点级别 */
    private int    level;

    /** 当前坐标x值 */
    public float x;
    /** 当前坐标y值 */
    public float y;

    /** 前一个状态的x坐标 */
    float px;
    /** 前一个状态的y坐标 */
    float py;

    /** 根据子节点自动计算，weight 越大，该节点越不容易被拖动 */
    int weight;

    /** 节点半径 */
    private float radius = 50f;

    /** 节点状态，该状态决定了是否处于稳定状态 */
    private short state;

    /**
     * 构造函数，创建一个根节点级别的FNode对象
     * @param text 节点显示的文本内容
     */
    public FNode(String text) {
        this(text, 50f, ROOT_NODE_LEVEL);
    }

    /**
     * 构造函数，创建指定半径和级别的FNode对象
     * @param text 节点显示的文本内容
     * @param radius 节点半径
     * @param level 节点级别
     */
    public FNode(String text, float radius, int level) {
        this.text = text;
        this.radius = radius;
        this.level = level;
        x=y=-1f;
        weight=1;
    }

    /**
     * 设置节点携带的对象
     * @param obj 要设置的对象
     */
    public void setObj(Object obj) {
        this.obj = obj;
    }

    /**
     * 获取节点显示的文本内容
     * @return 节点显示的文本内容
     */
    public String getText() {
        return text;
    }

    /**
     * 获取节点携带的对象
     * @return 节点携带的对象
     */
    public Object getObj() {
        return obj;
    }

    /**
     * 获取节点级别
     * @return 节点级别
     */
    public int getLevel() {
        return level;
    }

    /**
     * 设置节点级别
     * @param level 要设置的节点级别
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * 获取节点半径
     * @return 节点半径
     */
    float getRadius() {
        return radius;
    }

    /**
     * 判断是否为根节点
     * @return true表示是根节点，false表示不是根节点
     */
    boolean isRootNode() {
        return level == ROOT_NODE_LEVEL;
    }

    /**
     * 给定一个坐标 (x, y)，判断该坐标是否在节点所在范围内。用来判断是否点击了该节点。
     * @param x x坐标
     * @param y y坐标
     * @param scale 缩放比例
     * @return true 表示 (x, y) 在该节点内部
     */
    boolean isInside(float x, float y, float scale) {
        float left = (this.x - radius) * scale;
        float top = (this.y - radius) * scale;
        float right = (this.x + radius) * scale;
        float bottom = (this.y + radius) * scale;
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    /**
     * 判断节点是否处于稳定状态
     * @return true表示节点处于稳定状态，false表示节点不稳定
     */
    boolean isStable() {
        return state != 0;
    }

    /**
     * 设置节点的状态是否正在被手指拖动。
     * @param state {@linkplain #DRAG_START} 开始拖动；{@linkplain #DRAG_END} 结束拖动。
     */
    void setDragState(@State short state) {
        switch (state) {
            case DRAG_START:
                this.state |= state;
                break;
            case DRAG_END:
                this.state &= ~state;
                break;
        }
    }

    @ShortDef({DRAG_START, DRAG, DRAG_END})
    @Retention(SOURCE)
    public @interface State {}

    @Retention(SOURCE)
    @Target({ANNOTATION_TYPE})
    public @interface ShortDef {
        short[] value() default {};
        boolean flag() default false;
    }

}
