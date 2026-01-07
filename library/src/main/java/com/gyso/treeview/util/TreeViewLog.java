package com.gyso.treeview.util;

import android.util.Log;


/**
 * @Author: 怪兽N
 * @Time: 2021/5/8  15:10
 * @Email: 674149099@qq.com
 * @WeChat: guaishouN
 * @Describe: logger
 */
public class TreeViewLog {
    public static boolean isDebug = true;

    public static void d(String tag, String msg) {
        if (isDebug) {
            Log.d(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (isDebug) {
            Log.e(tag, msg);
        }
    }
}
