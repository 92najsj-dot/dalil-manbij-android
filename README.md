# Dalil Manbij Android — test build

This is a separate Android WebView wrapper for the existing Dalil Manbij website. It does not modify the production website, Cloudflare configuration, or Supabase schema.

## Build from an Android phone with GitHub Actions
1. Open the **Actions** tab in this repository and enable Actions if GitHub asks.
2. Open **Build Android APK** and tap **Run workflow** (or push a commit to `main` to trigger it).
3. Open the completed run and download the `dalil-manbij-debug-apk` artifact.
4. Extract the artifact ZIP on your phone and install the APK for testing.

## Included
- Website wrapper for https://dalil-manbij-new.pages.dev/
- JavaScript and DOM storage enabled for the web app
- Android back navigation and image/file chooser support
- External links opened in suitable apps
- Basic connection error screen and retry button

## Important
- This is a first test build, not a verified release.
- It requires internet and depends on the website being available.
- Test sign-in/out, ad publishing/editing, image uploads, shop pages and WhatsApp links before distributing.
- Never put Supabase service-role keys or other secrets in this repository.
- This repository is separate from the production website; do not connect it to Cloudflare Pages.


## OAuth callback preview (before testing Google/Facebook)
This preview uses the Android deep-link URI `dalilmanbij://auth/callback` so the browser can return the OAuth result to the APK instead of opening the installed browser PWA.

Before testing, add exactly this URI to **Supabase Dashboard → Authentication → URL Configuration → Redirect URLs**:
`dalilmanbij://auth/callback`

Do not replace the existing website URLs. Keep them and add this URI as an additional allowed redirect. Then build/install the APK from branch `preview/oauth-return-to-apk-2026-10-22` and test Google and Facebook separately. This change is not production-ready until the redirect URI is allowlisted and both provider flows are verified on-device.


## Phase 3: sharing and updates preview
- The Android WebView exposes a native share sheet through `DalilAndroid.shareText(title, text)`.
- Shared listing/shop links use `?listingId=<id>` and `?businessId=<id>`; the website scrolls to and highlights the matching card when the item is loaded.
- The website has a “Share app” action and an “About app and updates” panel showing version 1.0.0.
- The APK's unverified HTTPS intent filter is intentionally not enabled yet. Automatic opening of web links in the APK requires Android App Links verification with a stable signing certificate and a matching `/.well-known/assetlinks.json` on the production domain. This is deferred to avoid interfering with the currently installed browser PWA or production site.
- Build and test this branch before distributing. The website changes are in a preview branch and have not been deployed to production.
