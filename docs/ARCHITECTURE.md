# DropDroid Architecture

DropDroid has two parts:

- Android receiver app
- Desktop sender web app

## Android receiver

The Android app starts a foreground receiver service with a small local HTTP receiver on port `47881`.

The foreground service keeps the local receiver available when the app is backgrounded and shows a persistent Android notification with ready/receiving status.

It also broadcasts a lightweight UDP discovery message on port `47882`, so the desktop sender can find the phone automatically on the same local network when broadcast is allowed.

The Android app requires QR pairing before accepting uploads. The scanned QR contains a portal id and a high-entropy shared secret. Upload requests must include signed DropDroid headers.

Received files are written to the app's default external files download directory unless the user chooses a folder through Android's system folder picker. Chosen folders use persisted Storage Access Framework permissions.

The receiver reports progress through `ReceiverState` while bytes are streaming. Completed files are added to an in-memory recent files list, and tapping a file opens it with Android's default app resolution.

APK files are treated like normal files unless the APK install helper toggle is enabled.

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

## Local-only networking

DropDroid is local-only. File contents are never uploaded to the internet, cloud storage, GitHub, or a DropDroid server.

The connection can be Wi-Fi, phone hotspot, LAN, Ethernet-to-router, USB tethering, or a local VPN/tunnel, as long as the desktop can reach the Android device by local IP.

Discovery is best-effort local broadcast. Manual IP entry is the fallback when broadcast discovery is blocked.

The desktop sender prefers the UDP packet source address for discovered devices because Android devices can have multiple interfaces. The Android app also displays all local IPv4 addresses so users can manually choose a reachable address when discovery selects an unreachable interface.

## Current limitations

- The local transfer protocol is not encrypted.
- Discovery can be blocked by some routers, guest networks, VPNs, or isolated access points.
- Silent APK installation is not supported because Android requires user confirmation.

Manual IP entry is included as a fallback when UDP discovery is unavailable.
