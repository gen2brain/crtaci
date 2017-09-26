package com.github.gen2brain.crtaci.activities;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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
import com.github.gen2brain.crtaci.utils.Update;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.github.gen2brain.crtaci.fragments.CharactersFragment;
import com.github.gen2brain.crtaci.R;
import com.github.gen2brain.crtaci.entities.Character;

import crtaci.Crtaci;

public class CharactersActivity extends AppCompatActivity {

    public static final String TAG = "CharactersActivity";

    private boolean twoPane;
    private ArrayList<Character> characters;
    private ProgressBar progressBar;

    public static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 313;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_characters);

        progressBar = findViewById(R.id.progressbar);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        twoPane = findViewById(R.id.cartoons_container) != null;

        if(Update.checkUpdate(this)) {
            new UpdateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        int permissionCheck = ContextCompat.checkSelfPermission(CharactersActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
       	if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CharactersActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
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
        if(id == R.id.action_about) {
            Dialogs.showAbout(this);
            return true;
        } else if(id == R.id.action_refresh) {
            startCharactersTask();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
       	switch(requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE: {
               	if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), getString(R.string.error_storage_not_granted), Toast.LENGTH_LONG).show();
               	}
               	break;
            }
       	}
    }

    public void startCharactersTask() {
        CharactersTask charactersTask = new CharactersTask();
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
                result = Crtaci.list();
            } catch(Exception e) {
                e.printStackTrace();
            }

            if(result == null || result.equals("empty")) {
                return null;
            }

            Type listType = new TypeToken<ArrayList<Character>>() {}.getType();
            try {
                return new Gson().fromJson(result, listType);
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
            return Update.updateExists();
        }

        protected void onPostExecute(Boolean result) {
            if(result) {
                Dialogs.showUpdate(CharactersActivity.this);
            }
        }
    }

}
