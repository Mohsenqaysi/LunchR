package com.example.aaron.lunchr;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;


/**
 * Created by dylandowling on 07/11/2016.
 */

public class Result extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    protected static final String TAG = "ResultActivity";

    protected GoogleApiClient mGoogleApiClient;

    public static int radius = 400; // min distance

    public static void setRadius(int r) {
        radius = r;
    }

    // Represents a geographical location.
    protected Location mLastLocation;
    protected double mLatitude;
    protected double mLongitude;
    public static Button goButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        buildGoogleApiClient();
        goButton = (Button) findViewById(R.id.goButton);
        setButtonText("5 mins"); // default

        goButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do not allow the the user to go to the next activity unless
                // They have an internet connect
                if (isNetworkConnected()){
                    Log.e("Passed radius: ", String.valueOf(radius) + " " + mLatitude + " " + mLongitude);
                    Intent intent = new Intent(getBaseContext(), Selection.class);
                    intent.putExtra("radius", String.valueOf(radius));
                    intent.putExtra("Latitude", String.valueOf(mLatitude));
                    intent.putExtra("Longitude", String.valueOf(mLongitude));
                    startActivity(intent);
                }else {
                    popMessage();

                }
            }
        });
    }

    // Checking the user is connected to the internet
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    public static void setButtonText(String s) {
        goButton.setText(s);
    }

    // https://developer.android.com/training/location/retrieve-current.html
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check for internet connect before trying to connect to mGoogleApiClient
        if (isNetworkConnected() == true) {
            mGoogleApiClient.connect();
        }else {

            mGoogleApiClient.disconnect();
            popMessage();
        }
    }

    protected  void popMessage(){
        AlertDialog.Builder builder = new AlertDialog.Builder(Result.this);

        builder.setCancelable(false);

        builder.setTitle("Internet Error Connection...");
        builder.setMessage("Please make sure you are connected to the internet!");

        // Setting Positive "Yes" Button
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Checking to see if the user has allow permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            //request specific permission from user
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitude = mLastLocation.getLatitude();
            mLongitude = mLastLocation.getLongitude();
            Log.d("LOCATION: ", "LAT: " + mLatitude + "\tLONG: " + mLongitude);
        } else {
            Log.d("LOCATION: ", " Not Found");
            Log.d("LOCATION: ", "LAT: " + mLatitude + "\tLONG: " + mLongitude);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    /*
    Inner class for drawing circle begins here
     */
    public static class ResultsView extends View {

        // Drawing variables
        Paint black_paintbrush_stroke;
        private float circle_radius;
        private float circle_x;
        private float circle_y;
        private float maxRadius;
        private float minRadius;
        private float radiusStart;// = (int) Math.round(maxRadius/3);
        private float startPosX;
        private float startPosY;
        private float endPosX;
        private float endPosY;
        private float density;
        private Paint backgroundPaint;
        private int buttonText = 5;

        public ResultsView(Context context, AttributeSet attr) {
            super(context, attr);

            // getting screen width and height to make various size adjustments
            // the following link helped me understand how to capture screen width and height
            // hhttp://ingenious-camel.blogspot.ie/2012/04/how-to-get-width-and-height-in-android.html
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics display = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(display);
            density = getResources().getDisplayMetrics().density;
            maxRadius = display.heightPixels / density;

            float screenW = display.widthPixels;
            float screenH = display.heightPixels;
            circle_x = screenW;
            circle_y = screenH;
            radiusStart = Math.round(maxRadius/3);
            minRadius = (int) (50 * density + 0.5f);
            circle_radius = (int) (50 * density + 0.5f);
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.WHITE);

            black_paintbrush_stroke = new Paint();
            black_paintbrush_stroke.setColor(Color.parseColor("#4688F1"));
            black_paintbrush_stroke.setStyle(Paint.Style.STROKE);
            black_paintbrush_stroke.setStrokeWidth(10);
            black_paintbrush_stroke.setAntiAlias(true);

        }

        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            double new_radius = 0;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    startPosX = event.getX();
                    startPosY = event.getY();
                    Log.d("startPosX: ", "" + startPosX);
                    Log.d("startPosY: ", "" + startPosY);
                    break;

                case MotionEvent.ACTION_MOVE:
                    endPosX = event.getX();
                    endPosY = event.getY();
                    new_radius = Math.sqrt(((endPosX - startPosX) * (endPosX - startPosX)) + ((endPosY - startPosY) * (endPosY - startPosY)));
                    Log.d("new radius: ", "" + new_radius);
                    if (new_radius >= minRadius && new_radius <= maxRadius) {
                        circle_radius = (float) new_radius;
                    }
                    // set ranges for time values here
                    // distance in meters
                    // time values: 5mins, 10mins, 15mins, 20mins,30mins
                    if (circle_radius >= radiusStart * 2) {
                        setRadius(radius = 900); // 15 mins walk
                        buttonText = 15;
                    } else if (circle_radius >= radiusStart && circle_radius < radiusStart*2) {
                        setRadius(radius = 600); // 10 min walk
                        buttonText = 10;
                    } else {
                        setRadius(radius = 400); //5 min walk
                        buttonText = 5;
                    }

                case MotionEvent.ACTION_UP:
                    Log.d("RADIUS: ", "" + radius);
                    setButtonText(buttonText+" mins");
                    break;
            }
            return (true);
        }
        public void draw(Canvas canvas) {
            canvas.drawCircle(circle_x/2, (circle_y/2), circle_radius, black_paintbrush_stroke);
            invalidate(); // for re-drawing the circle after changes made
        }

    }
}