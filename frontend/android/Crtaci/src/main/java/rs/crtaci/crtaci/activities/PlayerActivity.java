package rs.crtaci.crtaci.activities;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewClient;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import rs.crtaci.crtaci.R;
import rs.crtaci.crtaci.entities.Cartoon;
import rs.crtaci.crtaci.utils.VideoEnabledWebChromeClient;
import rs.crtaci.crtaci.utils.VideoEnabledWebView;


public class PlayerActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {

    public static final String TAG = "PlayerActivity";

    public static final String apiKey = "YOUR_API_KEY";

    private VideoEnabledWebView webView;
    private VideoEnabledWebChromeClient webChromeClient;

    private Cartoon cartoon;
    private String video;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        cartoon = (Cartoon) bundle.get("cartoon");
        video = bundle.getString("video");

        if(cartoon.service.equals("youtube")) {
            if(video != null && !video.isEmpty()) {
                videoPlayer(video);
            } else {
                playYouTube();
            }
        } else if(cartoon.service.equals("dailymotion")) {
            if(video != null && !video.isEmpty()) {
                videoPlayer(video);
            } else {
                playDailyMotion();
            }
        } else if(cartoon.service.equals("vimeo")) {
            if(video != null && !video.isEmpty()) {
                videoPlayer(video);
            } else {
                playVimeo();
            }
        }
    }

    public void playYouTube() {
        setContentView(R.layout.player_youtube);
        YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        youTubeView.initialize(apiKey, this);
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
        webView.setWebViewClient(new WebViewClient());
        webView.setKeepScreenOn(true);
        webView.setBackgroundColor(0x00000000);
        webView.loadUrl(url);

        if(cartoon.service.equals("dailymotion")) {
            ViewGroup.LayoutParams params = webView.getLayoutParams();
            params.height = 400;
            webView.setLayoutParams(params);
        }
    }

    public void videoPlayer(String url) {
        setContentView(R.layout.player);
        final VideoView videoView = (VideoView) findViewById(R.id.video_view);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                progressBar.setVisibility(View.INVISIBLE);
                videoView.start();
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                onBackPressed();
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d(TAG, "onError");
                if(cartoon.service.equals("youtube")) {
                    playYouTube();
                } else if(cartoon.service.equals("dailymotion")) {
                    playDailyMotion();
                } else if(cartoon.service.equals("vimeo")) {
                    playVimeo();
                }
                return true;
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
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
        if(!wasRestored) {
            player.loadVideo(cartoon.id);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult result) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if(video == null || video.isEmpty()) {
            if(cartoon.service.equals("dailymotion") || cartoon.service.equals("vimeo")) {
                webChromeClient.onBackPressed();
                webView.destroy();
            }
        }
        super.onBackPressed();
    }

}
