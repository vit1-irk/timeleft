package vit01.timeleft;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class VibrationService extends Service {
    native public int sendNotifyOrNot();
    native public String update_wrapper();

    public Vibrator vibrator;
    public Uri ringroneuri;
    public Ringtone ringtone;

    private Handler mHandler = new Handler();
    public Intent in;
    public SharedPreferences sharedPref;

    public VibrationService() {
        in=new Intent("VibrationService");
        new Thread(new Task()).start();
    }
    static {
        System.loadLibrary("timeleft");
    }

    class Task implements Runnable {
        public void run() {
            Context appContext=getApplicationContext();
            String shift=PreferenceManager.getDefaultSharedPreferences(appContext).getString("currshift", "1");
            sharedPref = getSharedPreferences("settings" + shift, MODE_PRIVATE);

            vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            ringroneuri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            ringtone=RingtoneManager.getRingtone(appContext, ringroneuri);
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mHandler.post(new Runnable() {
                                  @Override
                                  public void run() {
                                      in.putExtra("minutely", update_wrapper());
                                      sendBroadcast(in);
                                      spyLessons();
                                  }}
                );
            }
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    public void spyLessons() {
        boolean notifyUser=sharedPref.getBoolean("notifyUser", true);

        if (notifyUser) {
            int sendNotify = sendNotifyOrNot();
            if (sendNotify == 1) {
                vibrator.vibrate(1000);
                ringtone.play();
            }
        }
    }

}
