package com.gyso.gysotreeviewapplication.base;

import com.gyso.treeview.model.NodeItem;

import java.util.UUID;

/**
 * @Author: 怪兽N
 * @Time: 2021/5/7  19:12
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 * @Describe: node bean
 */
public class Animal extends NodeItem {
    public int headId;
    public String name;

    public Animal(int headId, String name) {
        this.headId = headId;
        this.name = name;
        this.itemId = UUID.randomUUID().toString();
    }

    @Override
    public String toString() {
        return "Animal[" + name + "]";
    }

    @Override
    public NodeItem neuBuild() {
        return new Animal(headId, name);
    }
}
