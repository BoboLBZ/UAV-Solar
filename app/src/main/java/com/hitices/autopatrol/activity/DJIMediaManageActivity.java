package com.hitices.autopatrol.activity;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextView;

import com.hitices.autopatrol.R;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.camera.FetchMediaTaskScheduler;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;

public class DJIMediaManageActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = DJIMediaManageActivity.class.getName();
    private static DJIMediaManageActivity activity;
    private Button mBackBtn, mDeleteBtn, mReloadBtn, mDownloadBtn, mStatusBtn;
    private Button mPlayBtn, mResumeBtn, mPauseBtn, mStopBtn, mMoveToBtn;
    private RecyclerView listView;
    private SlidingDrawer mPushDrawerSd;
    private ImageView mDisplayImageView;
    private TextView mPushTv;

//    private FileListAdapter mListAdapter;
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    private MediaManager mMediaManager;
    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    private ProgressDialog mLoadingDialog;
    private FetchMediaTaskScheduler scheduler;
    private int lastClickViewIndex = -1;
    private View lastClickView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dji_media_manage);
        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        lastClickView = null;
//        DemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
//            @Override
//            public void onResult(DJIError mError) {
//                if (mError != null) {
//                    ToastHelper.showShortToast(activity, "Set Shoot Photo Mode Failed" + mError.getDescription());
//                }
//            }
//        });
        if (mediaFileList != null) {
            mediaFileList.clear();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_btn: {
                break;
            }
            case R.id.delete_btn: {
                break;
            }
            case R.id.reload_btn: {
                break;
            }
            case R.id.download_btn: {
                break;
            }
            case R.id.status_btn: {
                break;
            }
            case R.id.play_btn: {
                break;
            }
            case R.id.resume_btn: {
                break;
            }
            case R.id.pause_btn: {
                break;
            }
            case R.id.stop_btn: {
                break;
            }
            case R.id.moveTo_btn: {
                break;
            }
            default:
                break;
        }
    }

    void initUI() {
        //Init RecyclerView
        listView = (RecyclerView) findViewById(R.id.filelistView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(DJIMediaManageActivity.this, OrientationHelper.VERTICAL, false);
        listView.setLayoutManager(layoutManager);
        mPushDrawerSd = (SlidingDrawer) findViewById(R.id.pointing_drawer_sd);
        mPushTv = (TextView) findViewById(R.id.pointing_push_tv);
        mBackBtn = (Button) findViewById(R.id.back_btn);
        mDeleteBtn = (Button) findViewById(R.id.delete_btn);
        mDownloadBtn = (Button) findViewById(R.id.download_btn);
        mReloadBtn = (Button) findViewById(R.id.reload_btn);
        mStatusBtn = (Button) findViewById(R.id.status_btn);
        mPlayBtn = (Button) findViewById(R.id.play_btn);
        mResumeBtn = (Button) findViewById(R.id.resume_btn);
        mPauseBtn = (Button) findViewById(R.id.pause_btn);
        mStopBtn = (Button) findViewById(R.id.stop_btn);
        mMoveToBtn = (Button) findViewById(R.id.moveTo_btn);
        mDisplayImageView = (ImageView) findViewById(R.id.imageView);
        mDisplayImageView.setVisibility(View.VISIBLE);
        mBackBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);
        mReloadBtn.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);
        mStatusBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mResumeBtn.setOnClickListener(this);
        mPauseBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mMoveToBtn.setOnClickListener(this);

        //Init FileListAdapter
//        mListAdapter = new FileListAdapter();
//        listView.setAdapter(mListAdapter);
        //Init Loading Dialog
        mLoadingDialog = new ProgressDialog(activity);
        mLoadingDialog.setMessage("Please wait");
        mLoadingDialog.setCanceledOnTouchOutside(false);
        mLoadingDialog.setCancelable(false);
    }

    private void showProgressDialog() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mLoadingDialog != null) {
                    mLoadingDialog.show();
                }
            }
        });
    }

    private void hideProgressDialog() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (null != mLoadingDialog && mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
            }
        });
    }

    private MediaManager.FileListStateListener updateFileListStateListener = new MediaManager.FileListStateListener() {
        @Override
        public void onFileListStateChange(MediaManager.FileListState state) {
            currentFileListState = state;
        }
    };
//    private void initMediaManager() {
//        if (DemoApplication.getProductInstance() == null) {
//            mediaFileList.clear();
//            mListAdapter.notifyDataSetChanged();
//            DJILog.e(TAG, "Product disconnected");
//            return;
//        } else {
//            if (null != DemoApplication.getCameraInstance() && DemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
//                mMediaManager = DemoApplication.getCameraInstance().getMediaManager();
//            } else if (null != DemoApplication.getCameraInstance()
//                    && !DemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
//                setResultToToast("Media Download Mode not Supported");
//            }
//        }
//        return;
//    }
}
