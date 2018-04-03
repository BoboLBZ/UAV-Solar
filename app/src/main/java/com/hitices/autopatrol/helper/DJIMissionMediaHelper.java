package com.hitices.autopatrol.helper;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.entity.FlightRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.MediaFile;

/**
 * Created by dusz7 on 20180326.
 */

public class DJIMissionMediaHelper extends DJIMediaHelper {

    private final String TAG = DJIMissionMediaHelper.class.getName();

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

    public void analyseMission(FlightRecord mission) {
        // get missionDir
        missionDir = new File(AutoPatrolApplication.MISSION_PHOTO_DIR + "/" +
                mission.getName() + "_" + mission.getStartTime());
        // get missionDate
        missionStartDate = mission.getStartTime();
        missionEndDate = mission.getEndTime();
    }

    public void downloadFilesByMission() {
        if (missionStartDate == null || missionEndDate == null) {
            return;
        }
        List<MediaFile> missionFileList = new ArrayList<>();
        Log.d(TAG, "mission start date:" + missionStartDate);
        Log.d(TAG, "mission end date:" + missionEndDate);
        int fileIndex = 0;
        for (MediaFile file : super.getMediaFileList()) {
            String fileCreateDateStr = file.getDateCreated();
            Date fileCreateDate = getMediaFileDate(fileCreateDateStr);
            Log.d(TAG, "file create date:" + fileCreateDate);
            if (fileCreateDate.equals(missionStartDate) ||
                    fileCreateDate.equals(missionEndDate) ||
                    (fileCreateDate.after(missionStartDate) && fileCreateDate.before(missionEndDate))) {

                downLoadFile(fileIndex, missionDir, downloadOnePicListener);
                missionFileList.add(file);
            }
            fileIndex++;
        }
        ToastHelper.showShortToast("本次任务一共拍摄 " + missionFileList.size() + " 张照片");
        if (null != completeListener) {
            // 添加handler？？？
            completeListener.onComplete();
        }
    }

//    private DownloadListener<String> downloadOnePicListener = new DownloadListener<String>() {
//        @Override
//        public void onFailure(DJIError error) {
////            HideDownloadProgressDialog();
//            ToastHelper.showShortToast("Download File Failed" + error.getDescription());
////            currentProgress = -1;
//        }
//
//        @Override
//        public void onProgress(long total, long current) {
//        }
//
//        @Override
//        public void onRateUpdate(long total, long current, long persize) {
////            int tmpProgress = (int) (1.0 * current / total * 100);
////            if (tmpProgress != currentProgress) {
////                mDownloadDialog.setProgress(tmpProgress);
////                currentProgress = tmpProgress;
////            }
//        }
//
//        @Override
//        public void onStart() {
////            currentProgress = -1;
////            ShowDownloadProgressDialog();
//        }
//
//        @Override
//        public void onSuccess(String filePath) {
////            HideDownloadProgressDialog();
//            ToastHelper.showShortToast("Download File Success" + ":" + filePath);
////            currentProgress = -1;
//        }
//    };

    private Date getMediaFileDate(String mediaFileDateStr) {
        String[] strs1 = mediaFileDateStr.split(" +");
        String[] date = strs1[0].split("-");
        String[] time = strs1[1].split(":");

        return new Date(Integer.parseInt(date[0]) - 1900, Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]),
                Integer.parseInt(time[0]), Integer.parseInt(time[1]), Integer.parseInt(time[2]));
    }

    public File getMissionDir() {
        return missionDir;
    }

    public interface FilesDownloadCompleteListener {
        public void onStart();

        public void onComplete();
    }
}
