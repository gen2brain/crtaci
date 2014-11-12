package com.github.gen2brain.crtaci.fragments;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.github.gen2brain.crtaci.R;
import com.github.gen2brain.crtaci.activities.PlayerActivity;
import com.github.gen2brain.crtaci.entities.Cartoon;
import com.github.gen2brain.crtaci.utils.Utils;

import go.main.Main;


public class CartoonsFragment extends Fragment {

    public static final String TAG = "CartoonsFragment";

    private boolean twoPane;
    private ArrayList<Cartoon> cartoons;
    private Cartoon selectedCartoon;
    private ProgressBar progressBar;

    protected ImageLoader imageLoader = ImageLoader.getInstance();

    public static CartoonsFragment newInstance(ArrayList<Cartoon> cartoons, boolean twoPane) {
        CartoonsFragment fragment = new CartoonsFragment();
        Bundle args = new Bundle();
        args.putSerializable("cartoons", cartoons);
        args.putBoolean("twoPane", twoPane);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        if(savedInstanceState != null) {
            cartoons = (ArrayList<Cartoon>) savedInstanceState.getSerializable("cartoons");
        } else {
            cartoons = (ArrayList<Cartoon>) getArguments().getSerializable("cartoons");
        }

        twoPane = getArguments().getBoolean("twoPane");

        View view = inflater.inflate(R.layout.fragment_cartoons, container, false);

        if(!imageLoader.isInited()) {
            File cacheDir = new File(getActivity().getCacheDir().toString());
            ImageLoaderConfiguration config = new
                    ImageLoaderConfiguration.Builder(getActivity().getApplicationContext())
                    .discCache(new UnlimitedDiscCache(cacheDir))
                    .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
                    .build();
            imageLoader.init(config);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = (ProgressBar) view.getRootView().findViewById(R.id.progressbar);
        createListView(view);

        Tracker tracker = Utils.getTracker(getActivity());
        tracker.setScreenName(cartoons.get(0).character);
        tracker.send(new HitBuilders.AppViewBuilder().build());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if(cartoons != null && !cartoons.isEmpty()) {
            outState.putSerializable("cartoons", cartoons);
        }
    }

    public void createListView(View view) {
        ListView listView = (ListView) view.findViewById(R.id.cartoons);
        ListAdapter adapter = new ItemAdapter();
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedCartoon = cartoons.get(position);
                if(selectedCartoon.service.equals("vk")) {
                    new ExtractTask().execute(selectedCartoon.service, selectedCartoon.url);
                } else {
                    new ExtractTask().execute(selectedCartoon.service, selectedCartoon.id);
                }
            }
        });
    }


    class ItemAdapter extends BaseAdapter {

        private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_stub)
                .showImageForEmptyUri(R.drawable.ic_empty)
                .showImageOnFail(R.drawable.ic_error)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();

        private class ViewHolder {
            public TextView title;
            public ImageView thumbnail;
        }

        @Override
        public int getCount() {
            return cartoons.size();
        }

        @Override
        public Object getItem(int position) {
            return cartoons.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            final ViewHolder holder;

            Cartoon cartoon = cartoons.get(position);

            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater(null);
                view = inflater.inflate(R.layout.item_list_cartoon, parent, false);

                holder = new ViewHolder();
                holder.title = (TextView) view.findViewById(R.id.title);
                holder.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);

                Typeface tf=Typeface.createFromAsset(getActivity().getAssets(), "fonts/ComicRelief.ttf");
                holder.title.setTypeface(tf);

                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            view.setBackgroundResource(R.drawable.item_background_cartoon);

            SpannableString spanString = new SpannableString(getTitle(cartoon));
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
            holder.title.setText(spanString);

            String thumb;
            if(twoPane) {
                thumb = cartoon.thumbLarge;
            } else {
                thumb = cartoon.thumbSmall;
            }

            imageLoader.displayImage(thumb, holder.thumbnail, options, animateFirstListener);

            return view;
        }

        private String getTitle(Cartoon cartoon) {
            String ch = cartoon.formattedTitle;
            String se = "";
            if(cartoon.season != -1) {
                se += String.format("S%02d", cartoon.season);
            }
            if(cartoon.episode != -1) {
                se += String.format("E%02d", cartoon.episode);
            }
            if(!se.isEmpty()) {
                se = " - " + se;
            }
            return Utils.toTitleCase(ch) + se;
        }
    }


    private static class AnimateFirstDisplayListener extends SimpleImageLoadingListener {

        static final List<String> displayedImages = Collections.synchronizedList(new LinkedList<String>());

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if(loadedImage != null) {
                ImageView imageView = (ImageView) view;
                boolean firstDisplay = !displayedImages.contains(imageUri);
                if(firstDisplay) {
                    FadeInBitmapDisplayer.animate(imageView, 500);
                    displayedImages.add(imageUri);
                }
            }
        }
    }


    private class ExtractTask extends AsyncTask<String, Void, String> {

        protected void onPreExecute() {
            super.onPreExecute();
            if(progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        protected String doInBackground(String... params) {
            String service = params[0];
            String videoId = params[1];

            String result = Main.Extract(service, videoId);

            if(result == null || result.isEmpty()) {
                return null;
            }

            try {
                JsonElement jsonElement = new Gson().fromJson(result, JsonElement.class);
                if(jsonElement != null) {
                    return jsonElement.getAsString();
                } else {
                    return null;
                }

            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(String results) {
            Log.d(TAG, "onPostExecute");
            if(progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            Activity activity = getActivity();
            if(activity != null) {
                Intent intent = new Intent(activity, PlayerActivity.class);
                intent.putExtra("video", results);
                intent.putExtra("cartoon", selectedCartoon);
                startActivity(intent);
            }
        }

    }

}
