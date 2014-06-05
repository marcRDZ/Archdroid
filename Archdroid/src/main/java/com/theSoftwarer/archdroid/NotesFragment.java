package com.theSoftwarer.archdroid;

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
import java.util.HashMap;

/**
 * Created by Ras-Mars on 05/06/2014.
 */
public class NotesFragment extends ListFragment {


    private static Handler handler;
    private String title, idDataset, index, count, jsn;
    private Uri.Builder annotationsBuilder;
    public NotesFragment() {

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                Bundle b = msg.getData();
                jsn = b.getString("data");

                if(b.getInt("switcher") == CREATE_LISTS){

                    try {
                        createListFromJson(jsn);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            index = Integer.toString(getArguments().getInt("index"));
            count = Integer.toString(getArguments().getInt("count"));
            title = getArguments().getString("title");
            idDataset = getArguments().getString("idDataset");
        }
        annotationsBuilder = new Uri.Builder();
        annotationsBuilder.scheme("http").authority("pelagios.dme.ait.ac.at")
                .path("/api/datasets/" + idDataset + "/annotations.json").appendQueryParameter("forPlace", urlPleiades);
        //Toast.makeText(getActivity().getApplicationContext(), annotationsBuilder.toString(), Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                try {
                    searchPelagiosData( annotationsBuilder.toString(), CREATE_LISTS);

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot retrieve annotations for this place", e);
                } catch (IllegalArgumentException e) {
                    Log.e(LOG_TAG, "Error connecting to service", e);
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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (annotations != null){
            AnnotationsAdapter adapter = new AnnotationsAdapter();
            setListAdapter(adapter);
        }

    }

    public static NotesFragment newInstance(String title, String id, int index, int count) {

        NotesFragment f = new NotesFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("idDataset", id);
        args.putInt("index", index);
        args.putInt("count", count);
        f.setArguments(args);

        return f;
    }

    private class AnnotationsAdapter extends ArrayAdapter<HashMap<String,String>> {

        public AnnotationsAdapter() {
            super(getActivity().getApplicationContext(), R.layout.list_item, annotations);
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

            holder.populateFrom(getItem(position));

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
            item.setText(hashMap.get("object"));
            url.setText(hashMap.get("url"));
        }

    }
}