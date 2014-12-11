package com.github.gen2brain.crtaci.activities;

import android.app.Activity;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.github.gen2brain.crtaci.R;
import com.github.gen2brain.crtaci.entities.Cartoon;
import com.github.gen2brain.crtaci.utils.VideoEnabledWebChromeClient;
import com.github.gen2brain.crtaci.utils.VideoEnabledWebView;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import go.main.Main;


public class PlayerActivity extends Activity {

    public static final String TAG = "PlayerActivity";

    private VideoEnabledWebView webView;
    private VideoEnabledWebChromeClient webChromeClient;
    private io.vov.vitamio.widget.VideoView vitamioView;

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

        if(cartoon.service.equals("youtube")) {
            if(video != null && !video.isEmpty()) {
                vitamioPlayer(video);
            } else {
                playYouTube();
            }
        } else if(cartoon.service.equals("dailymotion")) {
            if(video != null && !video.isEmpty()) {
                vitamioPlayer(video);
            } else {
                playDailyMotion();
            }
        } else if(cartoon.service.equals("vimeo")) {
            if(video != null && !video.isEmpty()) {
                vitamioPlayer(video);
            } else {
                playVimeo();
            }
        } else if(cartoon.service.equals("vk")) {
            if(video != null && !video.isEmpty()) {
                vitamioPlayer(video);
            } else {
                playVK();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(vitamioView != null) {
            vitamioView.setVideoLayout(io.vov.vitamio.widget.VideoView.VIDEO_LAYOUT_SCALE, 0);
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

    public void playVK() {
        webViewPlayer(cartoon.url);
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

    public void vitamioPlayer(String url) {
        setContentView(R.layout.player_vitamio);
        vitamioView = (io.vov.vitamio.widget.VideoView) findViewById(R.id.video_view);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        vitamioView.setBufferSize(1024 * 512);

        vitamioView.setOnPreparedListener(new io.vov.vitamio.MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(io.vov.vitamio.MediaPlayer mp) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        });

        vitamioView.setOnErrorListener(new io.vov.vitamio.MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(io.vov.vitamio.MediaPlayer mp, int what, int extra) {
                Log.d(TAG, "onError");
                if(retry >= 2) {
                    video = null;
                    if(cartoon.service.equals("youtube")) {
                        playYouTube();
                    } else if(cartoon.service.equals("dailymotion")) {
                        playDailyMotion();
                    } else if(cartoon.service.equals("vimeo")) {
                        playVimeo();
                    } else if(cartoon.service.equals("vk")) {
                        playVK();
                    }
                } else {
                    Log.d(TAG, "retry " + String.valueOf(retry));
                    if(cartoon.service.equals("vk")) {
                        new ExtractTask().execute(cartoon.service, cartoon.url);
                    } else {
                        new ExtractTask().execute(cartoon.service, cartoon.id);
                    }
                }
                return true;
            }
        });

        vitamioView.setOnCompletionListener(new io.vov.vitamio.MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(io.vov.vitamio.MediaPlayer mediaPlayer) {
                onBackPressed();
            }
        });

        io.vov.vitamio.widget.MediaController mediaController = new io.vov.vitamio.widget.MediaController(this);
        mediaController.setAnchorView(vitamioView);
        mediaController.setMediaPlayer(vitamioView);
        vitamioView.setKeepScreenOn(true);
        vitamioView.setMediaController(mediaController);
        vitamioView.setVideoURI(Uri.parse(url));
        vitamioView.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if(video == null || video.isEmpty()) {
            webChromeClient.onBackPressed();
            webView.destroy();
        }
        super.onBackPressed();
    }

    private class ExtractTask extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... params) {
            String service = params[0];
            String videoId = params[1];

            String result = null;
            try {
                result = Main.Extract(service, videoId);
            } catch(Exception e) {
                e.printStackTrace();
            }

            if(result == null || result.equals("empty")) {
                return null;
            }

            try {
                JsonElement jsonElement = new Gson().fromJson(result, JsonElement.class);
                if(jsonElement != null) {
                    return jsonElement.getAsString();
                } else {
                    return null;
                }
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(String results) {
            Log.d(TAG, "onPostExecute");
            retry += 1;
            if(results != null) {
                video = results;
                vitamioPlayer(video);
            }
        }

    }

}
