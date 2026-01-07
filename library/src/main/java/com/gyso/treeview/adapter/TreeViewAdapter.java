package com.gyso.treeview.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.gyso.treeview.line.BaseLine;
import com.gyso.treeview.listener.TreeViewNotifier;
import com.gyso.treeview.model.NodeItem;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.util.TreeViewLog;

/**
 * @Author: 怪兽N
 * @Time: 2021/4/23  15:19
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 * @Describe: The view adapter for the {@link com.gyso.treeview.TreeViewContainer}
 */
public abstract class TreeViewAdapter<T extends NodeItem> {
    private TreeViewNotifier<T> notifier;
    private TreeModel<T> treeModel;

    public void setTreeModel(TreeModel<T> treeModel) {
        this.treeModel = treeModel;
        notifyDataSetChange();
    }

    /**
     * Get tree model
     *
     * @return tree model
     */
    public TreeModel<T> getTreeModel() {
        return treeModel;
    }

    /**
     * For create view holder by your self
     *
     * @param viewGroup parent
     * @param model     node
     * @return holder
     */
    public abstract TreeViewHolder<T> onCreateViewHolder(@NonNull ViewGroup viewGroup, NodeModel<T> model);

    /**
     * Draw line between node and node by you decision.
     * If you return an BaseLine, line will be draw by the return one instead of TreeViewLayoutManager's.
     *
     * @param drawInfo provides all you need to draw you line
     * @return the line draw you want to use for different nodes
     */
    public abstract BaseLine onDrawLine(DrawInfo drawInfo);

    public abstract void onBindViewHolder(@NonNull TreeViewHolder<T> holder);

    /**
     * when bind the holder, set up you view
     *
     * @param holder holder
     */
    public void bindViewHolder(@NonNull TreeViewHolder<T> holder) {
        holder.getView().setTag(holder.getNode());
        onBindViewHolder(holder);
    }

    /**
     * for recycling holder, exactly for recycling views
     *
     * @param node
     * @return
     */
    public int getHolderType(NodeModel<T> node) {
        return 0;
    }

    public void setNotifier(TreeViewNotifier<T> notifier) {
        this.notifier = notifier;
    }

    public void notifyDataSetChange() {
        if (notifier != null) {
            notifier.onDataSetChange();
        }
    }

    public void notifyItemViewChange(NodeModel<T> node) {
        if (notifier != null) {
            notifier.onItemViewChange(node);
        }
    }

    public void updateNodeView(@NonNull NodeModel<T> node, @NonNull T newData) {
        if (node == null) return;
        node.setValue(newData);
        TreeViewLog.d("updatenodeview", "updateNodeView: ");
        notifyItemViewChange(node);
    }

}
