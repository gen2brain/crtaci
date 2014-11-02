package rs.crtaci.crtaci.activities;

import android.content.Intent;
import android.os.AsyncTask;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import rs.crtaci.crtaci.fragments.CharactersFragment;
import rs.crtaci.crtaci.services.CrtaciHttpService;
import rs.crtaci.crtaci.R;
import rs.crtaci.crtaci.entities.Character;
import rs.crtaci.crtaci.utils.Utils;


public class CharactersActivity extends ActionBarActivity {

    public static final String TAG = "CharactersActivity";

    private boolean twoPane;
    private ArrayList<Character> characters;
    private CharactersTask charactersTask;
    private ProgressBar progressBar;

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

        if(savedInstanceState != null) {
            characters = (ArrayList<Character>) savedInstanceState.getSerializable("characters");
            replaceFragment(characters);
        } else {
            if(!Utils.isServiceRunning(this)) {
                Intent intent = new Intent(this, CrtaciHttpService.class);
                startService(intent);
            }
            startCharactersTask();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        cancelCharactersTask();
        if(Utils.isServiceRunning(this)) {
            Intent intent = new Intent(getApplication(), CrtaciHttpService.class);
            stopService(intent);
        }
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
            cancelCharactersTask();
            startCharactersTask();
        }
        return super.onOptionsItemSelected(item);
    }

    public void startCharactersTask() {
        charactersTask = new CharactersTask();
        charactersTask.execute();
    }

    public void cancelCharactersTask() {
        if(charactersTask != null) {
            if(charactersTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
                charactersTask.cancel(true);
            }
            charactersTask = null;
        }
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
            String result;
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }

            if(isCancelled()) {
                return null;
            }

            result = Utils.httpGet(CrtaciHttpService.url + "list", getApplication());

            if(result == null) {
                if(Utils.portAvailable(7313)) {
                    Intent intent = new Intent(getApplication(), CrtaciHttpService.class);
                    stopService(intent);
                    startService(intent);
                }

                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }

                result = Utils.httpGet(CrtaciHttpService.url + "list", getApplication());
                if(result == null) {
                    return null;
                }
            }

            if(isCancelled()) {
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

}
