package com.gyso.treeview.model;

/**
 * NodeItem抽象类，用于表示节点项的基本信息和状态
 * 包含节点项的ID、选择状态和编辑状态
 */
abstract public class NodeItem {
    /**
     * 节点项的唯一标识符
     */
    public String itemId = "";

    /**
     * 节点项的选择状态，true表示被选中，false表示未选中
     */
    public boolean isSelected = false;

    /**
     * 节点项的编辑状态，true表示已被编辑，false表示未被编辑
     */
    public boolean isEdited = false;

    /**
     * 创建一个新的NodeItem实例
     *
     * @return 返回一个新的NodeItem实例
     */
    public abstract NodeItem neuBuild();
}

