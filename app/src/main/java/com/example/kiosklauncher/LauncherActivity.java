package com.example.kiosklauncher;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.webkiosk.KioskDeviceAdminReceiver;

import java.util.Date;

public class LauncherActivity extends Activity {

    private static final String KIOSK_PACKAGE = "com.example.webkiosk";
    private static final String KIOSK_ACTIVITY = "com.example.webkiosk.MainActivity";

    private TextView clockView;
    private Handler clockHandler;
    private Runnable clockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout: clock at top, kiosk icon centered below
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(0xFF1a3c6d);

        // Clock widget
        clockView = new TextView(this);
        clockView.setTextColor(0xFFFFFFFF);
        clockView.setTextSize(36);
        clockView.setGravity(Gravity.CENTER);
        clockView.setPadding(0, 0, 0, 40);
        clockView.setContentDescription("Current time");
        layout.addView(clockView);
        startClock();

        // Kiosk app icon
        TextView icon = new TextView(this);
        icon.setText("CU Denver Equity");
        icon.setTextColor(0xFFFFFFFF);
        icon.setTextSize(18);
        icon.setGravity(Gravity.CENTER);
        icon.setPadding(40, 40, 40, 40);
        icon.setBackgroundColor(0xFF2d5a8f);
        icon.setContentDescription("Open CU Denver Equity website");
        icon.setOnClickListener(v -> launchKioskApp());
        layout.addView(icon);

        TextView label = new TextView(this);
        label.setText("Tap to open");
        label.setTextColor(0xFFCCCCCC);
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, 16, 0, 0);
        layout.addView(label);

        // Chrome button — opens Google Chrome in lock task mode
        TextView chromeButton = new TextView(this);
        chromeButton.setText("Chrome");
        chromeButton.setTextColor(0xFFFFFFFF);
        chromeButton.setTextSize(16);
        chromeButton.setGravity(Gravity.CENTER);
        chromeButton.setPadding(40, 28, 40, 28);
        chromeButton.setBackgroundColor(0xFF2d5a8f);
        chromeButton.setContentDescription("Open Google Chrome");
        chromeButton.setOnClickListener(v -> openChrome());
        LinearLayout.LayoutParams chromeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        chromeParams.setMargins(0, 48, 0, 0);
        chromeButton.setLayoutParams(chromeParams);
        layout.addView(chromeButton);

        // System WiFi settings button — opens the tablet's WiFi settings screen
        TextView sysWifiButton = new TextView(this);
        sysWifiButton.setText("WiFi Settings");
        sysWifiButton.setTextColor(0xFFFFFFFF);
        sysWifiButton.setTextSize(16);
        sysWifiButton.setGravity(Gravity.CENTER);
        sysWifiButton.setPadding(40, 28, 40, 28);
        sysWifiButton.setBackgroundColor(0xFF2d5a8f);
        sysWifiButton.setContentDescription("Open system WiFi settings");
        sysWifiButton.setOnClickListener(v -> openSystemWifiSettings());
        LinearLayout.LayoutParams sysWifiParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        sysWifiParams.setMargins(0, 16, 0, 0);
        sysWifiButton.setLayoutParams(sysWifiParams);
        layout.addView(sysWifiButton);

        // Add WiFi Network button — lets users re-add credentials if the
        // tablet gets disconnected from the network
        TextView wifiButton = new TextView(this);
        wifiButton.setText("Add WiFi Network");
        wifiButton.setTextColor(0xFFFFFFFF);
        wifiButton.setTextSize(16);
        wifiButton.setGravity(Gravity.CENTER);
        wifiButton.setPadding(40, 28, 40, 28);
        wifiButton.setBackgroundColor(0xFF2d5a8f);
        wifiButton.setContentDescription("Add a WiFi network");
        wifiButton.setOnClickListener(v -> openWifiSettings());
        LinearLayout.LayoutParams wifiParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        wifiParams.setMargins(0, 16, 0, 0);
        wifiButton.setLayoutParams(wifiParams);
        layout.addView(wifiButton);

        setContentView(layout);

        // If this app is the device owner, make sure Chrome and WiFi settings
        // are in the lock task allowlist so they can run in kiosk mode.
        KioskDeviceAdminReceiver.configureLockTask(this);
    }

    private void openChrome() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
            intent.setPackage("com.android.chrome");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Chrome is not installed.", Toast.LENGTH_LONG).show();
        }
    }

    private void openSystemWifiSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open WiFi settings.", Toast.LENGTH_LONG).show();
        }
    }

    private void startClock() {
        clockHandler = new Handler(Looper.getMainLooper());
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                clockView.setText(DateFormat.format("h:mm a", new Date()));
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private void openWifiSettings() {
        try {
            Intent intent = new Intent();
            intent.setClassName(KIOSK_PACKAGE, "com.example.webkiosk.WifiSettingsActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open WiFi settings.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void launchKioskApp() {
        try {
            Intent intent = new Intent();
            intent.setClassName(KIOSK_PACKAGE, KIOSK_ACTIVITY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Kiosk app not found. Please install it first.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If in lock task mode, stay locked
        DevicePolicyManager dpm = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.isLockTaskPermitted(getPackageName())) {
            // Already in lock task mode — do nothing
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }
}
