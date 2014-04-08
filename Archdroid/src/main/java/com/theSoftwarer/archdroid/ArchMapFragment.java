package com.theSoftwarer.archdroid;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

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
    private static final String PELAGIOS_QUERY = "http://pelagios.dme.ait.ac.at/api/places";
    private static final int CREATE_MARKERS = 1415;
    private static final int CREATE_PAGES = 9265;
    private static final int CREATE_LISTS = 3589;
    private String bounds, js;
    private static String urlPlace;
    private static Handler handler;
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

        LatLngBounds bBox = this.getMap().getProjection().getVisibleRegion().latLngBounds;
        DecimalFormat df = new DecimalFormat("#.##");
        bounds = df.format(bBox.southwest.longitude) +","+ df.format(bBox.southwest.latitude)
        +","+ df.format(bBox.northeast.longitude) +","+ df.format(bBox.northeast.latitude);

        new Thread(new Runnable() {
            public void run() {
                try {
                   searchPelagiosData(PELAGIOS_QUERY + ".json?bbox=" + bounds, CREATE_MARKERS);

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

        urlPlace = marker.getSnippet();
        new Thread(new Runnable() {
            public void run() {
                try {
                    searchPelagiosData(urlPlace + "/datasets.json", CREATE_PAGES);

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
                        )).snippet(jsonObj.getString("uri"))
                        //.icon(BitmapDescriptorFactory.fromResource(R.drawable.unknown))
                );

            }
        }catch (JSONException e){e.printStackTrace();}

    }

    private void createPagesFromJson (String json) throws JSONException {

        List<HashMap<String,String>> datasets = new ArrayList<HashMap<String,String>>();
        HashMap<String, String> hm = new HashMap<String,String>();

        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                hm.put("uri", jsonObj.getString("uri"));
                hm.put("title", jsonObj.getString("title"));
                datasets.add(hm);
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
                jsonObj = jsonObj.getJSONObject("annotations");
                hm.put("name", jsonObj.getString("target_title"));
                hm.put("url", jsonObj.getString("hasTarget"));
                annotations.add(hm);

            }
        }catch (JSONException e){e.printStackTrace();}

    }

    public static class AnnotationsFragment extends ListFragment {


        private String uri;
        String[] countries = new String[] {
                "India",
                "Pakistan",
                "Sri Lanka",
                "China",
                "Bangladesh",
                "Nepal",
                "Afghanistan",
                "North Korea",
                "South Korea",
                "Japan"
        };
        String[] currency = new String[]{
                "Indian Rupee",
                "Pakistani Rupee",
                "Sri Lankan Rupee",
                "Renminbi",
                "Bangladeshi Taka",
                "Nepalese Rupee",
                "Afghani",
                "North Korean Won",
                "South Korean Won",
                "Japanese Yen"
        };
        public AnnotationsFragment() {
            annotations = null;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            uri = getArguments().getString("uri");
/*
            new Thread(new Runnable() {
                public void run() {
                    try {
                        searchPelagiosData( uri + "/annotations.json?forPlace=" + urlPlace, CREATE_LISTS);

                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Cannot retrieve annotations for this place", e);
                    } catch (IllegalArgumentException e) {
                        Log.e(LOG_TAG, "Error connecting to service", e);
                    }
                }
            }).start();*/

        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

            List<HashMap<String,String>> aList = new ArrayList<HashMap<String,String>>();
            for(int i=0;i<10;i++){
                HashMap<String, String> hm = new HashMap<String,String>();
                hm.put("name", "Country : " + countries[i]);
                hm.put("url","Currency : " + currency[i]);
                aList.add(hm);
            }
            // Keys used in Hashmap
            String[] from = {"name", "url"};
            // Ids of views in listview_layout
            int[] to = { R.id.name, R.id.url};
            // Instantiating an adapter to store each items
            // R.layout.list_layout defines the layout of each item

                SimpleAdapter adapter = new SimpleAdapter(getActivity().getBaseContext(), aList, R.layout.list_layout, from, to);
                setListAdapter(adapter);

            return super.onCreateView(inflater, container, savedInstanceState);
        }

        public static AnnotationsFragment newInstance(String uri) {

            AnnotationsFragment f = new AnnotationsFragment();
            Bundle args = new Bundle();
            args.putString("uri", uri);
            f.setArguments(args);

            return f;
        }


    }

    public static class DatasetsAdapter extends FragmentStatePagerAdapter {

        List<HashMap<String,String>> dSets;
        HashMap<String, String> stringStringHashMap;
        AnnotationsFragment annotationsFragment;

        public DatasetsAdapter(FragmentManager fm, List<HashMap<String,String>> datasets) {
            super(fm);
            this.dSets = datasets;
        }

        @Override
        public Fragment getItem(int position) {
            stringStringHashMap = dSets.get(position);
            annotationsFragment = annotationsFragment.newInstance(stringStringHashMap.get("uri"));
            return annotationsFragment;
        }

        @Override
        public int getCount() {
            return dSets.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            stringStringHashMap = dSets.get(position);
            return stringStringHashMap.get("title") +" "+ position +" "+ dSets.size();
        }
    }

}
