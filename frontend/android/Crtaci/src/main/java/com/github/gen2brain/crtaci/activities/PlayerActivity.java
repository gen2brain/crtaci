package com.github.gen2brain.crtaci.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.listener.VideoControlsButtonListener;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.ui.widget.VideoControlsMobile;

import com.github.gen2brain.crtaci.R;
import com.github.gen2brain.crtaci.entities.Cartoon;
import com.github.gen2brain.crtaci.utils.Utils;

import crtaci.Crtaci;


public class PlayerActivity extends Activity {

    public static final String TAG = "PlayerActivity";

    private String video;
    private Cartoon cartoon;

    private VideoView videoView;

    private int position;
    private int retry = 0;

    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        cartoon = (Cartoon) bundle.get("cartoon");
        video = bundle.getString("video");

        if(video != null && !video.isEmpty()) {
            player(video);
        } else {
            Toast.makeText(PlayerActivity.this, getString(R.string.error_video), Toast.LENGTH_LONG).show();
            onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        try {
            Crtaci.cancel();
        } catch(Exception e) {
            e.printStackTrace();
        }

        if(videoView != null) {
            if(videoView.isPlaying()) {
                videoView.stopPlayback();
            }
            videoView.release();
        }

        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        position = (int) videoView.getCurrentPosition();
        if(videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(videoView != null) {
            videoView.seekTo(position);
            videoView.start();
        }
    }

    public void player(String url) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String player = prefs.getString("player", "default");
        if(player.equals("external")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "video/*");
            startActivity(intent);
        } else if(player.equals("default")) {
            videoPlayer(url);
        }
    }

    public void videoPlayer(String url) {
        setContentView(R.layout.player);
        videoView = findViewById(R.id.video_view);
        final ProgressBar progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        final VideoControls controls = new Controls(this);
        videoView.setControls(controls);

        controls.setPreviousButtonRemoved(true);
        controls.setNextButtonRemoved(true);
        controls.setButtonListener(new ControlsListener());

        videoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared() {
                progressBar.setVisibility(View.INVISIBLE);
                controls.setTitle(Utils.toTitleCase(cartoon.character) + " - " + Utils.toTitleCase(cartoon.formattedTitle));
                videoView.start();
            }
        });

        videoView.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(Exception e) {
                Log.d(TAG, "onError");
                if(retry >= 2) {
                    Toast.makeText(PlayerActivity.this, getString(R.string.error_video), Toast.LENGTH_LONG).show();
                    onBackPressed();
                } else {
                    Log.d(TAG, "retry " + String.valueOf(retry));
                    new ExtractTask().execute(cartoon.service, cartoon.id);
                }
                return true;
            }
        });

        videoView.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion() {
                onBackPressed();
            }
        });

        videoView.setKeepScreenOn(true);
        videoView.setVideoURI(Uri.parse(url));
        videoView.requestFocus();
    }

    private class Controls extends VideoControlsMobile {

        public Controls(Context context) {
            super(context);
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.player_controls;
        }

    }

    private class ControlsListener implements VideoControlsButtonListener {
        @Override
        public boolean onPlayPauseClicked() {
            return false;
        }

        @Override
        public boolean onPreviousClicked() {
            return false;
        }

        @Override
        public boolean onNextClicked() {
            return false;
        }

        @Override
        public boolean onRewindClicked() {
            return false;
        }

        @Override
        public boolean onFastForwardClicked() {
            return false;
        }
    }

    private class ExtractTask extends AsyncTask<String, Void, String> {

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
                return null;
            }

            return result;
        }

        protected void onPostExecute(String results) {
            Log.d(TAG, "onPostExecute");
            retry += 1;
            if(results != null) {
                video = results;
                player(video);
            }
        }

    }

}
