package rs.crtaci.crtaci.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;


public class CrtaciHttpService extends Service {

    public static final String TAG = "CrtaciHttpService";

    String command;
    Process process;

    public static final String bind = "127.0.0.1:7313";
    public static final String url = String.format("http://%s/", bind);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        command = getApplicationInfo().nativeLibraryDir + "/libcrtaci-http.so";
        Log.d(TAG, command);
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        (new Thread() { public void run() {
            try {
                if(process != null) {
                    process.destroy();
                    process = null;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }}).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        CrtaciHttpThread thread = new CrtaciHttpThread(this);
        thread.start();

        return START_STICKY;
    }


    private class CrtaciHttpThread extends Thread {

        Context context;

        public CrtaciHttpThread(Context ctx) {
            context = ctx;
        }

        @Override
        public void run() {
            super.run();
            try {
                ArrayList<String> params = new ArrayList<String>();
                params.add(command);
                params.add("-bind");
                params.add(bind);

                ProcessBuilder pb = new ProcessBuilder(params);
                process = pb.start();
                process.waitFor();
            } catch(Exception e){
                e.printStackTrace();
            }
        }

    }

}
