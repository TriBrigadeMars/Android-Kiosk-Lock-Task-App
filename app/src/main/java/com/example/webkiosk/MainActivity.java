package com.example.webkiosk;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebResourceResponse;
import android.net.Uri;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private boolean isDarkMode = false;
    private boolean mInLockTask = false;

    private static final String START_URL = "https://www.ucdenver.edu/offices/equity";
    private static final String[] ALLOWED_DOMAINS = {"ucdenver.edu", "cm.maxient.com"};
    private static final String PREFS_NAME = "kiosk_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore dark mode preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);

        webView = new WebView(this);
        webView.setContentDescription("CU Denver Equity Office website");
        webView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        // Stability: limit cache and enable safe browsing
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        // Disable features that can cause crashes
        settings.setSaveFormData(false);
        settings.setAllowFileAccess(false);

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
            public void onReceivedError(WebView view, WebResourceRequest request,
                                         WebResourceError error) {
                // Gracefully handle page load errors instead of crashing
                if (request.isForMainFrame()) {
                    view.loadData(
                            "<html><body style='text-align:center;padding:40px;font-family:sans-serif;color:#333'>"
                            + "<h2>Page Unavailable</h2>"
                            + "<p>The website could not be loaded.</p>"
                            + "<p>Please check your internet connection.</p>"
                            + "<br><button onclick='location.reload()' "
                            + "style='padding:10px 20px;font-size:16px'>Retry</button>"
                            + "</body></html>",
                            "text/html", "UTF-8");
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Apply dark mode after page loads (for CSS injection method)
                if (isDarkMode && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    injectDarkModeCSS();
                }
            }
        });

        webView.loadUrl(START_URL);
        setContentView(webView);
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

    private void applyDarkMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            WebSettings settings = webView.getSettings();
            settings.setForceDark(isDarkMode
                    ? WebSettings.FORCE_DARK_ON
                    : WebSettings.FORCE_DARK_OFF);
        }
    }

    private void injectDarkModeCSS() {
        if (!isDarkMode) return;
        String js = "javascript:(function(){" +
            "if(document.getElementById('kiosk-dark'))return;" +
            "var css='html{filter:invert(1) hue-rotate(180deg)}" +
            "img,video,canvas,iframe{filter:invert(1) hue-rotate(180deg)}';" +
            "var s=document.createElement('style');s.id='kiosk-dark';" +
            "s.innerHTML=css;document.head.appendChild(s);" +
            "})()";
        webView.evaluateJavascript(js, null);
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

    // ========== Lifecycle ==========

    @Override
    protected void onResume() {
        super.onResume();
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
        // Pause WebView to reduce memory pressure
        try {
            webView.onPause();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
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
