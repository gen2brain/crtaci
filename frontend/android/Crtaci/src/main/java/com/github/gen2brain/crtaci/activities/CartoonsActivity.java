package com.github.gen2brain.crtaci.activities;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
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
import android.widget.Toast;

import com.github.gen2brain.crtaci.utils.Dialogs;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.github.gen2brain.crtaci.fragments.CartoonsFragment;
import com.github.gen2brain.crtaci.R;
import com.github.gen2brain.crtaci.entities.Character;
import com.github.gen2brain.crtaci.entities.Cartoon;
import com.github.gen2brain.crtaci.utils.Connectivity;
import com.github.gen2brain.crtaci.utils.Utils;

import crtaci.Crtaci;


public class CartoonsActivity extends AppCompatActivity {

    public static final String TAG = "CartoonsActivity";

    private boolean twoPane;
    private Character character;
    private ArrayList<Cartoon> cartoons;
    private CartoonsTask cartoonsTask;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cartoons);

        progressBar = findViewById(R.id.progressbar);

        twoPane = findViewById(R.id.cartoons_container) != null;

        Toolbar toolbar = findViewById(R.id.toolbar);

        Bundle bundle = getIntent().getExtras();
        character = (Character) bundle.get("character");

        if(character != null) {
            String title = character.name;
            title = Utils.toTitleCase(title);

            toolbar.setTitle(title);
            toolbar.setNavigationIcon(R.drawable.ic_chevron_left);
            setSupportActionBar(toolbar);
        }

        if(savedInstanceState != null) {
            cartoons = (ArrayList<Cartoon>) savedInstanceState.getSerializable("cartoons");
            if(cartoons != null) {
                replaceFragment(cartoons);
            } else {
                startCartoonsTask();
            }
        } else {
            startCartoonsTask();
        }

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        if(cartoonsTask != null) {
            if(cartoonsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
               	cartoonsTask.cancel(true);
            }
            Crtaci.cancel();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String player = prefs.getString("player", "default");

        menu.setGroupCheckable(R.id.player_group, true, true);
        if(player.equals("external")) {
            MenuItem menuItem = menu.findItem(R.id.action_external_player);
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
        if(id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if(id == R.id.action_about) {
            Dialogs.showAbout(this);
            return true;
        } else if(id == R.id.action_refresh) {
            startCartoonsTask();
        } else if(id == R.id.action_external_player) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("player", "external");
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
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public void startCartoonsTask() {
        String query;
        if(!character.altname.isEmpty()) {
            query = character.altname;
        } else {
            query = character.name;
        }

        if(Connectivity.isConnected(this)) {
            cartoonsTask = new CartoonsTask();
            cartoonsTask.execute(query);
        } else {
            Toast.makeText(this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
        }
    }

    public void replaceFragment(ArrayList<Cartoon> results) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentById(R.id.container);
        if(prev != null) {
            ft.remove(prev);
            ft.commit();
            getSupportFragmentManager().executePendingTransactions();
        }
        ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, CartoonsFragment.newInstance(results, twoPane));
        ft.commit();
    }


    private class CartoonsTask extends AsyncTask<String, Void, ArrayList<Cartoon>> {

        protected void onPreExecute() {
            super.onPreExecute();
            if(progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        protected ArrayList<Cartoon> doInBackground(String... params) {
            String query = params[0];

            String result = null;
            try {
                result = Crtaci.search(query);
            } catch(Exception e) {
                e.printStackTrace();
            }

            if(result == null || result.equals("empty") || isCancelled()) {
                return null;
            }

            Type listType = new TypeToken<ArrayList<Cartoon>>(){}.getType();
            try {
                return new Gson().fromJson(result, listType);
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<Cartoon> results) {
            Log.d(TAG, "onPostExecute");
            if(progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if(results != null) {
                cartoons = results;
                try {
                    replaceFragment(results);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getApplication(), getString(R.string.error_network), Toast.LENGTH_LONG).show();
            }
        }

    }

}
