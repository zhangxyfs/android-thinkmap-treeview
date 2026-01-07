package com.gyso.treeview.layout;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.gyso.treeview.TreeViewContainer;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.line.BaseLine;
import com.gyso.treeview.model.ITraversal;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.util.NodeCheck;
import com.gyso.treeview.util.TreeViewLog;
import com.gyso.treeview.util.ViewBox;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/**
 * guaishouN xw 674149099@qq.com
 */

/**
 * BoxRightTreeLayoutManager 是一个用于实现右侧横向树形布局的管理器。
 * 它继承自 TreeLayoutManager，并提供了特定于右向水平排列的测量与布局逻辑。
 *
 * @param <T> 节点数据类型，必须是 NodeItem 的子类
 */
public class BoxRightTreeLayoutManager<T extends NodeItem> extends TreeLayoutManager<T> {
    private static final String TAG = BoxRightTreeLayoutManager.class.getSimpleName();
    private final Deque<NodeModel<T>> parentsStack = new ArrayDeque<>(); // 存储尚未完成布局的父节点
    private final Set<NodeModel<T>> childrenSet = new HashSet<>();       // 已经处理过的叶子节点集合
    private final Map<NodeModel<T>, ViewBox> nodeToBoxMap = new HashMap<>(); // 每个节点对应的尺寸盒子映射
    private BoxRightTreeLayoutListener<T> mListener = null;              // 布局监听器

