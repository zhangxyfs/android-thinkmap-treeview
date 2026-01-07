package com.gyso.treeview.algorithm.ring;

import android.graphics.Point;
import android.graphics.PointF;
import android.util.SparseIntArray;

import com.gyso.treeview.model.ITraversal;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.util.TreeViewLog;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 环形布局计算器，用于计算树形结构中节点的环形排列位置
 * @param <T> 节点类型，必须继承自NodeItem
 */
public class RingForSimple<T extends NodeItem> {
    public static final String TAG = RingForSimple.class.getSimpleName();
    private final static Map<TreeModel<?>, RingForSimple> RING_MAP = new HashMap<>();
    private final PointF center = new PointF();
    private final TreeModel<T> model;
    private final Map<NodeModel<T>, PointF> nodeModelPointFMap = new HashMap<>();
    private final Map<NodeModel<T>, Double> nodeModelAngleMap = new HashMap<>();
    protected SparseIntArray floorStart = new SparseIntArray(200);
    private final AtomicInteger multi = new AtomicInteger(1);
    private int maxDeep;
    private final Map<NodeModel<T>, Point> nodeWidthMap = new HashMap<>();

    /**
     * 私有构造函数，创建环形布局计算器实例
     * @param model 树形模型
     */
    private RingForSimple(TreeModel<T> model) {
        this.model = model;
    }

    /**
     * 获取环形布局计算器实例（单例模式）
     * @param model 树形模型
     * @return 环形布局计算器实例
     */
    public static RingForSimple getInstance(TreeModel<?> model) {
        if (model == null) {
            return null;
        }
        RingForSimple ring = RING_MAP.get(model);
        if (ring == null) {
            ring = new RingForSimple(model);
            RING_MAP.put(model, ring);
        }
        return ring;
    }

    /**
     * 设置环形布局的中心点坐标
     * @param x 中心点x坐标
     * @param y 中心点y坐标
     * @return 当前实例，支持链式调用
     */
    public RingForSimple setCenter(float x, float y) {
        center.x = x;
        center.y = y;
        TreeViewLog.e(TAG, "center[" + x + "," + y + "]");
        return this;
    }

    /**
     * 设置每层的起始半径
     * @param floorStart 每层对应的起始半径数组
     * @return 当前实例，支持链式调用
     */
    public RingForSimple setFloorStart(SparseIntArray floorStart) {
        this.floorStart = floorStart;
        return this;
    }

    /**
     * 重构树形结构，计算每个节点的深度信息
     * @param mTreeModel 需要重构的树形模型
     */
    public void reconstruction(TreeModel<?> mTreeModel) {
        TreeViewLog.e(TAG, "reconstruction start");
        nodeWidthMap.clear();
        Deque<NodeModel> deque = new ArrayDeque<>();
        NodeModel rootNode = mTreeModel.getRootNode();
        deque.add(rootNode);
        SparseIntArray deepSum = new SparseIntArray();
        //计算基础深度
        while (!deque.isEmpty()) {
            NodeModel cur = deque.poll();
            if (cur == null) {
                return;
            }
            cur.deep = deepSum.get(cur.floor, 0);
            record(cur);
            NodeModel tmp = cur.parentNode;
            while (!tmp.equals(rootNode)) {
                record(tmp);
                tmp = tmp.parentNode;
            }
            deepSum.put(cur.floor, cur.deep + 1);
            LinkedList childNodes = cur.getChildNodes();
            if (childNodes.size() > 0) {
                deque.addAll(childNodes);
            }
        }
    }

    /**
     * 记录节点的深度信息到映射表中
     * @param node 需要记录的节点模型
     */
    private void record(NodeModel<T> node) {
        if (node == null) {
            return;
        }
        Point point = nodeWidthMap.get(node);
        if (point == null) {
            point = new Point();
            nodeWidthMap.put(node, point);
        }
        maxDeep = Math.max(node.deep, maxDeep);
        point.x = Math.max(node.deep, point.x);
        point.y = Math.min(node.deep, point.y);
    }

    /**
     * 生成所有节点的环形排列位置坐标
     * @return 节点模型到坐标的映射表
     */
    public Map<NodeModel<T>, PointF> genPositions() {
        if (model == null) {
            return null;
        }
        NodeModel<T> rootNode = model.getRootNode();
        nodeModelPointFMap.clear();
        nodeModelPointFMap.put(rootNode, new PointF(center.y, center.x));
        int leafCount = maxDeep;
        LinkedList<? extends NodeModel<T>> rootNodeChildNodes = rootNode.getChildNodes();
        if (leafCount == 0 || rootNodeChildNodes.isEmpty()) {
            return nodeModelPointFMap;
        }

        //保持饼图中心对齐
        double pieAngle = 2f * Math.PI / leafCount;

        //遍历所有节点
        model.doTraversalNodes((ITraversal<NodeModel<T>>) next -> {
            if (next.equals(rootNode)) {
                return;
            }
            PointF pointF = new PointF();
            int deep = next.deep;
            int floor = next.floor;
            double angle = pieAngle * deep;
            LinkedList<? extends NodeModel<T>> childNodes = next.getChildNodes();
            if (!childNodes.isEmpty()) {
                Point point = nodeWidthMap.get(next);
                int count = point.x - point.y;
                if (count >= 2) {
                    double d = pieAngle * (count - 1) / 2f;
                    angle += d;
                }
            }
            float radius = floorStart.get(floor);
            TreeViewLog.e(TAG, "radius[" + radius + "]angle[" + angle + "]");
            pointF.x = (float) (radius * Math.sin(angle)) + center.y;
            pointF.y = (float) (radius * Math.cos(angle)) + center.x;
            nodeModelPointFMap.put(next, pointF);
            nodeModelAngleMap.put(next, angle);
        });
        return nodeModelPointFMap;
    }
}
