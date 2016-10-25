package com.github.gen2brain.crtaci.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import java.util.Locale;

import go.crtaci.Crtaci;


public class Update {

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

    static String getCurrentVersion(Context ctx) {
        return Crtaci.Version;
    }

    private static String getUpdateVersion(Context ctx) {
        String ver = getCurrentVersion(ctx);
        float version = Float.parseFloat(ver) + 0.1f;
        return String.format(Locale.ROOT, "%.1f", version);
    }

    private static String getUpdateUrl(Context ctx) {
        String ver = getUpdateVersion(ctx);
        return String.format(Locale.ROOT, "https://crtaci.rs/download/crtaci-%s.apk", ver);
    }

    public static boolean updateExists(Context ctx) {
        try {
            return Crtaci.checkUpdate();
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void downloadUpdate(Context ctx) {
        DownloadManager downloadmanager;
        downloadmanager = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(getUpdateUrl(ctx));
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle("Downloading update");
        request.setDescription("Crtaci");
        request.setMimeType("application/vnd.android.package-archive");
       	request.setVisibleInDownloadsUi(true);
       	request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "crtaci-"+getUpdateVersion(ctx)+".apk");
        downloadmanager.enqueue(request);
    }

}
