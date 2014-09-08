package rs.crtaci.crtaci.fragments;


import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
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
import android.widget.TextView;

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

import rs.crtaci.crtaci.R;
import rs.crtaci.crtaci.activities.PlayerActivity;
import rs.crtaci.crtaci.entities.Cartoon;
import rs.crtaci.crtaci.utils.Utils;


public class CartoonsFragment extends Fragment {

    public static final String TAG = "CartoonsFragment";

    private ArrayList<Cartoon> cartoons;

    protected ImageLoader imageLoader = ImageLoader.getInstance();

    public static CartoonsFragment newInstance(ArrayList<Cartoon> cartoons) {
        CartoonsFragment fragment = new CartoonsFragment();
        Bundle args = new Bundle();
        args.putSerializable("cartoons", cartoons);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        getActivity().setProgressBarIndeterminateVisibility(false);

        if(savedInstanceState != null) {
            cartoons = (ArrayList<Cartoon>) savedInstanceState.getSerializable("cartoons");
        } else {
            cartoons = (ArrayList<Cartoon>) getArguments().getSerializable("cartoons");
        }

        View rootView = inflater.inflate(R.layout.fragment_cartoons, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.cartoons);
        ListAdapter adapter = new ItemAdapter();
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra("cartoon", cartoons.get(position));
                startActivity(intent);
            }
        });

        if(!imageLoader.isInited()) {
            File cacheDir = new File(getActivity().getCacheDir().toString());
            ImageLoaderConfiguration config = new
                    ImageLoaderConfiguration.Builder(getActivity().getApplicationContext())
                    .discCache(new UnlimitedDiscCache(cacheDir))
                    .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
                    .build();
            imageLoader.init(config);
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if(cartoons != null && !cartoons.isEmpty()) {
            outState.putSerializable("cartoons", cartoons);
        }
    }


    class ItemAdapter extends BaseAdapter {

        private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

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

            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater(null);
                view = inflater.inflate(R.layout.item_list_cartoon, parent, false);

                holder = new ViewHolder();
                holder.title = (TextView) view.findViewById(R.id.title);
                holder.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            view.setBackgroundResource(R.drawable.item_background_cartoon);

            Cartoon cartoon = cartoons.get(position);

            Typeface tf=Typeface.createFromAsset(getActivity().getAssets(), "fonts/comic.ttf");
            holder.title.setTypeface(tf);

            SpannableString spanString = new SpannableString(getTitle(cartoon));
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
            holder.title.setText(spanString);

            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .showImageOnLoading(R.drawable.ic_stub)
                    .showImageForEmptyUri(R.drawable.ic_empty)
                    .showImageOnFail(R.drawable.ic_error)
                    .cacheInMemory(true)
                    .cacheOnDisc(true)
                    .considerExifParams(true)
                    .displayer(new SimpleBitmapDisplayer())
                    .build();

            String thumb;
            int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
            if(screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE) {
                thumb = cartoon.thumbnails.medium;
            } else {
                thumb = cartoon.thumbnails.small;
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

}
