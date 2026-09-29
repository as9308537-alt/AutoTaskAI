# AutoTask AI — Teach Once → Run Again

Android automation assistant starter/full v1.

## What works
- Voice command input.
- Accessibility Service based recorder.
- Records semantic click, text-entry and scroll events from other apps.
- Saves workflows locally as JSON-backed SharedPreferences.
- Replays workflows using view IDs, visible text/content description and class fallback.
- Variable replacement: `old text=new text, old2=new2`.
- Workflow list with one-tap run.
- Password fields are deliberately not recorded/replayed.
- GitHub Actions workflow builds a debug APK and uploads it as an artifact.

## How to use
1. Install APK.
2. Android Settings → Accessibility → AutoTask AI → Allow.
3. Open AutoTask AI.
4. Enter a training name, e.g. `Amazon Listing`.
5. START TRAINING.
6. Switch to the target app/site and perform the task normally.
7. Return to AutoTask AI and STOP + SAVE TRAINING.
8. Tap the saved workflow to replay it.

## GitHub APK
Push the project to a GitHub repository. Open **Actions → Build APK → Run workflow**. The workflow produces `app-debug.apk` as an artifact.

## Limitations
Android security means this cannot universally automate every app. Some apps expose no usable accessibility nodes, and CAPTCHA/OTP/payment/password steps require human interaction. This version intentionally excludes password fields.
