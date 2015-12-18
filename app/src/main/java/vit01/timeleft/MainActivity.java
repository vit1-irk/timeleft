package vit01.timeleft;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewDebug;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    native public void resources_init();
    native public void setConfig_fromstring(String text);
    native public String getTimetable();

    String mytext;
    TextView maintextview;
    TableLayout tableView;
    TabHost maintabs;
    BroadcastReceiver service;
    IntentFilter filter;

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
        filter=new IntentFilter();
        filter.addAction("VibrationService");
        service=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("VibrationService")) {
                    mytext=intent.getStringExtra("minutely");
                    maintextview.setText(mytext);
                }
            }
        };
        registerReceiver(service, filter);
        startService(new Intent(this, VibrationService.class));
    }

    protected void onResume() {
        updatePrefs();
        super.onResume();
    }

    protected void onDestroy() {
        stopService(new Intent(this, VibrationService.class));
        unregisterReceiver(service);
        super.onDestroy();
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
        int bint7=Integer.parseInt(sharedPref.getString("vibrateOffsetMinutes", "2")) * 60;
        String btext7 = String.valueOf(bint7);
        // Toast.makeText(this, btext1, Toast.LENGTH_SHORT).show();

        String confText=btext1+":"+btext2+":"+btext3+":"+btext7+"\n";
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
