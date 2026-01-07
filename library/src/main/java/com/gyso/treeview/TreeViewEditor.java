package com.gyso.treeview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.view.View;

import androidx.core.view.ViewCompat;

import com.gyso.treeview.adapter.TreeViewAdapter;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @Author: 怪兽N
 * @Time: 2021/6/9  15:31
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 * @Describe: Android developer
 * <p>
 * helper you edit your tree view.
 * Move node by dragging and remove node is support now.
 * <p>
 * Note:
 * 1 An adapter must be set to GysoTreeView before you get an editor
 * 2 If you has set a new adapter, you should get an new editor
 */
public class TreeViewEditor<T extends NodeItem> {
    private final WeakReference<TreeViewAdapter<T>> adapterWeakReference;
    private final WeakReference<TreeViewContainer<T>> containerWeakReference;


    protected TreeViewEditor(TreeViewContainer<T> container) {
        this.containerWeakReference = new WeakReference<>(container);
        this.adapterWeakReference = new WeakReference<>(container.getAdapter());
    }

    /**
     * 对外开放 getContainer()
     *
     * @return
     */
    public TreeViewContainer<T> getContainer() {
        return containerWeakReference.get();
    }

    private TreeViewAdapter<T> getAdapter() {
        return adapterWeakReference.get();
    }

    /**
     * let add node in window viewport
     */
    public void focusMidLocation(boolean usedAnimation) {
        TreeViewContainer<T> container = getContainer();
        if (container != null) container.focusMidLocation(usedAnimation);
    }

    public void toScaleTree(float scale, boolean isBig) {
        TreeViewContainer<T> container = getContainer();
        if (container != null) container.toScaleTree(scale, isBig);
    }


    public void focusByRootNode() {
        TreeViewContainer<T> container = getContainer();
        if (container != null) container.focusByRootNode();
    }

    public void focusByRootNodeP1() {
        TreeViewContainer<T> container = getContainer();
        if (container != null) container.focusByRootNodeP1();
    }

    public View anchorNodeOnMidViewport(NodeModel<?> targetNode) {
        //TODO move targetNode at center  of viewport
        return null;
    }

    /**
     * change layout algorithm
     */
    public void changeLayoutAlgorithm() {

    }

    /**
     * get current relation ship, you can use when you change
     *
     * @param traverse relations node
     */
    public <T> void getCurrentRelationships(TraverseRelationshipCallback traverse) {

    }

    /**
     * get current relation ship, you can use when you change
     *
     * @param traverse           relations node
     * @param jsonStringFilePath jsonStringFilePath
     */
    public boolean save(String jsonStringFilePath, TraverseRelationshipCallback traverse) {
        return false;
    }

    public boolean save(File jsonStringFile, TraverseRelationshipCallback traverse) {
        return false;
    }

    /**
     * load data  from json String
     *
     * @param jsonString string
     */
    public void load(String jsonString) {

    }

    /**
     * load data  from file
     *
     * @param jsonStringFile file
     */
    public void load(File jsonStringFile) {

    }

    /**
     * expand by node
     *
     * @param targetParentNode targetParentNode
     */
    public void collapse(NodeModel<?> targetParentNode) {

    }

    /**
     * expand by node
     *
     * @param targetParentNode targetParentNode
     */
    public void expand(NodeModel<?> targetParentNode) {

    }

    /**
     * focus on node
     *
     * @param targetNode targetNode
     */
    public void focusOn(NodeModel<?> targetNode) {

    }

    /**
     * un focus on node
     *
     * @param targetNode targetNode
     */
    public void unFocusOn(NodeModel<?> targetNode) {

    }

    /**
     * for support scroll view
     */
    public void lockDragDirection() {

    }

    /**
     * save  current state
     */
    public void saveLastSate() {

    }

    /**
     * keep last location and
     */
    public void restoreLastSate() {

    }

    /**
     * add on select listener
     */
    public void addOnSelectedListener() {

    }

    /**
     * default: auto restructure by dragging;
     * totally free drag;
     *
     * @param status status
     */
    public void setEditStatus(int status) {

    }

