package com.hitices.autopatrol.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.helper.DJIMediaHelper;
import com.hitices.autopatrol.helper.ToastHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.camera.DownloadListener;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.FetchMediaTaskContent;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;

// 该Activity用于选择任务，同时下载本次任务采集图片，跳转到结果展示界面
public class MissionSelectActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = MissionSelectActivity.class.getName();

//    File destDir = new File(AutoPatrolApplication.PHOTO_DIR);
//    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
//    private MediaManager mMediaManager;
//    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
//    private int currentProgress = -1;
//
//    private ProgressDialog mLoadingDialog;
//    private ProgressDialog mDownloadDialog;

    private DJIMediaHelper mediaHelper;

    private Button selectMissionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_select);

        ToastHelper.showShortToast("select the mission");
        initUI();
        mediaHelper = new DJIMediaHelper(this, this, taskCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();

//        initMediaManager();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    private void initUI() {

//        //Init Loading Dialog
//        mLoadingDialog = new ProgressDialog(MissionSelectActivity.this);
//        mLoadingDialog.setMessage("Please wait");
//        mLoadingDialog.setCanceledOnTouchOutside(false);
//        mLoadingDialog.setCancelable(false);
//
//        //Init Download Dialog
//        mDownloadDialog = new ProgressDialog(MissionSelectActivity.this);
//        mDownloadDialog.setTitle("Downloading file");
//        mDownloadDialog.setIcon(android.R.drawable.ic_dialog_info);
//        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        mDownloadDialog.setCanceledOnTouchOutside(false);
//        mDownloadDialog.setCancelable(true);
//        mDownloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                if (mMediaManager != null) {
//                    mMediaManager.exitMediaDownloading();
//                }
//            }
//        });

        selectMissionButton = findViewById(R.id.btn_select_mission);
        selectMissionButton.setOnClickListener(this);
    }

    // FetchMediaTask.Callback示例
    private FetchMediaTask.Callback taskCallback = new FetchMediaTask.Callback() {
        @Override
        public void onUpdate(MediaFile file, FetchMediaTaskContent option, DJIError error) {
            if (null == error) {
//                if (option == FetchMediaTaskContent.PREVIEW) {
//                    runOnUiThread(new Runnable() {
//                        public void run() {
//                            mListAdapter.notifyDataSetChanged();
//                        }
//                    });
//                }
//                if (option == FetchMediaTaskContent.THUMBNAIL) {
//                    runOnUiThread(new Runnable() {
//                        public void run() {
//                            mListAdapter.notifyDataSetChanged();
//                        }
//                    });
//                }
            } else {
                DJILog.e(TAG, "Fetch Media Task Failed" + error.getDescription());
            }
        }
    };

//    private void initMediaManager() {
//        if (AutoPatrolApplication.getProductInstance() == null) {
//            mediaFileList.clear();
//            DJILog.e(TAG, "Product disconnected");
//            return;
//        } else {
//            if (null != AutoPatrolApplication.getCameraInstance() && AutoPatrolApplication.getCameraInstance().isMediaDownloadModeSupported()) {
//                mMediaManager = AutoPatrolApplication.getCameraInstance().getMediaManager();
//                if (null != mMediaManager) {
//                    mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
////                    mMediaManager.addMediaUpdatedVideoPlaybackStateListener(this.updatedVideoPlaybackStateListener);
//                    AutoPatrolApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
//                        @Override
//                        public void onResult(DJIError error) {
//                            if (error == null) {
//                                DJILog.e(TAG, "Set cameraMode success");
//                                showProgressDialog();
//                                getFileList();
//                            } else {
//                                ToastHelper.showShortToast("Set cameraMode failed");
//                            }
//                        }
//                    });
//                    if (mMediaManager.isVideoPlaybackSupported()) {
//                        DJILog.e(TAG, "Camera support video playback!");
//                    } else {
//                        ToastHelper.showShortToast("Camera does not support video playback!");
//                    }
//                }
//
//            } else if (null != AutoPatrolApplication.getCameraInstance()
//                    && !AutoPatrolApplication.getCameraInstance().isMediaDownloadModeSupported()) {
//                ToastHelper.showShortToast("Media Download Mode not Supported");
//            }
//        }
//        return;
//    }

