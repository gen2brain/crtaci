package rs.crtaci.crtaci.utils;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.alertdialogpro.AlertDialogPro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;

import rs.crtaci.crtaci.R;
import rs.crtaci.crtaci.services.CrtaciHttpService;


public class Utils {

    public static boolean isNetworkReachable() {
        boolean reachable = false;
        try {
            SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);
            Socket sock = new Socket();

            int timeout = 5000;
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

    public static String httpGet(String uri, Context context){
        URL url;
        String result = null;
        InputStream data = null;
        HttpURLConnection urlConnection = null;
        //System.setProperty("http.keepAlive", "false");

        try {
            url = new URL(uri);

            if(Connectivity.isConnectedMobile(context)) {
                String proxyHost = getProxyHost(context);
                int proxyPort = getProxyPort(context);
                if(!proxyHost.isEmpty() && proxyPort != -1) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                    urlConnection = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    urlConnection = (HttpURLConnection) url.openConnection();
                }
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }

            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setRequestMethod("GET");
            //urlConnection.setRequestProperty("Connection", "close");
            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(15000);

            urlConnection.connect();

            int errorCode = urlConnection.getResponseCode();
            boolean isError = errorCode >= 400;

            if(isError) {
                Log.d("STATUS", String.valueOf(errorCode));
            }

            data = isError ? urlConnection.getErrorStream() : urlConnection.getInputStream();

            if(data != null) {
                result = convertInputStreamToString(data);
                urlConnection.disconnect();
            }
        } catch(Exception e) {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }
            if(data != null) {
                try {
                    data.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            e.printStackTrace();
        }
        return result;
    }

    public static String getProxyHost(Context context) {
        String proxyHost = new String();
        int apiVersion = android.os.Build.VERSION.SDK_INT;
        try {
            if(apiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                proxyHost = System.getProperty("http.proxyHost");
            } else {
                proxyHost = android.net.Proxy.getHost(context);
            }
        } catch (Exception ex) {
        }
        if(proxyHost == null || proxyHost.equals("")) {
            return new String();
        } else {
            return proxyHost;
        }
    }

    public static int getProxyPort(Context context) {
        int proxyPort = -1;
        int apiVersion = android.os.Build.VERSION.SDK_INT;
        try {
            if(apiVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                proxyPort = Integer.valueOf(System.getProperty("http.proxyPort"));
            } else {
                proxyPort = android.net.Proxy.getPort(context);
            }
        } catch (Exception ex) {
        }
        return proxyPort;
    }

    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        bufferedReader.close();
        reader.close();
        inputStream.close();
        return result;

    }

    public static boolean portAvailable(int port) {
        Socket s = null;
        try {
            s = new Socket("127.0.0.1", port);
            return false;
        } catch (IOException e) {
            return true;
        } finally {
            if(s != null){
                try {
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View messageView = inflater.inflate(R.layout.about_dialog, null, false);

        AlertDialogPro.Builder builder = new AlertDialogPro.Builder(ctx);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

}
