package com.example.webkiosk;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private View errorLayout;
    private Button btnRetry;
    private boolean isDarkMode = false;
    private boolean mInLockTask = false;

    private static final String START_URL = "https://www.ucdenver.edu/offices/equity";
    private static final String[] ALLOWED_DOMAINS = {"ucdenver.edu", "cm.maxient.com"};
    private static final String PREFS_NAME = "kiosk_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Prevent screen capture & recording for FERPA privacy compliance
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        // Restore dark mode preference
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);

        webView = findViewById(R.id.web_view);
        errorLayout = findViewById(R.id.error_layout);
        btnRetry = findViewById(R.id.btn_retry);

        btnRetry.setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.reload();
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false); // Disable DOM storage caching
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        // FERPA Hardening: disable caching, enforce HTTPS mixed content restrictions
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSaveFormData(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setDatabaseEnabled(false);

        applyDarkMode();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null) {
                    for (String domain : ALLOWED_DOMAINS) {
                        if (host.endsWith(domain)) {
                            return false;
                        }
                    }
                }
                // Block external links
                Toast.makeText(MainActivity.this,
                        "Navigation blocked: " + host, Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // When a new navigation starts, show webView and hide error layout
                errorLayout.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                         WebResourceError error) {
                // Gracefully handle page load errors with native layout
                if (request.isForMainFrame()) {
                    webView.setVisibility(View.GONE);
                    errorLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.loadUrl(START_URL);
    }

    // ========== Fullscreen ==========

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterFullscreen();
        }
    }

    private void enterFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            webView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    // ========== Dark Mode ==========

    @SuppressWarnings("deprecation")
    private void applyDarkMode() {
        WebSettings settings = webView.getSettings();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            settings.setAlgorithmicDarkeningAllowed(isDarkMode);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            settings.setForceDark(isDarkMode
                    ? WebSettings.FORCE_DARK_ON
                    : WebSettings.FORCE_DARK_OFF);
        }
    }

    // ========== Navigation ==========

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                exitToHome();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            exitToHome();
        }
    }

    private void exitToHome() {
        Toast.makeText(this, "Returning to home screen...", Toast.LENGTH_SHORT).show();
        clearSessionData();

        android.content.Intent homeIntent = new android.content.Intent(
                android.content.Intent.ACTION_MAIN);
        homeIntent.addCategory(android.content.Intent.CATEGORY_HOME);
        homeIntent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                android.app.ActivityOptions options = android.app.ActivityOptions.makeBasic();
                options.setLockTaskEnabled(true);
                startActivity(homeIntent, options.toBundle());
            } catch (Exception e) {
                // Fallback: just finish if lock task options fail
                startActivity(homeIntent);
            }
        } else {
            startActivity(homeIntent);
        }
    }

    // ========== FERPA Session & Inactivity Handling ==========

    private static final long INACTIVITY_TIMEOUT_MS = 180000; // 3 minutes
    private Handler inactivityHandler = new Handler(Looper.getMainLooper());
    private Runnable inactivityRunnable = new Runnable() {
        @Override
        public void run() {
            onInactivityTimeout();
        }
    };

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetInactivityTimer();
    }

    private void resetInactivityTimer() {
        if (inactivityHandler != null && inactivityRunnable != null) {
            inactivityHandler.removeCallbacks(inactivityRunnable);
            inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT_MS);
        }
    }

    private void stopInactivityTimer() {
        if (inactivityHandler != null && inactivityRunnable != null) {
            inactivityHandler.removeCallbacks(inactivityRunnable);
        }
    }

    private void onInactivityTimeout() {
        Toast.makeText(this, "Session reset due to inactivity", Toast.LENGTH_SHORT).show();
        clearSessionData();
        if (webView != null) {
            webView.loadUrl(START_URL);
        }
    }

    private void clearSessionData() {
        if (webView != null) {
            webView.clearCache(true);
            webView.clearHistory();
            webView.clearFormData();
        }
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
            WebStorage.getInstance().deleteAllData();
        } catch (Exception ignored) {}
    }

    // ========== Lifecycle ==========

    @Override
    protected void onResume() {
        super.onResume();
        resetInactivityTimer();
        if (!mInLockTask) {
            DevicePolicyManager dpm = (DevicePolicyManager)
                    getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm.isLockTaskPermitted(getPackageName())) {
                try {
                    startLockTask();
                    mInLockTask = true;
                } catch (Exception e) {
                    // Already in lock task or permission issue — safe to ignore
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopInactivityTimer();
        // Pause WebView to reduce memory pressure
        try {
            webView.onPause();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        stopInactivityTimer();
        clearSessionData();
        // Clean up WebView to prevent memory leaks
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
