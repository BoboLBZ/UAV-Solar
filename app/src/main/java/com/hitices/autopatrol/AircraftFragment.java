package com.hitices.autopatrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AircraftFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AircraftFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AircraftFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String AIRCRAFT_STATE_SAVE_IS_HIDDEN="AIRCRAFT_STATE_SAVE_IS_HIDDEN";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private TextView tvAircraftType;
    private TextView tvCameraType;
    private SeekBar seekBarBegin;
    private OnFragmentInteractionListener mListener;
    private String[] testItems = {"mission one","mission two","mission three","mission four","mission five","mission six"};

    public AircraftFragment() {
        // Required empty public constructor
    }
    public static AircraftFragment newInstance(String param1, String param2) {
        AircraftFragment fragment = new AircraftFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(AutoPatrolApplication.FLAG_CONNECTION_CHANGE);
        getActivity().registerReceiver(mReceiver, filter);

        if(savedInstanceState != null){
            boolean isSupportHidden=savedInstanceState.getBoolean(AIRCRAFT_STATE_SAVE_IS_HIDDEN);
            FragmentTransaction fragmentTransaction=getFragmentManager().beginTransaction();
            if(isSupportHidden){
                fragmentTransaction.hide(this);
            }else {
                fragmentTransaction.show(this);
            }
            fragmentTransaction.commit();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_aircraft,container,false);
        tvAircraftType = view.findViewById(R.id.aircraft_type);
        tvCameraType = view.findViewById(R.id.camera_type);
        Spinner spinnerMission = view.findViewById(R.id.missionSelected);
        seekBarBegin =view.findViewById(R.id.seekBar_begin);
        ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(view.getContext(),android.R.layout.simple_spinner_item,testItems);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMission.setAdapter(arrayAdapter);

        spinnerMission.setOnItemSelectedListener(onItemSelectedListener);
        seekBarBegin.setOnSeekBarChangeListener(onSeekBarChangeListener);
        seekBarBegin.setMax(100);
        seekBarBegin.setProgress(0);

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
    @Override
    public void onSaveInstanceState(Bundle bundle){
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(AIRCRAFT_STATE_SAVE_IS_HIDDEN,isHidden());
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
        }
    };
    private void setResultToToast(final String result) {
        Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();
    }
    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = AutoPatrolApplication.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {
            Log.v("connection", "refreshSDK: True");

            String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
            if (null != mProduct.getModel()) {
                tvAircraftType.setText("type:" + mProduct.getModel().getDisplayName());
                if(null != mProduct.getCamera())
                   tvCameraType.setText("camera:"+mProduct.getCamera().getDisplayName());
                else {
                    tvCameraType.setText(R.string.camera_unknown);
                }
            } else {
                tvAircraftType.setText(R.string.aircraft_unknown);
                tvCameraType.setText(R.string.camera_unknown);
            }

        } else {
            Log.v("connection", "refreshSDK: False");
            tvAircraftType.setText(R.string.aircraft_unknown);
            tvCameraType.setText(R.string.camera_unknown);
        }
    }
    AdapterView.OnItemSelectedListener onItemSelectedListener =new AdapterView.OnItemSelectedListener() {
       @Override
       public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

       }

       @Override
       public void onNothingSelected(AdapterView<?> adapterView) {

       }
   };
    SeekBar.OnSeekBarChangeListener onSeekBarChangeListener=new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (i > 90){
                Toast.makeText(getContext(),"begin",Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
}
