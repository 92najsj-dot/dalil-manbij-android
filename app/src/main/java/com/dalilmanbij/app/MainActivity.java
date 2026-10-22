package com.dalilmanbij.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {
    private static final String HOME = "https://dalil-manbij-new.pages.dev/";
    private static final String OAUTH_SCHEME = "dalilmanbij";
    private WebView webView;
    private LinearLayout errorPanel;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER_REQUEST = 2041;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(18, 92, 75));
        getWindow().setNavigationBarColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        // Keep all app content inside the safe area, below the status bar and above navigation.
        // This prevents Android 15+ edge-to-edge layouts from drawing the website under system icons.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            view.setPadding(0, topInset, 0, bottomInset);
            return insets.consumeSystemWindowInsets();
        });
        root.setFitsSystemWindows(true);

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        root.addView(progress, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(3)));

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(24), dp(24), dp(24), dp(24));
        errorPanel.setVisibility(View.GONE);

        TextView title = new TextView(this);
        title.setText("تعذّر الاتصال بدليل منبج");
        title.setTextSize(20);
        title.setTextColor(Color.rgb(30, 50, 45));
        title.setGravity(Gravity.CENTER);
        errorPanel.addView(title);

        TextView detail = new TextView(this);
        detail.setText("تأكد من اتصال الإنترنت ثم حاول مرة أخرى.");
        detail.setTextSize(15);
        detail.setTextColor(Color.DKGRAY);
        detail.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        detailParams.setMargins(0, dp(12), 0, dp(16));
        errorPanel.addView(detail, detailParams);

        Button retry = new Button(this);
        retry.setText("إعادة المحاولة");
        retry.setOnClickListener(v -> {
            errorPanel.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(HOME);
        });
        errorPanel.addView(retry);
        root.addView(errorPanel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        WebSettings settings = webView.getSettings();
        // Let the website recognize the APK and use its dedicated OAuth return URI.
        settings.setUserAgentString(settings.getUserAgentString() + " DalilManbijAndroid");
        settings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new DalilAndroidBridge(), "DalilAndroid");
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    return false;
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

                if ("https".equals(scheme) &&
                        (host.equals("dalil-manbij-new.pages.dev") ||
                         host.endsWith(".dalil-manbij-new.pages.dev"))) {
                    return false;
                }

                openExternal(uri);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                errorPanel.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) showError();
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            android.webkit.WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 500) showError();
            }
        });

        if (isOAuthCallback(getIntent())) {
            webView.loadUrl(buildOAuthReturnUrl(getIntent().getData()));
        } else if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(HOME);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (isOAuthCallback(intent) && webView != null) {
            webView.loadUrl(buildOAuthReturnUrl(intent.getData()));
        }
    }

    private boolean isOAuthCallback(Intent intent) {
        if (intent == null || intent.getData() == null) return false;
        Uri uri = intent.getData();
        return OAUTH_SCHEME.equals(uri.getScheme())
                && "auth".equals(uri.getHost())
                && "/callback".equals(uri.getPath());
    }

    // Pass only OAuth result parameters back into the existing WebView origin so
    // Supabase JS can exchange the PKCE code using the verifier stored in WebView.
    private String buildOAuthReturnUrl(Uri callback) {
        Uri.Builder builder = Uri.parse(HOME).buildUpon();
        if (callback != null) {
            for (String key : new String[]{"code", "error", "error_code", "error_description"}) {
                String value = callback.getQueryParameter(key);
                if (value != null && !value.isEmpty()) builder.appendQueryParameter(key, value);
            }
        }
        return builder.build().toString();
    }

    /** Small bridge for native Android share sheet; the WebView only loads our trusted site. */
    private final class DalilAndroidBridge {
        @JavascriptInterface
        public void shareText(String title, String text) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_SUBJECT, title == null ? "دليل منبج" : title);
                send.putExtra(Intent.EXTRA_TEXT, text == null ? HOME : text);
                startActivity(Intent.createChooser(send, "مشاركة عبر"));
            });
        }

        @JavascriptInterface
        public String getAppVersion() {
            return "1.0.0";
        }
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            new AlertDialog.Builder(this)
                    .setMessage("لا يوجد تطبيق مناسب لفتح هذا الرابط.")
                    .setPositiveButton("حسناً", null)
                    .show();
        }
    }

    private void showError() {
        webView.setVisibility(View.GONE);
        errorPanel.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
