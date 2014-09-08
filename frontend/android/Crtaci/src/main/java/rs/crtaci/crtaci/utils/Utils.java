package rs.crtaci.crtaci.utils;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

import rs.crtaci.crtaci.R;
import rs.crtaci.crtaci.services.CrtaciHttpService;


public class Utils {

    public static boolean isNetworkAvailable(Context context) {
        final ConnectivityManager conMgr =  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNetworkReachable() {
        boolean reachable = false;
        try {
            SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);
            Socket sock = new Socket();

            int timeout = 3000;
            sock.connect(sockaddr, timeout);
            sock.close();
            reachable = true;
        }catch(Exception e){
        }
        return reachable;
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(CrtaciHttpService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static InputStream httpGet(String url){
        URI uri;
        InputStream data = null;
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            uri = new URI(url);
            HttpGet method = new HttpGet(uri);
            HttpResponse response = httpClient.execute(method);
            data = response.getEntity().getContent();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return data;
    }

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
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View messageView = inflater.inflate(R.layout.about_dialog, null, false);

        TextView textView = (TextView) messageView.findViewById(R.id.about);
        int defaultColor = textView.getTextColors().getDefaultColor();
        textView.setTextColor(defaultColor);

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

}
