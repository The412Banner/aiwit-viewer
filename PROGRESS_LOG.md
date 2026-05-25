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

---

## 2026-05-25 — Session 1.5: UX quick wins (commit `7f591e8`, run 26417768900 ✅)

- Date picker on ClipsScreen — Material3 DatePicker dialog; clips for any past day
- Pull-to-refresh on both camera list + clips list — Material3 `PullToRefreshContainer`
- Per-clip "download .ts2" — enqueues an Android `DownloadManager` job for the
  OSS-signed URL, saves to public Downloads with a system notification.
- Added `WRITE_EXTERNAL_STORAGE android:maxSdkVersion="28"` to manifest so the
  DownloadManager dest works on pre-Q devices.

## 2026-05-25 — Session 1.6: Playback + live-PIP stub (commit `cd6c282`, run 26418217587 ✅)

### Bundled libraries (5.5 MB, in jniLibs/arm64-v8a/)
Extracted from `~/aiwit-re/apk/arm64.apk` (AIWIT 3.5.6):
- `libEZMediaPlayer.so` (2.0 MB) + `libEZMediaUtils.so` (35 KB) — the
  `cn.coderfly.mediacodec` cloud-TS player. Does the `.ts2` decryption.
- FFmpeg vendored copies: `libavcodec.so` (1.2 MB), `libavformat.so` (677 KB),
  `libavutil.so` (330 KB), `libavfilter.so` (97 KB), `libpostproc.so` (30 KB),
  `libswresample.so` (67 KB), `libswscale.so` (264 KB).
- `libc++_shared.so` (977 KB) — NDK runtime.
Provenance documented in `LIBS_NOTICE.md`.

### Java port of EZCloudStoragePlayer
Lives at `app/src/main/java/cn/coderfly/mediacodec/EZCloudStoragePlayer.java`.
Why Java not Kotlin: the .so's JNI symbols are mangled
`Java_cn_coderfly_mediacodec_EZCloudStoragePlayer_<method>`. A Kotlin
companion-object with `@JvmStatic external` mangles to a different symbol
(`...EZCloudStoragePlayer_00024Companion_create`) and wouldn't bind.

### PlayerScreen
- Single Activity, SurfaceView via `AndroidView`.
- Lifecycle in a `DisposableEffect`: construct `EZCloudStoragePlayer`,
  setExceptedLength + setFormatFlag + setPKey + setListener on enter;
  stop() + release() on exit.
- `play(endpoint, bucket, fileName)` is kicked from
  `SurfaceHolder.Callback.surfaceCreated()` so the surface is valid before
  render starts. Same call shape as AIWIT's
  `Historical2VideosForPlayOnline.Y4()`.

### Open question on OSS auth in the native lib
AIWIT calls `play(endpoint, bucket, fileName)` with no token/credential
passed. `libEZMediaPlayer.so` exports no `setStsToken`-style method and
its strings don't reference any callback-target class paths into Java.
Hypothesis: either anonymous OSS access for these specific path patterns
(unlikely for a private bucket), OR there's a globally-set credential
state owned by another part of AIWIT's code we haven't traced.

**To verify on device**: tap a clip → watch for errors. If 403/auth fails,
the next step is `strace`-style hooking on the .so's network calls
(or LD_PRELOAD intercept) to see what HTTP request it actually issues.

### Live PIP stub
- `LivePipTile` (96×54 dp) — inline on each camera row in the camera list.
- `LivePipBanner` (full-width 16:9) — above the clips list, scrolls.
- Both show online/offline via the device's `state` field. Explicitly
  labeled "(implemented once P2P RE lands)". Real live stream is blocked
  on Task #10 (libNatType.so RE).

