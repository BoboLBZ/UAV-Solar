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

import com.amap.api.maps2d.model.LatLng;
import com.hitices.autopatrol.missions.WaypointsMission;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
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
    private static final String AIRCRAFT_STATE_SAVE_IS_HIDDEN="AIRCRAFT_STATE_SAVE_IS_HIDDEN";

    private TextView tvAircraftType;
    private TextView tvCameraType;
    private SeekBar seekBarBegin;
    private Spinner spinnerMission;
    private ArrayAdapter<String> arrayAdapter;
    private OnFragmentInteractionListener mListener;
    private WaypointsMission waypointsMission;
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
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_aircraft,container,false);
        tvAircraftType = view.findViewById(R.id.aircraft_type);
        tvCameraType = view.findViewById(R.id.camera_type);
        spinnerMission = view.findViewById(R.id.missionSelected);
        seekBarBegin =view.findViewById(R.id.seekBar_begin);
        arrayAdapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_spinner_item,AutoPatrolApplication.getMissionList());
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
            if(intent.getAction().equals(AutoPatrolApplication.FLAG_CONNECTION_CHANGE))
               refreshSDKRelativeUI();
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
                if(null != mProduct.getCamera())
                   tvCameraType.setText("camera:"+mProduct.getCamera().getDisplayName());
                else {
                    tvCameraType.setText(R.string.camera_unknown);
                }
                getView().setBackgroundColor(getResources().getColor(R.color.selected));
            } else {
                tvAircraftType.setText(R.string.aircraft_unknown);
                tvCameraType.setText(R.string.camera_unknown);
                getView().setBackgroundColor(getResources().getColor(R.color.background));
            }

        } else {
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
           if(name.length()>0)
             waypointsMission=readMission(AutoPatrolApplication.missionDir+"/"+name+".xml");
           else setResultToToast("选择任务失败");
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

    public WaypointsMission readMission(String path){
        try {
            //need test,
            File file = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
//            //root
//            doc.getDocumentElement().normalize();
//            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            //name
            NodeList nodes=doc.getElementsByTagName("missionName");
            if(nodes.item(0) == null){
                return null;
            }
            WaypointsMission newMission=new WaypointsMission(nodes.item(0).getTextContent());

            nodes=doc.getElementsByTagName("speed");
            if(nodes.item(0) != null){
                newMission.speed=Float.parseFloat(nodes.item(0).getTextContent());
            }
            //finishedAction
            nodes=doc.getElementsByTagName("finishedAction");
            if(nodes.item(0) != null){
                newMission.finishedAction=getFinishedAction(nodes.item(0).getTextContent());
            }
            //headingMode
            nodes=doc.getElementsByTagName("headingMode");
            if(nodes.item(0) != null){
                newMission.headingMode=getHeadingMode(nodes.item(0).getTextContent());
            }
            //Waypoints
            nodes=doc.getElementsByTagName("Waypoints");
            Node node=nodes.item(0);
            //single waypoint
            NodeList nWaypointList = ((Element)node).getElementsByTagName("waypoint");
            for (int temp = 0; temp < nWaypointList.getLength(); temp++) {
                Node nNode = nWaypointList.item(temp);
                System.out.println("\nCurrent Element :" + nNode.getNodeName());
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    LatLng ll=new LatLng(
                            Double.parseDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent()),
                            Double.parseDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent()));
                    Waypoint w=new Waypoint(
                            ll.latitude,ll.longitude,
                            Float.parseFloat(eElement.getElementsByTagName("altitude").item(0).getTextContent()));
                    NodeList eActions=eElement.getElementsByTagName("actions");
                    for(int j=0;j<eActions.getLength();j++){
                        Node n=eActions.item(j);
                        if(n.getNodeType() == Node.ELEMENT_NODE) {
                            Element e=(Element)n;
                            NodeList t=e.getChildNodes();
                            for(int k=0;k<t.getLength();k++){
                                WaypointActionType type=getAction(t.item(k).getNodeName());
                                w.addAction(new WaypointAction(type,k));
                            }
                        }
                    }
                    newMission.waypointList.add(w);
                    newMission.waypoints.put(ll,w);
                    System.out.println("\nlat and lng" + String.valueOf(ll.latitude)+" : "+String.valueOf(ll.longitude));
                }
            }
            newMission.FLAG_ISSAVED=true;
            return newMission;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private WaypointMissionFinishedAction getFinishedAction(String s){
        if(s.equals(WaypointMissionFinishedAction.AUTO_LAND.toString())){
            return WaypointMissionFinishedAction.AUTO_LAND;
        }else if(s.equals(WaypointMissionFinishedAction.GO_FIRST_WAYPOINT.toString())) {
            return WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
        }else if(s.equals(WaypointMissionFinishedAction.GO_HOME.toString())) {
            return WaypointMissionFinishedAction.GO_HOME;
        }else  {
            return WaypointMissionFinishedAction.NO_ACTION;
        }
    }
    private WaypointMissionHeadingMode getHeadingMode(String s){
        if(s.equals(WaypointMissionHeadingMode.AUTO.toString())){
            return WaypointMissionHeadingMode.AUTO;
        }else if(s.equals(WaypointMissionHeadingMode.USING_INITIAL_DIRECTION.toString())) {
            return WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
        }else if(s.equals(WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER.toString())) {
            return WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
        }else  {
            return WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
        }
    }
    private WaypointActionType getAction(String s){
        if( WaypointActionType.STAY.toString().equals(s))
            return WaypointActionType.STAY;
        else if( WaypointActionType.CAMERA_ZOOM.toString().equals(s))
            return WaypointActionType.CAMERA_ZOOM;
        else if( WaypointActionType.CAMERA_FOCUS.toString().equals(s))
            return WaypointActionType.CAMERA_FOCUS;
        else if( WaypointActionType.GIMBAL_PITCH.toString().equals(s))
            return WaypointActionType.GIMBAL_PITCH;
        else if( WaypointActionType.START_RECORD.toString().equals(s))
            return WaypointActionType.START_RECORD;
        else if( WaypointActionType.STOP_RECORD.toString().equals(s))
            return WaypointActionType.STOP_RECORD;
        else if( WaypointActionType.ROTATE_AIRCRAFT.toString().equals(s))
            return WaypointActionType.ROTATE_AIRCRAFT;
        else if( WaypointActionType.START_TAKE_PHOTO.toString().equals(s))
            return WaypointActionType.START_TAKE_PHOTO;
        else return null;
    }
}
