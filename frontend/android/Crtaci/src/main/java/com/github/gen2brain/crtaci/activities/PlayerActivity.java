package com.github.gen2brain.crtaci.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.github.gen2brain.crtaci.R;
import com.github.gen2brain.crtaci.entities.Cartoon;
import com.github.gen2brain.crtaci.utils.VideoEnabledWebChromeClient;
import com.github.gen2brain.crtaci.utils.VideoEnabledWebView;

import go.crtaci.Crtaci;


public class PlayerActivity extends Activity {

    public static final String TAG = "PlayerActivity";

    private VideoEnabledWebView webView;
    private VideoEnabledWebChromeClient webChromeClient;

    private String video;
    private Cartoon cartoon;

    private int retry = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        cartoon = (Cartoon) bundle.get("cartoon");
        video = bundle.getString("video");

        switch(cartoon.service) {
            case "youtube":
                if(video != null && !video.isEmpty()) {
                    player(video);
                } else {
                    playYouTube();
                }
                break;
            case "dailymotion":
                if(video != null && !video.isEmpty()) {
                    player(video);
                } else {
                    playDailyMotion();
                }
                break;
            case "vimeo":
                if(video != null && !video.isEmpty()) {
                    player(video);
                } else {
                    playVimeo();
                }
                break;
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
            defaultPlayer(url);
        }
    }

    public void playYouTube() {
        String params = "?fs=1&autoplay=1&disablekb=1&showinfo=0";
        String url = "https://www.youtube.com/embed/" + cartoon.id + params;
        webViewPlayer(url);
    }

    public void playDailyMotion() {
        String params = "?html=1&fullscreen=1&autoplay=1&related=0&logo=0&info=0";
        String url = "http://www.dailymotion.com/embed/video/" + cartoon.id + params;
        webViewPlayer(url);
    }

    public void playVimeo() {
        String params = "?autoplay=1&badge=0&byline=0&portrait=0&title=0";
        String url = "http://player.vimeo.com/video/" + cartoon.id + params;
        webViewPlayer(url);
    }

    public void webViewPlayer(String url) {
        setContentView(R.layout.player_webview);
        webView = (VideoEnabledWebView) findViewById(R.id.webView);

        View nonVideoLayout = findViewById(R.id.nonVideoLayout);
        ViewGroup videoLayout = (ViewGroup) findViewById(R.id.videoLayout);
        View loadingView = getLayoutInflater().inflate(R.layout.loading, null);

        webChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, loadingView);
        webView.setWebChromeClient(webChromeClient);

        webView.setKeepScreenOn(true);
        webView.setBackgroundColor(0x00000000);
        webView.loadUrl(url);
    }

    public void defaultPlayer(String url) {
        setContentView(R.layout.player);
        final VideoView videoView = (VideoView) findViewById(R.id.video_view);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        videoView.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(android.media.MediaPlayer mp) {
                progressBar.setVisibility(View.INVISIBLE);
                videoView.start();
            }
        });

        videoView.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                Log.d(TAG, "onError");
                if(retry >= 2) {
                    video = null;
                    switch(cartoon.service) {
                        case "youtube":
                            playYouTube();
                            break;
                        case "dailymotion":
                            playDailyMotion();
                            break;
                        case "vimeo":
                            playVimeo();
                            break;
                    }
                } else {
                    Log.d(TAG, "retry " + String.valueOf(retry));
                    new ExtractTask().execute(cartoon.service, cartoon.id);
                }
                return true;
            }
        });

        videoView.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(android.media.MediaPlayer mediaPlayer) {
                onBackPressed();
            }
        });

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        mediaController.setMediaPlayer(videoView);
        videoView.setKeepScreenOn(true);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.parse(url));
        videoView.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if(video == null || video.isEmpty()) {
            if(webChromeClient != null && webView != null) {
                webChromeClient.onBackPressed();
                webView.destroy();
            }
        }
        super.onBackPressed();
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
