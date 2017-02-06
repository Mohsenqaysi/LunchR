package com.example.aaron.lunchr;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.R.string.no;

/**
 * Created by kev on 25/11/16.
 */

/**
 * Class using the Directions API to render a set of polygons on the map to indicate the direction
 * between the current location and destination
 */
public class Directions {

    Context context;
    GoogleMap map;
    LatLng start, end;

    /**
     * Setup
     * @param context The application context
     * @param map The map used to plot the waypoints
     */
    Directions(Context context, GoogleMap map){
        this.context = context;
        this.map = map;
    }

    /**
     * To get a JSON string from a URL is slow, so do this in the background to not hold up other tasks
     */
    private class connectAsyncTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog progressDialog;
        String url;

        connectAsyncTask(String urlPass){
            url = urlPass;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result!=null){
                drawPath(result);
            }
        }
    }

    /**
     * Will form the URL to query and will get the JSON response from the AsyncTask
     * @param start the start location
     * @param end the end location
     */
    public void setDirections(LatLng start, LatLng end){
        this.start = start; this.end = end;
        this.start = start; this.end = end;
        String url = "https://maps.googleapis.com/maps/api/directions/json?";
        url += "origin="+start.latitude+","+start.longitude+"&";
        url += "destination="+end.latitude+","+end.longitude+"&";
        url += "sensor=false&mode=walking&alternatives=true";
        new connectAsyncTask(url).execute();
    }

    /**
     * Rather ugly method to generate the set of waypoints for the directions
     * @param encoded the waypoint data
     * @return a list of waypoints
     */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }

    /**
     * Draws the polys on the map for the directions
     * @param result the JSON string used to plot the waypoints for the directions
     */
    public void drawPath(String result) {

        try {
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);
            Polyline line = map.addPolyline(new PolylineOptions()
                    .addAll(list)
                    .width(12)
                    .color(Color.parseColor("#59428c"))//lunchr purple :)
                    .geodesic(true)
            );
        }
        catch (JSONException e) {}
    }
}

