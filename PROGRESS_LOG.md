# aiwit-viewer — Progress Log

Third-party Android client for AIWIT / Kemo Pro / EKEN-OEM doorbells.
Companion repo to the reverse-engineering workspace at `~/aiwit-re/`
(which holds the decompiled vendor app + Python prototype + RE notes).

## 2026-05-25 — Session 1: scaffold + initial commit

### Sources of truth
- Cloud REST protocol: cracked in `~/aiwit-re/` (login flow + sign algorithm
  + endpoint catalog). See `~/aiwit-re/PROGRESS_LOG.md`.
- Python prototype: `~/aiwit-re/client/aiwit_client.py` — verified end-to-end:
  login, listDevices, listRecordings, ossToken, signedDownloadUrl, .ts2 download.
- This repo is a Kotlin/Compose port of that prototype with a UI on top.

### Scope shipped tonight
- Project skeleton: Gradle Kotlin DSL, Android 26+ min, target 34, Kotlin 1.9.22,
  Compose BOM 2024.02.00, kotlinx-serialization plugin.
- `data/Models.kt` — @Serializable models for ApiEnvelope, LoginContent,
  Device, Recording, OssToken.
- `data/AiwitClient.kt` — Kotlin REST client. Same login + sign-salt protocol
  as Python prototype (`MD5(username + "/" + password + "EKDB_ni&Hb&Zt&zz^7qn9")`).
  Uses stdlib HttpURLConnection — no Retrofit/OkHttp dependency tree.
- `data/OssSigner.kt` — Alibaba OSS pre-signed URL builder. Critical detail
  encoded in comments: `security-token` must be in the canonical resource
  (not just a query param) for the signature to match. Discovered the hard way
  via a 403 that quoted OSS's expected StringToSign verbatim.
- `data/CredentialStore.kt` — EncryptedSharedPreferences (AES-256-GCM, keys
  in Android Keystore). Stores email + password + persistent `appSn` UUID.
- UI screens: LoginScreen (email + password + show-password toggle),
  CameraListScreen (device rows with name/model/firmware/battery/state),
  ClipsScreen (today's clips per device, trigger-source labels: motion vs live).
- `MainActivity.kt` — single activity, sealed-class screen routing, lifecycleScope
  for async, auto-login if credentials saved.
- `.github/workflows/build.yml` — CI builds debug APK via gradle, uploads as
  artifact. Includes a `gradle wrapper` first-run safety net so the repo can
  build even before the wrapper jar is committed (we deliberately don't commit
  the wrapper binary).
- `README.md` — standard 4-element badge header (Discord + Total Downloads +
  Latest Release + 📥 link) per The412Banner convention.
- `.gitignore` — excludes vendor .so files from `jniLibs/` (we'll bundle them
  locally when wiring playback, but they shouldn't be in the public repo).

### Deliberately deferred to next session
- **Playback**: `libEZMediaPlayer.so` + dependencies (libavcodec/format/util/
  filter, libc++_shared, libswscale, libswresample) need to be extracted from
  the AIWIT APK and dropped into `app/src/main/jniLibs/arm64-v8a/`. The Java
  wrapper classes (`cn.coderfly.mediacodec.EZCloudStoragePlayer`,
  `EZCloudStorageDownloader`, etc.) need to be ported to Kotlin, or the JNI
  bridge stubs need to be matched so the .so calls back into our package
  namespace correctly. Risk: the .so probably checks the calling package name
  for licensing — we may need to either (a) keep the original
  `cn.coderfly.mediacodec` package paths for the Kotlin classes, or
  (b) patch the .so. Open question.
- **Thumbnails**: blocked on `.ts2` decryption (Phase 5b). For now clip rows
  show timestamp + duration + size only.
- **Live view**: P2P RE blocked on `libNatType.so` (Phase 5c). UI placeholder
  not yet added — first session will be just recordings.

### Known protocol details encoded in code (so they survive code review)
- Salt: `EKDB_ni&Hb&Zt&zz^7qn9` (constant across login/register/password-reset).
- Required headers: `AppName: aiwit`, `AppLang: en`, `AppVersion: 3.5.6`,
  `AppID: APK_<uuid>`, `OS: 2`.
- AIWIT app version we cloned the protocol from: `3.5.6`.
- All endpoint paths embed the session_id and appSn in the URL path
  (not in headers): `/path/{session_id}/{appSn or YYYYMMDD or device_sn}`.
- Battery field is in tenths-of-percent on some EKEN firmware (e.g., 356 = 35.6%);
  CameraListScreen's `formatBattery` handles this.

### Build target
- CI: `.github/workflows/build.yml` — push to main, debug APK in artifacts.
- No local builds (per project workflow rule).
