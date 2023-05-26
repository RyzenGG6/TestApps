package com.example.test;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int USAGE_STATS_PERMISSION_REQUEST_CODE = 100;
    private ListView listView;
    private AppAdapter appAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.list);

        if (hasUsageStatsPermission()) {
            loadAppUsageStats();
        } else {
            requestUsageStatsPermission();
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivityForResult(intent, USAGE_STATS_PERMISSION_REQUEST_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == USAGE_STATS_PERMISSION_REQUEST_CODE) {
            if (hasUsageStatsPermission()) {
                loadAppUsageStats();
            } else {
                Toast.makeText(this, "Please grant usage access permission.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void loadAppUsageStats() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long currentTime = System.currentTimeMillis();
        long startTime = currentTime - (DateUtils.DAY_IN_MILLIS * 7); // Load usage stats for the last 7 days

        List<UsageStats> appUsageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, currentTime);

        if (appUsageStatsList != null && appUsageStatsList.size() > 0) {
            Collections.sort(appUsageStatsList, new Comparator<UsageStats>() {
                @Override
                public int compare(UsageStats stats1, UsageStats stats2) {
                    return Long.compare(stats2.getTotalTimeInForeground(), stats1.getTotalTimeInForeground());
                }
            });

            appAdapter = new AppAdapter(appUsageStatsList);
            listView.setAdapter(appAdapter);
        }
    }

    private class AppAdapter extends BaseAdapter {

        private List<UsageStats> appUsageStatsList;

        public AppAdapter(List<UsageStats> appUsageStatsList) {
            this.appUsageStatsList = appUsageStatsList;
        }

        @Override
        public int getCount() {
            return appUsageStatsList.size();
        }

        @Override
        public Object getItem(int position) {
            return appUsageStatsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_app_usage, parent, false);
            }

            UsageStats appUsageStats = appUsageStatsList.get(position);

            PackageManager packageManager = getPackageManager();
            ApplicationInfo appInfo;
            Drawable appIcon;
            String appName;
            String usageTime;

            try {
                appInfo = packageManager.getApplicationInfo(appUsageStats.getPackageName(), 0);
                appIcon = packageManager.getApplicationIcon(appUsageStats.getPackageName());
                appName = packageManager.getApplicationLabel(appInfo).toString();
                usageTime = DateUtils.formatElapsedTime(appUsageStats.getTotalTimeInForeground() / 1000);
            } catch (PackageManager.NameNotFoundException e) {
                appInfo = null;
                appIcon = null;
                appName = appUsageStats.getPackageName();
                usageTime = "";
                e.printStackTrace();
            }

            ImageView imageView = convertView.findViewById(R.id.image);
            TextView textViewAppName = convertView.findViewById(R.id.text_app_name);
            TextView textViewUsageTime = convertView.findViewById(R.id.text_usage_time);

            imageView.setImageDrawable(appIcon);
            textViewAppName.setText(appName);
            textViewUsageTime.setText(usageTime);

            return convertView;
        }
    }
}
