# DropDroid Architecture

DropDroid has two parts:

- Android receiver app
- Desktop sender web app

## Android receiver

The Android app starts a small local HTTP receiver on port `47881`.

It also broadcasts a lightweight UDP discovery message on port `47882`, so the desktop sender can find the phone automatically on the same Wi-Fi network.

The Android app requires QR pairing before accepting uploads. The scanned QR contains a portal id and a high-entropy shared secret. Upload requests must include signed DropDroid headers.

Received files are written to the app's external files download directory. APK files are treated like normal files unless the APK install helper toggle is enabled.

When the toggle is enabled and the incoming file ends with `.apk`, DropDroid opens Android's package installer with a `FileProvider` URI. Android still controls the final installation confirmation.

## Desktop sender

The desktop sender is a local Node.js server that serves the browser UI on port `38531`.

It creates a per-session pairing QR, listens for Android discovery broadcasts, and exposes discovered devices to the UI. When a user drops a file, the browser computes the file SHA-256 hash and the local sender signs the transfer before streaming the file to the selected Android device with an HTTP `PUT` request.

## Transfer authentication

Every upload includes these headers:

- `X-DropDroid-Portal-Id`
- `X-DropDroid-Timestamp`
- `X-DropDroid-Nonce`
- `X-DropDroid-Content-Sha256`
- `X-DropDroid-Signature`

The signature is HMAC-SHA256 over the method, request path, filename, file size, timestamp, nonce, and file hash.

The Android receiver rejects:

- unpaired transfers
- missing signatures
- wrong portal ids
- expired timestamps
- repeated nonces
- invalid signatures
- file hash mismatches

## Ports

- `38531`: desktop sender UI
- `47881`: Android file receiver
- `47882`: UDP discovery

## Current limitations

- The local transfer protocol is not encrypted.
- Discovery can be blocked by some routers or guest Wi-Fi networks.
- Silent APK installation is not supported because Android requires user confirmation.

Manual IP entry is included as a fallback when UDP discovery is unavailable.
