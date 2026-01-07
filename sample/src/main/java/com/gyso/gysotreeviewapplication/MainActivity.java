package com.gyso.gysotreeviewapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gyso.gysotreeviewapplication.base.Animal;
import com.gyso.gysotreeviewapplication.base.AnimalTreeViewAdapter;
import com.gyso.gysotreeviewapplication.databinding.ActivityMainBinding;
import com.gyso.treeview.TreeViewContainer;
import com.gyso.treeview.TreeViewEditor;
import com.gyso.treeview.layout.BoxRightTreeLayoutManager;
import com.gyso.treeview.layout.BoxVerticalUpAndDownLayoutManager;
import com.gyso.treeview.layout.TreeLayoutManager;
import com.gyso.treeview.line.AngledLine;
import com.gyso.treeview.line.BaseLine;
import com.gyso.treeview.line.SmoothLine;
import com.gyso.treeview.listener.TreeViewControlListener;
import com.gyso.treeview.model.NodeModel;
import com.gyso.treeview.model.Position;
import com.gyso.treeview.model.TreeModel;
import com.gyso.treeview.util.TreeViewLog;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends AppCompatActivity implements AnimalTreeViewAdapter.OnItemClickListener, TreeViewContainer.OnNodeClickListener<Animal> {
    public static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private final Stack<NodeModel<Animal>> removeCache = new Stack<>();
    private NodeModel<Animal> targetNode;
    private final AtomicInteger atomicInteger = new AtomicInteger();
    private final Handler handler = new Handler();
    private boolean mEditMode = false;
    private NodeModel<Animal> lastNode = null;
    private final AnimalTreeViewAdapter mAdapter = new AnimalTreeViewAdapter();
    private TreeViewEditor<Animal> mEditor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //demo init
        initWidgets();
        TreeViewLog.isDebug = true;
    }

    @Override
    protected void onDestroy() {
        binding.baseTreeView.release();
        super.onDestroy();
    }

    /**
     * To use a tree view, you should do 6 steps as follows:
     * 1 customs adapter
     * <p>
     * 2 configure layout manager. Space unit is dp.
     * You can custom you line by extends {@link BaseLine}
     * <p>
     * 3 view setting
     * <p>
     * 4 nodes data setting
     * <p>
     * 5 if you want to edit the map, then get and use and tree view editor
     * <p>
     * 6 you own others jobs
     */
    private void initWidgets() {
        //1 customs adapter
        mAdapter.setOnItemListener(this);

        //3 view setting
        binding.baseTreeView.setAdapter(mAdapter);
        binding.baseTreeView.setTreeLayoutManager(getTreeLayoutManager());

        //4 nodes data setting
        setData(mAdapter);

        //5 get an editor. Note: an adapter must set before get an editor.
        mEditor = binding.baseTreeView.getEditor();

        //6 you own others jobs
        doYourOwnJobs();
        binding.baseTreeView.setDebug(false);
    }

    private boolean checkXYInView(View parent, View checkView, Position position) {
        float clickRelativeX = position.x - parent.getLeft();
        float clickRelativeY = position.y - parent.getTop();

        boolean isXInView = clickRelativeX >= checkView.getLeft() && clickRelativeX <= checkView.getRight();
        boolean isYInView = clickRelativeY >= checkView.getTop() && clickRelativeY <= checkView.getBottom();
        boolean isVisible = checkView.isShown();

        return isXInView && isYInView && isVisible;
    }

    @Override
    public void onNodeClicked(NodeModel<Animal> node, View view, Position position) {
        if (!mEditMode || node == null) return;
        if (lastNode != null && !lastNode.value.itemId.equals(node.value.itemId)) {
            lastNode.value.isEdited = false;
            lastNode.value.isSelected = false;
            mAdapter.updateNodeView(lastNode, lastNode.value);
        }
        node.value.isSelected = true;
        mAdapter.updateNodeView(node, node.value);
        lastNode = node;

        if (checkXYInView(view, view.findViewById(R.id.endAddBtn), position)) {
            Log.e(TAG, "endAddBtn click");
            binding.addNodesBt.performClick();
        } else if (checkXYInView(view, view.findViewById(R.id.endContractBtn), position)) {
            Log.e(TAG, "contract click");
            node.setContract(true);
            mAdapter.updateNodeView(node, node.value);
        } else if (checkXYInView(view, view.findViewById(R.id.endExpandBtn), position)) {
            Log.e(TAG, "expand click");
            node.setContract(false);
            mAdapter.updateNodeView(node, node.value);
        }

    }

    @Override
    public void onNodeDoubleClicked(NodeModel<Animal> node, View view, Position position) {
        if (!mEditMode || node == null || !node.value.isSelected) return;
        node.value.isEdited = true;
        mAdapter.updateNodeView(node, node.value);
        mEditor.draggingNodeMode(false);
        lastNode = node;
    }

    @Override
    public void onOutsideNodeClicked() {
        if (lastNode != null) {
            lastNode.value.isEdited = false;
            mAdapter.updateNodeView(lastNode, lastNode.value);
        }
        mEditor.draggingNodeMode(true);
    }

    @Override
    public void onEdit(NodeModel<Animal> node, EditText editText, TextView textView) {
        InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (node.value.isEdited) {
            editText.requestFocus();
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        } else {
            if (TextUtils.isEmpty(editText.getText())) return;
            node.value.isEdited = false;
            node.value.name = editText.getText().toString();
            textView.setText(editText.getText().toString());
            editText.getText().clear();
            mAdapter.updateNodeView(node, node.value);
            mEditor.toScaleTree(0.1f, false);
        }
    }

    void doYourOwnJobs() {
        //drag to move node
        binding.dragEditModeRd.setOnCheckedChangeListener((v, isChecked) -> {
            mEditMode = isChecked;
            if (lastNode != null) {
                if (lastNode.value.isEdited || lastNode.value.isSelected) {
                    lastNode.value.isEdited = false;
                    lastNode.value.isSelected = false;
                    mAdapter.updateNodeView(lastNode, lastNode.value);
                }
                lastNode = null;
            }

            mEditor.requestMoveNodeByDragging(isChecked);
        });

        binding.baseTreeView.setOnNodeClickListener(this);
        //focus, means that tree view fill center in your window viewport
        binding.viewCenterBt.setOnClickListener(v -> mEditor.focusMidLocation(false));

        binding.copyNodeBt.setOnClickListener(v -> {
            if (!mEditMode || lastNode == null || lastNode.floor == 0) return;
            mEditor.copySubtree(lastNode.parentNode, lastNode, handler, 10);
        });

        //add some nodes
        binding.addNodesBt.setOnClickListener(v -> {
            if (lastNode == null) {
                return;
            }
            lastNode.value.isEdited = false;
            lastNode.value.isSelected = false;
            mAdapter.notifyItemViewChange(lastNode);
            mEditor.draggingNodeMode(true);

            Animal animal = new Animal(R.drawable.ic_10, "add-" + atomicInteger.getAndIncrement());
            animal.isSelected = true;
            animal.isEdited = true;
            NodeModel<Animal> childNode = new NodeModel<>(animal);

            handler.postDelayed(() -> {
                mEditor.addChildNodes(lastNode, childNode);
                lastNode = childNode;
                mEditor.draggingNodeMode(false);
            }, 10);
            //add to remove demo cache
        });
        binding.toScaleBigBt.setOnClickListener(v -> {
            mEditor.toScaleTree(0.1f, true);
        });
        binding.toScaleSmallBt.setOnClickListener(v -> {
            mEditor.toScaleTree(0.1f, false);
        });
        binding.bitmapBt.setOnClickListener(v -> {
            Bitmap bitmap = mEditor.requestTreeBitmap();
            Log.w("tag", "...........");
        });

        //remove node
        binding.removeNodeBt.setOnClickListener(v -> {
            if (lastNode == null) return;
            mEditor.removeNode(lastNode);
        });
        binding.toScaleOriginalBt.setOnClickListener(v -> {

        });
        binding.centerRootNodeBt.setOnClickListener(v -> {
            mEditor.focusByRootNodeP1();
        });


        //treeView control listener
        final Object token = new Object();
        Runnable dismissRun = () -> {
            binding.scalePercent.setVisibility(View.GONE);
        };
        binding.baseTreeView.setTreeViewControlListener(new TreeViewControlListener<Animal>() {
            @Override
            public void onScaling(int state, int percent) {
                Log.e(TAG, "onScaling: " + state + "  " + percent);
                binding.scalePercent.setVisibility(View.VISIBLE);
                if (state == TreeViewControlListener.MAX_SCALE) {
                    binding.scalePercent.setText("MAX");
                } else if (state == TreeViewControlListener.MIN_SCALE) {
                    binding.scalePercent.setText("MIN");
                } else {
                    binding.scalePercent.setText(percent + "%");
                }
                handler.removeCallbacksAndMessages(token);
                handler.postAtTime(dismissRun, token, SystemClock.uptimeMillis() + 2000);
            }

            @Override
            public void onDragMoveNodesHit(@Nullable NodeModel<Animal> draggingNode, @Nullable NodeModel<Animal> hittingNode, @Nullable View draggingView, @Nullable View hittingView) {
                Log.e(TAG, "onDragMoveNodesHit: draging[" + draggingNode + "]hittingNode[" + hittingNode + "]");
            }

            @Override
            public void onTouchMove(int action) {

            }

            @Override
            public void onDragMoveNodeComplete(@Nullable NodeModel<Animal> childNode, @Nullable NodeModel<Animal> toParentNode) {

            }
        });
    }

    /**
     * Box[XXX]TreeLayoutManagers are recommend for your project for they are running stably. Others treeLayoutManagers are developing.
     *
     * @return layout manager
     */
    private TreeLayoutManager<Animal> getTreeLayoutManager() {
        int space_50dp = 30;
        int space_20dp = 20;
        BaseLine line = getLine();
        return new BoxRightTreeLayoutManager<>(this, space_50dp, space_20dp, line);
//        return new TableRightTreeLayoutManager(this, space_50dp, space_20dp, line);
        //return new BoxDownTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new BoxLeftTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new BoxUpTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new BoxHorizonLeftAndRightLayoutManager(this,space_50dp,space_20dp,line);
        //return new BoxVerticalUpAndDownLayoutManager(this,space_50dp,space_20dp,line);


        //TODO !!!!! the layoutManagers below are just for test don't use in your projects. Just for test now
        //return new TableRightTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new TableLeftTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new TableDownTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new TableUpTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new TableHorizonLeftAndRightLayoutManager(this,space_50dp,space_20dp,line);
        //return new TableVerticalUpAndDownLayoutManager(this,space_50dp,space_20dp,line);

        //return new CompactRightTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new CompactLeftTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new CompactHorizonLeftAndRightLayoutManager(this,space_50dp,space_20dp,line);
        //return new CompactDownTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new CompactUpTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new CompactVerticalUpAndDownLayoutManager(this,space_50dp,space_20dp,line);

        //return new CompactRingTreeLayoutManager(this,space_50dp,space_20dp,line);
        //return new ForceDirectedTreeLayoutManager(this,line);
    }

    private BaseLine getLine() {
        AngledLine line = new AngledLine();
        line.setLineWidth(1);
        //return new StraightLine(Color.parseColor("#055287"),2);
        //return new DashLine(Color.parseColor("#F1286C"),3);
        //return new AngledLine();
        return line;
    }

    private void setData(AnimalTreeViewAdapter adapter) {
        //root
        NodeModel<Animal> root = new NodeModel<>(new Animal(R.drawable.ic_01, "-root-\n%%%%%%%%%%%%%%%%\n%%%%%%%%%%\n%%%%%%%%%%\n%%%%%%%%%%\n%%%%%%%%%%"));
        TreeModel<Animal> treeModel = new TreeModel<>(root);

        //child nodes
        NodeModel<Animal> sub0 = new NodeModel<>(new Animal(R.drawable.ic_02, "sub00的节点信息描述更多"));
        NodeModel<Animal> sub1 = new NodeModel<>(new Animal(R.drawable.ic_03, "sub01的节点文字信息丰富"));
        NodeModel<Animal> sub2 = new NodeModel<>(new Animal(R.drawable.ic_04, "sub02节点说明文字内容长"));
        NodeModel<Animal> sub3 = new NodeModel<>(new Animal(R.drawable.ic_05, "sub03===\n=====\n======\n===\n=====\n=====\n===\n====\n=======\n=====\n======\n=====\n======\n========\n=====\n=====\n===\n======="));
        NodeModel<Animal> sub4 = new NodeModel<>(new Animal(R.drawable.ic_06, "sub04节点信息描述更多"));
        NodeModel<Animal> sub5 = new NodeModel<>(new Animal(R.drawable.ic_07, "sub05****************************************************"));
        NodeModel<Animal> sub6 = new NodeModel<>(new Animal(R.drawable.ic_08, "sub06节点内容说明文字更长，超过十个字"));
        NodeModel<Animal> sub7 = new NodeModel<>(new Animal(R.drawable.ic_09, "sub07详细的节点信息描述"));
        NodeModel<Animal> sub8 = new NodeModel<>(new Animal(R.drawable.ic_10, "sub08节点文字信息丰富详细"));
        NodeModel<Animal> sub9 = new NodeModel<>(new Animal(R.drawable.ic_11, "sub09节点说明文字超过十字"));
        NodeModel<Animal> sub10 = new NodeModel<>(new Animal(R.drawable.ic_12, "sub10节点描述详细内容丰富"));
        NodeModel<Animal> sub11 = new NodeModel<>(new Animal(R.drawable.ic_13, "sub11的节点信息内容丰富"));
        NodeModel<Animal> sub12 = new NodeModel<>(new Animal(R.drawable.ic_14, "sub12节点文字描述超过十个字"));
        NodeModel<Animal> sub13 = new NodeModel<>(new Animal(R.drawable.ic_15, "sub13节点说明文字详细完整"));
        NodeModel<Animal> sub14 = new NodeModel<>(new Animal(R.drawable.ic_13, "sub14节点详细说明文字信息"));
        NodeModel<Animal> sub15 = new NodeModel<>(new Animal(R.drawable.ic_14, "sub15节点文字描述超过十个字"));
        NodeModel<Animal> sub16 = new NodeModel<>(new Animal(R.drawable.ic_15, "sub16节点说明文字详细描述"));
        NodeModel<Animal> sub17 = new NodeModel<>(new Animal(R.drawable.ic_08, "sub17节点文字信息丰富"));
        NodeModel<Animal> sub18 = new NodeModel<>(new Animal(R.drawable.ic_09, "sub18节点描述文字详细完整"));
        NodeModel<Animal> sub19 = new NodeModel<>(new Animal(R.drawable.ic_10, "sub19节点说明文字内容丰富"));
        NodeModel<Animal> sub20 = new NodeModel<>(new Animal(R.drawable.ic_02, "sub20节点文字信息超过十字"));
        NodeModel<Animal> sub21 = new NodeModel<>(new Animal(R.drawable.ic_03, "sub21节点说明文字详细完整"));
        NodeModel<Animal> sub22 = new NodeModel<>(new Animal(R.drawable.ic_04, "sub22节点文字描述信息丰富"));
        NodeModel<Animal> sub23 = new NodeModel<>(new Animal(R.drawable.ic_05, "sub23节点说明内容文字详细"));
        NodeModel<Animal> sub24 = new NodeModel<>(new Animal(R.drawable.ic_06, "sub24节点描述信息超过十个字"));
        NodeModel<Animal> sub25 = new NodeModel<>(new Animal(R.drawable.ic_07, "sub25节点说明文字信息详细"));
        NodeModel<Animal> sub26 = new NodeModel<>(new Animal(R.drawable.ic_08, "sub26节点文字信息描述丰富"));
        NodeModel<Animal> sub27 = new NodeModel<>(new Animal(R.drawable.ic_09, "sub27节点说明文字内容丰富"));
        NodeModel<Animal> sub28 = new NodeModel<>(new Animal(R.drawable.ic_10, "sub28节点描述文字信息完整"));
        NodeModel<Animal> sub29 = new NodeModel<>(new Animal(R.drawable.ic_11, "sub29节点说明文字详细描述"));
        NodeModel<Animal> sub30 = new NodeModel<>(new Animal(R.drawable.ic_02, "sub30节点描述内容详细信息"));
        NodeModel<Animal> sub31 = new NodeModel<>(new Animal(R.drawable.ic_03, "sub31节点说明文字信息丰富"));
        NodeModel<Animal> sub32 = new NodeModel<>(new Animal(R.drawable.ic_04, "sub32节点详细描述文字信息"));
        NodeModel<Animal> sub33 = new NodeModel<>(new Animal(R.drawable.ic_05, "sub33节点说明文字内容丰富"));
        NodeModel<Animal> sub34 = new NodeModel<>(new Animal(R.drawable.ic_06, "sub34节点描述文字信息详细"));
        NodeModel<Animal> sub35 = new NodeModel<>(new Animal(R.drawable.ic_07, "sub35节点说明信息文字丰富"));
        NodeModel<Animal> sub36 = new NodeModel<>(new Animal(R.drawable.ic_08, "sub36节点文字描述超过十个字"));
        NodeModel<Animal> sub37 = new NodeModel<>(new Animal(R.drawable.ic_09, "sub37节点说明详细内容丰富"));
        NodeModel<Animal> sub38 = new NodeModel<>(new Animal(R.drawable.ic_10, "sub38节点文字信息描述丰富详细"));
        NodeModel<Animal> sub39 = new NodeModel<>(new Animal(R.drawable.ic_11, "sub39节点说明文字超过十个字"));
        NodeModel<Animal> sub40 = new NodeModel<>(new Animal(R.drawable.ic_02, "sub40&&\n&&&\n&&&&\n&&&\n节点说明文字超过十个字"));
        NodeModel<Animal> sub41 = new NodeModel<>(new Animal(R.drawable.ic_03, "sub41节点描述文字信息完整"));
        NodeModel<Animal> sub42 = new NodeModel<>(new Animal(R.drawable.ic_04, "sub42节点说明文字详细信息"));
        NodeModel<Animal> sub43 = new NodeModel<>(new Animal(R.drawable.ic_05, "sub43节点文字描述信息丰富"));
        NodeModel<Animal> sub44 = new NodeModel<>(new Animal(R.drawable.ic_06, "sub44节点说明文字超过十个字"));
        NodeModel<Animal> sub45 = new NodeModel<>(new Animal(R.drawable.ic_07, "sub45节点文字信息描述详细"));
        NodeModel<Animal> sub46 = new NodeModel<>(new Animal(R.drawable.ic_08, "sub46节点说明文字详细完整"));
        NodeModel<Animal> sub47 = new NodeModel<>(new Animal(R.drawable.ic_09, "sub47节点文字信息内容丰富"));
        NodeModel<Animal> sub48 = new NodeModel<>(new Animal(R.drawable.ic_10, "sub48节点说明文字超过十个字"));
        NodeModel<Animal> sub49 = new NodeModel<>(new Animal(R.drawable.ic_11, "sub49节点文字信息描述丰富"));
        NodeModel<Animal> sub50 = new NodeModel<>(new Animal(R.drawable.ic_05, "sub50节点说明文字信息完整"));
        NodeModel<Animal> sub51 = new NodeModel<>(new Animal(R.drawable.ic_07, "sub51节点文字描述超过十个字"));
        NodeModel<Animal> sub52 = new NodeModel<>(new Animal(R.drawable.ic_07, "sub52节点说明文字内容详细"));
        NodeModel<Animal> sub53 = new NodeModel<>(new Animal(R.drawable.ic_07, "sub53节点文字信息描述丰富"));
        NodeModel<Animal> sub54 = new NodeModel<>(new Animal(R.drawable.ic_02, "sub54的描述文字多于十个字"));
        NodeModel<Animal> sub55 = new NodeModel<>(new Animal(R.drawable.ic_03, "sub55的描述文字多于十个字"));
        NodeModel<Animal> sub56 = new NodeModel<>(new Animal(R.drawable.ic_04, "sub56节点信息很长哦"));
        NodeModel<Animal> sub57 = new NodeModel<>(new Animal(R.drawable.ic_05, "sub57节点详细说明文字"));
        NodeModel<Animal> sub58 = new NodeModel<>(new Animal(R.drawable.ic_06, "sub58节点内容超过十字"));
        NodeModel<Animal> sub59 = new NodeModel<>(new Animal(R.drawable.ic_07, "sub59节点详细说明内容"));
        NodeModel<Animal> sub60 = new NodeModel<>(new Animal(R.drawable.ic_08, "sub60信息长度超过十字"));
        NodeModel<Animal> sub61 = new NodeModel<>(new Animal(R.drawable.ic_09, "sub61详细节点说明信息"));
        NodeModel<Animal> sub62 = new NodeModel<>(new Animal(R.drawable.ic_10, "sub62节点描述超过十字"));
        NodeModel<Animal> sub63 = new NodeModel<>(new Animal(R.drawable.ic_11, "sub63节点详细信息说明"));
        NodeModel<Animal> sub64 = new NodeModel<>(new Animal(R.drawable.ic_02, "sub64节点内容丰富文字"));
        NodeModel<Animal> sub65 = new NodeModel<>(new Animal(R.drawable.ic_03, "sub65节点信息说明超过十字"));
        NodeModel<Animal> sub66 = new NodeModel<>(new Animal(R.drawable.ic_04, "sub66节点内容详细说明"));
        NodeModel<Animal> sub67 = new NodeModel<>(new Animal(R.drawable.ic_05, "sub67节点文字信息丰富"));
        NodeModel<Animal> sub68 = new NodeModel<>(new Animal(R.drawable.ic_06, "sub68节点描述超过十字"));
        NodeModel<Animal> sub69 = new NodeModel<>(new Animal(R.drawable.ic_07, "sub69节点说明文字信息"));
        NodeModel<Animal> sub70 = new NodeModel<>(new Animal(R.drawable.ic_08, "sub70节点详细文字描述"));
        NodeModel<Animal> sub71 = new NodeModel<>(new Animal(R.drawable.ic_09, "sub71节点信息超过十字"));
        NodeModel<Animal> sub72 = new NodeModel<>(new Animal(R.drawable.ic_10, "sub72详细节点文字说明"));
        NodeModel<Animal> sub73 = new NodeModel<>(new Animal(R.drawable.ic_11, "sub73节点说明文字较长"));


        //build relationship
        treeModel.addNode(root, sub0, sub1, sub3, sub4);
        treeModel.addNode(sub3, sub12, sub13);
        treeModel.addNode(sub1, sub2);
        treeModel.addNode(sub0, sub34, sub5, sub38, sub39);
        treeModel.addNode(sub4, sub6);
        treeModel.addNode(sub5, sub7, sub8);
        treeModel.addNode(sub6, sub9, sub10, sub11);
        treeModel.addNode(sub11, sub14, sub15);
        treeModel.addNode(sub10, sub40);
        treeModel.addNode(sub40, sub16);
        treeModel.addNode(sub9, sub47, sub48);
        treeModel.addNode(sub47, sub49);
        treeModel.addNode(sub12, sub37);
        treeModel.addNode(sub0, sub36);
        treeModel.addNode(sub39, sub52, sub53);
        treeModel.addNode(sub1, sub54, sub55);
        treeModel.addNode(sub5, sub56, sub57);
        treeModel.addNode(sub6, sub58, sub59);
        treeModel.addNode(sub7, sub60, sub61);
        treeModel.addNode(sub8, sub62, sub63);
        treeModel.addNode(sub9, sub64, sub65);
        treeModel.addNode(sub10, sub66, sub67);
        treeModel.addNode(sub11, sub68, sub69);
        treeModel.addNode(sub12, sub70, sub71);
        treeModel.addNode(sub13, sub72, sub73);
        //mark
//        parentToRemoveChildren = sub0;
        targetNode = sub1;

        //set data
        adapter.setTreeModel(treeModel);

    }
}