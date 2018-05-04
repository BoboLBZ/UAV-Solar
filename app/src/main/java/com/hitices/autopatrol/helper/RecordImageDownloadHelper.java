package com.hitices.autopatrol.helper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;

import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.DataDownloadActivity;
import com.hitices.autopatrol.entity.dataSupport.FlightRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dji.common.error.DJIError;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.DownloadListener;
import dji.sdk.camera.MediaFile;

/**
 * Created by dusz7 on 20180326.
 */

public class RecordImageDownloadHelper {

    private final String TAG = RecordImageDownloadHelper.class.getName();

    private DJIMediaHelper djiMediaHelper = null;
    private FlightRecord flightRecord = null;
    private int recordImageNum = 0;
    private int downloadNumNow = 0;
    private int currentProgress = -1;
    // 本次任务照片存储路径
    private File recordVisibleDir = null;
    private File recordInfraredDir = null;

    private Date executeStartDate = null;
    private Date executeEndDate = null;

    private FilesDownloadCompleteListener completeListener;

    private Activity activity;
    private ProgressDialog mDownloadDialog;

    public RecordImageDownloadHelper(Activity activity, Context context) {
        // init djiMedia
        djiMediaHelper = new DJIMediaHelper(activity, context, null);
        this.activity = activity;
        initDialog(context);
    }

    private void setFlightRecord(FlightRecord record) {
        if (null != record) {
            this.flightRecord = record;
            // get recordVisibleDir
            recordVisibleDir = RecordImageHelper.getRecordVisibleImagePath(record);
            recordInfraredDir = RecordImageHelper.getRecordInfraredImagePath(record);
            // get missionDate
            executeStartDate = record.getStartTime();
            executeEndDate = record.getEndTime();
        } else {
            Log.d(TAG, "record is null");
        }
    }

    private void setCompleteListener(FilesDownloadCompleteListener listener) {
        this.completeListener = listener;
    }

    public void downloadFilesByMissionRecord(FlightRecord record, FilesDownloadCompleteListener listener) {
        BaseProduct mProduct = AutoPatrolApplication.getProductInstance();
        if (null != mProduct && mProduct.isConnected() &&
                null != mProduct.getModel() && null != mProduct.getCamera()) {
            Log.d(TAG, "product connected");

        } else {
            Log.d(TAG, "product not connect");
            ToastHelper.getInstance().showShortToast("无人机断开连接");
            return;
        }

        Log.d(TAG, "download by mission record");
        setFlightRecord(record);
        setCompleteListener(listener);

        if (executeStartDate == null || executeEndDate == null) {
            return;
        }
        List<Integer> missionFilesIndex = new ArrayList<>();
        Log.d(TAG, "mission start date:" + executeStartDate);
        Log.d(TAG, "mission end date:" + executeEndDate);
        // 目前只针对可见光图像集
        // 在这里该怎么处理一下呢？？？？？
        if (flightRecord.isHasVisible() && flightRecord.isHasInfrared()) {

        }
        if (flightRecord.isHasVisible()) {

        }
        if (flightRecord.isHasInfrared()) {

        }
        // 获得符合时间要求的照片
        int fileIndex = 0;
        for (MediaFile file : djiMediaHelper.getMediaFileList()) {
            String fileCreateDateStr = file.getDateCreated();
            Date fileCreateDate = getMediaFileDate(fileCreateDateStr);
            if (fileCreateDate.equals(executeStartDate) ||
                    fileCreateDate.equals(executeEndDate) ||
                    (fileCreateDate.after(executeStartDate) && fileCreateDate.before(executeEndDate))) {
                missionFilesIndex.add(fileIndex);
            }
            fileIndex++;
        }
        // 开始下载
        recordImageNum = missionFilesIndex.size();
        downloadNumNow = 0;
        showDownloadProgressDialog();
        for (int index : missionFilesIndex) {
            djiMediaHelper.downLoadFile(index, recordVisibleDir, downloadOnePicListener);
        }

    }

    private void initDialog(Context context) {

        //Init Download Dialog
        mDownloadDialog = new ProgressDialog(context);
        mDownloadDialog.setTitle(ContextHelper.getApplicationContext().getResources().getString(R.string.photo_download_title));
        mDownloadDialog.setIcon(android.R.drawable.ic_dialog_info);
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDownloadDialog.setCanceledOnTouchOutside(false);
        mDownloadDialog.setCancelable(true);
        mDownloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (djiMediaHelper.getmMediaManager() != null) {
                    djiMediaHelper.getmMediaManager().exitMediaDownloading();
                }
            }
        });
    }

    private DownloadListener<String> downloadOnePicListener = new DownloadListener<String>() {
        @Override
        public void onFailure(DJIError error) {
            hideDownloadProgressDialog();
            ToastHelper.getInstance().showShortToast("Download File Failed" + error.getDescription());
            currentProgress = -1;
        }

        @Override
        public void onProgress(long total, long current) {
        }

        @Override
        public void onRateUpdate(long total, long current, long persize) {
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onSuccess(String filePath) {
//            ToastHelper.getInstance().showShortToast("Download File Success" + ":" + filePath);
            downloadNumNow++;
            int tmpProgress = (int) (1.0 * downloadNumNow / recordImageNum * 100);
            if (tmpProgress != currentProgress) {
                mDownloadDialog.setProgress(tmpProgress);
                currentProgress = tmpProgress;
            }
            if (downloadNumNow == recordImageNum) {
                // 最后一张图片下载完成
                // 等上面执行完了才显示与执行
                ToastHelper.getInstance().showShortToast("本次任务一共拍摄 " + recordImageNum + " 张照片");
                if (null != completeListener) {
                    completeListener.onComplete();
                }
                hideDownloadProgressDialog();
            }
        }
    };

    private void showProgressDialog() {
//        activity.runOnUiThread(new Runnable() {
//            public void run() {
//                if (mLoadingDialog != null) {
//                    mLoadingDialog.show();
//                }
//            }
//        });
    }

    private void hideProgressDialog() {
//        activity.runOnUiThread(new Runnable() {
//            public void run() {
//                if (null != mLoadingDialog && mLoadingDialog.isShowing()) {
//                    mLoadingDialog.dismiss();
//                }
//            }
//        });
    }

    private void showDownloadProgressDialog() {
        if (mDownloadDialog != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    mDownloadDialog.incrementProgressBy(-mDownloadDialog.getProgress());
                    mDownloadDialog.show();
                }
            });
        }
    }

    private void hideDownloadProgressDialog() {
        if (null != mDownloadDialog && mDownloadDialog.isShowing()) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    mDownloadDialog.dismiss();
                }
            });
        }
    }

    private Date getMediaFileDate(String mediaFileDateStr) {
        String[] strs1 = mediaFileDateStr.split(" +");
        String[] date = strs1[0].split("-");
        String[] time = strs1[1].split(":");

        return new Date(Integer.parseInt(date[0]) - 1900, Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]),
                Integer.parseInt(time[0]), Integer.parseInt(time[1]), Integer.parseInt(time[2]));
    }

    public interface FilesDownloadCompleteListener {
        public void onComplete();
    }
}
