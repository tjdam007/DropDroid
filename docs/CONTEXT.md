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
- 2026-11-11 11:58 IST: scheduled upload run fired two days early. AAB (0.1.4 / code 5) verified
  signed with the new key (SHA1 `34:4F:…:C9:B5`); `bin/play-upload.py … --draft --dry-run` was
  rejected: `upload bundle failed (403): The upload certificate this APK is signed with is not yet
  valid because it has been recently reset.` Nothing uploaded, no Console changes, no review sent.
  Re-run the same steps after 13 Nov 2026 11:15 IST.
- **2026-11-13 ~11:35 IST: 0.1.4 (code 5) uploaded and live on internal testing.** Rebuilt AAB
  (signed SHA1 `34:4F:…:C9:B5`), `play-upload.py --draft` with mapping.txt OK → in Console
  "Save and publish" → internal track shows 0.1.4 "Available to internal testers". Only release
  warning: native code has no debug symbols (recommendation, not blocking; deobfuscation file is
  attached). Publishing overview: 10 changes (production countries 177 + rest of world, en-GB
  listing, content rating, target audience 16+, privacy policy, ads, data safety, health apps,
  app category Tools) **sent for review** — status "Changes in review", Google's quick checks were
  still running (auto-sent when they pass). No production release created.

- **2026-11-13 ~12:15 IST: 0.1.4 (code 5) promoted to production** (owner approved) via
  `bin/play-upload.py --version-code 5 --track production` (full rollout, en-GB what's new).
  Publishing overview: "Production 0.1.4 — Start full rollout" in Changes in review, quick checks
  running, auto-sent to Google when they pass (managed publishing off). Dashboard shows Production
  "Inactive" until Google approves.

## Next steps

1. Wait for Google review of the setup changes + production 0.1.4 (typically ≤7 days); watch for
   policy rejections, especially REQUEST_INSTALL_PACKAGES / FGS dataSync declarations.
2. ~~Promote 0.1.4 to production~~ done 2026-11-13. Next build: version code 6, pass `--mapping`.
3. Optional: upload native debug symbols to silence the release warning
   (`ndk { debugSymbolLevel = "SYMBOL_TABLE" }`).
4. "Declare your trader status" banner is showing on the developer account — owner should handle.
