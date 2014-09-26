package rs.crtaci.crtaci.activities;

import android.os.AsyncTask;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import rs.crtaci.crtaci.fragments.CartoonsFragment;
import rs.crtaci.crtaci.services.CrtaciHttpService;
import rs.crtaci.crtaci.R;
import rs.crtaci.crtaci.entities.Character;
import rs.crtaci.crtaci.entities.Cartoon;
import rs.crtaci.crtaci.utils.Utils;


public class CartoonsActivity extends ActionBarActivity {

    public static final String TAG = "CartoonsActivity";

    private boolean twoPane;
    private Character character;
    private ArrayList<Cartoon> cartoons;
    private CartoonsTask cartoonsTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_cartoons);

        if(findViewById(R.id.cartoons_container) != null) {
            twoPane = true;
        } else {
            twoPane = false;
        }

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        character = (Character) bundle.get("character");

        String title = character.name;
        title = title.replace(" - ", "").replace("-", "");
        title = String.format("%s / %s", getResources().getString(R.string.app_name), Utils.toTitleCase(title));
        actionBar.setTitle(title);

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

        if(Utils.isNetworkAvailable(this)) {
            cartoonsTask = new CartoonsTask();
            cartoonsTask.execute(query);
        } else {
            Toast.makeText(this, getString(R.string.error_network), Toast.LENGTH_LONG).show();
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
            setSupportProgressBarIndeterminateVisibility(true);
        }

        protected ArrayList<Cartoon> doInBackground(String... params) {
            String query = params[0];
            String url = CrtaciHttpService.url + "search/" + query.replace(" ", "%20");

            InputStream input = null;
            while(input == null) {
                if(!Utils.isNetworkReachable()) {
                    return null;
                }

                if(isCancelled()) {
                    return null;
                }

                input = Utils.httpGet(url);
            }

            Reader reader;
            try {
                reader = new InputStreamReader(input, "UTF-8");
            } catch(UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }

            Type listType = new TypeToken<ArrayList<Cartoon>>(){}.getType();
            try {
                ArrayList<Cartoon> list = new Gson().fromJson(reader, listType);
                return list;
            } catch(JsonSyntaxException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<Cartoon> results) {
            Log.d(TAG, "onPostExecute");
            setSupportProgressBarIndeterminateVisibility(false);
            if(results != null && !results.isEmpty()) {
                cartoons = results;
                try {
                    replaceFragment(results);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getApplication(), getString(R.string.error_network), Toast.LENGTH_LONG).show();
                finish();
            }
        }

    }

}
