package com.hitices.autopatrol.helper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.activity.MissionSelectActivity;

import java.io.File;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJICameraError;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.camera.DownloadListener;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.FetchMediaTaskContent;
import dji.sdk.camera.FetchMediaTaskScheduler;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;

/**
 * Created by dusz7 on 20180326.
 */

public class DJIMediaHelper {
    private final String TAG = DJIMediaHelper.class.getName();

    // 应用照片存储路径
    private File photoDir = new File(AutoPatrolApplication.PHOTO_DIR);

    private FetchMediaTaskScheduler scheduler;
    FetchMediaTask.Callback fetchMediaTaskCallback;
    private MediaManager mMediaManager;
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    private int currentProgress = -1;

    private Activity activity;
    private ProgressDialog mLoadingDialog;
    private ProgressDialog mDownloadDialog;

    /**
     * @param activity 用于加载uithread显示dialog等ui控件
     * @param context  用于初始化ui控件等
     * @param callback 图片Thumbnail和Preview加载完后的回调
     */
    public DJIMediaHelper(Activity activity, Context context, FetchMediaTask.Callback callback) {
        this.activity = activity;
        this.fetchMediaTaskCallback = callback;

        initDialog(context);
        initMediaManager();
    }

    // FetchMediaTask.Callback示例
//    private FetchMediaTask.Callback taskCallback = new FetchMediaTask.Callback() {
//        @Override
//        public void onUpdate(MediaFile file, FetchMediaTaskContent option, DJIError error) {
//            if (null == error) {
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
//            } else {
//                DJILog.e(TAG, "Fetch Media Task Failed" + error.getDescription());
//            }
//        }
//    };

    public List<MediaFile> getMediaFileList() {
        return this.mediaFileList;
    }

    /**
     * @param index    要删除照片的index
     * @param callback 删除完照片后的callback
     */
    public void deleteFileByIndex(final int index,
                                  CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError> callback) {
        ArrayList<MediaFile> fileToDelete = new ArrayList<MediaFile>();
        if (mediaFileList.size() > index) {
            fileToDelete.add(mediaFileList.get(index));
            mMediaManager.deleteFiles(fileToDelete, callback);
        }
    }

    // CommonCallbacks.CompletionCallbackWithTwoParam示例
//    private CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError> callback =
//            new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
//                @Override
//                public void onSuccess(List<MediaFile> x, DJICameraError y) {
//                    DJILog.e(TAG, "Delete file success");
//                    activity.runOnUiThread(new Runnable() {
//                        public void run() {
//                            MediaFile file = mediaFileList.remove(index);
//
//                            //Reset select view
//                            lastClickViewIndex = -1;
//                            lastClickView = null;
//
//                            //Update recyclerView
//                            mListAdapter.notifyItemRemoved(index);
//                        }
//                    });
//                }
//
//                @Override
//                public void onFailure(DJIError error) {
//                    ToastHelper.showShortToast("Delete file failed");
//                }
//            };

    /**
     * @param index 要下载图片的index
     */
    public void downloadFileByIndex(final int index) {
        if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA)
                || (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS)) {
            return;
        }

        downLoadFile(index, photoDir, downloadOnePicListener);
    }

    public void downLoadFile(final int index, File storePath, DownloadListener<String> listener) {
        mediaFileList.get(index).fetchFileData(storePath, null, listener);
    }

    private DownloadListener<String> downloadOnePicListener = new DownloadListener<String>() {
        @Override
        public void onFailure(DJIError error) {
            HideDownloadProgressDialog();
            ToastHelper.showShortToast("Download File Failed" + error.getDescription());
            currentProgress = -1;
        }

        @Override
        public void onProgress(long total, long current) {
        }

        @Override
        public void onRateUpdate(long total, long current, long persize) {
            int tmpProgress = (int) (1.0 * current / total * 100);
            if (tmpProgress != currentProgress) {
                mDownloadDialog.setProgress(tmpProgress);
                currentProgress = tmpProgress;
            }
        }

        @Override
        public void onStart() {
            currentProgress = -1;
            ShowDownloadProgressDialog();
        }

        @Override
        public void onSuccess(String filePath) {
            HideDownloadProgressDialog();
            ToastHelper.showShortToast("Download File Success" + ":" + filePath);
            currentProgress = -1;
        }
    };

    private void initDialog(Context context) {
        //Init Loading Dialog
        mLoadingDialog = new ProgressDialog(context);
        mLoadingDialog.setMessage("Please wait");
        mLoadingDialog.setCanceledOnTouchOutside(false);
        mLoadingDialog.setCancelable(false);

        //Init Download Dialog
        mDownloadDialog = new ProgressDialog(context);
        mDownloadDialog.setTitle("Downloading file");
        mDownloadDialog.setIcon(android.R.drawable.ic_dialog_info);
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDownloadDialog.setCanceledOnTouchOutside(false);
        mDownloadDialog.setCancelable(true);
        mDownloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mMediaManager != null) {
                    mMediaManager.exitMediaDownloading();
                }
            }
        });
    }

    private void initMediaManager() {
        if (AutoPatrolApplication.getProductInstance() == null) {
            mediaFileList.clear();
//            mListAdapter.notifyDataSetChanged();
            DJILog.e(TAG, "Product disconnected");
            return;
        } else {
            if (null != AutoPatrolApplication.getCameraInstance() && AutoPatrolApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                mMediaManager = AutoPatrolApplication.getCameraInstance().getMediaManager();
                if (null != mMediaManager) {
                    mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
//                    mMediaManager.addMediaUpdatedVideoPlaybackStateListener(this.updatedVideoPlaybackStateListener);
                    AutoPatrolApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                DJILog.e(TAG, "Set cameraMode success");
                                showProgressDialog();
                                getFileList();
                            } else {
                                ToastHelper.showShortToast("Set cameraMode failed");
                            }
                        }
                    });
