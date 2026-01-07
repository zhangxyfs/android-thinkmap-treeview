/**
 * Created by Z.Pan on 2016/10/9.
 */
package com.gyso.treeview.algorithm.force;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * 力导向图布局算法实现类
 * 用于模拟物理系统中的节点和连接，实现动态布局效果
 */
public class Force {

    /**
     * 定时器执行周期（毫秒）
     */
    private static final long PERIOD_MILLIS = 160;
    /**
     * 默认连接距离
     */
    private static final int DEFAULT_LINK_DISTANCE = 20;
    /**
     * 默认连接强度
     */
    private static final float DEFAULT_LINK_STRENGTH = 0.1f;
    /**
     * 默认电荷值（用于排斥力计算）
     */
    private static final float DEFAULT_CHARGE = -30f;
    /**
     * 默认摩擦系数
     */
    private static final float DEFAULT_FRICTION = 0.9f;
    /**
     * 默认重力系数
     */
    private static final float DEFAULT_GRAVITY = 0.1f;
    /**
     * 默认theta值（用于四叉树计算）
     */
    private static final float DEFAULT_THETA = 0.8f;
    /**
     * 默认alpha值（用于衰减计算）
     */
    private static final float DEFAULT_ALPHA = 0.1f;

    /**
     * 力导向图监听器
     */
    private ForceListener listener;

    /**
     * 所有节点集合
     */
    private ArrayList<FNode> allNodes;
    /**
     * 所有连接集合
     */
    private ArrayList<FLink> allLinks;

    /**
     * 当前显示的节点集合
     */
    ArrayList<FNode> nodes;
    /**
     * 当前显示的连接集合
     */
    ArrayList<FLink> links;

    /**
     * 布局区域宽度
     */
    private int width;
    /**
     * 布局区域高度
     */
    private int height;
    /**
     * 连接距离
     */
    private int distance = DEFAULT_LINK_DISTANCE;
    /**
     * 连接强度
     */
    private float strength = DEFAULT_LINK_STRENGTH;
    /**
     * 电荷值
     */
    private float charge = DEFAULT_CHARGE;
    /**
     * 摩擦系数
     */
    private float friction = DEFAULT_FRICTION;
    /**
     * 重力系数
     */
    private float gravity = DEFAULT_GRAVITY;
    /**
     * theta值
     */
    private float theta = DEFAULT_THETA;
    /**
     * alpha值
     */
    private float alpha = DEFAULT_ALPHA;
    /**
     * 当前层级
     */
    private int currentLevel = Integer.MAX_VALUE;

    /**
     * 定时器处理器
     */
    private ForceHandler handler;
    /**
     * 定时器
     */
    private Timer timer;
    /**
     * 定时任务
     */
    private TickTask task;

    /**
     * 布局区域边界坐标
     */
    private float minX, minY, maxX, maxY;

    /**
     * 构造函数
     *
     * @param listener 力导向图事件监听器
     */
    public Force(ForceListener listener) {
        this.listener = listener;
        handler = new ForceHandler(this, Looper.getMainLooper());
    }

    /**
     * 设置节点集合
     *
     * @param nodes 节点集合
     * @return 当前Force实例
     */
    public Force setNodes(ArrayList<FNode> nodes) {
        allNodes = nodes;
        resetNodes();
        return this;
    }

    /**
     * 设置连接集合
     *
     * @param links 连接集合
     * @return 当前Force实例
     */
    public Force setLinks(ArrayList<FLink> links) {
        allLinks = links;
        resetLinks();
        return this;
    }

    /**
     * 设置当前层级
     *
     * @param level 层级值
     * @return 当前Force实例
     */
    Force setCurrentLevel(int level) {
        this.currentLevel = level;
        resetNodes();
        resetLinks();
        return this;
    }

    /**
     * 获取当前层级
     *
     * @return 当前层级值
     */
    int getCurrentLevel() {
        return currentLevel;
    }

    /**
     * 重置节点集合，根据当前层级过滤节点
     */
    private void resetNodes() {
        nodes = new ArrayList<>();
        if (allNodes != null) {
            for (int i = 0; i < allNodes.size(); i++) {
                FNode node = allNodes.get(i);
                if (node.getLevel() <= currentLevel) {
                    nodes.add(node);
                }
            }
        }
    }

    /**
     * 重置连接集合，根据当前层级过滤连接
     */
    private void resetLinks() {
        links = new ArrayList<>();
        if (allLinks != null) {
            for (int i = 0; i < allLinks.size(); i++) {
                FLink link = allLinks.get(i);
                if (link.source.getLevel() <= currentLevel && link.target.getLevel() <= currentLevel) {
                    links.add(link);
                }
            }
        }
    }

