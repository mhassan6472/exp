# Orange Ledger — Android project source

Android Studio Kotlin project for a personal offline expense ledger.

## Features
- Charcoal / near-black UI with dark orange accent
- Room local database for expenses and settings
- Allowance and savings fields; expenditure summary and remaining allowance
- Today, rolling 7-day, current-month, all-records and custom date range filter
- Android home-screen widget: tap to open a compact quick-add screen, enter expense and save
- Local JSON backup to a user-selected SAF folder, import/restore from JSON
- Daily backup alarm scheduled for 23:59, replacing `orange-ledger-backup.json` in selected folder
- No INTERNET permission in manifest (app has no network permission)

## Build APK
Open this folder in Android Studio, sync Gradle, then choose **Build > Build APK(s)**.
The resulting APK is in `app/build/outputs/apk/debug/`.

## Caveats
- Android's exact 23:59 execution is subject to OS idle/battery restrictions; the receiver uses `setAndAllowWhileIdle`, and will schedule the next run after execution. Android can defer it in Doze.
- Selecting a backup folder is required for automatic backups.
- Restoring replaces the currently stored expense list/settings. Keep a separate copy of important backups.
- This source has not been compiled or run on a physical device in this environment; treat it as a development build pending Android Studio compile and device verification.
