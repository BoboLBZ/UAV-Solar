package com.hitices.autopatrol.helper;

import android.content.Context;
import android.widget.Toast;

import com.hitices.autopatrol.activity.PatrolMainActivity;

/**
 * Created by dusz7 on 20180323.
 */

public class ToastHelper {

    public static void showShortToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }
}
