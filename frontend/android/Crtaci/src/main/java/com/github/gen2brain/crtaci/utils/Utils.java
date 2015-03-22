package com.github.gen2brain.crtaci.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;

import com.alertdialogpro.AlertDialogPro;

import com.github.gen2brain.crtaci.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;


public class Utils {

    public static Boolean playStore = false;

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

    public static void rateThisApp(Context ctx) {
        Uri uri = Uri.parse("market://details?id=" + ctx.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            ctx.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + ctx.getPackageName())));
        }
    }

    public static void showAbout(Context ctx) {
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View messageView = inflater.inflate(R.layout.about_dialog, null, false);

        String ver = Update.getCurrentVersion(ctx);
        String title = String.format("%s %s", ctx.getResources().getString(R.string.app_name), ver);

        AlertDialogPro.Builder builder = new AlertDialogPro.Builder(ctx);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(title);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

    public static void showUpdate(Context ctx) {
        final Context context = ctx;
        AlertDialogPro.Builder builder = new AlertDialogPro.Builder(ctx);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.update_available);
        builder.setMessage(R.string.update_download);
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Update.downloadUpdate(context);
                    }
                }
        );
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }
        );
        builder.create();
        builder.show();
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
        int last = prefs.getInt("last", -1);
        return last;
    }

    public static long getUnixTime() {
        return System.currentTimeMillis() / 1000L;
    }

}