    /**
     * 构造方法：初始化布局管理器的基本配置
     *
     * @param context            上下文环境
     * @param spaceParentToChild 父节点到子节点之间的间距
     * @param spacePeerToPeer    同级节点之间的垂直间距
     * @param baseline           对齐基线设置
     */
    public BoxRightTreeLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline) {
        super(context, spaceParentToChild, spacePeerToPeer, baseline);
    }

    /**
     * 构造方法：带监听器的初始化方式
     *
     * @param context            上下文环境
     * @param spaceParentToChild 父节点到子节点之间的间距
     * @param spacePeerToPeer    同级节点之间的垂直间距
     * @param baseline           对齐基线设置
     * @param listener           布局事件监听器
     */
    public BoxRightTreeLayoutManager(Context context, int spaceParentToChild, int spacePeerToPeer, BaseLine baseline, BoxRightTreeLayoutListener<T> listener) {
        this(context, spaceParentToChild, spacePeerToPeer, baseline);
        mListener = listener;
    }

    /**
     * 执行具体的布局算法（当前为空实现）
     *
     * @param mTreeModel 树模型对象
     */
    @Override
    public void calculateByLayoutAlgorithm(TreeModel<T> mTreeModel) {

    }

    /**
     * 获取当前使用的布局类型标识符
     *
     * @return 返回 LAYOUT_TYPE_HORIZON_RIGHT 表示右向水平布局
     */
    @Override
    public int getTreeLayoutType() {
        return LAYOUT_TYPE_HORIZON_RIGHT;
    }

    /**
     * 执行测量阶段的操作。遍历所有节点并计算其在视图中的位置大小信息。
     *
     * @param treeViewContainer 包含整个树结构的容器组件
     */
    @Override
    public void performMeasure(TreeViewContainer<T> treeViewContainer) {
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            mContentViewBox.clear();
            nodeToBoxMap.clear();
            parentsStack.clear();
            childrenSet.clear();

            // 遍历器定义：逐个访问节点进行测量
            ITraversal<NodeModel<T>> traversal = new ITraversal<>() {
                @Override
                public void next(NodeModel<T> next) {
                    TreeViewLog.d(TAG, "performMeasure:" + next);
                    if (!next.childNodes.isEmpty()) {
                        parentsStack.add(next); // 若有子节点则加入待处理栈中
                    } else {
                        childrenSet.add(next);  // 叶子节点直接标记为已处理
                    }
                    measure(next, treeViewContainer); // 测量该节点
                }

                @Override
                public void finish() {
                    // 处理剩余未完成布局的父节点
                    while (!parentsStack.isEmpty()) {
                        NodeModel<T> oneParent = parentsStack.pollFirst();
                        if (childrenSet.containsAll(oneParent.childNodes)) {
                            layoutByBox(oneParent, treeViewContainer); // 布局该父节点及其子节点
                            childrenSet.add(oneParent);
                            childrenSet.removeAll(oneParent.childNodes);
                        } else {
                            parentsStack.addLast(oneParent); // 将无法立即处理的节点重新放回队列末尾
                        }
                    }

                    // 设置根节点的整体边界框
                    ViewBox rootBox = nodeToBoxMap.get(mTreeModel.getRootNode());
                    mContentViewBox.setValues(rootBox);

                    // 触发后续测量结束回调
                    onManagerFinishMeasureAllNodes(treeViewContainer);
                }
            };

            // 开始深度优先遍历所有节点
            mTreeModel.doTraversalNodes(traversal, false);
        }
    }

    /**
     * 在所有节点测量完成后执行额外调整操作，如适配窗口比例、边距等
     *
     * @param treeViewContainer 树容器组件
     */
    @Override
    public void onManagerFinishMeasureAllNodes(TreeViewContainer<T> treeViewContainer) {
        getPadding(treeViewContainer);
        mContentViewBox.bottom += (paddingBox.bottom + paddingBox.top);
        mContentViewBox.right += (paddingBox.left + paddingBox.right);
        fixedViewBox.setValues(mContentViewBox);

        if (winHeight == 0 || winWidth == 0) {
            return;
        }

        float scale = 1f * winWidth / winHeight;
        float wr = 1f * mContentViewBox.getWidth() / winWidth;
        float hr = 1f * mContentViewBox.getHeight() / winHeight;

        // 自适应缩放以匹配屏幕宽高比
        if (wr >= hr) {
            float bh = mContentViewBox.getWidth() / scale;
            fixedViewBox.bottom = (int) bh;
        } else {
            float bw = mContentViewBox.getHeight() * scale;
            fixedViewBox.right = (int) bw;
        }

        mFixedDx = paddingBox.top + (fixedViewBox.getWidth() - mContentViewBox.getWidth()) / 2;
        mFixedDy = paddingBox.left + (fixedViewBox.getHeight() - mContentViewBox.getHeight()) / 2;
    }

    /**
     * 测量单个节点的实际尺寸，并更新其对应的位置盒子
     *
     * @param node              当前要测量的节点
     * @param treeViewContainer 树容器组件
     */
    private void measure(NodeModel<T> node, TreeViewContainer<T> treeViewContainer) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(node);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();

        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }

        int curW = currentNodeView.getMeasuredWidth();
        int curH = currentNodeView.getMeasuredHeight();

        ViewBox viewBox = nodeToBoxMap.get(node);
        if (viewBox == null) {
            viewBox = new ViewBox(0, 0, curH, curW);
            nodeToBoxMap.put(node, viewBox);
        }

        viewBox.clear();
        viewBox.right = curW;
        viewBox.bottom = curH;
    }

    /**
     * 执行实际布局过程，将各节点放置在其最终坐标上
     *
     * @param treeViewContainer 树容器组件
     */
    @Override
    public void performLayout(final TreeViewContainer<T> treeViewContainer) {
        final TreeModel<T> mTreeModel = treeViewContainer.getTreeModel();
        if (mTreeModel != null) {
            ITraversal<NodeModel<T>> traversal = new ITraversal<>() {
                @Override
                public void next(NodeModel<T> next) {
                    layoutNodes(next, treeViewContainer); // 布局每一个节点
                }

                @Override
                public void finish() {
                    onManagerFinishLayoutAllNodes(treeViewContainer); // 结束后触发动画或收尾工作
                }
            };

            mTreeModel.doTraversalNodes(traversal, false);
        }
    }

    /**
     * 获取整体布局区域的尺寸盒子
     *
     * @return 整体布局范围的 ViewBox 对象
     */
    @Override
    public ViewBox getTreeLayoutBox() {
        return fixedViewBox;
    }

    /**
     * 根据子节点的尺寸来确定父节点的布局位置及尺寸
     *
     * @param parentNode        父节点
     * @param treeViewContainer 树容器组件
     */
    private void layoutByBox(NodeModel<T> parentNode, TreeViewContainer<T> treeViewContainer) {
        TreeViewHolder<?> parentHolder = treeViewContainer.getTreeViewHolder(parentNode);
        View parentNodeView = parentHolder == null ? null : parentHolder.getView();

        if (parentNodeView == null) {
            throw new NullPointerException(" parentNodeView can not be null");
        }

        ViewBox parentLocationBox = nodeToBoxMap.get(parentNode);
        int maxChildWidth = 0;
        int sumHeight = 0;

        // 计算所有子节点的总高度和最大宽度
        for (NodeModel<T> childNode : parentNode.childNodes) {
            ViewBox childLocationBox = nodeToBoxMap.get(childNode);
            maxChildWidth = Math.max(maxChildWidth, childLocationBox.getWidth());

            int childWidth = childLocationBox.getWidth();
            int childHeight = childLocationBox.getHeight();

            childLocationBox.top = sumHeight;
            childLocationBox.left = spaceParentToChild * 2 + parentLocationBox.getWidth();
            childLocationBox.bottom = childLocationBox.top + childHeight;
            childLocationBox.right = childLocationBox.left + childWidth;

            nodeToBoxMap.put(childNode, childLocationBox);
            sumHeight += childLocationBox.getHeight() + spacePeerToPeer;
        }

        sumHeight -= spacePeerToPeer;

        // 居中对齐子节点相对于父节点的高度
        int delta = (parentLocationBox.getHeight() - sumHeight) / 2;
        if (delta > 0) {
            ViewBox deltaBox = new ViewBox(delta, 0, delta, 0);
            for (NodeModel<T> childNode : parentNode.childNodes) {
                ViewBox childLocationBox = nodeToBoxMap.get(childNode);
                ViewBox newLocation = childLocationBox.add(deltaBox);
                nodeToBoxMap.put(childNode, newLocation);
            }
        }

        parentLocationBox.right = spaceParentToChild * 2 + parentLocationBox.getWidth() + maxChildWidth;
        parentLocationBox.bottom = Math.max(parentLocationBox.getHeight(), sumHeight);

        nodeToBoxMap.put(parentNode, parentLocationBox);
    }

    /**
     * 实际执行某个节点的布局定位操作
     *
     * @param currentNode       当前需要布局的节点
     * @param treeViewContainer 树容器组件
     */
    private void layoutNodes(NodeModel<T> currentNode, TreeViewContainer<T> treeViewContainer) {
        TreeViewHolder<?> currentHolder = treeViewContainer.getTreeViewHolder(currentNode);
        View currentNodeView = currentHolder == null ? null : currentHolder.getView();

        if (currentNodeView == null) {
            throw new NullPointerException(" currentNodeView can not be null");
        }

        int currentWidth = currentNodeView.getMeasuredWidth();
        int currentHeight = currentNodeView.getMeasuredHeight();

        ViewBox viewBox = nodeToBoxMap.get(currentNode);
        int centerFix = Math.max(viewBox.getHeight(), currentHeight) - currentHeight;
        int top = mFixedDy + viewBox.top + centerFix / 2;
        int left = mFixedDx + viewBox.left;

        // 递归向上叠加祖先节点偏移量
        NodeModel<T> pNode = currentNode.parentNode;
        while (pNode != null) {
            ViewBox upViewBox = nodeToBoxMap.get(pNode);
            top += upViewBox.top;
            left += upViewBox.left;
            pNode = pNode.parentNode;
        }

        int bottom = top + currentHeight;
        int right = left + currentWidth;

        ViewBox finalLocation = new ViewBox(top, left, bottom, right);

        onManagerLayoutNode(currentNode, currentNodeView, finalLocation, treeViewContainer);

        // 控制隐藏被折叠的父节点下的子节点
        currentNodeView.setVisibility(NodeCheck.parentNodeContract(currentNode) ? View.GONE : View.VISIBLE);

        if (mListener != null) {
            mListener.onLayoutNodes(currentNode, currentNodeView);
        }
    }

    /**
     * 在具体节点布局时调用此方法，支持动画或其他扩展行为
     *
     * @param currentNode       当前节点
     * @param currentNodeView   当前节点对应的视图
     * @param finalLocation     最终布局位置
     * @param treeViewContainer 树容器组件
     */
    @Override
    public void onManagerLayoutNode(NodeModel<T> currentNode,
                                    View currentNodeView,
                                    ViewBox finalLocation,
                                    TreeViewContainer<T> treeViewContainer) {
        if (!layoutAnimatePrepare(currentNode, currentNodeView, finalLocation, treeViewContainer)) {
            currentNodeView.layout(finalLocation.left, finalLocation.top, finalLocation.right, finalLocation.bottom);
        }
    }

    /**
     * 所有节点布局完毕后的统一回调接口
     *
     * @param treeViewContainer 树容器组件
     */
    @Override
    public void onManagerFinishLayoutAllNodes(TreeViewContainer<T> treeViewContainer) {
        layoutAnimate(treeViewContainer);
    }

    /**
     * 布局监听器接口，允许外部监听布局过程中关键节点的变化
     *
     * @param <T> 节点数据类型
     */
    public interface BoxRightTreeLayoutListener<T extends NodeItem> {
        /**
         * 当某节点完成布局时触发
         *
         * @param currentNode     当前节点
         * @param currentNodeView 当前节点对应的视图
         */
        void onLayoutNodes(NodeModel<T> currentNode, @NonNull View currentNodeView);

        /**
         * 测量阶段结束后触发
         */
        void performMeasureFinish();
    }

    @Override
    public void release() {
        mListener = null;
        parentsStack.clear();
        childrenSet.clear();
        nodeToBoxMap.clear();
    }
}
