package com.github.gen2brain.crtaci.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.Locale;

import com.github.gen2brain.crtaci.R;
import com.github.gen2brain.crtaci.activities.PlayerActivity;
import com.github.gen2brain.crtaci.entities.Cartoon;
import com.github.gen2brain.crtaci.utils.Utils;

import crtaci.Crtaci;


public class CartoonsFragment extends Fragment {

    public static final String TAG = "CartoonsFragment";

    private boolean twoPane;
    private ArrayList<Cartoon> cartoons;
    private Cartoon selectedCartoon;
    private ProgressBar progressBar;

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

        return inflater.inflate(R.layout.fragment_cartoons, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = view.getRootView().findViewById(R.id.progressbar);
        createListView(view);

        if(cartoons != null && !cartoons.isEmpty()) {
            Tracker tracker = Utils.getTracker(getActivity());
            tracker.setScreenName(cartoons.get(0).character);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
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
        ListView listView = view.findViewById(R.id.cartoons);
        final ItemAdapter adapter = new ItemAdapter();
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
               	MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.context, menu);
               	return true;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
               	int id = item.getItemId();
               	if(id == R.id.action_download) {
                    for(Cartoon c: adapter.getSelectedItems()) {
                        new DownloadTask().execute(c.service, c.id, c.title);
                    }
                    mode.finish();
                    return true;
               	}

               	return false;
            }

            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
               	if(checked) {
                    adapter.selectItem(position);
               	} else {
                    adapter.unselectItem(position);
               	}

               	int count = adapter.getSelectedCount();
               	mode.setTitle(count + " selected");
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
               	return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
               	adapter.unselectItems();
            }
       	});

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedCartoon = cartoons.get(position);
                new ExtractTask().execute(selectedCartoon.service, selectedCartoon.id);
            }
        });
    }


    private class ItemAdapter extends BaseAdapter {

        private ArrayList<Cartoon> selectedCartoons = new ArrayList<>();

        private class ViewHolder {
            public TextView title;
            ImageView thumbnail;
        }

        @Override
        public int getCount() {
            if(cartoons != null) {
                return cartoons.size();
            } else {
                return 0;
            }
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

            final Cartoon cartoon = cartoons.get(position);

            if(convertView == null) {
                Context context = parent.getContext();
                LayoutInflater inflater = LayoutInflater.from(context);

                view = inflater.inflate(R.layout.item_list_cartoon, parent, false);

                holder = new ViewHolder();
                holder.title = view.findViewById(R.id.title);
                holder.thumbnail = view.findViewById(R.id.thumbnail);

                Typeface tf=Typeface.createFromAsset(getActivity().getAssets(), "fonts/ComicRelief.ttf");
                holder.title.setTypeface(tf);

                view.setTag(holder);
                view.setBackgroundResource(R.drawable.item_background_cartoon);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            SpannableString spanString = new SpannableString(getTitle(cartoon));
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
            holder.title.setText(spanString);

            String thumb;
            if(twoPane) {
                thumb = cartoon.thumbLarge;
            } else {
                thumb = cartoon.thumbSmall;
            }

            Glide.with(getContext()).load(thumb).into(holder.thumbnail);

            return view;
        }

        private String getTitle(Cartoon cartoon) {
            String ch = Utils.toTitleCase(cartoon.formattedTitle);
            String se = "";
            if(cartoon.season != -1) {
                se += String.format(Locale.ROOT, "S%02d", cartoon.season);
            }
            if(cartoon.episode != -1) {
                se += String.format(Locale.ROOT, "E%02d", cartoon.episode);
            }
            if(!se.isEmpty()) {
                se = " - " + se;
            }
            return String.format(Locale.ROOT, "%s%s (%s)", ch, se, cartoon.durationString);
        }

        void selectItem(int position) {
            Cartoon cartoon = cartoons.get(position);
            selectedCartoons.add(cartoon);
        }

        void unselectItem(int position) {
            Cartoon cartoon = cartoons.get(position);
            selectedCartoons.remove(cartoon);
        }

        void unselectItems() {
            selectedCartoons = new ArrayList<>();
        }

        ArrayList<Cartoon> getSelectedItems() {
            return selectedCartoons;
        }

        int getSelectedCount() {
            return selectedCartoons.size();
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

            String result = null;
            try {
                result = Crtaci.extract(service, videoId);
            } catch(Exception e) {
                e.printStackTrace();
            }

            if(result == null || result.equals("empty")) {
                try {
                    result = Crtaci.extract(service, videoId);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            if(result == null) {
                return null;
            } else if(result.equals("empty")) {
                return "empty";
            }

            return result;
        }

        protected void onPostExecute(String results) {
            Log.d(TAG, "onPostExecute");
            if(progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            Activity activity = getActivity();
            if(activity != null) {
                if(results != null && !results.equals("empty")) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                    String player = prefs.getString("player", "default");
                    if(player.equals("external")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(results), "video/*");
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(activity, PlayerActivity.class);
                        intent.putExtra("video", results);
                        intent.putExtra("cartoon", selectedCartoon);
                        startActivity(intent);
                    }
                } else if(results != null && results.equals("empty")) {
                    Toast.makeText(getActivity(), getString(R.string.error_video), Toast.LENGTH_LONG).show();
                }
            }
        }

    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        private String title;

        protected String doInBackground(String... params) {
            String service = params[0];
            String videoId = params[1];
            title = params[2];

            String result = null;
            try {
                result = Crtaci.extract(service, videoId);
            } catch(Exception e) {
                e.printStackTrace();
            }

            if(result == null) {
                return null;
            } else if(result.equals("empty")) {
                return "empty";
            }

            return result;
        }

        protected void onPostExecute(String results) {
            Log.d(TAG, "onPostExecute");

            if(results != null && !results.equals("empty")) {
                Utils.downloadVideo(getActivity(), results, title);
            }
        }

    }

}