    /**
     * request tree bitmap
     *
     * @return bitmap
     */
    public Bitmap requestTreeBitmap() {
        TreeViewContainer<T> container = getContainer();
        if (container != null) {
            if (!ViewCompat.isLaidOut(container)) {
                return null;
            }
            try {
                Bitmap bitmap = Bitmap.createBitmap(container.getWidth(), container.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.translate(-container.getScrollX(), -container.getScrollY());
                container.draw(canvas);
                return bitmap;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * before you edit, requestMoveNodeByDragging(true), so than you can drag to move the node
     *
     * @param wantEdit true for edit mode
     */
    public void requestMoveNodeByDragging(boolean wantEdit) {
        TreeViewContainer<T> container = getContainer();
        if (container != null) container.requestMoveNodeByDragging(wantEdit);
    }

    /**
     * 拖拽模式开启或者关闭 只改变boolean 值
     *
     * @param isOpen
     */
    public void draggingNodeMode(boolean isOpen) {
        TreeViewContainer<T> container = getContainer();
        if (container != null) {
            container.draggingNodeMode(isOpen);
        }
    }

    /**
     * 是否开启编辑模式
     *
     * @return
     */
    public boolean isEditMode() {
        TreeViewContainer<T> container = getContainer();
        if (container != null) {
            return container.isEditMode();
        }
        return false;
    }

    public void modifyChildNode(NodeModel<T> child) {
        TreeViewContainer<T> container = getContainer();
        if (container != null) {
            container.onItemViewChange(child);
        }
    }

    /**
     * add child nodes
     *
     * @param parent     parent node should has been in tree model
     * @param childNodes new nodes that will be add to tree model
     */
    public void addChildNodes(@NotNull NodeModel<T> parent, @NotNull NodeModel<T>... childNodes) {
        for (NodeModel<T> childNode : childNodes) {
            if (childNode == parent) return;
        }
        TreeViewContainer<T> container = getContainer();
        if (container != null) {
            container.onAddNodes(parent, childNodes);
        }
    }

    /**
     * 复制子树结构到目标节点下
     *
     * @param rootNode   目标根节点，复制的子树将作为其子节点添加
     * @param sourceNode 源节点，包含要复制的子树结构
     * @param handler    用于延迟执行的处理器
     * @param delay      延迟执行的时间（毫秒）
     * @author add fuction by zxy 2025/12/11 9:50
     */
    public void copySubtree(@NotNull NodeModel<T> rootNode, @NotNull NodeModel<T> sourceNode, @NotNull Handler handler, long delay) {
        TreeViewContainer<T> container = getContainer();
        if (container != null) {
            container.copySubtree(rootNode, sourceNode, handler, delay);
        }
    }

    /**
     * 获取复制操作是否成功的状态
     *
     * @return boolean 复制操作的结果状态，true表示成功，false表示失败
     * @author add fuction by zxy 2025/12/11 9:52
     */
    public boolean isCopySuccess() {
        TreeViewContainer<T> container = getContainer();
        if (container != null) {
            return container.isCopySuccess();
        }
        return true;
    }

    /**
     * 定位指定节点ID在树形结构中的位置
     *
     * @param nodeId 要定位的节点ID，不能为空
     * @return 返回对应节点的位置模型，如果未找到或容器不存在则返回null
     */
    public NodeModel<T> locateNodePosition(String nodeId) {
        // 获取当前容器实例
        TreeViewContainer<T> container = getContainer();
        if (container == null) {
            return null;
        }
        // 委托容器执行具体的节点定位操作
        return container.locateNodePosition(nodeId);
    }


    /**
     * remove node
     *
     * @param nodeToRemove node to remove
     */
    public void removeNode(NodeModel<T> nodeToRemove) {
        TreeViewContainer<T> container = getContainer();
        if (container != null) {
            container.onRemoveNode(nodeToRemove);
        }
    }

    /**
     * remove children nodes by parent node
     *
     * @param parentNode parent node to remove children
     */
    public void removeNodeChildren(NodeModel<T> parentNode) {
        TreeViewContainer<T> container = getContainer();
        if (container != null) {
            container.onRemoveChildNodes(parentNode);
        }
    }

    public interface TraverseRelationshipCallback {
        <T extends NodeItem> void callback(T root, T parent, T child);

        default void callbackView(View rootView, View parentView, View childView) {
        }

        ;
    }

    public interface OnNodeSelectedCallback {
        <T extends NodeItem> void callback(T clickNode, List<T> selectedList);
    }
}
