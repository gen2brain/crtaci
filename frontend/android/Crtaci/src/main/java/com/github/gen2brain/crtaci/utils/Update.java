package com.github.gen2brain.crtaci.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class Update {

    public static String getCurrentVersion(Context ctx) {
        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getUpdateVersion(Context ctx) {
        String ver = getCurrentVersion(ctx);
        float version = Float.parseFloat(ver) + 0.1f;
        return String.format("%.1f", version);
    }

    public static boolean checkUpdate(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        long now = Utils.getUnixTime();
        long checked = prefs.getLong("checked", 0);
        long diff = now - checked;

        if(diff > 86400) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putLong("checked", now);
            edit.apply();
            return true;
        }
        return false;
    }

    public static String getUpdateUrl(Context ctx) {
        String ver = getUpdateVersion(ctx);
        String url = String.format(
                "https://github.com/gen2brain/crtaci/releases/download/%s/crtaci-%s.apk", ver, ver);
        return url;
    }

    public static boolean updateExists(Context ctx) {
        try {
            String url = getUpdateUrl(ctx);
            HttpsURLConnection.setFollowRedirects(false);
            HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("HEAD");
            return (conn.getResponseCode() == 302);
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static BroadcastReceiver getDownloadReceiver(Context ctx) {
        BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent view = new Intent();
                view.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
                context.startActivity(view);
            }
        };
        return downloadCompleteReceiver;
    }

    public static void downloadUpdate(Context ctx) {
        DownloadManager downloadmanager;
        downloadmanager = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(getUpdateUrl(ctx));
        DownloadManager.Request request = new DownloadManager.Request(uri);
        downloadmanager.enqueue(request);
    }

}
