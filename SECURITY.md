# Security Policy

## Supported versions

DropDroid is in early development. Please report security issues against the latest `main` branch.

## Reporting a vulnerability

Please do not open a public issue for sensitive security reports.

Send the maintainer a private report through GitHub security advisories if enabled on the repository, or contact the repository owner directly.

## Security notes

- DropDroid is designed for trusted local Wi-Fi networks.
- Files are transferred over the local network without cloud storage.
- Uploads require QR pairing and HMAC-signed transfer headers.
- The current local protocol authenticates transfers but does not encrypt file contents.
- Android always requires user confirmation before installing APK files.

Avoid using DropDroid on untrusted public networks until encrypted pairing is added.
