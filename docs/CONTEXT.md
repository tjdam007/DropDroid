# DropDroid — CONTEXT

Open-source (github.com/tjdam007/DropDroid) local-only file sharing: a desktop sender (Electron /
browser portal, `desktop/`) pushes files to an Android receiver app (`android-app/`, Kotlin +
Compose) over the LAN. QR pairing + HMAC-signed transfers. No cloud. See `README.md`,
`docs/ARCHITECTURE.md`, `docs/TESTING.md`.

## Android app facts

- applicationId `dev.mjandroid.dropdroid`, namespace `io.github.tjdam007.dropdroid`
- minSdk 24, compileSdk 36. Single module `app/`, versions in `gradle/libs.versions.toml`.
- Sensitive permissions for Play: `REQUEST_INSTALL_PACKAGES` (APK-install helper; Play permitted
  use = "file sharing/transfer" → needs a declaration), `CAMERA` (QR pairing),
  `FOREGROUND_SERVICE_DATA_SYNC` (receiver service → needs FGS declaration + video).

## Play Console state (checked 2026-11-11 via API)

- App exists, default language en-GB, contact mjAndroidDev@gmail.com / https://mjandroid.dev/.
- Internal track: version code 1 ("Initital Release - 1 (v1.0)"), completed.
- Listing: en-GB text + icon + feature graphic + 4 screenshots pushed 2026-11-11 (sent for Google review).
- **Upload key for code 1 is lost** (expected SHA1 `CA:08:26:…:53:39`; searched repo, keystores/,
  iCloud backups, Android Studio settings, every .jks/.keystore on the Mac). New upload key made
  2026-11-11 → upload key reset needed in Console (steps in `docs/store/PLAY_CONSOLE_SETUP.md`).

## Release / signing

- Upload key: `keystores/dropdroid-release.keystore` (alias `dropdroid`), creds in
  `keystores/dropdroid-release-credentials.txt`, both + PEM backed up to iCloud
  `Backups/signing-keys/keystores/`. `android-app/keystore.properties` (gitignored) feeds Gradle.
- Build: `cd android-app && ./gradlew bundleRelease`. Store listing: `listing/` (en-GB) via
  `bin/play-listing.py`; graphics sources `docs/store/source/frames.html` (+ raw emulator shots).
- Privacy policy: https://mjandroid.dev/dropdroid/privacy (`mjandroiddev/src/pages/dropdroid/`).

## Recent work

- 2026-11-11: Play prep. targetSdk 35→36, v0.1.4 (code 5), release signing config. Tested on a
  fresh API 37 16 KB-page emulator (DD_Store_Phone): debug + minified release launch fine, real
  portal→phone transfers verified (hash/HMAC), unpaired sender rejected, native libs 16 KB aligned.
  Store listing pushed to Play (in review); privacy page live at mjandroid.dev/dropdroid/privacy. AAB built
  but can't upload until the upload key reset is approved.

## Known issues

- Uncommitted WIP in the tree (desktop renderer/main/preload, MainScreen first-launch folder
  prompt) — it IS in the 0.1.4 build; on first launch it opens the system folder picker.
- Emulator QR pairing: seed `shared_prefs/apk_drop.xml` via `run-as` on a debug build (camera
  can't scan); `adb forward tcp:47881 tcp:47881` then send to ip=127.0.0.1.

## Console status (2026-11-11, via Chrome)

- Console app id 4975107270445216911 (dev account 7969366987968864481). Still a **draft app**;
  sending for review needs a release on a track, which waits on the upload key reset.
- 10 App content forms were done in May 2026. Privacy policy in Console is
  https://dropdroid.mjandroid.dev/legal/privacy (live); mjandroid.dev/dropdroid/privacy also exists.
- 2026-11-11: **Request install packages** (file sharing + App functionality) and **FGS dataSync**
  (Network processing → Other) declarations saved, both with video
  https://drive.google.com/file/d/1ZC9IluT0xjF8HQ-70dNcysnwVjfNYXsW/view (Drive, tjdam007).
  Drive file is public (anyone with the link) — verified 2026-11-11.
- Demo video: `docs/store/dropdroid_play_demo.mp4` (88 s, captioned; built from emulator screencaps
  of a scripted run — screencap loop is reliable, `adb emu screenrecord`/`screenrecord` were not).
- Upload key reset requested 2026-11-11 (owner uploaded the PEM). **New upload key valid from
  13 Nov 2026 11:15** — upload 0.1.4 AAB after that.

## Next steps

1. ~~Request upload key reset~~ done; wait until 13 Nov 2026 11:15.
2. Owner: App content forms + privacy URL in Console (§3), incl. FGS video + install-packages declaration.
3. After reset approval: upload 0.1.4 AAB to internal, then production (older account: no 12-tester closed-test rule). Ask before production.
