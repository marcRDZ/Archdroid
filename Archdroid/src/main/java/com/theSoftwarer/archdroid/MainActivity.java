package com.theSoftwarer.archdroid;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.theSoftwarersApprentice.archdroid.R;

import static android.content.SharedPreferences.Editor;

public class MainActivity extends SherlockFragmentActivity  implements
        GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener{

    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private GoogleMap gMap;
    private ArchMapFragment archMapFragment;
    private ShareLocationDialogFragment mFragment;
    private LocationClient mLocationClient;
    private static final LocationRequest mLocationRequest = LocationRequest.create()
            .setInterval(5000)
            .setFastestInterval(16)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    protected boolean isUpdatesRequested;
    protected SharedPreferences mPrefs;
    protected Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
        getLocationUpdatesPreferences(mPrefs);

    }

    @Override
    protected void onStart() {
        super.onStart();
        setUpMapIfNeeded();
        setUpLocationClientIfNeeded();
        if (isUpdatesRequested){
            mLocationClient.connect();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        getLocationUpdatesPreferences(mPrefs);
        setUpMapIfNeeded();
        setUpLocationClientIfNeeded();
        if (isUpdatesRequested) {
            mLocationClient.connect();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLocationClient != null) {
            mLocationClient.disconnect();
        }
        setLocationUpdatesPreferences(mEditor);
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
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this,REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_RECOVER_PLAY_SERVICES:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Google Play Services must be installed.", Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
    }

    private void setUpLocationClientIfNeeded() {
            if (mLocationClient == null) {
                mLocationClient = new LocationClient(getApplicationContext(),this,this);
            }
    }

    private boolean isPlayServicesInstalled() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {

                GooglePlayServicesUtil.getErrorDialog(status, this,REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
            } else {
                Toast.makeText(this, "This device is not supported.", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void getLocationUpdatesPreferences(SharedPreferences sharedPreferences) {
        if (!sharedPreferences.contains("KEY_UPDATES_ON")) {
            mFragment = new ShareLocationDialogFragment();
            mFragment.show(getSupportFragmentManager(),"Location Updates");
        } else {
            isUpdatesRequested = sharedPreferences.getBoolean("KEY_UPDATES_ON", false);
        }

    }

    private void setLocationUpdatesPreferences(Editor editor) {
        editor.putBoolean("KEY_UPDATES_ON", isUpdatesRequested);
        editor.commit();
    }


    public class ShareLocationDialogFragment extends SherlockDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.share_location)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            isUpdatesRequested = true;
                            //mFragment.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            isUpdatesRequested = false;
                            //mFragment.dismiss();
                        }
                    });
            return builder.create();
        }
    }

}