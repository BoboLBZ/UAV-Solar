package com.hitices.autopatrol.helper;

import android.app.Activity;
import android.content.Context;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private Date missionStartDate = null;
    private Date missionEndDate = null;

    private FilesDownloadCompleteListener completeListener;

    public DJIMissionMediaHelper(Activity activity, Context context, FetchMediaTask.Callback callback) {
        super(activity, context, callback);
    }

    public void setCompleteListener(FilesDownloadCompleteListener listener) {
        this.completeListener = listener;
    }

    public void analyseMission(MyMission mission) {
        // get missionDir

        // get missionDate
    }

    public void downloadFilesByMission(File missionStorePath) {
        if (missionStartDate == null || missionEndDate == null) {
            return;
        }
        List<MediaFile> missionFileList = new ArrayList<>();
        for (MediaFile file : super.getMediaFileList()) {
            String fileCreateDateStr = file.getDateCreated();
            try {
                Date fileCreateDate = DateFormat.getDateInstance().parse(fileCreateDateStr);
                if (fileCreateDate.equals(missionStartDate) ||
                        fileCreateDate.equals(missionEndDate) ||
                        (fileCreateDate.after(missionStartDate) && fileCreateDate.before(missionEndDate))) {

                    downLoadFile(file.getIndex(), missionStorePath, downloadOnePicListener);
                    missionFileList.add(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        ToastHelper.showShortToast("本次任务一共拍摄 " + missionFileList.size() + " 张照片");
        if (null != completeListener) {
            completeListener.onComplete();
        }
    }

    private DownloadListener<String> downloadOnePicListener = new DownloadListener<String>() {
        @Override
        public void onFailure(DJIError error) {
//            HideDownloadProgressDialog();
            ToastHelper.showShortToast("Download File Failed" + error.getDescription());
//            currentProgress = -1;
        }

        @Override
        public void onProgress(long total, long current) {
        }

        @Override
        public void onRateUpdate(long total, long current, long persize) {
//            int tmpProgress = (int) (1.0 * current / total * 100);
//            if (tmpProgress != currentProgress) {
//                mDownloadDialog.setProgress(tmpProgress);
//                currentProgress = tmpProgress;
//            }
        }

        @Override
        public void onStart() {
//            currentProgress = -1;
//            ShowDownloadProgressDialog();
        }

        @Override
        public void onSuccess(String filePath) {
//            HideDownloadProgressDialog();
            ToastHelper.showShortToast("Download File Success" + ":" + filePath);
//            currentProgress = -1;
        }
    };
}


interface FilesDownloadCompleteListener {
    public void onStart();

    public void onComplete();
}
