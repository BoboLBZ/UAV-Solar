package com.hitices.autopatrol.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.PolygonOptions;
import com.amap.api.maps2d.model.PolylineOptions;
import com.hitices.autopatrol.AutoPatrolApplication;
import com.hitices.autopatrol.R;
import com.hitices.autopatrol.missions.PolygonMission;
import com.hitices.autopatrol.missions.PolygonScenario;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * PolygonMissionEcecuteActivity
 * 执行区域任务
 * 根据区域，应用全覆盖路径规划算法后等到航点
 */
public class PolygonMissionEcecuteActivity extends AppCompatActivity {
    private PolygonMission polygonMission;
    public static WaypointMission.Builder builder;
    private FlightController mFlightController;
    private WaypointMissionOperator insatnce;
    private MapView mapView;
    private AMap aMap;
    private Marker droneMarker;
    private LatLng droneLocation;
    private ImageButton uplaod, start, stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polygon_mission_ececute);
        Intent intent = getIntent();
        String path = AutoPatrolApplication.missionDir + "/" + intent.getStringExtra("missionName") + ".xml";
        polygonMission = readMission(path);

        initMapView(savedInstanceState);
        addListener();
        initUI();
        initFlightController();
        showPreviewDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initFlightController();
    }

    private void initUI() {
        uplaod = findViewById(R.id.execute_uploadMission_pm);
        start = findViewById(R.id.execute_startMission_pm);
        stop = findViewById(R.id.execute_stopMission_pm);
        uplaod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadMission();
            }
        });
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMission();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopMission();
            }
        });
    }

    private void initMapView(Bundle savedInstanceState) {
        mapView = findViewById(R.id.execute_mapview_pm);
        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        UiSettings settings = aMap.getUiSettings();
        settings.setZoomControlsEnabled(false);
    }

    private void initFlightController() {
        BaseProduct product = AutoPatrolApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }
        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocation = new LatLng(djiFlightControllerCurrentState.getAircraftLocation().getLatitude(),
                                    djiFlightControllerCurrentState.getAircraftLocation().getLongitude());
                            updateDroneLocation(droneLocation);
                        }
                    });
            setResultToToast("in flight control");
        }

    }

    private void updateDroneLocation(LatLng latLng) {
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_location_marker)));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }
                droneMarker = aMap.addMarker(markerOptions);
            }
        });

    }

    private void showPreviewDialog() {
        LinearLayout pmPreview = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_preview_polygon, null);
        //ui
        final TextView name, aircraft, camera, time, startpoint, seekBar_text;
        SeekBar sb_speed;
        final EditText altitude;

        name = pmPreview.findViewById(R.id.preview_polygon_mission_name);
        aircraft = pmPreview.findViewById(R.id.preview_aircraft_name_pm);
        camera = pmPreview.findViewById(R.id.preview_camera_pm);
        time = pmPreview.findViewById(R.id.preview_time_pm);
        startpoint = pmPreview.findViewById(R.id.preview_start_point_pm);
        seekBar_text = pmPreview.findViewById(R.id.preview_seekBar_text_pm);

        sb_speed = pmPreview.findViewById(R.id.preview_speed_pm);
        altitude = pmPreview.findViewById(R.id.preview_altitude_pm);

        if (polygonMission != null) {
            sb_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    float tSpeed = (((float) i) / 100) * 15;
                    seekBar_text.setText(String.valueOf((int) (tSpeed + 0.5)) + "m/s");
                    polygonMission.setSpeed(tSpeed);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            //update text
            name.setText("任务名称:" + polygonMission.missionName);
            time.setText("预计执行时间:" + String.valueOf(polygonMission.getSpeed()));
            //convert between float and int
            //rate=waypointsMission.speed/15
            sb_speed.setProgress((int) (polygonMission.getSpeed() / 15 * 100 + 0.5));
            seekBar_text.setText(String.valueOf((int) (polygonMission.getSpeed() + 0.5)) + "m/s");

            altitude.setInputType(InputType.TYPE_CLASS_NUMBER);
            altitude.setText(String.valueOf(polygonMission.getAltitude()));
        } else {
            name.setText("XXXXXXXXXXXXXXXXXXXXXXXXXXX");
        }
        Aircraft myAircraft = AutoPatrolApplication.getAircraftInstance();
        Camera myCamera = AutoPatrolApplication.getCameraInstance();
        if (myAircraft != null) {
            aircraft.setText("飞行器型号:" + myAircraft.getModel().getDisplayName());
        } else {
            aircraft.setText("飞行器型号:xxx");
        }
        if (myCamera != null) {
            camera.setText("相机型号:" + myCamera.getDisplayName());
        } else {
            camera.setText("相机型号：xxx");
        }
        if (droneLocation != null) {
            startpoint.setText(" [" + String.valueOf(droneLocation.latitude) + "," + String.valueOf(droneLocation.longitude) + "] ");
        } else {
            startpoint.setText("null");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("")
                .setView(pmPreview)
                .setPositiveButton("upload", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        //deal with altitude
                        polygonMission.setAltitude(Float.valueOf(altitude.getText().toString()));
                        missionProcessing();
                    }
                });
    }

    private void missionProcessing() {
        loadMission();
        drawSwapLines();
        setCameraMode();
    }

    private void loadMission() {
        setResultToToast("on load");
        if (droneLocation == null) {
            droneLocation = polygonMission.getVertexs().get(0);
        }
        builder = polygonMission.getMissionBuilder(droneLocation);
        if (builder != null) {
            DJIError error = getWaypointMissionOperator().loadMission(builder.build());
            if (error == null) {
                setResultToToast("load success");
            } else {
                setResultToToast("load mission failed:" + error.getDescription());
            }
        } else {
            setResultToToast("builder is null");
        }

    }

    private void uploadMission() {
        setResultToToast("on upload");
        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error2) {
                if (error2 == null) {
                    setResultToToast("Mission upload successfully!");
                } else {
                    setResultToToast("Mission upload failed, error: " + error2.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });
    }

    private void startMission() {
        setResultToToast("on start");
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error3) {
                setResultToToast("Mission Start: " + (error3 == null ? "Successfully" : error3.getDescription()));
            }
        });
    }

    private void stopMission() {
        setResultToToast("on stop");
        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error3) {
                setResultToToast("Mission Stop: " + (error3 == null ? "Successfully" : error3.getDescription()));
            }
        });
    }

    private void setCameraMode() {
        if (AutoPatrolApplication.getGimbalInstance() != null) {
            AutoPatrolApplication.getGimbalInstance().setMode(GimbalMode.FPV, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
            Rotation.Builder b = new Rotation.Builder();
            b.mode(RotationMode.ABSOLUTE_ANGLE);
            b.pitch(-90f);
            AutoPatrolApplication.getGimbalInstance().rotate(b.build(), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        Log.i("rhys", "rotateGimbal success");
                    } else {
                        Log.i("rhys", "rotateGimbal error " + djiError.getDescription());
                    }
                }
            });
        }
    }

    private void drawSwapLines() {
        aMap.addPolygon(new PolygonOptions().addAll(polygonMission.getVertexs()).fillColor(getResources().getColor(R.color.fillColor)));
        List<LatLng> points = new ArrayList<>();
        points.add(droneLocation);
        for (int i = 0; i < polygonMission.waypointList.size(); i++) {
            points.add(new LatLng(polygonMission.waypointList.get(i).coordinate.getLatitude(),
                    polygonMission.waypointList.get(i).coordinate.getLongitude()));
        }
        aMap.addPolyline(new PolylineOptions().addAll(points).color(Color.argb(128, 1, 255, 1)));
        cameraUpdate(points.get(0));
    }

    private void cameraUpdate(LatLng pos) {
//        LatLng pos =new LatLng(droneLocationLat,droneLocationLng);
        float zoomLevel = (float) 16.0;
        CameraUpdate cameraupdate = CameraUpdateFactory.newLatLngZoom(pos, zoomLevel);
        aMap.moveCamera(cameraupdate);
    }

    private WaypointMissionOperator getWaypointMissionOperator() {
        if (insatnce == null) {
            insatnce = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        if (insatnce == null) {
            setResultToToast("waypointMissionOperator is null");
        }
        return insatnce;
    }

    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
            setResultToToast("add listener");
        }
    }

    private void removeListener() {

        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
            setResultToToast("remove listener");
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {
        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {
        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {
        }

        @Override
        public void onExecutionStart() {
        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
        }
    };

    private PolygonMission readMission(String path) {
        try {
            //need test,
            File file = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
//            //root
            doc.getDocumentElement().normalize();
            String type = doc.getDocumentElement().getNodeName();
            System.out.println("Root element :" + type);
            //name
            PolygonMission newMission;

            NodeList nodes = doc.getElementsByTagName("missionName");
            if (nodes.item(0) == null) {
                return null;
            } else {
                if (type.equals("PolygonMission")) {
                    newMission = new PolygonMission(nodes.item(0).getTextContent());
                    readPolygonMission(doc, newMission);
                } else {
                    return null;
                }
            }
            newMission.FLAG_ISSAVED = true;
            return newMission;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void readPolygonMission(Document doc, PolygonMission newPolygonMission) {
        NodeList nodes = doc.getElementsByTagName("speed");
        if (nodes.item(0) != null) {
            newPolygonMission.setSpeed(Float.parseFloat(nodes.item(0).getTextContent()));
        }
        nodes = doc.getElementsByTagName("speed");
        if (nodes.item(0) != null) {
            newPolygonMission.setSpeed(Float.parseFloat(nodes.item(0).getTextContent()));
        }
        nodes = doc.getElementsByTagName("altitude");
        if (nodes.item(0) != null) {
            newPolygonMission.setAltitude(Float.parseFloat(nodes.item(0).getTextContent()));
        }
        nodes = doc.getElementsByTagName("scenario");
        if (nodes.item(0) != null) {
            String s = nodes.item(0).getTextContent();
            if (s.equals(PolygonScenario.TYPEA.name()))
                newPolygonMission.setScenario(PolygonScenario.TYPEA);
            else if (s.equals(PolygonScenario.TYPEB.name()))
                newPolygonMission.setScenario(PolygonScenario.TYPEB);
        }
        nodes = doc.getElementsByTagName("horizontalOverlapRate");
        if (nodes.item(0) != null) {
            newPolygonMission.setHorizontalOverlapRate(Integer.parseInt(nodes.item(0).getTextContent()));
        }
        nodes = doc.getElementsByTagName("verticalOverlapRate");
        if (nodes.item(0) != null) {
            newPolygonMission.setVerticalOverlapRate(Integer.parseInt(nodes.item(0).getTextContent()));
        }

        nodes = doc.getElementsByTagName("Vertexs");
        Node node = nodes.item(0);
        NodeList nVertexList = ((Element) node).getElementsByTagName("vertex");
        for (int temp = 0; temp < nVertexList.getLength(); temp++) {
            Node nNode = nVertexList.item(temp);
            System.out.println("\nvertex :" + nNode.getNodeName());
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                LatLng ll = new LatLng(
                        Double.parseDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent()),
                        Double.parseDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent()));
                newPolygonMission.addVertex(ll);
                System.out.println("\npolygon" + String.valueOf(ll.latitude) + " : " + String.valueOf(ll.longitude));
            }
        }
        newPolygonMission.FLAG_ISSAVED = true;
    }

    private void setResultToToast(final String msg) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
