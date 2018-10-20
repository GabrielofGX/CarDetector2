package com.gabriel.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.widget.EditText;

import java.lang.reflect.Method;

/**
 * Created by Administrator on 2017/9/5/005.
 */
public class Utils {


   //获取设备IMEI编号
    public static String getDeviceIMEI(Context context){
        String imei  = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE))
                .getDeviceId();
        return imei;
    }

    /**
     * 禁止Edittext弹出软件盘，光标依然正常显示。
     */
    public static void disableShowSoftInput(EditText et) {
        if (android.os.Build.VERSION.SDK_INT <= 10) {
            et.setInputType(InputType.TYPE_NULL);
        } else {
            Class<EditText> cls = EditText.class;
            Method method;
            try {
                method = cls.getMethod("setShowSoftInputOnFocus", boolean.class);
                method.setAccessible(true);
                method.invoke(et, false);
            } catch (Exception e) {
            }

            try {
                method = cls.getMethod("setSoftInputShownOnFocus", boolean.class);
                method.setAccessible(true);
                method.invoke(et, false);
            } catch (Exception e) {
            }
        }
    }
}
