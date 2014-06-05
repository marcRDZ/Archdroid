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
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Ras-Mars on 14/02/14.
 */
public class ArchMapFragment extends SupportMapFragment implements GoogleMap.OnCameraChangeListener,
        GoogleMap.OnMarkerClickListener, LocationListener {

    private static final String LOG_TAG = "Archdroid";
    private static final int CREATE_MARKERS = 1415;
    private static final int CREATE_PAGES = 9265;
    private static final int CREATE_LISTS = 3589;
    private String bounds, js;
    private static String urlPleiades;
    private static Handler handler;
    private DatasetsAdapter mPagerAdapter;
    private ViewPager mPager;
    private Uri.Builder builder;
    private static List<HashMap<String,String>> annotations;

    public ArchMapFragment() {

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                Bundle b = msg.getData();
                js = b.getString("data");

                switch (b.getInt("switcher")){
                    case CREATE_MARKERS:
                        try {
                            createMarkersFromJson(js);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    break;
                    case CREATE_PAGES:
                        try {
                            createPagesFromJson(js);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
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

        LatLngBounds bBox = getMap().getProjection().getVisibleRegion().latLngBounds;
        DecimalFormat df = new DecimalFormat("#.##");
        bounds = df.format(bBox.southwest.longitude) +","+ df.format(bBox.southwest.latitude)
        +","+ df.format(bBox.northeast.longitude) +","+ df.format(bBox.northeast.latitude);

        builder = new Uri.Builder();
        builder.scheme("http").authority("pelagios.dme.ait.ac.at").path("/api/places.json").appendQueryParameter("bbox", bounds);

        new Thread(new Runnable() {
            public void run() {
                try {
                    searchPelagiosData(builder.toString(), CREATE_MARKERS);
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
                    searchPelagiosData(builder.toString() + "/datasets.json", CREATE_PAGES);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot retrieve datasets for this places", e);
                } catch (IllegalArgumentException e) {
                    Log.e(LOG_TAG, "Error connecting to service", e);
                }
            }
        }).start();

        return false;
    }

    private static void searchPelagiosData(String url, int switcher) throws IOException, IllegalArgumentException {

        final StringBuilder json = new StringBuilder();
        try {

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(new HttpGet(url));
            InputStreamReader in = new InputStreamReader(response.getEntity().getContent());

            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                json.append(buff, 0, read);
            }
            in.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to service", e);
            throw new IOException("IOError connecting to service", e);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Error connecting to service", e);
            throw new IllegalArgumentException("HttpError connecting to service", e);
        }finally {
            sendToUIThread(json.toString(), switcher);
        }
    }

    private static void sendToUIThread(String string, int switcher) {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("data", string);
        bundle.putInt("switcher", switcher);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    private void createMarkersFromJson(String json) throws JSONException {

        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObj = jsonArray.getJSONObject(i);
                getMap().addMarker(new MarkerOptions().position(new LatLng(
                                jsonObj.getJSONObject("geometry").getJSONArray("coordinates").optDouble(1),
                                jsonObj.getJSONObject("geometry").getJSONArray("coordinates").optDouble(0)
                        )).snippet(jsonObj.getString("source"))
                        //.icon(BitmapDescriptorFactory.fromResource(R.drawable.unknown))
                );

            }
        }catch (JSONException e){e.printStackTrace();}

    }

    private void createPagesFromJson (String json) throws JSONException {

        List<Fragment> datasets = new ArrayList<Fragment>();

        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                String idDataset = jsonObj.getString("uri").substring(43);
                        Fragment fragment = NotesFragment.newInstance(jsonObj.getString("title"),idDataset, i + 1, jsonArray.length());
                datasets.add(fragment);
            }
        }catch (JSONException e){e.printStackTrace();}

        mPagerAdapter = new DatasetsAdapter(getActivity().getSupportFragmentManager(), datasets);
        mPager = (ViewPager)getActivity().findViewById(R.id.viewpager_layout);
        mPager.setAdapter(mPagerAdapter);
        mPager.setVisibility(View.VISIBLE);
    }

    private void createListFromJson(String json) throws JSONException {

        annotations = new ArrayList<HashMap<String,String>>();
        HashMap<String, String> hm = new HashMap<String,String>();

        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("annotations");

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObj = jsonArray.getJSONObject(i);
                hm.put("object", jsonObj.optString("target_title"));
                hm.put("place", jsonObj.optString("title"));
                hm.put("url", jsonObj.getString("hasTarget"));
                annotations.add(hm);

            }
            Toast.makeText(getActivity().getApplicationContext(), annotations.toString(), Toast.LENGTH_SHORT).show();
        }catch (JSONException e){e.printStackTrace();}

    }

    //public class NotesFragment

    public static class DatasetsAdapter extends FragmentStatePagerAdapter {

        private List<Fragment> dSets;

        public DatasetsAdapter(FragmentManager fm, List<Fragment> datasets) {
            super(fm);
            this.dSets = datasets;
        }

        @Override
        public Fragment getItem(int position) {
            return dSets.get(position);
        }

        @Override
        public int getCount() {
            return dSets.size();
        }

    }



}