//    private void getFileList() {
//        mMediaManager = AutoPatrolApplication.getCameraInstance().getMediaManager();
//        if (mMediaManager != null) {
//
//            if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
//                DJILog.e(TAG, "Media Manager is busy.");
//            } else {
//                mMediaManager.refreshFileList(new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError error) {
//                        if (null == error) {
//                            hideProgressDialog();
//
//                            //Reset data
//                            if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
//                                mediaFileList.clear();
//                            }
//
//                            mediaFileList = mMediaManager.getFileListSnapshot();
//                            Collections.sort(mediaFileList, new Comparator<MediaFile>() {
//                                @Override
//                                public int compare(MediaFile lhs, MediaFile rhs) {
//                                    if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
//                                        return 1;
//                                    } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
//                                        return -1;
//                                    }
//                                    return 0;
//                                }
//                            });
//                        } else {
//                            hideProgressDialog();
//                            ToastHelper.showShortToast("Get Media File List Failed:" + error.getDescription());
//                        }
//                    }
//                });
//            }
//        }
//    }

//    private void downloadFileByIndex(final int index) {
//        if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA)
//                || (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS)) {
//            return;
//        }
//
//        mediaFileList.get(index).fetchFileData(destDir, null, new DownloadListener<String>() {
//            @Override
//            public void onFailure(DJIError error) {
//                HideDownloadProgressDialog();
//                ToastHelper.showShortToast("Download File Failed" + error.getDescription());
//                currentProgress = -1;
//            }
//
//            @Override
//            public void onProgress(long total, long current) {
//            }
//
//            @Override
//            public void onRateUpdate(long total, long current, long persize) {
//                int tmpProgress = (int) (1.0 * current / total * 100);
//                if (tmpProgress != currentProgress) {
//                    mDownloadDialog.setProgress(tmpProgress);
//                    currentProgress = tmpProgress;
//                }
//            }
//
//            @Override
//            public void onStart() {
//                currentProgress = -1;
//                ShowDownloadProgressDialog();
//            }
//
//            @Override
//            public void onSuccess(String filePath) {
//                HideDownloadProgressDialog();
//                ToastHelper.showShortToast("Download File Success" + ":" + filePath);
//                currentProgress = -1;
//            }
//        });
//    }

//    private void showProgressDialog() {
//        runOnUiThread(new Runnable() {
//            public void run() {
//                if (mLoadingDialog != null) {
//                    mLoadingDialog.show();
//                }
//            }
//        });
//    }
//
//    private void hideProgressDialog() {
//
//        runOnUiThread(new Runnable() {
//            public void run() {
//                if (null != mLoadingDialog && mLoadingDialog.isShowing()) {
//                    mLoadingDialog.dismiss();
//                }
//            }
//        });
//    }
//
//    private void ShowDownloadProgressDialog() {
//        if (mDownloadDialog != null) {
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    mDownloadDialog.incrementProgressBy(-mDownloadDialog.getProgress());
//                    mDownloadDialog.show();
//                }
//            });
//        }
//    }
//
//    private void HideDownloadProgressDialog() {
//
//        if (null != mDownloadDialog && mDownloadDialog.isShowing()) {
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    mDownloadDialog.dismiss();
//                }
//            });
//        }
//    }
//
//    //Listeners
//    private MediaManager.FileListStateListener updateFileListStateListener = new MediaManager.FileListStateListener() {
//        @Override
//        public void onFileListStateChange(MediaManager.FileListState state) {
//            currentFileListState = state;
//        }
//    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_select_mission:

                ToastHelper.showShortToast("start download");
                for (int i = 0; i < mediaHelper.getMediaFileList().size(); i++) {
                    mediaHelper.downloadFileByIndex(i);
                }

                break;
            default:
                break;
        }
    }

}
