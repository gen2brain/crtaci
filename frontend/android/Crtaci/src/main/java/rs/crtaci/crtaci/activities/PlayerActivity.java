package rs.crtaci.crtaci.activities;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import rs.crtaci.crtaci.R;
import rs.crtaci.crtaci.entities.Cartoon;
import rs.crtaci.crtaci.utils.DMWebVideoView;
import rs.crtaci.crtaci.utils.HTML5WebView;


public class PlayerActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {

    public static final String TAG = "PlayerActivity";

    public static final String apiKey = "YOUR_API_KEY";

    private DMWebVideoView dailymotionView;
    private HTML5WebView vimeoView;

    private Cartoon cartoon;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        cartoon = (Cartoon) bundle.get("cartoon");

        if(cartoon.service.equals("youtube")) {
            setContentView(R.layout.player_youtube);
            YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
            youTubeView.initialize(apiKey, this);
        } else if(cartoon.service.equals("dailymotion")) {
            setContentView(R.layout.player_dailymotion);
            dailymotionView = ((DMWebVideoView) findViewById(R.id.dailymotion_view));
            dailymotionView.setKeepScreenOn(true);
            dailymotionView.setAutoPlaying(true);
            dailymotionView.setAllowAutomaticNativeFullscreen(true);
            dailymotionView.setBackgroundColor(0x00000000);
            dailymotionView.setVideoId(cartoon.id);
        } else if(cartoon.service.equals("vimeo")) {
            vimeoView = new HTML5WebView(this);
            setContentView(vimeoView.getLayout());
            vimeoView.setKeepScreenOn(true);
            vimeoView.setBackgroundColor(0x00000000);
            String params = "?autoplay=1&&badge=0&byline=0&portrait=0&title=0";
            vimeoView.loadUrl("https://player.vimeo.com/video/" + cartoon.id + params);
        }
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
        if(cartoon.service.equals("dailymotion")) {
            dailymotionView.handleBackPress(this);
        } else if(cartoon.service.equals("vimeo")) {
            vimeoView.hideCustomView();
            //vimeoView.goBack();
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            if(cartoon.service.equals("dailymotion")) {
                dailymotionView.onPause();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            if(cartoon.service.equals("dailymotion")) {
                dailymotionView.onResume();
            }
        }
    }

}
