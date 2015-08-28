package com.github.gen2brain.crtaci.activities;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
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

import go.crtaci.Crtaci;
import io.vov.vitamio.LibsChecker;


public class CharactersActivity extends AppCompatActivity {

    public static final String TAG = "CharactersActivity";

    private boolean twoPane;
    private ArrayList<Character> characters;
    private CharactersTask charactersTask;
    private ProgressBar progressBar;
    private BroadcastReceiver downloadReceiver;
    private Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        if(!LibsChecker.checkVitamioLibs(this)) {
            return;
        }

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

        tracker = Utils.getTracker(this);
        tracker.setScreenName("Characters");
        tracker.send(new HitBuilders.AppViewBuilder().build());

        if(Update.checkUpdate(this)) {
            downloadReceiver = Update.getDownloadReceiver(this);
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new UpdateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                new UpdateTask().execute();
            }
        }

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
        if(downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if(characters != null && !characters.isEmpty()) {
            outState.putSerializable("characters", characters);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String player = prefs.getString("player", "vitamio");

        menu.setGroupCheckable(R.id.player_group, true, true);
        if(player.equals("vitamio")) {
            MenuItem menuItem = menu.findItem(R.id.action_vitamio_player);
            menuItem.setChecked(true);
        } else if(player.equals("default")) {
            MenuItem menuItem = menu.findItem(R.id.action_default_player);
            menuItem.setChecked(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_about) {
            Utils.showAbout(this);
            return true;
        } else if(id == R.id.action_refresh) {
            startCharactersTask();
        } else if(id == R.id.action_vitamio_player) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("player", "vitamio");
            edit.apply();
            item.setChecked(true);
        } else if(id == R.id.action_default_player) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("player", "default");
            edit.apply();
            item.setChecked(true);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
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
                ft.commit();
                getSupportFragmentManager().executePendingTransactions();
            }
            ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.characters_container, CharactersFragment.newInstance(results, twoPane));
            ft.commit();
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentById(R.id.container);
            if (prev != null) {
                ft.remove(prev);
                ft.commit();
                getSupportFragmentManager().executePendingTransactions();
            }
            ft = getSupportFragmentManager().beginTransaction();
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

            String result = null;
            try {
                result = Crtaci.List();
            } catch(Exception e) {
                e.printStackTrace();
            }

            if(result == null || result.equals("empty")) {
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
            if(results != null) {
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
