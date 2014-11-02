package rs.crtaci.crtaci.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;

import rs.crtaci.crtaci.fragments.CartoonsFragment;
import rs.crtaci.crtaci.services.CrtaciHttpService;
import rs.crtaci.crtaci.R;
import rs.crtaci.crtaci.entities.Character;
import rs.crtaci.crtaci.entities.Cartoon;
import rs.crtaci.crtaci.utils.Connectivity;
import rs.crtaci.crtaci.utils.Utils;


public class CartoonsActivity extends ActionBarActivity {

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

        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        if(findViewById(R.id.cartoons_container) != null) {
            twoPane = true;
        } else {
            twoPane = false;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle bundle = getIntent().getExtras();
        character = (Character) bundle.get("character");

        String title = character.name;
        title = String.format(Utils.toTitleCase(title));

        toolbar.setSubtitle(title);
        toolbar.setLogo(R.drawable.ic_launcher);
        toolbar.setNavigationIcon(R.drawable.ic_chevron_left);

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
        cancelCartoonsTask();
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
        getMenuInflater().inflate(R.menu.cartoons, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if(id == R.id.action_about) {
            Utils.showAbout(this);
            return true;
        } else if(id == R.id.action_rate) {
            Utils.rateThisApp(this);
        } else if(id == R.id.action_refresh) {
            cancelCartoonsTask();
            startCartoonsTask();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        NavUtils.navigateUpFromSameTask(this);
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

    public void cancelCartoonsTask() {
        if(cartoonsTask != null) {
            if(cartoonsTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
                cartoonsTask.cancel(true);
            }
            cartoonsTask = null;
        }
    }

    public void replaceFragment(ArrayList<Cartoon> results) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentById(R.id.container);
        if (prev != null) {
            ft.remove(prev);
        }
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
            try {
                query = URLEncoder.encode(query, "utf-8");
                query = query.replaceAll("\\+", "%20");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String url = CrtaciHttpService.url + "search/" + query;

            String result;
            if(!Utils.isNetworkReachable()) {
                return null;
            }

            if(isCancelled()) {
                return null;
            }

            result = Utils.httpGet(url, getApplication());

            if(result == null) {
                if(Utils.portAvailable(7313)) {
                    Intent intent = new Intent(getApplication(), CrtaciHttpService.class);
                    stopService(intent);
                    startService(intent);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                result = Utils.httpGet(url, getApplication());
                if(result == null) {
                    return null;
                }
            }

            if(isCancelled()) {
                return null;
            }

            Type listType = new TypeToken<ArrayList<Cartoon>>(){}.getType();
            try {
                ArrayList<Cartoon> list = new Gson().fromJson(result, listType);
                return list;
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
            if(results != null && !results.isEmpty()) {
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
