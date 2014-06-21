package com.theSoftwarer.archdroid;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ras-Mars on 14/02/14.
 */
public class ArchMapFragment extends SupportMapFragment implements GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMarkerClickListener, LocationListener {

    public static final String LOG_TAG = "Archdroid";
    private static final int CREATE_MARKERS = 1415;
    private static final int CREATE_PAGES = 9265;
    private String js;
    private static String urlPleiades;
    private static Handler handler;
    private DatasetsAdapter mPagerAdapter;
    private List<Fragment> datasets;
    private ViewPager mPager;
    private Uri.Builder builder;

    public ArchMapFragment() {

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                Bundle b = msg.getData();
                js = b.getString("data");

                switch (b.getInt("switcher")){
                    case CREATE_MARKERS:

                        try {
                            JsonManager.createMarkersFromJson(js, getMap(), getActivity());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    break;
                    case CREATE_PAGES:

                        datasets = new ArrayList<Fragment>();
                        try {
                            JsonManager.createPagesFromJson(js, urlPleiades, datasets);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                            mPagerAdapter = new DatasetsAdapter(getActivity().getSupportFragmentManager(), datasets);
                            mPager = (ViewPager)getActivity().findViewById(R.id.viewpager_layout);
                            mPager.setAdapter(mPagerAdapter);
                            mPager.setVisibility(View.VISIBLE);
                    break;
                }
            }
        };

    }

    @Override
    public void onLocationChanged(Location location) {

        LatLng mLatLng = new LatLng(location.getLatitude(),location.getLongitude());
        getMap().animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(mLatLng, 15, 30, 0)));
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        builder = new Uri.Builder();
        builder.scheme("http").authority("pelagios.dme.ait.ac.at").path("/api/places.json").appendQueryParameter("bbox", getBbox());

        new Thread(new Runnable() {
            public void run() {
                try {
                    JsonManager.searchPelagiosData(builder.toString(), CREATE_MARKERS, handler);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot retrieve places", e);
                } catch (IllegalArgumentException e) {
                    Log.e(LOG_TAG, "Error connecting to service", e);
                }
            }
        }).start();

    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        urlPleiades = marker.getSnippet();
        builder = new Uri.Builder();
        builder.scheme("http").authority("pelagios.dme.ait.ac.at").path("/api/places/").appendPath(urlPleiades);

        new Thread(new Runnable() {
            public void run() {
                try {
                    JsonManager.searchPelagiosData(builder.toString() + "/datasets.json", CREATE_PAGES, handler);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot retrieve datasets for this places", e);
                } catch (IllegalArgumentException e) {
                    Log.e(LOG_TAG, "Error connecting to service", e);
                }
            }
        }).start();

        return false;
    }

    private String getBbox(){

        LatLngBounds bBox = getMap().getProjection().getVisibleRegion().latLngBounds;
        DecimalFormat df = new DecimalFormat("#.##");

        return df.format(bBox.southwest.longitude) +","+ df.format(bBox.southwest.latitude)
                +","+ df.format(bBox.northeast.longitude) +","+ df.format(bBox.northeast.latitude);
    }

    public static class DatasetsAdapter extends FragmentStatePagerAdapter {

        private List<Fragment> dSets;

        public DatasetsAdapter(FragmentManager fm, List<Fragment> datasets) {
            super(fm);
            this.dSets = datasets;
        }

        @Override
        public Fragment getItem(int position) { return dSets.get(position);
        }

        @Override
        public int getCount() {
            return dSets.size();
        }

    }



}
