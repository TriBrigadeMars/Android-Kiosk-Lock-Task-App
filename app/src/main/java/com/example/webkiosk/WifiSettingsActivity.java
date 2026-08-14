package com.example.webkiosk;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets a user add WiFi credentials from inside the kiosk app, e.g. when the
 * tablet has been disconnected from the network. Uses the WifiNetworkSuggestion
 * API (Android 10+) so the system handles the actual connection and prompts
 * the user to approve the network.
 */
public class WifiSettingsActivity extends Activity {

    private static final int REQ_PERMISSIONS = 1001;

    private WifiManager wifiManager;
    private TextView statusView;
    private EditText ssidInput;
    private EditText passwordInput;
    private TextView messageView;

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        layout.setBackgroundColor(0xFF1a3c6d);

        TextView title = new TextView(this);
        title.setText("WiFi Settings");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 24);
        layout.addView(title);

        statusView = new TextView(this);
        statusView.setTextColor(0xFFCCE0FF);
        statusView.setTextSize(16);
        statusView.setPadding(0, 0, 0, 24);
        layout.addView(statusView);

        TextView ssidLabel = new TextView(this);
        ssidLabel.setText("Network name (SSID)");
        ssidLabel.setTextColor(0xFFFFFFFF);
        ssidLabel.setTextSize(16);
        layout.addView(ssidLabel);

        ssidInput = new EditText(this);
        ssidInput.setSingleLine(true);
        ssidInput.setHint("e.g. CU Denver Guest");
        ssidInput.setTextColor(0xFF000000);
        ssidInput.setHintTextColor(0xFF888888);
        ssidInput.setBackgroundColor(0xFFFFFFFF);
        ssidInput.setPadding(16, 16, 16, 16);
        layout.addView(ssidInput);

        TextView passLabel = new TextView(this);
        passLabel.setText("Password (leave blank for an open network)");
        passLabel.setTextColor(0xFFFFFFFF);
        passLabel.setTextSize(16);
        passLabel.setPadding(0, 24, 0, 0);
        layout.addView(passLabel);

        passwordInput = new EditText(this);
        passwordInput.setSingleLine(true);
        passwordInput.setHint("WiFi password");
        passwordInput.setTextColor(0xFF000000);
        passwordInput.setHintTextColor(0xFF888888);
        passwordInput.setBackgroundColor(0xFFFFFFFF);
        passwordInput.setPadding(16, 16, 16, 16);
        layout.addView(passwordInput);

        Button connect = new Button(this);
        connect.setText("Connect to this network");
        connect.setTextSize(16);
        connect.setPadding(24, 24, 24, 24);
        connect.setOnClickListener(v -> addNetwork());
        layout.addView(connect);

        messageView = new TextView(this);
        messageView.setTextColor(0xFFFFD27F);
        messageView.setTextSize(15);
        messageView.setPadding(0, 24, 0, 0);
        layout.addView(messageView);

        Button back = new Button(this);
        back.setText("Back");
        back.setTextSize(16);
        back.setPadding(24, 24, 24, 24);
        back.setOnClickListener(v -> finish());
        layout.addView(back);

        scroll.addView(layout);
        setContentView(scroll);

        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, filter);
        updateStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(wifiReceiver);
        } catch (Exception ignored) {
        }
    }

    private void updateStatus() {
        if (wifiManager == null) return;
        if (!wifiManager.isWifiEnabled()) {
            statusView.setText("WiFi is OFF.\nTurn on WiFi in the tablet's Settings app, then return here.");
            return;
        }
        String ssid = getCurrentSsid();
        if (ssid != null && !ssid.isEmpty()) {
            statusView.setText("Connected to: " + ssid);
        } else {
            statusView.setText("Not connected to any WiFi network.");
        }
    }

    private String getCurrentSsid() {
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null) return null;
        String ssid = info.getSSID();
        if (ssid == null) return null;
        if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() > 1) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    private void addNetwork() {
        String ssid = ssidInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (ssid.isEmpty()) {
            messageView.setText("Please enter the network name (SSID).");
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            messageView.setText("This tablet's Android version is too old to add WiFi from here.");
            return;
        }

        if (!ensurePermissions()) {
            return;
        }

        WifiNetworkSuggestion.Builder builder =
                new WifiNetworkSuggestion.Builder().setSsid(ssid);
        if (!password.isEmpty()) {
            builder.setWpa2Passphrase(password);
        }

        List<WifiNetworkSuggestion> suggestions = new ArrayList<>();
        suggestions.add(builder.build());

        int result = wifiManager.addNetworkSuggestions(suggestions);
        if (result == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            messageView.setText("Network added. Follow the on-screen prompt to connect.");
            Toast.makeText(this, "WiFi network added", Toast.LENGTH_SHORT).show();
        } else if (result == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE) {
            messageView.setText("This network is already added. Check the on-screen prompt to connect.");
        } else {
            messageView.setText("Could not add the network. Check the name and password, then try again.");
        }
    }

    private boolean ensurePermissions() {
        List<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        if (!needed.isEmpty()) {
            requestPermissions(needed.toArray(new String[0]), REQ_PERMISSIONS);
            messageView.setText("Please approve the permission prompt, then tap Connect again.");
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean granted = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                }
            }
            if (granted) {
                messageView.setText("Permission granted. Tap Connect again.");
            } else {
                messageView.setText("Permission denied. WiFi connection requires this permission.");
            }
        }
    }
}
