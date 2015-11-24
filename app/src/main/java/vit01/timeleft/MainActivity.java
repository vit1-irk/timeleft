package vit01.timeleft;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.audiofx.BassBoost;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.os.Handler;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    native public void resources_init();
    native public String update_wrapper();
    native public void setConfig_fromstring(String text);
    native public String getTimetable();

    private Handler mHandler = new Handler();
    String mytext;
    TextView maintextview;
    TableLayout tableView;
    TabHost maintabs;

    static {
        System.loadLibrary("timeleft");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        maintextview=(TextView)findViewById(R.id.maintextview);
        tableView=(TableLayout)findViewById(R.id.tableLayout);

        maintabs=(TabHost)findViewById(R.id.tabHost);
        maintabs.setup();

        TabHost.TabSpec timetab=maintabs.newTabSpec("timetab");
        timetab.setContent(R.id.linearLayout);
        timetab.setIndicator("Время");
        TabHost.TabSpec rasptab=maintabs.newTabSpec("rasptab");
        rasptab.setContent(R.id.tableLayout);
        rasptab.setIndicator("Расписание");

        maintabs.addTab(timetab);
        maintabs.addTab(rasptab);

        resources_init();
        new Thread(new Task()).start();
    }

    protected void onResume() {
        updatePrefs();
        super.onResume();
    }

    public void frame_update() {
        mytext=update_wrapper();
        maintextview.setText(mytext);
    }
    class Task implements Runnable {
        public void run() {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mHandler.post(new Runnable() {
                                  @Override
                                  public void run() {
                                      frame_update();
                                  }}
                );
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_main_settings:
                Intent myIntent=new Intent(MainActivity.this, SettingsActivity.class);
                MainActivity.this.startActivity(myIntent);
                // User chose the "Settings" item, show the app settings UI...
        }
        return true;
    }
    public void updatePrefs() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String btext1 = sharedPref.getString("startHour", "");
        String btext2 = sharedPref.getString("startMinute", "");
        String btext3 = sharedPref.getString("localOffset", "");
        String btext4 = sharedPref.getString("countLessons", "");
        String btext5 = sharedPref.getString("lessonLength", "");
        String btext6 = sharedPref.getString("breaksText", "");
        // Toast.makeText(this, btext1, Toast.LENGTH_SHORT).show();

        String confText=btext1+":"+btext2+":"+btext3+"\n";
        confText+=btext4+":"+btext5+":10\n"+btext6;

        setConfig_fromstring(confText);
        resources_init();
        mytext=getTimetable();
        String[] lines = mytext.split("\n");
        tableView.removeAllViews();

        for(int i=0;i<lines.length;i++) {
            TableRow row=new TableRow(this);
            TextView tv1=new TextView(this);
            TextView tv2=new TextView(this);

            String[] tmp=lines[i].split("-");
            tv1.setText(tmp[0]);
            tv1.setTextColor(Color.BLACK);
            tv1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);

            tv2.setText(tmp[1]);
            tv2.setTextColor(Color.BLACK);
            tv2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);

            if (i%2==1) {
                row.setBackgroundColor(Color.LTGRAY);
            }
            row.addView(tv1);
            row.addView(tv2);
            tableView.addView(row);
        }

        Toast.makeText(MainActivity.this, "Подгружаем настройки", Toast.LENGTH_SHORT).show();
    }
}