//                    if (mMediaManager.isVideoPlaybackSupported()) {
//                        DJILog.e(TAG, "Camera support video playback!");
//                    } else {
//                        ToastHelper.showShortToast("Camera does not support video playback!");
//                    }
                    scheduler = mMediaManager.getScheduler();
                }

            } else if (null != AutoPatrolApplication.getCameraInstance()
                    && !AutoPatrolApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                ToastHelper.showShortToast("Media Download Mode not Supported");
            }
        }
        return;
    }

    private void getFileList() {
        mMediaManager = AutoPatrolApplication.getCameraInstance().getMediaManager();
        if (mMediaManager != null) {

            if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
                DJILog.e(TAG, "Media Manager is busy.");
            } else {
                mMediaManager.refreshFileList(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (null == error) {
                            hideProgressDialog();

                            //Reset data
                            if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
                                mediaFileList.clear();
//                                lastClickViewIndex = -1;
//                                lastClickView = null;
                            }

                            mediaFileList = mMediaManager.getFileListSnapshot();
                            Collections.sort(mediaFileList, new Comparator<MediaFile>() {
                                @Override
                                public int compare(MediaFile lhs, MediaFile rhs) {
                                    if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
                                        return 1;
                                    } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
                                        return -1;
                                    }
                                    return 0;
                                }
                            });
                            scheduler.resume(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError error) {
                                    if (error == null) {
                                        getThumbnails();
                                        getPreviews();
                                    }
                                }
                            });
                        } else {
                            hideProgressDialog();
                            ToastHelper.showShortToast("Get Media File List Failed:" + error.getDescription());
                        }
                    }
                });
            }
        }
    }

    private void getThumbnails() {
        if (mediaFileList.size() <= 0) {
            ToastHelper.showShortToast("No File info for downloading thumbnails");
            return;
        }
        for (int i = 0; i < mediaFileList.size(); i++) {
            getThumbnailByIndex(i);
        }
    }

    private void getPreviews() {
        if (mediaFileList.size() <= 0) {
            ToastHelper.showShortToast("No File info for downloading previews");
            return;
        }
        for (int i = 0; i < mediaFileList.size(); i++) {
            getPreviewByIndex(i);
        }
    }

    private void getThumbnailByIndex(final int index) {
        FetchMediaTask task = new FetchMediaTask(mediaFileList.get(index), FetchMediaTaskContent.THUMBNAIL, fetchMediaTaskCallback);
        scheduler.moveTaskToEnd(task);
    }

    private void getPreviewByIndex(final int index) {
        FetchMediaTask task = new FetchMediaTask(mediaFileList.get(index), FetchMediaTaskContent.PREVIEW, fetchMediaTaskCallback);
        scheduler.moveTaskToEnd(task);
    }

    private void showProgressDialog() {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (mLoadingDialog != null) {
                    mLoadingDialog.show();
                }
            }
        });
    }

    private void hideProgressDialog() {

        activity.runOnUiThread(new Runnable() {
            public void run() {
                if (null != mLoadingDialog && mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
            }
        });
    }

    private void ShowDownloadProgressDialog() {
        if (mDownloadDialog != null) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    mDownloadDialog.incrementProgressBy(-mDownloadDialog.getProgress());
                    mDownloadDialog.show();
                }
            });
        }
    }

    private void HideDownloadProgressDialog() {

        if (null != mDownloadDialog && mDownloadDialog.isShowing()) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    mDownloadDialog.dismiss();
                }
            });
        }
    }

    //Listeners
    private MediaManager.FileListStateListener updateFileListStateListener = new MediaManager.FileListStateListener() {
        @Override
        public void onFileListStateChange(MediaManager.FileListState state) {
            currentFileListState = state;
        }
    };

}
