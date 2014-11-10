package com.theSoftwarer.archdroid;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ras-Mars on 29/06/2014.
 */
public class PagesFragment extends Fragment {

    private String js, urlPleiades;
    private Handler handler;
    private DatasetsAdapter mPagerAdapter;
    private List<Fragment> datasets;
    private ViewPager mPager;
    private Uri.Builder builder;
    private ImageButton btnExpand;
    public FrameLayout frameLayout;
    private static int weight;

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
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.pager_fragment, container, false);

        TextView tvName = (TextView)v.findViewById(R.id.item_name);
        TextView tvType = (TextView)v.findViewById(R.id.item_type);
        ImageButton btnClose = (ImageButton)v.findViewById(R.id.btn_close);
        btnExpand = (ImageButton)v.findViewById(R.id.btn_expand);
        if (weight == 4){
            btnExpand.setImageResource(R.drawable.ic_action_expand);
        }else
            btnExpand.setImageResource(R.drawable.ic_action_collapse);

        tvName.setText(getArguments().getString("itemName"));
        tvType.setText(getArguments().getString("itemType"));
        btnExpand.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View view) {

                if (weight == 4) {
                    weight = 40;
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
                    frameLayout.setLayoutParams(layoutParams);
                    ((ActionBarActivity) getActivity()).getSupportActionBar().show();
                    btnExpand.setImageResource(R.drawable.ic_action_collapse);
                }
                else {
                    weight = 4;
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
                    frameLayout.setLayoutParams(layoutParams);
                    ((ActionBarActivity) getActivity()).getSupportActionBar().hide();
                    btnExpand.setImageResource(R.drawable.ic_action_expand);
                }
            }
        });
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    weight = 40;
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
                    frameLayout.setLayoutParams(layoutParams);
                    ((ActionBarActivity) getActivity()).getSupportActionBar().show();
                    closeFragment();

            }
        });

        return v;
    }

    private void closeFragment(){
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.lift_up, R.anim.lift_down, R.anim.lift_up, R.anim.lift_down);
        ft.remove(this).commit();
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
