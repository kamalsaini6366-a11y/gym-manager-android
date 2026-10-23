# Gym Manager Android App (offline)

This is a starter Android application based on the uploaded Gym Management manual. It stores data in a local SQLite database on the phone and does not require an account or internet connection for app use.

## Current features
- Dashboard counts
- Add, search and edit members
- Active/inactive membership toggle
- Daily attendance marking (one attendance entry per member per day)
- Fee payment history
- Measurement history (height, weight, biceps, triceps, shoulders)
- GitHub Actions workflow to build a debug APK

## Build APK using GitHub (no Android Studio required)
1. Sign in to GitHub and create a new repository, e.g. `gym-manager-android`.
2. Upload all files/folders from this project to the repository root. Keep `.github/workflows/build-apk.yml`.
3. Open the repository's **Actions** tab.
4. Select **Build Gym Manager APK** and click **Run workflow** (or push to main/master).
5. When the run finishes, open the successful workflow run and download the **Gym-Manager-debug-apk** artifact.
6. Extract the downloaded ZIP on your PC or phone, then install `app-debug.apk` on your Android phone. You may need to allow installing apps from that source in Android settings.

## Important limitations
- This is an initial offline version, not a complete replacement for every feature in the manual.
- Biometric/card reader integration, machine synchronization, email/SMS automation, role-based logins, advanced reports and Excel export are not implemented yet.
- Data is stored only on the phone. Back up your phone before uninstalling or clearing app data.
- Debug APK is for testing; a production release should be signed and tested before wider use.
