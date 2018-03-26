package com.hitices.autopatrol;

/**
 * Created by Rhys on 2018/1/24.
 * email: bozliu@outlook.com
 * 我也不知道这个类是干什么的，DJI SDK升级到4.4.2后官方demo这样用的
 */

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.hitices.autopatrol.helper.ContextHelper;
import com.secneo.sdk.Helper;

import org.litepal.LitePal;

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
        ContextHelper.initial(this);

        application.onCreate();
    }
}