    /**
     * 设置布局区域大小
     *
     * @param width  宽度
     * @param height 高度
     * @return 当前Force实例
     */
    public Force setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * 设置连接距离
     *
     * @param distance 连接距离
     * @return 当前Force实例
     */
    public Force setDistance(int distance) {
        this.distance = distance;
        return this;
    }

    /**
     * 设置连接强度
     *
     * @param strength 连接强度
     * @return 当前Force实例
     */
    public Force setStrength(float strength) {
        this.strength = strength;
        return this;
    }

    /**
     * 设置摩擦系数
     *
     * @param friction 摩擦系数
     * @return 当前Force实例
     */
    public Force setFriction(float friction) {
        this.friction = friction;
        return this;
    }

    /**
     * 设置电荷值
     *
     * @param charge 电荷值
     * @return 当前Force实例
     */
    public Force setCharge(float charge) {
        this.charge = charge;
        return this;
    }

    /**
     * 设置重力系数
     *
     * @param gravity 重力系数
     * @return 当前Force实例
     */
    public Force setGravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    /**
     * 设置theta值
     *
     * @param theta theta值
     * @return 当前Force实例
     */
    public Force setTheta(float theta) {
        this.theta = theta;
        return this;
    }

    /**
     * 设置alpha值并启动定时任务
     *
     * @param alpha alpha值
     * @return 当前Force实例
     */
    public Force setAlpha(float alpha) {
        if (alpha < 0) {
            alpha = 0;
        }

        this.alpha = alpha;

        startTickTask();

        return this;
    }

    /**
     * 启动力导向图计算
     * 初始化节点权重、位置等参数
     *
     * @return 当前Force实例
     */
    public Force start() {
        int nodeCount = 0;
        int linkCount = 0;

        if (nodes != null) {
            nodeCount = nodes.size();
        }

        if (links != null) {
            linkCount = links.size();
        }

        for (int i = 0; i < nodeCount; i++) {
            FNode node = nodes.get(i);
            node.weight = 0;
        }
        for (int i = 0; i < linkCount; i++) {
            FLink link = links.get(i);
            link.source.weight++;
            link.target.weight++;
        }
        for (int i = 0; i < nodeCount; i++) {
            FNode node = nodes.get(i);
            node.x = node.x == -1f ? getRandomPosition(width) : node.x;
            node.y = node.y == -1f ? getRandomPosition(height) : node.y;
            node.px = node.x;
            node.py = node.y;
        }

        return resume();
    }

    /**
     * 停止力导向图计算
     *
     * @return 当前Force实例
     */
    public Force stop() {
        return setAlpha(0);
    }

    /**
     * 恢复力导向图计算
     *
     * @return 当前Force实例
     */
    public Force resume() {
        return setAlpha(DEFAULT_ALPHA);
    }

    /**
     * 生成随机位置
     *
     * @param max 最大值范围
     * @return 随机位置值
     */
    private float getRandomPosition(int max) {
//        return (float) (Math.random() * max);
//        return (float) (max * 0.25f + Math.random() * max * 0.5f);
        float r = (float) Math.random() - 0.5f;
        return (r <= 0
                ? r * max
                : (r + 0.5f) * max) * 2;
    }

    /**
     * 根据坐标查找节点
     *
     * @param x     x坐标
     * @param y     y坐标
     * @param scale 缩放比例
     * @return 找到的节点，未找到返回null
     */
    public FNode getNode(float x, float y, float scale) {
        ArrayList<FNode> nodes = this.nodes;

        if (nodes == null) {
            return null;
        }

        for (int i = nodes.size() - 1; i >= 0; i--) {
            FNode node = nodes.get(i);
            if (node.isInside(x, y, scale)) {
                return node;
            }
        }

        return null;
    }

    /**
     * 计算连接距离（包含节点半径）
     *
     * @param link 连接对象
     * @return 连接距离
     */
    private float linkDistance(FLink link) {
        return distance + link.source.getRadius() + link.target.getRadius();
    }

