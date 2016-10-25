package com.github.gen2brain.crtaci.utils;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;


public class Utils {

    public static String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;
        for(char c : input.toCharArray()) {
            if(Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if(nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }

    public static Tracker getTracker(Context ctx) {
        Tracker tracker;
        String trackingId = "UA-56360203-1";
        Activity activity = (Activity) ctx;

        GoogleAnalytics analytics = GoogleAnalytics.getInstance(ctx);
        analytics.enableAutoActivityReports(activity.getApplication());
        tracker = analytics.newTracker(trackingId);
        tracker.setAnonymizeIp(true);
        return tracker;
    }

    public static void saveLastCharacter(Context ctx, int position) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt("last", position);
        edit.apply();
    }

    public static int getLastCharacter(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getInt("last", -1);
    }

    static long getUnixTime() {
        return System.currentTimeMillis() / 1000L;
    }

    public static void downloadVideo(Context ctx, String url, String title) {
       	DownloadManager downloadmanager;
       	downloadmanager = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
       	Uri uri = Uri.parse(url);
       	DownloadManager.Request request = new DownloadManager.Request(uri);
       	request.setTitle(title.replace(" ", "_")+".mp4");
       	request.setDescription("Crtaci");
       	request.setVisibleInDownloadsUi(true);
       	request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title.replace(" ", "_")+".mp4");
       	downloadmanager.enqueue(request);
    }

}