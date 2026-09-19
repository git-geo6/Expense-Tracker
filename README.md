# Expense Tracker — Android app (Kotlin + WebView)

This wraps `expense-tracker.html` (your app) in a small native Kotlin
Android app, and includes a GitHub Actions workflow that builds the APK
for you automatically — no Android Studio required.

## Why a custom Kotlin app instead of a no-code "HTML to APK" tool?

Those generic tools use a plain WebView with no control over how
downloads are handled, which is why "Export PDF" was unreliable. This
project adds one small but important piece: a native download bridge
(`AndroidDownloader`, in `MainActivity.kt`) that the web page calls
directly to save the PDF straight into the phone's real Downloads
folder. It's built with Android's own `MediaStore` API, so it doesn't
depend on WebView's flaky blob/data-URI download behavior at all.

Everything else about the app (your HTML/CSS/JS) is unchanged and
lives in `app/src/main/assets/expense-tracker.html`.

## Get the APK using GitHub Codespaces (recommended for you)

1. On GitHub, create a new **empty** repository (no README/.gitignore/license).
2. Open that repo → green **Code** button → **Codespaces** tab → **Create codespace on main**. This opens a full Linux terminal in your browser — no local install needed.
3. In the Codespace terminal, upload this zip: click the **Explorer** icon on the left → drag-and-drop `expense-tracker-android.zip` into the file list (or use the "Upload..." option from the `...` menu at the top of the Explorer panel).
4. Unzip and push it, right in the Codespace terminal:
   ```bash
   unzip expense-tracker-android.zip -d .
   rm expense-tracker-android.zip
   git add .
   git commit -m "Initial commit"
   git push
   ```
   (The repo is already connected since the Codespace was created from it — no need for `git init` or `git remote add`.)
5. Back on GitHub (regular browser tab, not the Codespace), open your repo's **Actions** tab. The "Build APK" workflow starts automatically on push.
6. When it finishes (green checkmark), open that run → **Artifacts** → download **expense-tracker-debug-apk** (a `.zip` containing `app-debug.apk`).
7. Transfer that APK to your phone and install it (you may need to allow "install from unknown sources" for whichever app you use to open it).

## Get the APK the regular way (local git, no Codespaces)

1. Create a new **empty** repository on GitHub (don't add a README, .gitignore, or license when creating it).
2. On your computer, unzip this project and open a terminal in that folder.
3. Push it to your new repo:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
4. On GitHub, open your repo → the **Actions** tab. A workflow run called
   "Build APK" should already be running (it triggers automatically on push).
5. Once it finishes (green checkmark, a few minutes), click into that run,
   scroll to **Artifacts**, and download **expense-tracker-debug-apk** —
   that's a `.zip` containing your `app-debug.apk`.
6. Transfer that APK to your phone (e.g. via a chat app, email, or Google
   Drive) and install it. You may need to allow "install from unknown
   sources" for whichever app you use to open it.

If Actions doesn't run automatically, go to the **Actions** tab →
**Build APK** (left sidebar) → **Run workflow** button.

## Updating the app later

Any time you want to change the app itself, just edit
`app/src/main/assets/expense-tracker.html` and push again — GitHub
Actions will rebuild a fresh APK automatically.

## Building locally instead (optional)

If you do have Android Studio installed, you can open this folder
directly as a project and click Run, or build from the command line
with a JDK 17 and the Android SDK installed:
```bash
gradle assembleDebug
```
The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Notes

- This produces a **debug** APK, which is fine for installing on your
  own phone. If you ever want to publish it to the Play Store, that
  needs a signed **release** build instead, which is a separate step.
- The app's data (trips, people, expenses) is stored locally on the
  device via WebView's local storage, same as before — see the earlier
  conversation for details on backup/restore options.
