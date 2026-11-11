# DropDroid — Play Console setup

App: `dev.mjandroid.dropdroid` · default language en-GB · developer contact mjandroiddev@gmail.com

## 1. Upload key reset (blocker for every new build)

Version code 1 on internal testing was signed with an upload key that no longer exists on this
Mac (Play expects SHA1 `CA:08:26:25:11:DD:6E:E4:56:59:6A:94:EE:2B:C7:78:E5:1C:53:39`).
If you have that keystore somewhere else, use it instead and skip this section.

New upload key: `keystores/dropdroid-release.keystore` (alias `dropdroid`, passwords in
`keystores/dropdroid-release-credentials.txt`, backed up to iCloud `Backups/signing-keys/keystores/`).
SHA1 `34:4F:CE:BC:F6:CF:1D:BB:FC:EB:1A:C7:EA:5A:F8:1F:77:22:C9:B5`.

1. Play Console → DropDroid → Test and release → App integrity → App signing.
2. "Request upload key reset" → reason: lost upload key.
3. Upload `docs/upload_certificate.pem`.
4. Google emails when the new key is active (usually ~48 h). Then:
   `bin/play-upload.py --package dev.mjandroid.dropdroid --aab DropDroid/android-app/app/build/outputs/bundle/release/app-release.aab --release-name 0.1.4 --whats-new-file ...`

## 2. Store listing

Scripted: `bin/play-listing.py push --package dev.mjandroid.dropdroid --dir DropDroid/listing`
(en-GB text, icon, feature graphic, 4 phone screenshots; sources in `docs/store/source/`).
Dry run passed 2026-11-11.

- App category: **Tools**. Tags: file transfer, productivity.
- Privacy policy URL: `https://mjandroid.dev/dropdroid/privacy` (page in `mjandroiddev/src/pages/dropdroid/`).
- Website: https://github.com/tjdam007/DropDroid

## 3. App content (Policy → App content)

| Form | Answer |
|---|---|
| Privacy policy | https://mjandroid.dev/dropdroid/privacy |
| Ads | No ads |
| App access | All functionality needs the desktop portal. Give reviewers instructions: install the desktop portal from https://github.com/tjdam007/DropDroid/releases (or `npm start` from the repo), open http://localhost:38531, scan the QR in the app, drop a file. No login. |
| Content rating | IARC questionnaire: Utility/Productivity; no violence, no user-to-user content sharing with strangers (transfers only between the user's own paired devices), no location sharing → Everyone / PEGI 3 |
| Target audience | 18+ (or 13+); not designed for children |
| News app | No |
| Data safety | Data collected: none by the developer. ML Kit (bundled barcode) may send diagnostics: declare *App info and performance → Diagnostics*, collected, not shared, processed ephemerally? → check Google's ML Kit data-disclosure page when filling. Data encrypted in transit: transfers are HMAC-signed over local HTTP (not TLS) → answer **No** for "encrypted in transit". Users can request deletion: data is on-device only; clearing storage deletes it. |
| Government app / Financial features / Health | No |
| **Foreground service** | Type `dataSync`: "Receives files the user sends from their own computer over the local network; the notification shows receiver status and transfer progress. User starts it by opening the app." Needs a short video (screen record: open app → send file from portal → notification progress). |
| **Request install packages** | Permitted use: *File sharing / transfer*. Explain: optional APK install helper (off by default) opens Android's installer for APKs the user sends from their own computer; Android asks for confirmation. If Play rejects it, remove the permission + helper from the Play build. |

## 4. Releases

- The mjandroid.dev account is older than Nov 2023, so there is **no 12-tester / 14-day closed-test
  rule**: internal → production directly once App content is approved.
- Ask before any production release.

## 5. Known risks

- REQUEST_INSTALL_PACKAGES review can bounce the app; fallback is a Play flavour without it.
- Reviewers can't test without the desktop portal — keep App access instructions precise.
