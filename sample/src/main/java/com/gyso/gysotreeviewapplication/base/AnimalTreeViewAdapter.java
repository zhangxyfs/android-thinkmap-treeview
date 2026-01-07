package com.gyso.gysotreeviewapplication.base;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gyso.gysotreeviewapplication.R;
import com.gyso.gysotreeviewapplication.databinding.NodeBaseLayoutBinding;
import com.gyso.treeview.adapter.DrawInfo;
import com.gyso.treeview.adapter.TreeViewAdapter;
import com.gyso.treeview.adapter.TreeViewHolder;
import com.gyso.treeview.line.BaseLine;
import com.gyso.treeview.line.DashLine;
import com.gyso.treeview.model.NodeModel;


/**
 * @Author: 怪兽N
 * @Time: 2021/4/23  16:48
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 * @Describe: Tree View Adapter for node data to view
 */
public class AnimalTreeViewAdapter extends TreeViewAdapter<Animal> {
    private DashLine dashLine = new DashLine(Color.parseColor("#F06292"), 6);
    private OnItemClickListener listener;

    public void setOnItemListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public TreeViewHolder<Animal> onCreateViewHolder(@NonNull ViewGroup viewGroup, NodeModel<Animal> node) {
        NodeBaseLayoutBinding nodeBinding = NodeBaseLayoutBinding.inflate(LayoutInflater.from(viewGroup.getContext()), viewGroup, false);
        return new TreeViewHolder<>(nodeBinding.getRoot(), node);
    }

    private NodeModel<Animal> mLastEditNode = null;

    @Override
    public void onBindViewHolder(@NonNull TreeViewHolder<Animal> holder) {
        NodeModel<Animal> node = holder.getNode();
        holder.getView().setTag(node);
        NodeBaseLayoutBinding binding = NodeBaseLayoutBinding.bind(holder.getView());
        Context context = binding.getRoot().getContext();
        final Animal animal = node.value;

        binding.mindNodeTv.setText(animal.name);
        binding.mindNodeEt.setHint(animal.name);
        float fontSize = 0f;
        switch (node.floor) {
            case 0:
                fontSize = getFontSize(context, R.dimen.font_sp_20);
                break;
            case 1:
                fontSize = getFontSize(context, R.dimen.font_sp_18);
                break;
            case 2:
                fontSize = getFontSize(context, R.dimen.font_sp_16);
                break;
            default:
                fontSize = getFontSize(context, R.dimen.font_sp_14);
                break;
        }
        binding.mindNodeLayout.setSelected(node.value.isSelected);
        binding.mindNodeTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
        binding.mindNodeEt.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize - getFontSize(context, R.dimen.font_sp_0_5) * 2);
        boolean isRoot = node.floor == 0;
        binding.mindNodeTv.getPaint().setFakeBoldText(isRoot);
        binding.endExpandBtn.setText(node.getChildCount() + "");

        binding.mindNodeEt.setVisibility(node.value.isEdited ? View.VISIBLE : View.GONE);
        binding.mindNodeTv.setVisibility(node.value.isEdited ? View.INVISIBLE : View.VISIBLE);
        binding.line.setVisibility(node.value.isSelected ? View.VISIBLE : View.GONE);
        binding.endAddBtn.setVisibility(node.value.isSelected ? View.VISIBLE : View.GONE);

        if (node.childNodes.isEmpty() || !node.value.isSelected) {
            binding.line1.setVisibility(View.GONE);
            binding.endExpandBtn.setVisibility(View.GONE);
            binding.endContractBtn.setVisibility(View.GONE);
        } else {
            binding.line1.setVisibility(View.VISIBLE);
            binding.endExpandBtn.setVisibility(node.isContract() ? View.VISIBLE : View.GONE);
            binding.endContractBtn.setVisibility(!node.isContract() ? View.VISIBLE : View.GONE);
        }

        if (listener != null) {
            if (animal.isEdited) {
                listener.onEdit(node, binding.mindNodeEt, binding.mindNodeTv);
                mLastEditNode = node;
            } else if (mLastEditNode != null) {
                listener.onEdit(node, binding.mindNodeEt, binding.mindNodeTv);
                mLastEditNode = null;
            }
        }
    }

    private float getFontSize(Context context, int resid) {
        return context.getResources().getDimension(resid);
    }

    @Override
    public BaseLine onDrawLine(DrawInfo drawInfo) {
        return null;
    }

    public interface OnItemClickListener {
        void onEdit(NodeModel<Animal> node, EditText editText, TextView textView);
    }
}
