# Contributing to DropDroid

Thanks for helping improve DropDroid.

## Development setup

Requirements:

- Node.js 20 or newer
- JDK 17 or newer
- Android SDK with a recent platform installed

Run the desktop sender:

```bash
npm start
```

Build the Android app:

```bash
cd android-app
./gradlew assembleDebug
```

Install on a connected development device:

```bash
cd android-app
./gradlew installDebug
```

For phone testing through USB, enable Developer options and USB debugging on the device.

## Pull request checklist

- Keep changes focused.
- Update documentation when behavior changes.
- Run the Android build before opening a pull request.
- Explain user-facing changes clearly in the pull request body.

## Code style

- Follow the existing Kotlin and JavaScript style.
- Prefer clear product language: DropDroid shares any file, and APK install handling is optional.
- Avoid adding cloud services or account requirements to the core local-sharing flow.
