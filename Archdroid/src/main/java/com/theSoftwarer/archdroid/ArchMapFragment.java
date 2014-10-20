package com.theSoftwarer.archdroid;


import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;


/**
 * Created by Ras-Mars on 14/02/14.
 */
public class ArchMapFragment extends SupportMapFragment implements GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, LocationListener {

    public static final String LOG_TAG = "Archdroid";
    private static final String PELAGIOS_API =  "http://pelagios.dme.ait.ac.at/api/places";
    private String js, query;
    private Handler handler;
    public PagesFragment pagesFragment;
    public Marker myLocation;

    public ArchMapFragment() {

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                Bundle b = msg.getData();
                js = b.getString("data");

                try {
                    JsonManager.createMarkersFromJson(js, getMap(), getActivity());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        };

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        query = PELAGIOS_API + ".json?bbox=" + getBbox();

        new Thread(new Runnable() {
            public void run() {
                try {
                    JsonManager.searchPelagiosData(query, handler);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot retrieve places", e);
                } catch (IllegalArgumentException e) {
                    Log.e(LOG_TAG, "Error connecting to service", e);
                }
            }
        }).start();

    }

    @Override
    public void onLocationChanged(Location location) {

        if (myLocation != null) myLocation.remove();
        LatLng mLatLng = new LatLng(location.getLatitude(),location.getLongitude());
        getMap().animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(mLatLng, 15, 30, 0)));
        myLocation = getMap().addMarker(new MarkerOptions().position(mLatLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher)));
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        String[] itemNameTypeSource = marker.getSnippet().split(":::");
        pagesFragment = PagesFragment.newInstance(itemNameTypeSource[0], itemNameTypeSource[1], itemNameTypeSource[2]);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.lift_up, R.anim.lift_down, R.anim.lift_up, R.anim.lift_down);
        ft.replace(R.id.container, pagesFragment);
        ft.addToBackStack(null);
        ft.commit();

        return false;
    }

    private String getBbox(){

        LatLngBounds bBox = getMap().getProjection().getVisibleRegion().latLngBounds;
        DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#.##", formatSymbols);

        return df.format(bBox.southwest.longitude) +","+ df.format(bBox.southwest.latitude)
                +","+ df.format(bBox.northeast.longitude) +","+ df.format(bBox.northeast.latitude);
    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

}
