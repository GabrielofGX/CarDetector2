package com.gabriel.util;

import android.util.Log;

/**
 * Created by Administrator off 2017/5/10/010.
 */
public class Logger {
    private final static String TAG = "Car_Detect";

    public static boolean isShowLog = true;

    public static void E(String e) {
        if(isShowLog){
            Log.e(TAG, e);
        }
    }

    public static void V(String v) {
        if(isShowLog){
            Log.v(TAG, v);
        }
    }

    public static void D(String d) {
        if(isShowLog){
            Log.d(TAG, d);
        }
    }

    public static void I(String i) {
        if(isShowLog){
            Log.i(TAG, i);
        }
    }

    public static void W(String w) {
        if(isShowLog){
            Log.w(TAG, w);
        }
    }
}
