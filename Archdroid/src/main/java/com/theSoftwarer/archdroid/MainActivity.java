package com.theSoftwarer.archdroid;


import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;

public class MainActivity extends ActionBarActivity implements
        GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener{

    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private GoogleMap gMap;
    public ArchMapFragment archMapFragment;
    private LocationClient mLocationClient;
    private static final LocationRequest mLocationRequest = LocationRequest.create()
            .setInterval(5000)
            .setFastestInterval(16)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLocationClient != null) {
            mLocationClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationClient.requestLocationUpdates(mLocationRequest, archMapFragment);
    }

    @Override
    public void onDisconnected() {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, REQUEST_CODE_RECOVER_PLAY_SERVICES);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE_RECOVER_PLAY_SERVICES)
            Toast.makeText(this, String.valueOf(resultCode), Toast.LENGTH_SHORT).show();
        switch (resultCode) {
            case ConnectionResult.CANCELED:
                Toast.makeText(this, "Google Play Services must be installed.", Toast.LENGTH_SHORT).show();
                finish();
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.navigation:
                setUpLocationClient(item);
            case R.id.normal_mode:
                if (!item.isChecked())
                    item.setChecked(true);
                gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            case R.id.hybrid_mode:
                if (!item.isChecked())
                    item.setChecked(true);
                gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            case R.id.satellite_mode:
                if (!item.isChecked())
                    item.setChecked(true);
                gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            case R.id.terrain_mode:
                if (!item.isChecked())
                    item.setChecked(true);
                gMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void setUpMapIfNeeded() {
        if (isPlayServicesInstalled()){
            if (gMap == null) {
                archMapFragment = (ArchMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_layout);
                gMap = archMapFragment.getMap();
            }
            else {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        gMap.setOnCameraChangeListener(archMapFragment);
        gMap.setOnMarkerClickListener(archMapFragment);
        gMap.setOnMapClickListener(archMapFragment);
    }

    private void setUpLocationClient(MenuItem item) {
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(getApplicationContext(),this,this);
        }
            if (mLocationClient.isConnected()) {
                mLocationClient.disconnect();
                if (archMapFragment.myLocation != null) archMapFragment.myLocation.remove();
                item.setIcon(R.drawable.selectable_location_off);
            }
            else {
                mLocationClient.connect();
                item.setIcon(R.drawable.selectable_location_on);
            }
    }

    private boolean isPlayServicesInstalled() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)){
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
            }else {
                Toast.makeText(this, "This device is not supported.", Toast.LENGTH_SHORT).show();
                finish();
            }return false;
        }return true;
    }

}