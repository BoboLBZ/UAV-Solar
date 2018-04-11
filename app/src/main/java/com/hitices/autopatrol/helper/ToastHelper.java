package com.hitices.autopatrol.helper;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by dusz7 on 20180323.
 */

public class ToastHelper {

    private static ToastHelper mToastHelper;
    private static Toast mToast;

    private ToastHelper() {
        if (null == mToast) {
            mToast = Toast.makeText(ContextHelper.getApplicationContext(), "", Toast.LENGTH_SHORT);
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
        mToast.setText(mString);
        mToast.setDuration(Toast.LENGTH_SHORT);
        // mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    public void showLongToast(String mString) {
        if (mToast == null) {
            return;
        }
        mToast.setText(mString);
        mToast.setDuration(Toast.LENGTH_LONG);
        // mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }
}
