package com.gyso.treeview.model;

abstract public class NodeItem {
    public String itemId = "";
    public boolean isSelected = false;
    public boolean isEdited = false;

    public abstract NodeItem neuBuild();
}
