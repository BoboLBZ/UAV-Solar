package com.hitices.autopatrol.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hitices.autopatrol.R;
import com.hitices.autopatrol.adapter.MediaViewPagerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MediaFragment extends Fragment {
    private static final String MEDIA_STATE_SAVE_IS_HIDDEN = "MEDIA_STATE_SAVE_IS_HIDDEN";
    TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };
    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
    private OnFragmentInteractionListener mListener;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    public MediaFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static MediaFragment newInstance() {
        MediaFragment fragment = new MediaFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            boolean isSupportHidden = savedInstanceState.getBoolean(MEDIA_STATE_SAVE_IS_HIDDEN);
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            if (isSupportHidden) {
                fragmentTransaction.hide(this);
            } else {
                fragmentTransaction.show(this);
            }
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(MEDIA_STATE_SAVE_IS_HIDDEN, isHidden());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the waypoint_preview_waypoint_detail for this fragment
        View view = inflater.inflate(R.layout.fragment_media, container, false);
        tabLayout = view.findViewById(R.id.media_tab);
        viewPager = view.findViewById(R.id.media_viewPager);
        tabLayout.addTab(tabLayout.newTab().setText("本地"));
        tabLayout.addTab(tabLayout.newTab().setText("相机"));
        tabLayout.addOnTabSelectedListener(onTabSelectedListener);
        tabLayout.setupWithViewPager(viewPager);

        //test
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(MediaLocalFragment.newInstance());
        fragments.add(MediaSDFragment.newInstance());

        String[] titles = new String[]{"本地", "相机"};
        MediaViewPagerAdapter viewPagerAdapter =
                new MediaViewPagerAdapter(getActivity().getSupportFragmentManager(), fragments, Arrays.asList(titles));
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(onPageChangeListener);
        setHasOptionsMenu(true);
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
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