    /**
     * 执行一次力导向图计算循环
     * 包含连接力、重力、电荷力的计算和节点位置更新
     */
    private void tick() {
        if (nodes == null || links == null || nodes.isEmpty() || links.isEmpty()) {
//            endTickTask();
            pauseTask();
            return;
        }
        ArrayList<FNode> nodes = this.nodes;
        ArrayList<FLink> links = this.links;

        if ((alpha *= 0.99) < 0.0001 / nodes.size()) {
            //endTickTask();
            pauseTask();
            return;
        }

        final int nodeCount = nodes.size();
        int linkCount = links.size();

        // 计算连接力
        for (int i = 0; i < linkCount; i++) {
            FLink link = links.get(i);
            FNode sourceNode = link.source;
            FNode targetNode = link.target;
            float dx = targetNode.x - sourceNode.x;
            float dy = targetNode.y - sourceNode.y;
            double d = dx * dx + dy * dy;
            if (d > 0) {
                d = Math.sqrt(d);

                d = alpha * linkStrength(link) * (d - linkDistance(link)) / d;
                dx *= d;
                dy *= d;

                float k = sourceNode.weight * 1.0f / (targetNode.weight + sourceNode.weight);
                targetNode.x -= dx * k;
                targetNode.y -= dy * k;

                k = 1 - k;
                sourceNode.x += dx * k;
                sourceNode.y += dy * k;
            }
        }

        // 计算重力
        float k = alpha * gravity;
        if (k != 0) {
            int w = width / 2;
            int h = height / 2;
            for (int i = 0; i < nodeCount; i++) {
                FNode node = nodes.get(i);
                node.x += (w - node.x) * k;
                node.y += (h - node.y) * k;
            }
        }

        // 计算电荷力（使用四叉树优化）
        if (charge != 0) {
            final QuadTree quadTree = getQuadTree(nodes);
            forceAccumulate(quadTree.root);
            for (int i = 0; i < nodeCount; i++) {
                final FNode node = nodes.get(i);
                if (!node.isStable()) {
                    visitQuadTree(quadTree.root, node, minX, minY, maxX, maxY);
                }
            }
        }

        // 更新节点位置和速度
        for (int i = 0; i < nodeCount; i++) {
            FNode node = nodes.get(i);
            if (node.isStable()) {
                node.x = node.px;
                node.y = node.py;
            } else {
                node.x -= (node.px - (node.px = node.x)) * friction;
                node.y -= (node.py - (node.py = node.y)) * friction;
            }
        }

//        for (int i = 0; i < nodeCount; i++) {
//            FNode node = nodes.get(i);
//            if (node.immobile) {
//                if (!node.isStable()) {
//                    node.x = width / 2f;
//                    node.y = height / 2f;
//                }
//                break;
//            }
//        }
//        if (rootNode != null && rootNode.immobile && !rootNode.isStable()) {
//            rootNode.x = width / 2f;
//            rootNode.y = height / 2f;
//        }

        if (listener != null) {
            listener.refresh();
        }

    }

    /**
     * 计算连接强度
     *
     * @param link 连接对象
     * @return 连接强度值
     */
    private float linkStrength(FLink link) {
        float k = 1;
        if (link != null) {
//            int sl = link.source.getLevel();
//            int tl = link.target.getLevel();
//            if(sl > 0 && tl > 0) {
//                    k = (sl + tl) * 0.5f;
//            }
        }
        return strength * k;
    }

    /**
     * 构建四叉树用于优化电荷力计算
     *
     * @param nodes 节点列表
     * @return 构建的四叉树对象
     */
    private QuadTree getQuadTree(List<FNode> nodes) {
        if (nodes == null) {
            return null;
        }

        int nodeCount = nodes.size();

        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;
        for (int i = 0; i < nodeCount; i++) {
            FNode node = nodes.get(i);
            minX = Math.min(minX, node.x);
            minY = Math.min(minY, node.y);
            maxX = Math.max(maxX, node.x);
            maxY = Math.max(maxY, node.y);
        }

        float dx = maxX - minX;
        float dy = maxY - minY;
        if (dx > dy) {
            maxY = minY + dx;
        } else {
            maxX = minX + dy;
        }

        QuadTree quadTree = new QuadTree();

        for (int i = 0; i < nodeCount; i++) {
            quadTree.insert(quadTree.root, nodes.get(i), minX, minY, maxX, maxY);
        }

        return quadTree;
    }

