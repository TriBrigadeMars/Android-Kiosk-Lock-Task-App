package com.example.webkiosk;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Device admin receiver. When the kiosk app is provisioned as the device
 * owner, this lets the app manage the lock task allowlist so it can run
 * Google Chrome and the system WiFi settings while in lock task mode.
 */
public class KioskDeviceAdminReceiver extends DeviceAdminReceiver {

    /** Packages that may run while the device is in lock task mode. */
    public static final String[] LOCK_TASK_PACKAGES = {
        "com.example.webkiosk",   // this kiosk app
        "com.android.chrome",     // Google Chrome
        "com.android.settings"    // system settings (WiFi)
    };

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        configureLockTask(context);
    }

    /**
     * Adds the kiosk app, Chrome, and Settings to the device owner's lock
     * task allowlist. Only the device owner may call setLockTaskPackages(),
     * so this is a no-op when the app is not the device owner.
     */
    public static void configureLockTask(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager)
                context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null || !dpm.isDeviceOwnerApp(context.getPackageName())) {
            return;
        }

        // Only allowlist packages that are actually installed, otherwise
        // setLockTaskPackages() may throw for missing packages.
        PackageManager pm = context.getPackageManager();
        List<String> installed = new ArrayList<>();
        for (String pkg : LOCK_TASK_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                installed.add(pkg);
            } catch (PackageManager.NameNotFoundException e) {
                // Not installed — skip it.
            }
        }

        ComponentName admin = new ComponentName(context, KioskDeviceAdminReceiver.class);
        try {
            dpm.setLockTaskPackages(admin, installed.toArray(new String[0]));
        } catch (Exception e) {
            // Ignore — lock task will simply not include unavailable packages.
        }
    }
}
