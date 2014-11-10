package com.theSoftwarer.archdroid;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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


    private String idDataset, pleiadesPlace, jsn;
    private Uri.Builder builder;
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

                try {
                    JsonManager.createListFromJson(jsn, annotations);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                adapter = new NotesAdapter(annotations);
                setListAdapter(adapter);

            }

        };
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (getArguments() != null) {

            idDataset = getArguments().getString("idDataset");
            pleiadesPlace = getArguments().getString("urlPleiades");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null){

            builder = new Uri.Builder();
            builder.scheme("http").authority("pelagios.dme.ait.ac.at")
                    .path("/api/datasets/" + idDataset + "/annotations.json").appendQueryParameter("forPlace", pleiadesPlace);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        JsonManager.searchPelagiosData(builder.toString(), handler);

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.notes_fragment, container, false);
        TextView tvTitle = (TextView)v.findViewById(R.id.name_database);
        TextView tvPosition = (TextView)v.findViewById(R.id.index);
        tvPosition.setText(Integer.toString(getArguments().getInt("index")) +"/"+ Integer.toString(getArguments().getInt("count")));
        tvTitle.setText(getArguments().getString("title"));

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

    public class NotesAdapter extends BaseAdapter {

        private List<HashMap<String, String>> notes;

        public NotesAdapter(List<HashMap<String, String>> notes) {
            this.notes = notes;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            AnnotationsHolder holder;

            if (convertView == null) {

                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.list_item, parent, false);
                holder = new AnnotationsHolder(convertView);
                convertView.setTag(holder);
            }
            else {
                holder = (AnnotationsHolder)convertView.getTag();
            }

            holder.populateFrom(getItem(position));

            return convertView;
        }

        @Override
        public HashMap<String, String> getItem(int position) {
            return notes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {

            return notes.size();
        }

    }

    public static class AnnotationsHolder {

        private TextView item, url;

        AnnotationsHolder(View row){
            item = (TextView)row.findViewById(R.id.name);
            url = (TextView)row.findViewById(R.id.url);
        }

        void populateFrom(HashMap<String,String> hashMap){

            item.setText(hashMap.get("name"));
            url.setText(hashMap.get("url"));
        }

    }

}