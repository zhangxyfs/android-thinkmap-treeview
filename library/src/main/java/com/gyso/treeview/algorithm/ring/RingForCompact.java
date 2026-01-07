package com.gyso.treeview.algorithm.ring;

import android.graphics.PointF;
import android.util.SparseIntArray;

import com.gyso.treeview.model.ITraversal;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.util.TreeViewLog;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 环形紧凑布局类，用于计算树形结构节点的环形紧凑布局位置
 * @param <T> 节点项类型，必须继承自NodeItem
 */
public class RingForCompact<T extends NodeItem> {
    public static final String TAG = RingForCompact.class.getSimpleName();
    private final static Map<TreeModel, RingForCompact> RING_MAP = new HashMap<>();
    private final PointF center = new PointF();
    private final TreeModel<T> model;

    /**
     * 存储节点模型与其对应坐标的映射关系
     */
    private final Map<NodeModel<T>, PointF> nodeModelPointFMap = new HashMap<>();

    /**
     * 存储节点模型与其对应角度的映射关系
     */
    private final Map<NodeModel<T>, Double> nodeModelAngleMap = new HashMap<>();

    /**
     * 存储节点模型与其新半径的映射关系
     */
    private final Map<NodeModel<T>, Double> nodeModelNewRadius = new HashMap<>();

    /**
     * 存储节点模型与其新深度的映射关系
     */
    private final Map<NodeModel<T>, Integer> nodeModelNewDeep = new HashMap<>();

    /**
     * 存储每层的起始索引，用于分层管理
     */
    protected SparseIntArray floorStart = new SparseIntArray(200);

    /**
     * 最小角度值，用于角度范围计算
     */
    private double minAngle = 0;

    /**
     * 最大角度值，用于角度范围计算
     */
    private double maxAngle = 0;

    /**
     * 最小深度值，用于深度范围计算
     */
    private int minDeep = 0;

    /**
     * 最大深度值，用于深度范围计算
     */
    private int maxDeep = 0;


    /**
     * 私有构造函数，创建环形紧凑布局实例
     * @param model 树形模型
     */
    private RingForCompact(TreeModel<T> model) {
        this.model = model;
    }

