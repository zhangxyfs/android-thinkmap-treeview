package com.gyso.treeview.algorithm.table;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 表键类，用于表示具有楼层和深度两个维度的键值
 */
public class TableKey {
    /**
     * 楼层值
     */
    public int floor;
    /**
     * 深度值
     */
    public int deep;

    /**
     * 构造函数
     * @param floor 楼层值
     * @param deep 深度值
     */
    public TableKey(int floor, int deep) {
        this.floor = floor;
        this.deep = deep;
    }

    /**
     * 重写hashCode方法，基于floor字段计算哈希值
     * @return floor字段的值作为哈希码
     */
    @Override
    public int hashCode() {
        return floor;
    }

    /**
     * 重写equals方法，比较两个TableKey对象是否相等
     * @param obj 待比较的对象
     * @return 如果对象类型为TableKey且floor和deep字段都相等则返回true，否则返回false
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof TableKey){
            TableKey o = (TableKey)obj;
            return floor == o.floor && deep==o.deep;
        }
        return false;
    }

    /**
     * 重写toString方法，返回对象的字符串表示
     * @return 格式为[floor,deep]的字符串
     */
    @NonNull
    @Override
    public String toString() {
        return "["+floor+","+deep+"]";
    }
}

