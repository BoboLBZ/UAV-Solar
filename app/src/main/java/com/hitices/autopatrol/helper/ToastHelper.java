package com.hitices.autopatrol.helper;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by dusz7 on 20180323.
 */

public class ToastHelper {

    private static ToastHelper mToastHelper;
    private static Toast mToast;

    private static String preText = null;
    private static long preTime = 0;
    private static long nextTime = 0;

    private ToastHelper() {
        if (null == mToast) {
            mToast = Toast.makeText(ContextHelper.getApplicationContext(), "", Toast.LENGTH_SHORT);
            preTime = System.currentTimeMillis();
            preText = "";
        }
    }

    public static ToastHelper getInstance() {
        if (null == mToastHelper) {
            mToastHelper = new ToastHelper();
        }
        return mToastHelper;
    }

    public void showShortToast(String mString) {
        if (mToast == null) {
            return;
        }
        nextTime = System.currentTimeMillis();
        if (nextTime - preTime > 2000) {
            mToast.setText(mString);
            preTime = nextTime;
            preText = mString;
        } else {
            mToast.setText(preText + '\n' + mString);
            preTime = nextTime;
            preText = preText + '\n' + mString;
        }
        mToast.setDuration(Toast.LENGTH_SHORT);
        // mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    public void showLongToast(String mString) {
        if (mToast == null) {
            return;
        }
        nextTime = System.currentTimeMillis();
        if (nextTime - preTime > 3500) {
            mToast.setText(mString);
            preTime = nextTime;
            preText = mString;
        } else {
            mToast.setText(preText + '\n' + mString);
            preTime = nextTime;
            preText = preText + '\n' + mString;
        }
        mToast.setText(mString);
        mToast.setDuration(Toast.LENGTH_LONG);
        // mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }
}
