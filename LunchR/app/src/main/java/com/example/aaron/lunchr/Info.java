package com.example.aaron.lunchr;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import com.squareup.picasso.Picasso;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.concurrent.ExecutionException;


public class Info extends FragmentActivity {

    private String photo_referenceVal = null;

    private String locationID = "";
    private String APIKEY = "AIzaSyAsyz_cGfPY1Jf57ow9LFEA267DoN_tfzU";
    private String formatted_address;
    private String formatted_phone_number;
    private String placeName;
    private String placeRating;
    private String placeWebsite;
    private String lat_value;
    private String lng_value;
    private String open_Now;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // Get the the placeID from the Intent
        Intent intent = getIntent();
        if (locationID != null){
            locationID = intent.getStringExtra("id");
        }else {
            Context context = getApplicationContext();
            CharSequence text = "Error... No location was available";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
        Log.e("LocID: ",locationID);

        // Do the getRequest() to get the place Full info
        try {
            getRequest(); // request a connection and parse JSONObject
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Set up the IDs of the view used in the project:
        final TextView mytext = (TextView) findViewById(R.id.textView_id);
        final RatingBar myRattingBar = (RatingBar) findViewById(R.id.ratingBar);
        final ImageView myImage = (ImageView) findViewById(R.id.Placeimageview_id);
        final TextView ratingtext = (TextView) findViewById(R.id.textViewRatingID);
        final TextView timeStatus = (TextView) findViewById(R.id.time_ID);

        // Build the Uri to request the image
        int imageSize = 400;
        final String REQUEST_LOCATION_PHOTO = "https://maps.googleapis.com/maps/api/place/photo?";
        final String MAX_WIDTH = "maxwidth";
        final String QUERY_PARAM = "photoreference";
        final String LOCATION_ID = "key";

        Uri builtUri = Uri.parse(REQUEST_LOCATION_PHOTO).buildUpon()
                .appendQueryParameter(MAX_WIDTH, Integer.toString(imageSize))
                .appendQueryParameter(QUERY_PARAM, photo_referenceVal)
                .appendQueryParameter(LOCATION_ID, APIKEY)
                .build();

        Log.v("URI_TAG", "Built URI " + builtUri.toString());
        String url = builtUri.toString();
        String result = placeName + "\n" + formatted_address;
        Log.e("TAG_Place_Info:", placeName + "\n" + formatted_address + "\n" + formatted_phone_number
                + "\n" + placeRating + "\n" + placeWebsite);

        mytext.setText(result); // set the details into the text view
        mytext.setText(result);
        if(placeRating != null) {
            ratingtext.setText(placeRating.toString()); // set rating text value
        }else{
            placeRating = "0";
            ratingtext.setText(placeRating.toString());
        }
        myRattingBar.setRating(Float.parseFloat(placeRating)); // set rating bar value
        // Picasso is an external library used to display the image:
        // http://square.github.io/picasso/
        Picasso.with(this).load(url).error(R.drawable.placeholder).into(myImage); // Display the image of the location

        // Check if the place is open or not and display the status
        if (open_Now == "true") {
            timeStatus.setTextColor(Color.parseColor("#6ba234"));
            timeStatus.setText("OPEN NOW");
        } else if (open_Now == null){
            timeStatus.setTextColor(Color.GRAY);
            timeStatus.setText("N/A");
        } else {
            timeStatus.setTextColor(Color.RED);
            timeStatus.setText("CLOSED");
        }
    }
    // Call the location international phone number
    // Using the Action call ... get permission first
    protected void MakePhoneCall(View view) {

        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + formatted_phone_number));
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, 10);  //request specific permission from user
            return;
        }
        try {
            startActivity(callIntent);  //call activity and make phone call
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "You do not have permissions to make the call",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Open the location website in the browser
    protected  void openWebSite(View view){
        if (placeWebsite != null ) {
            Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(placeWebsite));
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(), "Sorry the place does not have a website!"+"", Toast.LENGTH_SHORT).show();
        }
    }
    // Go to the social activity for Info activity
    protected void openSocial(View view) {
        Intent intent = new Intent(this, Social.class);
        startActivity(intent);
    }

    protected  void openMaps(View view){

        String url = String.format("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
                + lat_value + ","
                + lng_value
                + "&radius=" + 100 +
                "&types=food"
                + "&key=" + APIKEY);
            Intent intent = new Intent(getApplicationContext(), Map.class);
            intent.putExtra("URL", url);
            startActivity(intent);
    }
    // Request info about a location using its ID
    protected void getRequest() throws ExecutionException, InterruptedException {
        String url = String.format("https://maps.googleapis.com/maps/api/place/details/json?" +
                "placeid=" + locationID +
                "&key=" + APIKEY);
        Log.e("TAG", url);
        String output = new parseJSONObject().execute(url).get();
        photo_referenceVal = output;
        Log.e("Output_PhotoReference:", output);
    }
    // Parse the JSON Object returned from the Google API
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

                JSONObject JsonObject = new JSONObject(builder.toString()); // get JSON Object
                JSONObject jsonRoot = JsonObject.getJSONObject("result");
                // Get the place details:
                // Name, formatted_address, formatted_phone_number
                //geometry, rating, website

                placeName = jsonRoot.getString("name");
                formatted_address = jsonRoot.getString("formatted_address");
                formatted_phone_number = jsonRoot.getString("international_phone_number");

                // Get the geometry of the place
                JSONObject jsonGeometryObject = jsonRoot.getJSONObject("geometry");
                JSONObject location = jsonGeometryObject.getJSONObject("location");
                lat_value = location.getString("lat");
                lng_value = location.getString("lng");

                Log.e("jsonGeometryValue_Lat:", lat_value + " and " + lng_value);

                placeRating = jsonRoot.getString("rating");
                placeWebsite = jsonRoot.getString("website");

                // Get the photo of the location form the object
                JSONArray jsonArray = jsonRoot.getJSONArray("photos");
                JSONObject jsonValue = jsonArray.getJSONObject(0);
                result = jsonValue.getString("photo_reference");
                JSONObject openingHours = jsonRoot.getJSONObject("opening_hours");

                open_Now = openingHours.getString("open_now");
                Log.e("TAG_openingHours:", open_Now);

                urlConnection.disconnect(); // release the connection
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return result;
        }
        @Override
        protected void onPostExecute(String reference) {
            Log.i("TAG", "------------------------");
            Log.i("TAG", "photo_reference: " + reference);
            Log.i("TAG", "------------------------");
            //          Toast.makeText(getApplicationContext(), "Error...!!! "+reference, Toast.LENGTH_SHORT).show();
        }
    }
}