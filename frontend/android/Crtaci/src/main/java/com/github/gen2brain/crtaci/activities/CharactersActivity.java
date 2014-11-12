package com.github.gen2brain.crtaci.activities;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.github.gen2brain.crtaci.utils.Update;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.github.gen2brain.crtaci.fragments.CharactersFragment;
import com.github.gen2brain.crtaci.R;
import com.github.gen2brain.crtaci.entities.Character;
import com.github.gen2brain.crtaci.utils.Utils;

import go.Go;
import go.main.Main;


public class CharactersActivity extends ActionBarActivity {

    public static final String TAG = "CharactersActivity";

    private boolean twoPane;
    private ArrayList<Character> characters;
    private CharactersTask charactersTask;
    private ProgressBar progressBar;
    //private BroadcastReceiver downloadReceiver;
    private Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_characters);

        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setLogo(R.drawable.ic_launcher);

        if(findViewById(R.id.cartoons_container) != null) {
            twoPane = true;
        } else {
            twoPane = false;
        }

        Go.init(getApplicationContext());

        tracker = Utils.getTracker(this);
        tracker.setScreenName("Characters");
        tracker.send(new HitBuilders.AppViewBuilder().build());

        //if(Update.checkUpdate(this)) {
        //    downloadReceiver = Update.getDownloadReceiver(this);
        //    registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        //
        //    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        //        new UpdateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        //    } else {
        //        new UpdateTask().execute();
        //    }
        //}

        if(savedInstanceState != null) {
            characters = (ArrayList<Character>) savedInstanceState.getSerializable("characters");
            replaceFragment(characters);
        } else {
            startCharactersTask();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        //if(downloadReceiver != null) {
        //    unregisterReceiver(downloadReceiver);
        //}
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        //super.onSaveInstanceState(outState);
        if(characters != null && !characters.isEmpty()) {
            outState.putSerializable("characters", characters);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.characters, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_about) {
            Utils.showAbout(this);
            return true;
        } else if(id == R.id.action_rate) {
            Utils.rateThisApp(this);
        } else if(id == R.id.action_refresh) {
            startCharactersTask();
        }
        return super.onOptionsItemSelected(item);
    }

    public void startCharactersTask() {
        charactersTask = new CharactersTask();
        charactersTask.execute();
    }

    public void replaceFragment(ArrayList<Character> results) {
        if(twoPane) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentById(R.id.characters_container);
            if (prev != null) {
                ft.remove(prev);
            }
            ft.replace(R.id.characters_container, CharactersFragment.newInstance(results, twoPane));
            ft.commit();
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentById(R.id.container);
            if (prev != null) {
                ft.remove(prev);
            }
            ft.replace(R.id.container, CharactersFragment.newInstance(results, twoPane));
            ft.commit();
        }
    }


    private class CharactersTask extends AsyncTask<Void, Void, ArrayList<Character>> {

        protected void onPreExecute() {
            super.onPreExecute();
            if(progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        protected ArrayList<Character> doInBackground(Void... params) {

            String result = Main.List();

            if(result == null || result.isEmpty()) {
                return null;
            }

            Type listType = new TypeToken<ArrayList<Character>>() {}.getType();
            try {
                ArrayList<Character> list = new Gson().fromJson(result, listType);
                return list;
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<Character> results) {
            Log.d(TAG, "onPostExecute");
            if(progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if(results != null && !results.isEmpty()) {
                characters = results;
                try {
                    replaceFragment(results);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private class UpdateTask extends AsyncTask<Void, Void, Boolean> {

        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected Boolean doInBackground(Void... params) {
            return Update.updateExists(getApplication());
        }

        protected void onPostExecute(Boolean result) {
            if(result) {
                Utils.showUpdate(CharactersActivity.this);
            }
        }
    }

}
