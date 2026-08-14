package com.example.kiosklauncher;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

        setContentView(layout);
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
