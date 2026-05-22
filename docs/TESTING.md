# Testing DropDroid

## Normal user flow

1. Install DropDroid on the Android phone.
2. Open DropDroid and keep it in the foreground.
3. Start the desktop sender:

```bash
npm start
```

4. Open `http://localhost:38531`.
5. Tap "Scan QR" in the Android app.
6. Scan the QR shown in the desktop sender.
7. Select the Android device or enter the phone IP manually.
8. Drop a file into the sender.
9. Watch the progress indicator in the Android app.
10. Tap the received file in the recent files list and confirm Android opens it with a compatible app.

## Save folder checks

Default folder:

- Send a file without choosing a folder.
- Confirm the file appears in the Android recent files list.
- Tap the file and confirm it opens.

Custom folder:

- Tap "Choose folder" in the Android app.
- Pick a folder through Android's system folder picker.
- Send a file.
- Confirm the file is saved to the selected folder.

Reset:

- Tap "Use default".
- Send another file.
- Confirm DropDroid returns to the default app Downloads / DropDroid destination.

## Developer device setup

For development installs from the computer:

1. Open Android Settings.
2. Enable Developer options.
3. Enable USB debugging.
4. Connect the phone by USB.
5. Run:

```bash
cd android-app
./gradlew installDebug
```

After installation, transfers happen over Wi-Fi.

## Build checks

Run:

```bash
cd android-app
./gradlew assembleDebug
```

For desktop JavaScript syntax checks:

```bash
node --check desktop/server.js
node --check desktop/main.js
node --check desktop/preload.js
node --check desktop/renderer/renderer.js
```

## APK helper checks

With the Android toggle off:

- Send an APK.
- Confirm it is received as a normal file.

With the Android toggle on:

- Send an APK.
- Confirm Android opens the installer.
- Confirm Android still asks for install approval.

On Android 8 and newer, the first APK install may require allowing DropDroid to install unknown apps.

## Security checks

Before scanning the QR:

- Try sending a file.
- Confirm the Android app rejects the transfer with a pairing/authentication message.

After scanning the QR:

- Send a normal file and confirm it is received.
- Restart the desktop sender to create a new QR session.
- Confirm the phone requires scanning the new QR before accepting new transfers.
