package com.hitices.autopatrol.helper;

import android.content.Context;

/**
 * Created by dusz7 on 20180326.
 */

public class ContextHelper {
    private static Context applicationContext;

    public static void initial(Context context) {
        applicationContext = context;
    }

    public static Context getApplicationContext() {
        return applicationContext;
    }

}
