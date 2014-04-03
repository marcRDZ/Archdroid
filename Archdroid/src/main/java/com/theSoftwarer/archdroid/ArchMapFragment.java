package com.theSoftwarer.archdroid;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.theSoftwarersApprentice.archdroid.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
    private static final String PELAGIOS_BBOX_QUERY = "http://pelagios.dme.ait.ac.at/api/places.json?bbox=";
    private static final int CREATE_MARKERS = 1415;
    private static final int CREATE_PAGES = 9265;
    private static final int CREATE_LISTS = 3589;
    private String bounds, js, urlPlace, idMarker;
    private Handler handler;
    private DatasetsAdapter mPagerAdapter;
    private ViewPager mPager;
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
                        }break;
                    case CREATE_PAGES:
                        try {
                            createPagesFromJson(js);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }break;
                    case CREATE_LISTS:
                        try {
                            createListFromJson(js);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }break;

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

            new Thread(new Runnable() {
                public void run() {
                    try {
                       searchPelagiosData(PELAGIOS_BBOX_QUERY + bounds, CREATE_MARKERS);

                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Cannot retrieve places", e);
                    }
                }
            }).start();

    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        //urlPlace = marker.getSnippet();

        new Thread(new Runnable() {
            public void run() {
                try {
                    searchPelagiosData(urlPlace + "/datasets.json", CREATE_PAGES);

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot retrieve datasets for this places", e);
                }
            }
        }).start();

        return false;
    }

    private void searchPelagiosData(String string, int switcher) throws IOException {
        HttpURLConnection conn = null;
        final StringBuilder json = new StringBuilder();
        try {
// Connect to the web service
            URL url = new URL(string);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

// Read the JSON data into the StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                json.append(buff, 0, read);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to service", e);
            throw new IOException("Error connecting to service", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            sendToUIThread(json.toString(), switcher);
        }
    }

    private void sendToUIThread(String string, int switcher) {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("data", string);
        bundle.putInt("switcher", switcher);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    void createMarkersFromJson(String json) throws JSONException {

        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject jsonObj = jsonArray.getJSONObject(i);
            getMap().addMarker(new MarkerOptions().position(new LatLng(
                    jsonObj.getJSONObject("geometry").getJSONArray("coordinates").optDouble(1),
                    jsonObj.getJSONObject("geometry").getJSONArray("coordinates").optDouble(0)
            )).snippet(jsonObj.getString("uri"))
            //.icon(BitmapDescriptorFactory.fromResource(R.drawable.unknown))
            );
            urlPlace = jsonObj.getString("uri");
            idMarker = jsonObj.getString("source");

        }
    }

    void createPagesFromJson (String json) throws JSONException {

        List<String[]> datasets = new ArrayList<String[]>();
        String[] titleUri = new String[2];
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObj = jsonArray.getJSONObject(i);
            titleUri[0] = jsonObj.getString("uri");
            titleUri[1] = jsonObj.getString("title");
            datasets.add(titleUri);
        }
        mPagerAdapter = new DatasetsAdapter(getActivity().getSupportFragmentManager(), datasets);
        mPager = (ViewPager)getActivity().findViewById(R.id.viewpager_layout);
        mPager.setAdapter(mPagerAdapter);
        mPager.setVisibility(View.VISIBLE);
    }

    private void createListFromJson(String json) throws JSONException {

        JSONArray jsonArray = new JSONArray(json);
        annotations = new ArrayList<HashMap<String,String>>();
        HashMap<String, String> hm = new HashMap<String,String>();

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject jsonObj = jsonArray.getJSONObject(i);
            jsonObj = jsonObj.getJSONObject("annotations");
            hm.put("name", jsonObj.getString("target_title"));
            hm.put("url", jsonObj.getString("hasTarget"));
            annotations.add(hm);

        }

    }

    public class AnnotationsFragment extends SherlockListFragment {

        private String uri;

        public AnnotationsFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            uri = getArguments().getString("uri");
            new Thread(new Runnable() {
                public void run() {
                    try {
                        searchPelagiosData( uri + "/annotations.json?forPlace=" + idMarker, CREATE_LISTS);

                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Cannot retrieve annotations for this place", e);
                    }
                }
            }).start();

        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {


            TextView titleBar = (TextView) inflater.inflate(R.id.title, container);
            titleBar.setText(getArguments().getString("title"));
            // Keys used in Hashmap
            String[] from = {"name", "url"};
            // Ids of views in listview_layout
            int[] to = { R.id.name, R.id.url};
            // Instantiating an adapter to store each items
            // R.layout.list_layout defines the layout of each item
            SimpleAdapter adapter = new SimpleAdapter(getActivity().getBaseContext(), annotations, R.layout.list_layout, from, to);
            setListAdapter(adapter);

            return super.onCreateView(inflater, container, savedInstanceState);
        }

        public AnnotationsFragment newInstance(int ord, int count, String title, String uri) {

            AnnotationsFragment f = new AnnotationsFragment();
            Bundle args = new Bundle();
            args.putInt("ord", ord);
            args.putInt("count", count);
            args.putString("title", title);
            args.putString("uri", uri);
            f.setArguments(args);

            return f;
        }


    }

    public static class DatasetsAdapter extends FragmentPagerAdapter{

        List<String[]> dsets;
        String[] ttlUr;
        AnnotationsFragment anFrag;

        public DatasetsAdapter(FragmentManager fm, List<String[]> datasets) {
            super(fm);
            this.dsets = datasets;
        }

        @Override
        public Fragment getItem(int position) {
            ttlUr = dsets.get(position);
            return anFrag.newInstance(position, dsets.size(), ttlUr[0], ttlUr[1]);
        }

        @Override
        public int getCount() {
            return dsets.size();
        }
    }

}
