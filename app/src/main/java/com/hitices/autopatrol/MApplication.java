package com.hitices.autopatrol;

/**
 * Created by Rhys on 2018/1/24.
 * email: bozliu@outlook.com
 */
import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;

public class MApplication extends Application {

    private AutoPatrolApplication application;
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (application == null) {
            application = new AutoPatrolApplication();
            application.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application.onCreate();
    }
}