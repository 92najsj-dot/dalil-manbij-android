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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {
    private static final String HOME = "https://dalil-manbij-new.pages.dev/";
    private WebView webView;
    private LinearLayout errorPanel;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER_REQUEST = 2041;
    private boolean welcomeShownThisSession = false;

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
        settings.setJavaScriptEnabled(true);
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
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Show welcome once per app process/session: returning from background
                // does not show it again, but a fresh launch after the app is closed does.
                if (!welcomeShownThisSession) {
                    welcomeShownThisSession = true;
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("📍 دليل منبج")
                            .setMessage("💚 أهلاً وسهلاً فيك!\n\nكل منبج وريفها بين إيديك.\n\n🛍️ تسوّق   •   🏷️ بيع   •   📢 أعلن   •   🔎 اكتشف\n\n✨ كل ما تحتاجه بمنبج وريفها، بمكان واحد!\n\n🤲 صلّوا على النبي ﷺ\n\n💻 تم التطوير بواسطة عبد نعسو")
                            .setPositiveButton("✓ يلا نبدأ", (dialog, which) -> dialog.dismiss())
                            .setCancelable(false)
                            .show();
                }
                // Native-app-only enhancement. Wait for Supabase-backed promotions to render
                // before enabling the carousel; onPageFinished fires before async data arrives.
                String carouselScript = "(function(){try{" +
                    "if(location.hostname!=='dalil-manbij-new.pages.dev')return;" +
                    "var styleId='dalil-app-promo-carousel-style';" +
                    "if(!document.getElementById(styleId)){var st=document.createElement('style');st.id=styleId;" +
                    "st.textContent='#featuredPromotions #promotionGrid.app-promo-carousel{display:flex!important;flex-wrap:nowrap!important;gap:12px;overflow-x:auto;overflow-y:hidden;scroll-snap-type:x mandatory;direction:rtl;padding:6px 2px 14px;-webkit-overflow-scrolling:touch;scrollbar-width:none}'+ " +
                    "'#featuredPromotions #promotionGrid.app-promo-carousel::-webkit-scrollbar{display:none}'+ " +
                    "'#featuredPromotions #promotionGrid.app-promo-carousel>.promo-card{flex:0 0 min(82%,310px);min-width:0;scroll-snap-align:start}'+ " +
                    "'#featuredPromotions .promo-card{height:auto}';document.head.appendChild(st);}" +
                    "var grid=document.getElementById('promotionGrid');if(!grid)return;" +
                    "if(grid.dataset.appCarouselInit==='1')return;grid.dataset.appCarouselInit='1';" +
                    "var timer=null;function stop(){if(timer){clearInterval(timer);timer=null;}}" +
                    "function start(){stop();if(grid.children.length<2||document.hidden)return;timer=setInterval(function(){" +
                    "if(grid.children.length<2)return;var max=grid.scrollWidth-grid.clientWidth;if(max<8)return;" +
                    "var rtl=getComputedStyle(grid).direction==='rtl';var current=rtl?Math.abs(grid.scrollLeft):grid.scrollLeft;" +
                    "if(current>=max-8){grid.scrollTo({left:0,behavior:'smooth'});}" +
                    "else{grid.scrollBy({left:(rtl?-1:1)*Math.min(grid.clientWidth*.82,300),behavior:'smooth'});}" +
                    "},4500);}" +
                    "function apply(){if(grid.children.length){grid.classList.add('app-promo-carousel');start();}}" +
                    "var observer=new MutationObserver(function(){apply();});observer.observe(grid,{childList:true,subtree:false});" +
                    "apply();grid.addEventListener('touchstart',stop,{passive:true});grid.addEventListener('touchend',function(){setTimeout(start,2500);},{passive:true});" +
                    "document.addEventListener('visibilitychange',function(){document.hidden?stop():start();});" +
                    "}catch(e){console.log('App featured carousel:',e);}})();";
                view.evaluateJavascript(carouselScript, null);
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

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            Uri incoming = getIntent() != null ? getIntent().getData() : null;
            if (incoming != null && "https".equalsIgnoreCase(incoming.getScheme())
                    && "dalil-manbij-new.pages.dev".equalsIgnoreCase(incoming.getHost())) {
                webView.loadUrl(incoming.toString());
            } else {
                webView.loadUrl(HOME);
            }
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
