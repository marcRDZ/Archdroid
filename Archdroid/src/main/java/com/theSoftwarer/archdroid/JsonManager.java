package com.theSoftwarer.archdroid;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
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
import java.util.HashMap;
import java.util.List;

/**
 * Created by Ras-Mars on 05/06/2014.
 */
public class JsonManager {

    public static void searchPelagiosData(String url, Handler handler) throws IOException, IllegalArgumentException {

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
            Log.e(ArchMapFragment.LOG_TAG, "Error connecting to service", e);
            throw new IOException("IOError connecting to service", e);
        } catch (IllegalArgumentException e) {
            Log.e(ArchMapFragment.LOG_TAG, "Error connecting to service", e);
            throw new IllegalArgumentException("HttpError connecting to service", e);
        }finally {
            sendToUIThread(json.toString(), handler);
        }
    }

    private static void sendToUIThread(String string, Handler handler) {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("data", string);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    public static void createMarkersFromJson(String json, GoogleMap googleMap, Activity activity) throws JSONException {

        String placeType;

        try {

            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObj = jsonArray.getJSONObject(i);

                if (jsonObj.has("feature_type")) {
                    placeType = jsonObj.getString("feature_type").substring(50);
                }
                else placeType = "unknown";

                if (jsonObj.getJSONObject("geometry").getString("type").equals("Point")) {

                    googleMap.addMarker(new MarkerOptions().position(new LatLng(
                                    jsonObj.getJSONObject("geometry").getJSONArray("coordinates").optDouble(1),
                                    jsonObj.getJSONObject("geometry").getJSONArray("coordinates").optDouble(0)))
                                    .snippet(jsonObj.optString("label") + ":::" + jsonObj.getString("source") + ":::" + placeType)
                                    .icon(BitmapDescriptorFactory.fromBitmap(setCustomMarker(activity, placeType)))
                    );
                }

            }
        }catch (JSONException e){e.printStackTrace();}

    }

    public static void createPagesFromJson (String json, String place, List<Fragment> datasets) throws JSONException {

        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                Fragment fragment = NotesFragment.newInstance(jsonObj.getString("title"), jsonObj.getString("uri").substring(43), place, i + 1, jsonArray.length());
                datasets.add(fragment);
            }
        }catch (JSONException e){e.printStackTrace();}

    }

    public static void createListFromJson(String json, List<HashMap<String,String>> notes) throws JSONException {




        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("annotations");

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObj = jsonArray.getJSONObject(i);
                HashMap<String, String> hm = new HashMap<String,String>();
                if (jsonObj.has("target_title")) {
                    hm.put("name", jsonObj.getString("target_title"));
                }
                if (jsonObj.has("title")) {
                    hm.put("name", jsonObj.getString("title"));
                }
                hm.put("url", jsonObj.getString("hasTarget"));
                notes.add(hm);

            }
        } catch (JSONException e){e.printStackTrace();}

    }

    private static Bitmap setCustomMarker(Activity activity, String string){

        View view = ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.marker_layout, null);
        ImageView type = (ImageView) view.findViewById(R.id.place_type);
        type.setImageDrawable(activity.getResources().getDrawable(setCustomIcon(string)));

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }

    private static int setCustomIcon(String string) {

        if(iconsMap.containsKey(string))
            return iconsMap.get(string);

        return R.drawable.unknown;
    }

    public static final HashMap<String, Integer> iconsMap;
        static
        {
             iconsMap = new HashMap<String, Integer>();

             iconsMap.put("aqueduct", R.drawable.aqueduct);
             iconsMap.put("bath", R.drawable.aqueduct);
             iconsMap.put("whirlpool", R.drawable.aqueduct);
             iconsMap.put("wheel", R.drawable.aqueduct);
             iconsMap.put("dam", R.drawable.aqueduct);
             iconsMap.put("reservoir", R.drawable.aqueduct);
             iconsMap.put("cistern", R.drawable.aqueduct);

             iconsMap.put("cape", R.drawable.cape);
             iconsMap.put("lighthouse", R.drawable.cape);

            iconsMap.put("datasets", R.drawable.datasets);
            iconsMap.put("findspot", R.drawable.datasets);
            iconsMap.put("region", R.drawable.datasets);

            iconsMap.put("island", R.drawable.island);
            iconsMap.put("coast", R.drawable.island);
            iconsMap.put("bay", R.drawable.island);
            iconsMap.put("peninsula", R.drawable.island);
            iconsMap.put("archipelago", R.drawable.island);
            iconsMap.put("isthmus", R.drawable.island);
            iconsMap.put("lagoon", R.drawable.island);
            iconsMap.put("coastal-change", R.drawable.island);

            iconsMap.put("mine", R.drawable.mine);
            iconsMap.put("salt-pan-salina", R.drawable.mine);
            iconsMap.put("production", R.drawable.mine);

            iconsMap.put("mountain", R.drawable.mountain);
            iconsMap.put("ridge", R.drawable.mountain);
            iconsMap.put("cave", R.drawable.mountain);
            iconsMap.put("plain", R.drawable.mountain);
            iconsMap.put("hill", R.drawable.mountain);
            iconsMap.put("valley", R.drawable.mountain);
            iconsMap.put("forest", R.drawable.mountain);
            iconsMap.put("oasis", R.drawable.mountain);
            iconsMap.put("plateau", R.drawable.mountain);
            iconsMap.put("grove", R.drawable.mountain);

            iconsMap.put("people", R.drawable.people);

            iconsMap.put("port", R.drawable.port);
            iconsMap.put("canal", R.drawable.port);

            iconsMap.put("river", R.drawable.river);
            iconsMap.put("estuary", R.drawable.river);
            iconsMap.put("spring", R.drawable.river);
            iconsMap.put("water-inland", R.drawable.river);
            iconsMap.put("water-open", R.drawable.river);
            iconsMap.put("well", R.drawable.river);
            iconsMap.put("rapid", R.drawable.river);
            iconsMap.put("lake", R.drawable.river);
            iconsMap.put("fountain", R.drawable.river);
            iconsMap.put("marsh-wetland", R.drawable.river);

            iconsMap.put("road", R.drawable.road);

            iconsMap.put("sanctuary", R.drawable.sanctuary);
            iconsMap.put("church", R.drawable.sanctuary);
            iconsMap.put("monastery", R.drawable.sanctuary);
            iconsMap.put("mosque", R.drawable.sanctuary);
            iconsMap.put("basilica", R.drawable.sanctuary);

            iconsMap.put("settlement", R.drawable.settlement);
            iconsMap.put("settlement-modern", R.drawable.settlement);
            iconsMap.put("centuriation", R.drawable.settlement);
            iconsMap.put("fort-group", R.drawable.settlement);
            iconsMap.put("province", R.drawable.settlement);
            iconsMap.put("military-installation-or-camp-temporary", R.drawable.settlement);

            iconsMap.put("temple", R.drawable.temple);
            iconsMap.put("circus", R.drawable.temple);
            iconsMap.put("tumulus", R.drawable.temple);
            iconsMap.put("plaza", R.drawable.temple);
            iconsMap.put("amphitheatre", R.drawable.temple);
            iconsMap.put("theatre", R.drawable.temple);
            iconsMap.put("ekklesiasterion", R.drawable.temple);
            iconsMap.put("stoa", R.drawable.temple);
            iconsMap.put("palaistra", R.drawable.temple);
            iconsMap.put("stadion", R.drawable.temple);

            iconsMap.put("unknown", R.drawable.unknown);
            iconsMap.put("undefined", R.drawable.unknown);
            iconsMap.put("false", R.drawable.unknown);
            iconsMap.put("unlocated", R.drawable.unknown);

            iconsMap.put("villa", R.drawable.villa);
            iconsMap.put("estate", R.drawable.villa);
            iconsMap.put("province", R.drawable.villa);
            iconsMap.put("urban", R.drawable.villa);
            iconsMap.put("townhouse", R.drawable.villa);
            iconsMap.put("architecturalcomplex", R.drawable.villa);
            iconsMap.put("taberna-shop", R.drawable.villa);

            iconsMap.put("wall", R.drawable.wall);
            iconsMap.put("bridge", R.drawable.wall);
            iconsMap.put("earthwork", R.drawable.wall);
            iconsMap.put("pass", R.drawable.wall);
            iconsMap.put("tunnel", R.drawable.wall);
            iconsMap.put("cemetery", R.drawable.wall);
            iconsMap.put("causeway", R.drawable.wall);
            iconsMap.put("arch", R.drawable.wall);
            iconsMap.put("city-wall", R.drawable.wall);
            iconsMap.put("city-gate", R.drawable.wall);
            iconsMap.put("frontier-system-limes", R.drawable.wall);
        }
    }
