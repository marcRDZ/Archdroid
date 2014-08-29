package com.theSoftwarer.archdroid;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ras-Mars on 29/06/2014.
 */
public class PagesFragment extends Fragment implements View.OnTouchListener{

    private String js, urlPleiades;
    private Handler handler;
    private DatasetsAdapter mPagerAdapter;
    private List<Fragment> datasets;
    private ViewPager mPager;
    private Uri.Builder builder;
    public boolean isExpanded;
    public FrameLayout frameLayout;

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
            isExpanded = false;
            frameLayout = (FrameLayout) getActivity().findViewById(R.id.container);
    }

    @Override
    public void onStart() {
        super.onStart();

        urlPleiades = getArguments().getString("itemSource");

        builder = new Uri.Builder();
        builder.scheme("http").authority("pelagios.dme.ait.ac.at").path("/api/places/").appendPath(urlPleiades);

        new Thread(new Runnable() {
            public void run() {
                try {
                    JsonManager.searchPelagiosData(builder + "/datasets.json", handler);
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

        v.setOnTouchListener(this);
        TextView tvName = (TextView)v.findViewById(R.id.item_name);
        TextView tvType = (TextView)v.findViewById(R.id.item_type);
        tvName.setText(getArguments().getString("itemName"));
        tvType.setText(getArguments().getString("itemType"));

        return v;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

       int action = MotionEventCompat.getActionMasked(motionEvent);

            switch (action) {
                case MotionEvent.ACTION_DOWN:

                    if (isExpanded) {
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 40);
                        frameLayout.setLayoutParams(layoutParams);
                        ((ActionBarActivity) getActivity()).getSupportActionBar().show();
                        isExpanded = false;
                    }
                    else {
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 4);
                        frameLayout.setLayoutParams(layoutParams);
                        ((ActionBarActivity) getActivity()).getSupportActionBar().hide();
                        isExpanded = true;
                    }

                break;

            }

        return true;
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
