package com.theSoftwarer.archdroid;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ras-Mars on 29/06/2014.
 */
public class PagesFragment extends Fragment {


    private static final int CREATE_PAGES = 9265;
    private String js, urlPleiades;
    private Handler handler;
    private DatasetsAdapter mPagerAdapter;
    private List<Fragment> datasets;
    private ViewPager mPager;
    private Uri.Builder builder;

    public PagesFragment() {

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                Bundle b = msg.getData();
                js = b.getString("data");

                datasets = new ArrayList<Fragment>();
                try {
                    JsonManager.createPagesFromJson(js, urlPleiades, datasets);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mPagerAdapter = new DatasetsAdapter(getActivity().getSupportFragmentManager(), datasets);
                mPager = (ViewPager)getActivity().findViewById(R.id.viewpager_layout);
                mPager.setAdapter(mPagerAdapter);

            }

        };

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public void onStart() {
        super.onStart();

        urlPleiades = getArguments().get("itemSource").toString();

        builder = new Uri.Builder();
        builder.scheme("http").authority("pelagios.dme.ait.ac.at").path("/api/places/").appendPath(urlPleiades);

        new Thread(new Runnable() {
            public void run() {
                try {
                    JsonManager.searchPelagiosData(builder.toString() + "/datasets.json", handler);
                } catch (IOException e) {
                    Log.e(ArchMapFragment.LOG_TAG, "Cannot retrieve datasets for this places", e);
                } catch (IllegalArgumentException e) {
                    Log.e(ArchMapFragment.LOG_TAG, "Error connecting to service", e);
                }
            }
        }).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.pager_fragment, container, false);
        TextView tvName = (TextView)v.findViewById(R.id.item_name);
        TextView tvType = (TextView)v.findViewById(R.id.item_type);
        tvName.setText(getArguments().getString("itemName"));
        tvType.setText(getArguments().getString("itemType"));

        return v;
    }

    public static PagesFragment newInstance(String name, String source, String type) {

        PagesFragment pagesFragment = new PagesFragment();
        Bundle bundle = new Bundle();

        bundle.putString("itemName", name);
        bundle.putString("itemSource", source);
        bundle.putString("itemType", type);

        pagesFragment.setArguments(bundle);

        return pagesFragment;
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