    /**
     * 累积四叉树节点的电荷值
     *
     * @param root 四叉树根节点
     */
    private void forceAccumulate(QuadTree.Node root) {
        int cx = 0, cy = 0;
        root.charge = 0;
        if (!root.isLeaf) {
            QuadTree.Node[] children = root.children;
            int count = children.length;
            int i = -1;
            while (++i < count) {
                QuadTree.Node node = children[i];
                if (node == null) {
                    continue;
                }

                forceAccumulate(node);
                root.charge += node.charge;
                cx += node.charge * node.cx;
                cy += node.charge * node.cy;
            }
        }

        if (root.point != null) {
            if (!root.isLeaf) {
                root.point.x += Math.random() - 0.5;
                root.point.y += Math.random() - 0.5;
            }
            float k = alpha * nodeCharge(root.point);
            root.pointCharge = k;
            root.charge += k;
            cx += k * root.point.x;
            cy += k * root.point.y;
        }

        root.cx = cx / root.charge;
        root.cy = cy / root.charge;
    }

    /**
     * 计算节点电荷值
     *
     * @param node 节点对象
     * @return 节点电荷值
     */
    private float nodeCharge(FNode node) {
        if (node == null) {
            return charge;
        }
        int level = node.getLevel();
        return charge * node.getRadius() * (10 - level) / 20f;
    }

    /**
     * 计算排斥力
     *
     * @param root 四叉树节点
     * @param node 当前节点
     * @param x1   x坐标最小值
     * @param y1   y坐标最小值
     * @param x2   x坐标最大值
     * @param y2   y坐标最大值
     * @return 是否继续计算
     */
    private synchronized boolean repulse(QuadTree.Node root, FNode node, float x1, float y1, float x2, float y2) {
        if (root.point != node) {
            float dx = root.cx - node.x;
            float dy = root.cy - node.y;
            float dw = x2 - x1;
            float dn = dx * dx + dy * dy;
            if ((dw * dw) / (theta * theta) < dn) {
                if (dn < Float.POSITIVE_INFINITY) {
                    float k = root.charge / dn;
                    node.px -= dx * k;
                    node.py -= dy * k;
                }
                return true;
            }
            if (root.point != null && dn > 0 && dn < Float.POSITIVE_INFINITY) {
                float k = root.pointCharge / dn;
                node.px -= dx * k;
                node.py -= dy * k;
            }
        }
        return root.charge == 0;
    }

    /**
     * 遍历四叉树计算排斥力
     *
     * @param root 四叉树节点
     * @param node 当前节点
     * @param x1   x坐标最小值
     * @param y1   y坐标最小值
     * @param x2   x坐标最大值
     * @param y2   y坐标最大值
     */
    private void visitQuadTree(QuadTree.Node root, FNode node, float x1, float y1, float x2, float y2) {
        if (!repulse(root, node, x1, y1, x2, y2)) {
            float sx = (x1 + x2) * 0.5f;
            float sy = (y1 + y2) * 0.5f;
            QuadTree.Node[] children = root.children;
            if (children[0] != null) {
                visitQuadTree(children[0], node, x1, y1, sx, sy);
            }
            if (children[1] != null) {
                visitQuadTree(children[1], node, sx, y1, x2, sy);
            }
            if (children[2] != null) {
                visitQuadTree(children[2], node, x1, sy, sx, y2);
            }
            if (children[3] != null) {
                visitQuadTree(children[3], node, sx, sy, x2, y2);
            }
        }
    }

    /**
     * 启动定时任务
     */
    private void startTickTask() {
        if (timer != null && task != null) {
            task.resume();
        } else {
            /* execute tick() method once per PERIOD_MILLIS ms. */
            timer = new Timer(true);
            task = new TickTask();
            timer.schedule(task, 350, PERIOD_MILLIS);
        }
    }

    /**
     * 暂停定时任务
     */
    private void pauseTask() {
        if (task != null) {
            task.pause();
        }
    }

    /**
     * 结束定时任务
     */
    void endTickTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * 力导向图处理器，用于在主线程中执行tick方法
     */
    private static class ForceHandler extends Handler {
        private WeakReference<Force> forceReference;

        ForceHandler(Force force, Looper looper) {
            super(looper);
            forceReference = new WeakReference<Force>(force);
        }

        @Override
        public void handleMessage(Message msg) {
            Force force = forceReference.get();
            if (force != null) {
                force.tick();
            }
        }
    }

    /**
     * 定时任务类，用于定期执行tick方法
     */
    private class TickTask extends TimerTask {

        private volatile int pause;
        private volatile int isLocked;

        @Override
        public void run() {
            if (pause == 0) {
                handler.sendEmptyMessage(0);
            } else {
                try {
                    synchronized (this) {
                        isLocked = 1;
                        wait();
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        void pause() {
            pause |= 1;
        }

        void resume() {
            pause &= 0;
            synchronized (this) {
                if (isLocked == 1) {
                    isLocked = 0;
                    notify();
                }
            }
        }

    }

}

