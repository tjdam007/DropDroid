# DropDroid

DropDroid is an open-source local Wi-Fi file sharing tool for Android. It sends any file from a desktop browser to an Android phone on the same network.

When the shared file is an Android APK, the phone app can optionally open Android's installer after the transfer. Android still asks the user to confirm installation.

## Project status

DropDroid is in early development. The current build is useful for local testing and demos, but the protocol and UI may still change.

## What is included

- Android receiver app built with Kotlin and Jetpack Compose
- Desktop sender UI that runs locally in the browser
- QR-based secure pairing between the portal and phone
- HMAC-signed transfers with timestamp and replay protection
- Transfer progress inside the Android app
- Configurable Android save folder with a default destination
- Recent received files with tap-to-open behavior
- Automatic Android device discovery on the same Wi-Fi
- Manual IP fallback when discovery is blocked by the network
- APK install helper toggle inside the Android app

## How it works

1. Open DropDroid on the Android phone.
2. Start the desktop sender on your computer.
3. Keep both devices on the same Wi-Fi network.
4. Scan the QR code shown in the desktop sender.
5. Select the phone when it appears, or enter the phone IP manually.
6. Drop any file into the desktop sender.

The Android app receives the file over the local network. No cloud server is used.

## Save location and opening files

DropDroid saves files to a default app Downloads / DropDroid folder. Users can choose another Android folder from the app using the system folder picker.

Received files appear in the recent files list. Tapping a file asks Android to open it with the default compatible app.

## Secure pairing

DropDroid requires QR pairing before accepting transfers.

The desktop sender creates a high-entropy session secret and displays it as a QR code. The Android app scans that QR and stores the secret locally.

Every transfer is signed with HMAC-SHA256 and includes:

- paired portal id
- timestamp
- nonce
- file name
- file size
- SHA-256 file hash

The Android app rejects unsigned uploads, expired signatures, repeated nonces, wrong portal ids, and files whose hash does not match the signed hash.

## Run the desktop sender

```bash
npm start
```

Open:

```text
http://localhost:38531
```

The desktop sender uses only built-in Node.js modules, so no install step is required for the current browser-based sender.

## Build the Android APK

```bash
cd android-app
./gradlew assembleDebug
```

The debug APK is created at:

```text
android-app/app/build/outputs/apk/debug/app-debug.apk
```

## Install for testing

For normal users, install the APK once on the phone and then keep the app open while sending files.

For developer testing from a computer, enable Developer options and USB debugging on the Android phone, connect the phone by USB, then run:

```bash
cd android-app
./gradlew installDebug
```

After the app is installed, you can test file sharing directly over Wi-Fi from the desktop sender.

## APK install behavior

DropDroid cannot silently install apps. Android requires a user confirmation screen for APK installation.

The "APK install helper" toggle means:

- Off: APK files are received like normal files.
- On: APK files are received, then Android's installer opens automatically.

The first time, Android may ask the user to allow DropDroid to install unknown apps.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Testing guide](docs/TESTING.md)
- [Contributing](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

## License

DropDroid is released under the [MIT License](LICENSE).
