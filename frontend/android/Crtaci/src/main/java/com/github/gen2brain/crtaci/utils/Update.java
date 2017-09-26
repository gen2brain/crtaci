package com.github.gen2brain.crtaci.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import java.util.Locale;

import crtaci.Crtaci;


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

    static String getCurrentVersion() {
        return Crtaci.Version;
    }

    private static String getUpdateVersion() {
        String ver = getCurrentVersion();
        float version = Float.parseFloat(ver) + 0.1f;
        return String.format(Locale.ROOT, "%.1f", version);
    }

    public static boolean updateExists() {
        try {
            return Crtaci.updateExists();
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static void downloadUpdate(Context ctx) {
        DownloadManager downloadmanager;
        downloadmanager = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);

        String updateVersion = getUpdateVersion();
        String updateUrl = String.format(Locale.ROOT, "https://crtaci.rs/download/crtaci-%s.apk", updateVersion);

        Uri uri = Uri.parse(updateUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(String.format(Locale.ROOT, "crtaci-%s", updateVersion));
        request.setMimeType("application/vnd.android.package-archive");
       	request.setVisibleInDownloadsUi(true);
       	request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, String.format(Locale.ROOT, "crtaci-%s.apk", updateVersion));
        downloadmanager.enqueue(request);
    }

}
