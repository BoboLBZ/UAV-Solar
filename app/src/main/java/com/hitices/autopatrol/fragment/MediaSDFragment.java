package com.hitices.autopatrol.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.PatrolMainActivity;
import com.hitices.autopatrol.helper.DJIMediaHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.FetchMediaTaskContent;
import dji.sdk.camera.FetchMediaTaskScheduler;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;

public class MediaSDFragment extends Fragment {
    private static final String TAG = PatrolMainActivity.class.getName();
    private OnFragmentInteractionListener mListener;

    private GridView gridView;
    private FloatingActionButton refresh;

    private DJIMediaHelper helper;
    public MediaSDFragment() {
        // Required empty public constructor
    }

    public static MediaSDFragment newInstance() {
        MediaSDFragment fragment = new MediaSDFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the waypoint_preview_waypoint_detail for this fragment
        View view = inflater.inflate(R.layout.fragment_media_sd, container, false);
        gridView = view.findViewById(R.id.sd_imageGrid);
        refresh = view.findViewById(R.id.media_sd_refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        helper=new DJIMediaHelper(getActivity(),getContext(),taskCallback);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();
    }

    private FetchMediaTask.Callback taskCallback =new FetchMediaTask.Callback() {
        @Override
        public void onUpdate(MediaFile mediaFile, FetchMediaTaskContent fetchMediaTaskContent, DJIError djiError) {

        }
    };

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
