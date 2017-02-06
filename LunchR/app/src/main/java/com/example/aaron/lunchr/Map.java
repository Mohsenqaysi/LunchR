package com.example.aaron.lunchr;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * Activity to display a map fragment which will give the user directions from their current location to
 * the destination they selected
 */
public class Map extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private boolean is_network_connected, network_is_wifi, network_is_data;
    private String url = "";//"https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=53.3475933,-6.2597447&radius=100&type=food&key=AIzaSyAsyz_cGfPY1Jf57ow9LFEA267DoN_tfzU";
    private Place destination;
    private LatLng curr_location;

    /**
     * On creation of the activity, get the url string to parse for the destination from the intent that stated
     * the activity, set up the map fragment and add location API
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        is_network_connected = network_is_wifi = network_is_data = false;
        url = getIntent().getStringExtra("url");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Get the URL from the Intent
        Intent intent = getIntent();
        url = intent.getStringExtra("URL");

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        setUp();
    }

    /**
     * setUp() will perform the map setup. Setting the map type, button layout, location information
     * and perform some fancy camera animations around the set route
     */
    private void setUp() {
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        try {
            map.setMyLocationEnabled(true);
        } catch (SecurityException se) {
            Toast.makeText(getApplicationContext(), "Location not enabled!", Toast.LENGTH_LONG).show();
        }
        map.setIndoorEnabled(true);
        map.setBuildingsEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            public void onMapLoaded() {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                builder.include(destination.marker.getPosition());

                // curr_location will be null on android emulators. adding a check for this
                if (curr_location != null ) {
                    builder.include(curr_location);
                } else {
                    curr_location = new LatLng(51.116492, -0.541767);
                    Toast.makeText(getApplicationContext(), "Can't get current location!", Toast.LENGTH_LONG).show();
                }

                LatLngBounds bounds = builder.build();

                int width = getResources().getDisplayMetrics().widthPixels;
                int height = getResources().getDisplayMetrics().heightPixels;
                int padding = (int) (width * 0.20);

                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                double xDiff = destination.location.latitude - curr_location.latitude;
                double yDiff = destination.location.longitude - curr_location.longitude;
                final double bearing = Math.atan2(yDiff, xDiff) * 180.0 / Math.PI;
                map.animateCamera(cu, 3000, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder(map.getCameraPosition())
                                .bearing(map.getCameraPosition().bearing)
                                .zoom(map.getCameraPosition().zoom)
                                .tilt(45)
                                .build()), 2000, new GoogleMap.CancelableCallback() {
                            @Override
                            public void onFinish() {
                                map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                                        .target(curr_location)
                                        .zoom(16).bearing((float) bearing).tilt(30).build()), 3000, null);
                            }

                            public void onCancel() {
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                    }
                });
            }
        });
        this.addLocation();
    }

    /**
     * Overriding the superclass here to connect to the location service
     */
    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    /**
     * Overriding the superclass here to disconnect from the location service
     */
    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Perform the basic set up and functionality of this activity's location service (specify update interval)
     * and animate the camera to move to the last known/current location of the user
     * @param connectionHint
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            //request specific permission from user
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        curr_location = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(curr_location)
                .zoom(18).bearing(0).tilt(30).build();
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        Directions dir = new Directions(Map.this, map);
        dir.setDirections(curr_location, destination.location);
    }

    /**
     * Adds a new location for the destination if the user is connected to the internet
     */
    private void addLocation(){
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        is_network_connected = ni != null && ni.isConnectedOrConnecting();
        if(is_network_connected){
            try{
                String str = new JsonTask().execute(url).get();
                JSONArray results = new JSONObject(str).getJSONArray("results");
                JSONObject location = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                String placename = results.getJSONObject(0).getString("name");
                String id = results.getJSONObject(0).getString("id");
                destination = new Place(new LatLng(location.getDouble("lat"), location.getDouble("lng")), placename, id, null);
                destination.marker = map.addMarker(new MarkerOptions()
                        .position(destination.location)
                        .title(destination.name));
            }
            catch(InterruptedException ie){}
            catch(ExecutionException ee){}
            catch(JSONException je){}
        }
        else Toast.makeText(getApplicationContext(), "No Internet Connection\nCan't Get Location", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) { }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }
}

/**
 * Class used to get the JSON data from the places API URL asyncronously since the connection is often slow
 */
class JsonTask extends AsyncTask<String, String, String> {

    String json;

    public JsonTask(){ json = ""; }

    protected String doInBackground(String... params) {
        String json = new JSONParser().getJSONFromUrl(params[0]);
        return json;
    }
}

/**
 * Class used to get the JSON data obtained from JSONTask, will return a string of JSON info
 */
class JSONParser {

    static InputStream is = null;
    static JSONObject jObj = null;
    static String json = "";

    public JSONParser() {}

    public String getJSONFromUrl(String adress) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(adress);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuffer buffer = new StringBuffer();
            String line = "";

            while ((line = reader.readLine()) != null) buffer.append(line+"\n");

            json = buffer.toString();
            return json;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}

/**
 * Class used to store information regarding a place on the map. Contains the relevant info obtained from the
 * Places API such as place ID, name and the associated Maps Marker
 */
class Place{

    String name, id;
    LatLng location;
    Marker marker;

    Place(LatLng location, String name, String id, Marker marker){
        this.location = location;
        this.id = id; this.name = name;
        this.marker = marker;
    }
}