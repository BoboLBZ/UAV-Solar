package com.hitices.autopatrol.helper;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.activity.PatrolMainActivity;

/**
 * Created by dusz7 on 20180323.
 */

public class ToastHelper {

    public static void showShortToast(String msg) {
        Toast.makeText(ContextHelper.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(String msg) {
        Toast.makeText(ContextHelper.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    public static void showShortToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

//    public static void showShortToast(Activity activity, String msg) {
//        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
//    }
//
//    public static void showLongToast(Activity activity, String msg) {
//        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
//    }
}
