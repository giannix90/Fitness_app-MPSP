package com.example.gianni.mpsp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;

import com.dd.CircularProgressButton;
import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.CardProvider;
import com.dexafree.materialList.view.MaterialListView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * Created by gianni on 15/07/17.
 */

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    MapView mapView;
    GoogleMap map;
    Context mContext = this;
    GMapV2Direction md;
    Document doc;

    private TextView mLatlng;

    private LatLng mPosition;
    private LatLng mArrive;

    Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    String lat, lon;

    private LocationManager mLocationManager;
    private GoogleMap googleMap;
    private String serverKey = "AIzaSyAiPheorSRX4C1LpYI5lyiVz9IY77LWjBQ";
    private CircularProgressButton mCircularProgressButton;
    private NumberPicker mMeters;
    private NumberPicker mKmeters;
    private CaloriesRandomForestClassifier mCaloriesRandomForestClassifier;
    private GlobalVariables mGlobalVariables;
    private MaterialListView mInfoMaps;
    private int mStimatedTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_layout);
        setTitle("Maps Track");
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mLatlng = (TextView) findViewById(R.id.latlng);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mGlobalVariables=new SingletonGlobalVariables().getSingleton();

        //To classify the calories
        mCaloriesRandomForestClassifier=new CaloriesRandomForestClassifier();


        //Pointer to the buttons
        mCircularProgressButton=(CircularProgressButton) findViewById(R.id.okMap);
        mMeters=(NumberPicker) findViewById(R.id.mPick);
        mKmeters=(NumberPicker) findViewById(R.id.KmPick);
        mMeters.setMaxValue(9);
        mMeters.setMinValue(0);
        mKmeters.setMaxValue(50);
        mKmeters.setMinValue(0);
        mKmeters.setValue(1);

        mInfoMaps=(MaterialListView) findViewById(R.id.infoMaps);
        mInfoMaps.getAdapter().add(new Card.Builder(mContext)
                .withProvider(new CardProvider())
                .setLayout(R.layout.material_small_image_card)
                .setTitle("Info about the path")
                .setDescription("-Number of Steps: "+"\n\n-Estimated Time"+"\n\n- Address: "+"\n\n-Calories estimated: ")
                .endConfig()
                .build()
        );

        //I permit to perform Network operation on Main thread
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            //your codes here

        }


        md = new GMapV2Direction();
        doc = md.getDocument(new LatLng(37.35, -122.0), new LatLng(37.45, -122.0),
                GMapV2Direction.MODE_DRIVING);
        // no need to set a color here, palette will generate colors for us to be set
        // setImage(R.drawable.fitness_tracker_guide_cover_2);


        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) findViewById(R.id.mapview);

        mapView.onCreate(savedInstanceState);

        //Call onMapReady
        mapView.getMapAsync(this);

        //This is used to retrive my location coordinates
        buildGoogleApiClient();


    }

    @Override
    public void onMapReady(final GoogleMap map) {

        this.map=map;

        LatLng sydney = new LatLng(-33.867, 151.206);

        //Prepare the listener for the button
        mCircularProgressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mCircularProgressButton.setIndeterminateProgressMode(true); // turn on indeterminate progress
                mCircularProgressButton.setProgress(50); // set progress > 0 & < 100 to display indeterminate progress
                new Thread(new Runnable() {

                    //Launch new thread in order to don't stop the main thread
                    @Override
                    public void run() {
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Must be runned on main thread
                                mCircularProgressButton.setProgress(100);
                                mCircularProgressButton.setProgress(0);
                                int totMeters=(mKmeters.getValue()*1000)+(mMeters.getValue()*100);
                                LatLng arrive=putArriveOnCircle(totMeters);//I obtain the coordinates to new arrive
                                String address=drawPoly(totMeters,arrive);//plot path to arrive of fixed length
                                mCircularProgressButton.setIndeterminateProgressMode(true); // turn on indeterminate progress
                                //Prepare the input instance for the random forest classifier
                                FileWriter fw= null;
                                try {
                                    fw = new FileWriter(Environment.getExternalStorageDirectory() + File.separator + "RandomForestClassifier" + File.separator + "CalorieInstancy" + ".arff",false);
                                    fw.write("@relation whatever\n"+

                                            "@attribute Passi numeric\n"+
                                            "@attribute Distanza numeric\n"+
                                            "@attribute Calorieattività numeric\n\n"+


                                            "@data\n"+(int)getStepTowalk(totMeters)+" ,"+totMeters+" , ?");
                                    fw.flush();
                                    fw.close();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                double calories=mCaloriesRandomForestClassifier.classify();

                                mInfoMaps.getAdapter().clearAll();
                                mInfoMaps.getAdapter().add(new Card.Builder(mContext)
                                        .withProvider(new CardProvider())
                                        .setLayout(R.layout.material_small_image_card)
                                        .setTitle("Info about the path")
                                        .setDescription("-Number of Steps: "+(int)getStepTowalk(totMeters)+"\n\n-Estimated Time: "+mStimatedTime/60+" min"+"\n\n- Address: "+address+"\n\n-Calories estimated: "+new DecimalFormat("##.##").format(calories)+ " KCal")
                                        .endConfig()
                                        .build()
                                );

                            }
                        });

                    }
                }).start();

            }
        });


        try {
            map.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            Log.e("MapsActivity", "Permission not granted");
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 13));


        mapView.onResume();

    }

    private double getStepTowalk(int meters){
        int step=0;
        Cursor mUser=mGlobalVariables.getmDBHelper().getUser();
        if(mUser!=null) {
            mUser.moveToNext();
            //Get the value from the record
            int mHeight = mUser.getInt(mUser.getColumnIndex(fitnessDB.User.HEIGHT));

            if(mUser.getString(mUser.getColumnIndex(fitnessDB.User.GENDER)).compareTo("Man")==0){

                step=(int)((double)meters/(((double)mHeight*0.415)/(double) 100));

            }else{
                step=(int)((double)meters/(((double)mHeight*0.413)/(double) 100));

            }
        }
        return step;
    }

    private LatLng putArriveOnCircle(int r){
        //this function put momentanely the arrive on a random position on a circle of radius r
        double radius=(double)(r/(double)111303);
        double theta=Math.random()*360;//between 0 and 360
        LatLng mArrive1=new LatLng(mPosition.latitude+(radius*Math.sin(Math.toRadians(theta)))+0.1,mPosition.longitude+(radius*Math.cos(Math.toRadians(theta)))+0.1);
        Log.e("Maps Activity","mArrive: "+mArrive1.latitude+" , "+mArrive1.longitude);
        return mArrive1;
    }


    private String drawPoly(int distance,LatLng arrive){
        //This function print a path of given length between 2 point
        Document d=null;
        try{
            map.clear();

            d=md.getDocument(mPosition, arrive,GMapV2Direction.MODE_WALKING);


            ArrayList<LatLng> directionPoint = md.getDirection(d);
            PolylineOptions rectLine = new PolylineOptions().width(5).color(
                    Color.BLUE);
            int i;
            long dist=0;
            Location a;
            Location b;

            for (i=0; i < directionPoint.size(); i++) {
                //I add the point of path until i reach the distance i want
                rectLine.add(directionPoint.get(i));
                //A each step i calculate the current distance
                a=new Location("pointa");
                a.setLatitude(directionPoint.get(i).latitude);
                a.setLongitude(directionPoint.get(i).longitude);
                b=new Location("pointb");
                b.setLatitude(directionPoint.get(i+1).latitude);
                b.setLongitude(directionPoint.get(i+1).longitude);

                dist+=a.distanceTo(b);

                if(dist>=distance && distance!=0){
                    rectLine.add(arrive=directionPoint.get(i+1));
                    break;
                }
            }
            //mArrive= directionPoint.get(i+1);
            Polyline polylin = map.addPolyline(rectLine);

            d=md.getDocument(mPosition, arrive,GMapV2Direction.MODE_WALKING);


            map.addMarker(new MarkerOptions()
                    .title("Start")
                    .snippet("Your position")
                    .position(mPosition));
            map.addMarker(new MarkerOptions()
                    .title("Arrive")
                    .snippet("You have to arrive here, distance: "+md.getDistanceValue(d)+" meters "+"Time:"+md.getDurationValue(d))
                    .position(arrive));

            //Log.e("Maps Activity","Time:"+md.getDurationValue(d)+" address: "+md.getEndAddress(d));
            mStimatedTime=md.getDurationValue(d);

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(mPosition, 13));

            mapView.onResume();
        }catch (Exception e){
            //map may be null
        }
        return md.getEndAddress(d);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(myIntent, 0);
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }


    @Override
    public void onConnected(Bundle bundle) {


        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(100); // Update location every second

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, (com.google.android.gms.location.LocationListener) this);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            lat = String.valueOf(mLastLocation.getLatitude());
            lon = String.valueOf(mLastLocation.getLongitude());
            mPosition=new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());

            mArrive=new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()+0.1);

            drawPoly(500,mArrive);
        }


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        lat = String.valueOf(location.getLatitude());
        lon = String.valueOf(location.getLongitude());

        mPosition=new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());

        mArrive=new LatLng(mLastLocation.getLatitude()+0.1,mLastLocation.getLongitude()+0.1);

    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        buildGoogleApiClient();
    }

    synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }


}
