package com.github.gen2brain.crtaci.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.gen2brain.crtaci.R;


public class Dialogs {

    public static void showAbout(Context ctx) {
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View messageView = inflater.inflate(R.layout.about_dialog, null, false);

        String ver = Update.getCurrentVersion();
        String title = String.format("%s %s", ctx.getResources().getString(R.string.app_name), ver);

        MaterialDialog.Builder builder = new MaterialDialog.Builder(ctx);
        builder.icon(ContextCompat.getDrawable(ctx, R.drawable.ic_launcher));
        builder.title(title);
        builder.customView(messageView, false);
        builder.show();
    }

    public static void showUpdate(Context ctx) {
        final Context context = ctx;
        MaterialDialog.Builder builder = new MaterialDialog.Builder(ctx);
        builder.icon(ContextCompat.getDrawable(ctx, R.drawable.ic_launcher));
        builder.title(R.string.update_available);
        builder.content(R.string.update_download);
        builder.positiveText("OK");
        builder.negativeText("Cancel");

        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                Update.downloadUpdate(context);
            }
        });

        builder.onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }
}
