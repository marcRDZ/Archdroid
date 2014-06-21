package com.theSoftwarer.archdroid;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Ras-Mars on 05/06/2014.
 */
public class NotesFragment extends ListFragment {


    private String title, idDataset, pleiadesPlace, index, count, jsn;
    private Uri.Builder builder;
    private static final int CREATE_LISTS = 3589;
    private Handler handler;
    private List<HashMap<String,String>> annotations;
    private NotesAdapter adapter;

    public NotesFragment() {

        annotations = new ArrayList<HashMap<String, String>>();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                Bundle b = msg.getData();
                jsn = b.getString("data");

                if(b.getInt("switcher") == CREATE_LISTS){

                    try {
                        JsonManager.createListFromJson(jsn, annotations);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    adapter = new NotesAdapter(getActivity().getApplicationContext(), R.layout.list_item, annotations);
                    setListAdapter(adapter);
                }
            }
        };
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (getArguments() != null) {
            index = Integer.toString(getArguments().getInt("index"));
            count = Integer.toString(getArguments().getInt("count"));
            title = getArguments().getString("title");
            idDataset = getArguments().getString("idDataset");
            pleiadesPlace = getArguments().getString("urlPleiades");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        builder = new Uri.Builder();
        builder.scheme("http").authority("pelagios.dme.ait.ac.at")
                .path("/api/datasets/" + idDataset + "/annotations.json").appendQueryParameter("forPlace", pleiadesPlace);

        new Thread(new Runnable() {
            public void run() {
                try {
                    JsonManager.searchPelagiosData(builder.toString(), CREATE_LISTS, handler);

                } catch (IOException e) {
                    Log.e(ArchMapFragment.LOG_TAG, "Cannot retrieve annotations for this place", e);
                } catch (IllegalArgumentException e) {
                    Log.e(ArchMapFragment.LOG_TAG, "Error connecting to service", e);
                }
            }
        }).start();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.list_layout, container, false);
        TextView tvTitle = (TextView)v.findViewById(R.id.name_database);
        TextView tvPosition = (TextView)v.findViewById(R.id.index);
        tvPosition.setText(index +"/"+ count);
        tvTitle.setText(title);

        return v;

    }

    public static NotesFragment newInstance(String title, String id, String uri, int index, int count) {

        NotesFragment f = new NotesFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("idDataset", id);
        args.putString("urlPleiades", uri);
        args.putInt("index", index);
        args.putInt("count", count);
        f.setArguments(args);

        return f;
    }

    public class NotesAdapter extends ArrayAdapter<HashMap<String,String>> {

        private NotesAdapter(Context context, int resource, List<HashMap<String, String>> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row=convertView;
            AnnotationsHolder holder;

            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                row = inflater.inflate(R.layout.list_item, parent, false);
                holder = new AnnotationsHolder(row);
                row.setTag(holder);
            }
            else {
                holder = (AnnotationsHolder)row.getTag();
            }

            holder.populateFrom(annotations.get(position));

            return(row);
        }

    }
    private static class AnnotationsHolder {

        private TextView item, url;

        AnnotationsHolder(View row){
            item = (TextView)row.findViewById(R.id.name);
            url = (TextView)row.findViewById(R.id.url);
        }

        void populateFrom(HashMap<String,String> hashMap){
            if (hashMap.containsKey("place"))
                item.setText(hashMap.get("place"));
            else
                item.setText(hashMap.get("object"));
            url.setText(hashMap.get("url"));
        }

    }
}