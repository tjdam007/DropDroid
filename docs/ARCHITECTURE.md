# DropDroid Architecture

DropDroid has two parts:

- Android receiver app
- Desktop sender web app

## Android receiver

The Android app starts a small local HTTP receiver on port `47881`.

It also broadcasts a lightweight UDP discovery message on port `47882`, so the desktop sender can find the phone automatically on the same Wi-Fi network.

Received files are written to the app's external files download directory. APK files are treated like normal files unless the APK install helper toggle is enabled.

When the toggle is enabled and the incoming file ends with `.apk`, DropDroid opens Android's package installer with a `FileProvider` URI. Android still controls the final installation confirmation.

## Desktop sender

The desktop sender is a local Node.js server that serves the browser UI on port `38531`.

It listens for Android discovery broadcasts and exposes the discovered devices to the UI. When a user drops a file, the sender streams that file to the selected Android device with an HTTP `PUT` request.

## Ports

- `38531`: desktop sender UI
- `47881`: Android file receiver
- `47882`: UDP discovery

## Current limitations

- The local transfer protocol is not encrypted.
- Discovery can be blocked by some routers or guest Wi-Fi networks.
- Silent APK installation is not supported because Android requires user confirmation.

Manual IP entry is included as a fallback when UDP discovery is unavailable.
