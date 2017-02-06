package com.example.aaron.lunchr;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import static java.lang.Double.parseDouble;

public class Selection extends AppCompatActivity implements OnMapReadyCallback {

    private String APIKEY = "AIzaSyAsyz_cGfPY1Jf57ow9LFEA267DoN_tfzU";

    private String radius = "";

    private ArrayList<String> placeIDs = new ArrayList<String>();

    private HashMap<String, String> placeNameIdMap = new HashMap<>();
    private HashMap<String, String[]> placeNameGeoMap = new HashMap<>();
    // Latitude and longitude are stored in an array

    private LatLng myCurrentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        // Get the the radios and location
        Intent intent = getIntent();
        radius = String.valueOf(intent.getStringExtra("radius"));

        myCurrentLocation = new LatLng(parseDouble(intent.getStringExtra("Latitude"))
                , parseDouble(intent.getStringExtra("Longitude")));

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_ID);
        mapFragment.getMapAsync(this);

        try {
            getRequest();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        PlacesSelectionListFragment placesSelectionListFragment = new PlacesSelectionListFragment();
        getSupportFragmentManager().
                beginTransaction().
                add(R.id.selection_container, placesSelectionListFragment).commit();
    }

    public HashMap getNameIdMap() {
        return placeNameIdMap;
    }

    public void onMapReady(GoogleMap googleMap) {

        for (String placeName : placeNameGeoMap.keySet()) {

            double lat = Double.parseDouble(placeNameGeoMap.get(placeName)[0]);
            double lng = Double.parseDouble(placeNameGeoMap.get(placeName)[1]);

            googleMap.addMarker(new MarkerOptions().position(
                    new LatLng(lat, lng))
                    .title(placeName)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.rest_pin2)));
        }

        googleMap.addCircle(new CircleOptions()
                .center(myCurrentLocation)
                .radius(3)
                .strokeColor(Color.parseColor("#42aaf4"))
                .fillColor(Color.parseColor("#148be0")));

        googleMap.addMarker(new MarkerOptions()
                .position(myCurrentLocation)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.manshape)));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myCurrentLocation, 11)); //Zoom to currentLocation
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2500, null); // 2.5 Seconds
    }

    protected void getRequest() throws ExecutionException, InterruptedException {
        String url = String.format("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                + myCurrentLocation.latitude + ","
                + myCurrentLocation.longitude
                + "&radius=" + radius +
                "&types=food"
                + "&key=" + APIKEY);

        String output = new parseJSONObject().execute(url).get();
    }

    private class parseJSONObject extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = "UNDEFINED";
            try {
                URL url = new URL(strings[0]);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder builder = new StringBuilder();

                String inputString;
                while ((inputString = bufferedReader.readLine()) != null) {
                    builder.append(inputString);
                }

                JSONObject JsonArray = new JSONObject(builder.toString()); // get JSON Array
                JSONArray jsonRoot = JsonArray.getJSONArray("results");

                for (int i = 0; i < jsonRoot.length(); i++) {
                    JSONObject jsonNearbyObject = jsonRoot.getJSONObject(i);

                    if (jsonNearbyObject.has("photos") && (myCurrentLocation != null)) {
                        String placeName = jsonNearbyObject.getString("name");
                        String placeID = jsonNearbyObject.getString("place_id");
                        JSONObject jsonGeoObject = jsonNearbyObject.getJSONObject("geometry");
                        JSONObject location = jsonGeoObject.getJSONObject("location");

                        placeIDs.add(placeID);
                        placeNameIdMap.put(placeName, placeID);
                        placeNameGeoMap.put(placeName, new String[]{location.getString("lat"),
                                location.getString("lng")});
                    } else {
                        continue;
                    }
                }
                stream.close();
                urlConnection.disconnect(); // release the connection
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

}