package com.theSoftwarer.archdroid;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
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

    public static void searchPelagiosData(String url, int switcher, Handler handler) throws IOException, IllegalArgumentException {

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
            sendToUIThread(json.toString(), switcher, handler);
        }
    }

    private static void sendToUIThread(String string, int switcher, Handler handler) {
        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("data", string);
        bundle.putInt("switcher", switcher);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    public static void createMarkersFromJson(String json, GoogleMap googleMap) throws JSONException {

        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObj = jsonArray.getJSONObject(i);
                googleMap.addMarker(new MarkerOptions().position(new LatLng(
                                jsonObj.getJSONObject("geometry").getJSONArray("coordinates").optDouble(1),
                                jsonObj.getJSONObject("geometry").getJSONArray("coordinates").optDouble(0)
                        )).snippet(jsonObj.getString("source"))
                        //.icon(BitmapDescriptorFactory.fromResource(R.drawable.unknown))
                );

            }
        }catch (JSONException e){e.printStackTrace();}

    }

    public static void createPagesFromJson (String json, String place, List datasets) throws JSONException {

        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                String idDataset = jsonObj.getString("uri").substring(43);
                Fragment fragment = NotesFragment.newInstance(jsonObj.getString("title"), idDataset, place, i + 1, jsonArray.length());
                datasets.add(fragment);
            }
        }catch (JSONException e){e.printStackTrace();}

    }

    public static void createListFromJson(String json, List notes) throws JSONException {


        HashMap<String, String> hm = new HashMap<String,String>();

        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("annotations");

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject jsonObj = jsonArray.getJSONObject(i);
                hm.put("object", jsonObj.optString("target_title"));
                hm.put("place", jsonObj.optString("title"));
                hm.put("url", jsonObj.getString("hasTarget"));
                notes.add(hm);

            }
        } catch (JSONException e){e.printStackTrace();}

    }
}