    /**
     * 获取环形紧凑布局实例，使用单例模式
     * @param model 树形模型
     * @return 环形紧凑布局实例
     */
    public static RingForCompact getInstance(TreeModel<?> model) {
        if (model == null) {
            return null;
        }
        RingForCompact ring = RING_MAP.get(model);
        if (ring == null) {
            ring = new RingForCompact(model);
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
    public RingForCompact setCenter(float x, float y) {
        center.x = x;
        center.y = y;
        TreeViewLog.e(TAG, "center[" + x + "," + y + "]");
        return this;
    }

    /**
     * 设置每层的起始半径数组
     * @param floorStart 每层起始半径的SparseArray
     * @return 当前实例，支持链式调用
     */
    public RingForCompact setFloorStart(SparseIntArray floorStart) {
        this.floorStart = floorStart;
        return this;
    }

    /**
     * 生成节点的位置坐标映射
     * @return 节点模型到位置坐标的映射表
     */
    public Map<NodeModel<T>, PointF> genPositions() {
        if (model == null) {
            return null;
        }
        NodeModel<T> rootNode = model.getRootNode();
        nodeModelPointFMap.clear();
        nodeModelAngleMap.clear();
        nodeModelNewRadius.clear();
        nodeModelNewDeep.clear();
        minAngle = 2f * Math.PI;
        maxAngle = 0;
        minDeep = 0;
        maxDeep = 0;
        nodeModelPointFMap.put(rootNode, new PointF(center.y, center.x));
        int pieCount = model.getMaxDeep() - model.getMinDeep();
        LinkedList<? extends NodeModel<T>> rootNodeChildNodes = rootNode.getChildNodes();
        if (pieCount == 0 || rootNodeChildNodes.isEmpty()) {
            return nodeModelPointFMap;
        }
        //keep in pie center
        double pieAngle = 2f * Math.PI / pieCount;
        //doTraversalNodes
        model.doTraversalNodes((ITraversal<NodeModel<T>>) next -> {
            if (next.equals(rootNode)) {
                return;
            }
            TreeViewLog.e(TAG, next + " -gyso");
            NodeModel<T> nextParentNode = next.getParentNode();
            PointF pointF = nodeModelPointFMap.get(next);
            if (pointF == null) {
                pointF = new PointF();
                LinkedList<? extends NodeModel<T>> childNodes1 = next.getChildNodes();
                float deep = next.deep;
                if (!childNodes1.isEmpty()) {
                    float sum = 0;
                    for (NodeModel<T> child : childNodes1) {
                        sum += child.deep;
                    }
                    deep = sum / childNodes1.size();
                }

                int floor = next.floor;
                double angle = pieAngle * deep;
                float radius = floorStart.get(floor);
                TreeViewLog.e(TAG, "radius[" + radius + "]angle[" + angle + "]");
                pointF.x = (float) (radius * Math.sin(angle)) + center.y;
                pointF.y = (float) (radius * Math.cos(angle)) + center.x;
                nodeModelPointFMap.put(next, pointF);
                nodeModelAngleMap.put(next, angle);
                nodeModelNewRadius.put(next, (double) radius);
                recordMinMaxAngle(next);
                //keep acute angle for parent
                if (nextParentNode != null && !nextParentNode.equals(rootNode)) {
                    PointF parentPosition = nodeModelPointFMap.get(nextParentNode);
                    PointF rootPosition = nodeModelPointFMap.get(rootNode);
                    PointF currentPosition = nodeModelPointFMap.get(next);
                    double p2r = Math.hypot(parentPosition.x - rootPosition.x, parentPosition.y - rootPosition.y);
                    double c2r = Math.hypot(currentPosition.x - rootPosition.x, currentPosition.y - rootPosition.y);
                    double dAngle = Math.abs(nodeModelAngleMap.get(nextParentNode) - nodeModelAngleMap.get(next));
                    double l1 = c2r * Math.abs(Math.cos(dAngle));
                    TreeViewLog.e(TAG, "p2r[" + p2r + "]c2r[" + c2r + "]dAngle[" + dAngle + "]Math.sin(dAngle)[" + Math.sin(dAngle) + "]l1[" + l1 + "]l1 <= p2r[" + (l1 <= p2r) + "]");
                    //if(false){
                    if (l1 < p2r) {
                        TreeViewLog.e(TAG, "no acute children layout, should compact to acute!!");
                        //Math.PI for layout all children
                        LinkedList<? extends NodeModel<T>> childNodes = nextParentNode.getChildNodes();
                        double childAngle = Math.PI / childNodes.size();
                        float childRadius = (radius - floorStart.get(nextParentNode.floor)) * 2;
                        double h, w;
                        int count = 0;
                        for (NodeModel<T> child : childNodes) {
                            double sumAngle = Math.PI / 2 - (count + 1 / 2f) * childAngle;
                            h = childRadius * Math.sin(sumAngle);
                            w = childRadius * Math.cos(sumAngle) + floorStart.get(nextParentNode.floor);
                            double de = Math.atan2(h, w) + nodeModelAngleMap.get(nextParentNode);
                            double nR = Math.sqrt(h * h + w * w);
                            PointF nP = new PointF();
                            nP.x = (float) (nR * Math.sin(de)) + center.y;
                            nP.y = (float) (nR * Math.cos(de)) + center.x;
                            nodeModelPointFMap.put(child, nP);
                            nodeModelAngleMap.put(child, de);
                            nodeModelNewRadius.put(child, (double) nR);
                            nodeModelNewDeep.put(child, nextParentNode.deep);
                            recordMinMaxAngle(child);
                            count++;
                        }
                    }
                }
            }
        });
        // 计算角度和深度的间隔，用于后续的位置调整
        double spaceAngle = 2 * Math.PI - (maxAngle - minAngle);
        double deepSpace = maxDeep - minDeep;
        //   if(false){
        if (spaceAngle > (Math.PI / 8)) {
            double s = spaceAngle / pieCount;
            double d = deepSpace / pieCount;
            model.doTraversalNodes((ITraversal<NodeModel<T>>) next -> {
                if (!next.equals(rootNode)) {
                    nodeModelPointFMap.get(next);
                    PointF pointF = new PointF();
                    int deep = nodeModelNewDeep.get(next) == null ? next.deep : nodeModelNewDeep.get(next);
                    double angle = nodeModelAngleMap.get(next) + s * deep * (1 + d);
                    double radius = nodeModelNewRadius.get(next);
                    pointF.x = (float) (radius * Math.sin(angle)) + center.y;
                    pointF.y = (float) (radius * Math.cos(angle)) + center.x;
                    nodeModelPointFMap.put(next, pointF);
                    nodeModelAngleMap.put(next, angle);
                }
            });
        }
        return nodeModelPointFMap;
    }

    /**
     * 记录节点的角度和深度的最小最大值
     * @param node 节点模型
     */
    public void recordMinMaxAngle(NodeModel<T> node) {
        Double angle = nodeModelAngleMap.get(node);
        minAngle = Math.min(minAngle, angle);
        maxAngle = Math.max(maxAngle, angle);
        minDeep = Math.min(minDeep, node.deep);
        maxDeep = Math.max(maxDeep, node.deep);
    }
}
