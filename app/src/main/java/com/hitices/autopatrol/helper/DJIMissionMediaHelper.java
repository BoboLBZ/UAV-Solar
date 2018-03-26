package com.hitices.autopatrol.helper;

import android.app.Activity;
import android.content.Context;

import java.io.File;

import dji.common.error.DJIError;
import dji.sdk.camera.DownloadListener;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.MediaFile;

/**
 * Created by dusz7 on 20180326.
 */

public class DJIMissionMediaHelper extends DJIMediaHelper {

    // 本次任务照片存储路径
    private File missionDir = null;

    public DJIMissionMediaHelper(Activity activity, Context context, FetchMediaTask.Callback callback) {
        super(activity, context, callback);
    }
}
