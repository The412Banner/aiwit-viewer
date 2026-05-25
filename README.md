<div align="center">

# AIWIT Viewer

A third-party Android client for **AIWIT / Kemo Pro / EKEN-OEM doorbells** that
talks to the same GDXP cloud as the official `com.eken.aiwit` app — without ads,
without the bloat, with a layout we control.

[![Discord](https://img.shields.io/badge/Discord-The412Banner-5865F2?style=for-the-badge&logo=discord&logoColor=white)](#)
[![Downloads](https://img.shields.io/github/downloads/The412Banner/aiwit-viewer/total?style=for-the-badge&label=Total%20Downloads)](https://github.com/The412Banner/aiwit-viewer/releases)
[![Latest Release](https://img.shields.io/github/v/release/The412Banner/aiwit-viewer?style=for-the-badge&include_prereleases&display_name=tag)](https://github.com/The412Banner/aiwit-viewer/releases/latest)
[![📥 Download](https://img.shields.io/badge/📥%20Download%20Latest-darkgreen?style=for-the-badge)](https://github.com/The412Banner/aiwit-viewer/releases/latest)

</div>

## Status

| | |
| --- | --- |
| Login + session resume | ✅ Working |
| Camera list (name, model, battery, online state) | ✅ Working |
| Daily recording list (motion + live-view triggers) | ✅ Working |
| Recording playback | ⏳ Native player wiring next |
| Live view | ⏳ Phase 5c (P2P RE) |
| Thumbnails / scrubbable timeline | ⏳ Needs `.ts2` decryption |

## What's it for

The AIWIT app is shared by ~20 Amazon-listed doorbell rebrands: EKEN, Kemo Pro,
TMEZON, ZUMIMALL, FISHBOT, and others. They all use the same OEM cloud at
`api.v2.gdxp.com`. If you own one and want:

- a viewer that isn't full of ads
- a backup of your motion clips to a place you control
- multi-cam grid layouts the vendor doesn't offer
- to script around the cloud (CLI / cron / Home Assistant bridge)

…this is the start of one.

## Install

Download the latest debug APK from the [Actions tab](../../actions) (or
Releases once we cut one) and side-load. Tested against AIWIT app version 3.5.6
backend, May 2026.

## Build

CI does the lifting — push to `main` and the workflow uploads a debug APK as an
artifact. To trigger manually:

```
gh workflow run build.yml --ref main
```

## Architecture

Pure Kotlin + Jetpack Compose. No Retrofit, no Hilt — the REST surface is tiny
(login, device-list, recordings-list, OSS STS) and a stdlib `HttpURLConnection`
keeps the dependency tree thin.

```
app/src/main/kotlin/com/the412banner/aiwitviewer/
├── data/
│   ├── AiwitClient.kt     ← REST client (login, listDevices, listRecordings, ossToken)
│   ├── OssSigner.kt       ← Alibaba OSS pre-signed URL builder
│   ├── CredentialStore.kt ← EncryptedSharedPreferences for email + password
│   └── Models.kt          ← @Serializable response shapes
├── ui/
│   ├── LoginScreen.kt
│   ├── CameraListScreen.kt
│   └── ClipsScreen.kt
└── MainActivity.kt        ← Single activity, sealed-class screen routing
```

## Security notes

- The AIWIT cloud sends your password **cleartext** in the login form body
  (over TLS, but with no client-side hashing). Same as the vendor app. The app
  also has a trust-all SSL context so any local network attacker can MITM
  AIWIT's own traffic. Use a unique throwaway password for the AIWIT account if
  you reuse passwords anywhere else.
- This app stores your email + password in `EncryptedSharedPreferences`
  (AES-256-GCM, keys in the Android Keystore) so it can re-auth across restarts.
  Logging out wipes them.
- We do **not** intentionally weaken TLS in this client. Cert validation
  remains the system default.

## License

TBD. Not yet decided. Don't use this for anything except your own cameras.

## Acknowledgments

- The 2024 Consumer Reports Eken/AIWIT exposé that confirmed the cloud was
  worth poking at.
- The decompiled `com.eken.aiwit` 3.5.6 for documenting its own protocol so
  thoroughly inside the APK.
