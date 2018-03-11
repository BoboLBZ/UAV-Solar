package com.hitices.autopatrol.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.activity.PolygonMissionEcecuteActivity;
import com.hitices.autopatrol.activity.WaypointMissionExecuteActivity;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import dji.common.error.DJIError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.useraccount.UserAccountManager;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AircraftFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AircraftFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * create by Rhys
 * email: bozliu@outlook.com
 * 显示飞行器的连接情况
 */
public class AircraftFragment extends Fragment {
    private static final String AIRCRAFT_STATE_SAVE_IS_HIDDEN="AIRCRAFT_STATE_SAVE_IS_HIDDEN";

    private TextView tvAircraftType;
    private TextView tvCameraType;
    private boolean ISCONNECTED;
    private ImageButton take_off;
    private Spinner spinnerMission;
    private ArrayAdapter<String> arrayAdapter;
    private OnFragmentInteractionListener mListener;
    private String missionName;
    private String missionType;
    public AircraftFragment() {
        // Required empty public constructor
    }
    public static AircraftFragment newInstance() {
        AircraftFragment fragment = new AircraftFragment();
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        // Inflate the waypoint_preview_waypoint_detail for this fragment
        View view= inflater.inflate(R.layout.fragment_aircraft,container,false);
        tvAircraftType = view.findViewById(R.id.aircraft_type);
        tvCameraType = view.findViewById(R.id.camera_type);
        spinnerMission = view.findViewById(R.id.missionSelected);
        take_off=view.findViewById(R.id.btn_take_off);
        take_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( missionType.equals("WaypointsMission")){
                    Intent intent=new Intent(getActivity(),WaypointMissionExecuteActivity.class);
                    intent.putExtra("missionName",missionName);
//                    intent.putExtra("missionType","type");
                    startActivity(intent);
                }else if(missionType.equals("PolygonMission")){
                    Intent intent=new Intent(getActivity(),PolygonMissionEcecuteActivity.class);
                    intent.putExtra("missionName",missionName);
//                    intent.putExtra("missionType","type");
                    startActivity(intent);
                }
            }
        });

        arrayAdapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_item, AutoPatrolApplication.getMissionList());
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMission.setAdapter(arrayAdapter);
        spinnerMission.setOnItemSelectedListener(onItemSelectedListener);

        return view;
    }
    @Override
    public void onAttach(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AutoPatrolApplication.FLAG_CONNECTION_CHANGE);
        filter.addAction("MISSION_ITEMS_CHANGE");
        context.registerReceiver(mReceiver, filter);

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
        getContext().unregisterReceiver(mReceiver);
        super.onDetach();
        mListener = null;
    }
    @Override
    public void onResume(){
        super.onResume();

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
            if(intent.getAction().equals(AutoPatrolApplication.FLAG_CONNECTION_CHANGE)) {
                refreshSDKRelativeUI();
                loginAccount();
            }
            else if(intent.getAction().equals("MISSION_ITEMS_CHANGE"))
                refreshSpinner();
        }
    };
    private void setResultToToast(final String result) {
        Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();
    }
    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = AutoPatrolApplication.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {
            Log.v("connection", "refreshSDK: True");

           // String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
            if (null != mProduct.getModel()) {
                tvAircraftType.setText("type:" + mProduct.getModel().getDisplayName());
                if(null != mProduct.getCamera()) {
                    tvCameraType.setText("camera:" + mProduct.getCamera().getDisplayName());
                    ISCONNECTED=true;
                }
                else {
                    tvCameraType.setText(R.string.camera_unknown);
                    ISCONNECTED=false;
                }
                getView().setBackgroundColor(getResources().getColor(R.color.selected));
            } else {
                ISCONNECTED=false;
                tvAircraftType.setText(R.string.aircraft_unknown);
                tvCameraType.setText(R.string.camera_unknown);
                getView().setBackgroundColor(getResources().getColor(R.color.background));
            }

        } else {
            ISCONNECTED=false;
            setResultToToast("no connection");
            Log.v("connection", "refreshSDK: False");
            tvAircraftType.setText(R.string.aircraft_unknown);
            tvCameraType.setText(R.string.camera_unknown);
            getView().setBackgroundColor(getResources().getColor(R.color.background));
        }
    }
    private void refreshSpinner(){
        arrayAdapter.clear();
        arrayAdapter.addAll(AutoPatrolApplication.getMissionList());
    }

    AdapterView.OnItemSelectedListener onItemSelectedListener =new AdapterView.OnItemSelectedListener() {
       @Override
       public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
           String name=arrayAdapter.getItem(i);
           if(name.length()>0) {
               missionName=name;
               readBaseMission(AutoPatrolApplication.missionDir + "/" + name + ".xml");
               setResultToToast(missionName+";"+missionType);
           }
           else setResultToToast("选择任务失败");
       }
       @Override
       public void onNothingSelected(AdapterView<?> adapterView) {
       }
   };

    public void readBaseMission(String path){
        try {
            //need test,
            File file = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
//            //root
            doc.getDocumentElement().normalize();
            String type=doc.getDocumentElement().getNodeName();
            System.out.println("Root element :" + type);
            //name
            NodeList nodes=doc.getElementsByTagName("missionName");
            if(nodes.item(0) == null){
                return ;
            }else {
                if(type.equals("PolygonMission"))
                {
                    missionType="PolygonMission";
                }else if(type.equals("WaypointsMission")){
                    missionType="WaypointsMission";
                }else {
                    return ;
                }
            }

            return ;
        }catch (Exception e) {
            e.printStackTrace();
            return ;
        }
    }
    private void loginAccount(){
        UserAccountManager.getInstance().logIntoDJIUserAccount(getContext(),
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>(){
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                       setResultToToast("Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }
}
