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
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.webkiosk.R;

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
        // Prevent screen capture & recording for FERPA privacy compliance
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_launcher);

        clockView = findViewById(R.id.clock_view);
        startClock();

        findViewById(R.id.btn_launch_kiosk).setOnClickListener(v -> launchKioskApp());
        findViewById(R.id.btn_chrome).setOnClickListener(v -> openChrome());
        findViewById(R.id.btn_sys_wifi).setOnClickListener(v -> openSystemWifiSettings());
        findViewById(R.id.btn_add_wifi).setOnClickListener(v -> openWifiSettings());
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Consume BACK key to prevent exiting launcher
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        // Do nothing — Launcher cannot be backed out of
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
