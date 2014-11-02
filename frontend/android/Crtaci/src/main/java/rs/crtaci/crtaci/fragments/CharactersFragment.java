package rs.crtaci.crtaci.fragments;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;

import rs.crtaci.crtaci.R;
import rs.crtaci.crtaci.activities.CartoonsActivity;
import rs.crtaci.crtaci.entities.Cartoon;
import rs.crtaci.crtaci.entities.Character;
import rs.crtaci.crtaci.services.CrtaciHttpService;
import rs.crtaci.crtaci.utils.Connectivity;
import rs.crtaci.crtaci.utils.Utils;


public class CharactersFragment extends Fragment {

    public static final String TAG = "CharactersFragment";

    private boolean twoPane;
    private ArrayList<Character> characters;
    private CartoonsTask cartoonsTask;
    private int selectedListItem = -1;
    private ProgressBar progressBar;

    public static CharactersFragment newInstance(ArrayList<Character> characters, boolean twoPane) {
        CharactersFragment fragment = new CharactersFragment();
        Bundle args = new Bundle();
        args.putSerializable("characters", characters);
        args.putBoolean("twoPane", twoPane);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        if(savedInstanceState != null) {
            characters = (ArrayList<Character>) savedInstanceState.getSerializable("characters");
        } else {
            characters = (ArrayList<Character>) getArguments().getSerializable("characters");
        }

        twoPane = getArguments().getBoolean("twoPane");

        View view = inflater.inflate(R.layout.fragment_characters, container, false);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = (ProgressBar) view.getRootView().findViewById(R.id.progressbar);
        createListView(view);
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
        if(characters != null && !characters.isEmpty()) {
            outState.putSerializable("characters", characters);
        }
    }

    public void startCartoonsTask() {
        Character character = null;
        if(characters != null && !characters.isEmpty()) {
            character = characters.get(selectedListItem);
        }

        if(character == null) {
            return;
        }

        String query;
        if(!character.altname.isEmpty()) {
            query = character.altname;
        } else {
            query = character.name;
        }

        if(Connectivity.isConnected(getActivity())) {
            cartoonsTask = new CartoonsTask();
            cartoonsTask.execute(query);
        } else {
            Toast.makeText(getActivity(), getString(R.string.error_network), Toast.LENGTH_LONG).show();
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

    public void createListView(View view) {
        final ListView listView = (ListView) view.findViewById(R.id.characters);
        final ItemAdapter adapter = new ItemAdapter();

        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedListItem = position;
                if(twoPane) {
                    startCartoonsTask();
                    adapter.notifyDataSetChanged();
                } else {
                    Intent intent = new Intent(getActivity(), CartoonsActivity.class);
                    intent.putExtra("character", characters.get(position));
                    startActivity(intent);
                }
            }
        });

        if(twoPane) {
            listView.performItemClick(listView.getChildAt(0), 0, adapter.getItemId(0));
        }
    }


    class ItemAdapter extends BaseAdapter {

        private class ViewHolder {
            public TextView name;
            public ImageView icon;
        }

        @Override
        public int getCount() {
            if(characters != null && !characters.isEmpty()) {
                return characters.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            return characters.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = convertView;
            final ViewHolder holder;

            Character character = characters.get(position);

            if(convertView == null) {
                LayoutInflater inflater = getLayoutInflater(null);
                view = inflater.inflate(R.layout.item_list_character, parent, false);

                holder = new ViewHolder();
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.icon = (ImageView) view.findViewById(R.id.icon);

                Typeface tf=Typeface.createFromAsset(getActivity().getAssets(), "fonts/comic.ttf");
                holder.name.setTypeface(tf);

                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            SpannableString spanString = new SpannableString(getName(character));
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);

            holder.name.setText(spanString);

            if(position % 2 == 0) {
                view.setBackgroundResource(R.drawable.item_background);
            } else {
                view.setBackgroundResource(R.drawable.item_background_alternate);
            }

            if(position == selectedListItem) {
                view.setBackgroundColor(getResources().getColor(R.color.item_selected));
            }

            holder.icon.setImageDrawable(getIcon(character));

            return view;
        }

        private String getName(Character character) {
            String ch = character.name;
            return Utils.toTitleCase(ch);
        }

        private Drawable getIcon(Character character) {
            String ch;
            if(!character.altname.isEmpty()) {
                ch = character.altname;
            } else {
                ch = character.name;
            }
            ch = ch.replace(" ", "_");

            int resId = getResources().getIdentifier(ch, "drawable", getActivity().getPackageName());
            Drawable icon = getResources().getDrawable(resId);
            return icon;
        }
    }

    public void replaceFragment(ArrayList<Cartoon> results) {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment prev = getActivity().getSupportFragmentManager().findFragmentById(R.id.cartoons_container);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.replace(R.id.cartoons_container, CartoonsFragment.newInstance(results, twoPane));
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

            result = Utils.httpGet(url, getActivity());

            if(result == null) {
                if(Utils.portAvailable(7313)) {
                    Intent intent = new Intent(getActivity(), CrtaciHttpService.class);
                    getActivity().stopService(intent);
                    getActivity().startService(intent);
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                result = Utils.httpGet(url, getActivity());
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
                try {
                    replaceFragment(results);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getActivity(), getString(R.string.error_network), Toast.LENGTH_LONG).show();
            }
        }

    }
}
