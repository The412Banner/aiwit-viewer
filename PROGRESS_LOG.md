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

---

## 2026-05-25 — Session 1.7: pre-live-view foundation (commits `c4d9194` … `cea4c3b`)

### What landed
- 7 more native libs bundled (libVCTP2P, libNatType, libEZAEC, libg711a_jni,
  libnms, libaes3, libwebrtc_audio_preprocessing, libEZMediaUtils) — 8.9 MB total.
- Java wrappers copied verbatim with matching packages so JNI symbols bind:
  EZMediaUtils, P2PSession, Nat, G711ACodec, RectInfo.
- AIWIT internal protocol code copied: n1.a (1310 lines, EZJetterBuffer
  / jitter buffer), n1.b (FrameData), n1.c (RTPData / packet parser);
  plus stubs for u1.t (logger), u1.o (4 helper methods only), e1.a
  (build flag).
- Patched mbridge SDK constants inline (MBridgeConstans.ENDCARD_URL_TYPE_PL,
  ExtractorMediaSource.DEFAULT_LOADING_CHECK_INTERVAL_BYTES).
- Added Guava dep (Ascii.DEL + UnsignedBytes used by n1.*).
- CloudMessagingClient — minimal Kotlin TLS client that dials the same
  relay AIWIT's m1.f uses (47.107.28.145:9003, trust-all SSL); reads with
  m1.f.L()'s }{ split logic; sends a best-guess login JSON + 30s heartbeats.
  Wired into MainActivity post-login; logs to tag "CloudMsg".

### Open questions for next session
- Does the server accept plain JSON, or is it always on m1.f's encrypted
  path (M() / x1.a)? Need to inspect logcat after a fresh install to see
  what the server replies (banner, error, silence, close).
- If encrypted path mandatory: port x1.a's framing + AES (and discover
  where DoorbellApplication.f13736j1 / f13740k1 keys come from — likely
  from the REST login response or a derivation we haven't traced).
- What's the wake-camera command shape? Probably JSON with cmd:"preview-start"
  or similar, plus the device's SN. Need a captured exchange from AIWIT
  itself to be sure.

### What's NOT in this commit set
- No UI for live view yet — banner above the clips list and camera list
  remains "live preview pending" placeholder.
- No frame rendering pipeline yet (n1.* code is dead until we have bytes
  to feed it from P2PSession).
- Two-way audio (EZAECSpeaker) not wired.

---

## 2026-05-25 — Session 2: Live-view bootstrap end-to-end almost works (commits up to `b935d25`)

### What works
- TLS to the actual cmd-server (`8.222.190.34:8891`, found via AIWIT's
  `/proc/<pid>/net/tcp` after the firewall-filtered `47.107.28.145:9003`
  fallback failed).
- `app-login` handshake (cmd, udid, username, pushToken, lang, platform_id,
  AppName, k = "0" + MD5(salt + udid + "app-login")).
- Heartbeat with proper shape (cmd, udid, sn:<int>) every 20s — server
  echoes back.
- Subscribed to push notifications: device-heartbeat / device-state /
  led-info flow in unsolicited.
- `wakeup` (m1/a.java G) and `preview-start` (m1/a.java l).
- `preview-start` reply carries area, ip, ipv6, video_port, audio_port,
  speak_port, pk for the requested camera.
- LiveSession orchestrator: parses list_v2 config, picks
  p2p_encrypt_servers when present, calls `P2PSession.setEncrypt(true)`
  then `loginP2P()`, then on preview-start reply caches the pk and calls
  `connectToPeer(deviceSn)`, then a poller coroutine pulls n1.b frames
  from n1.a, decodes via `EZMediaUtils.decodeFrameWithData` → emits
  Bitmaps via StateFlow → renders into `LivePipBanner`.

### Where it stalls — empty peer-address list
After connectToPeer, the native lib logs `>>>p2p: list addr (empty)`
every 200ms — it's polling for known peer addresses and finding none.
`p2pConnected` callback never fires. Diagnosis:
- `stun_servers[0] = 47.107.28.145:17051` from list_v2 config is
  **firewall-filtered** from US networks (verified earlier with nmap —
  same dead IP as the legacy push_server fallback).
- `p2p_servers[0] = 47.245.120.253:17051` is open (verified).
- `chat_servers[0] = 47.236.9.175:29007` is open (verified).
- `47.251.54.119` is the US-region relay that the cloud points
  preview-start replies at — closed on `:17051` but listens on the
  ephemeral ports the cmd-server hands back.
- Native lib's STUN attempts to `47.107.28.145:17051` time out silently
  → its `list addr` stays empty → connectToPeer can't pick a peer
  → no traffic.

### Hypotheses to chase next session
1. AIWIT's `m1.f` connection passes additional state to the .so that we
   don't (the lib may have its OWN cmd-server connection that we're
   bypassing because we own ours).
2. A different STUN server is in play — possibly Google's public STUN
   if the lib has a fallback, or the lib expects to extract our public
   IP from the cmd-server's `app-login` reply `ip:<our_public>` (which
   it would only see if it owned the cmd-server connection).
3. Native lib expects `Nat.getNatType()` to be called before loginP2P
   to seed its NAT discovery (AIWIT only calls it for stats but maybe
   the side-effect matters).
4. We may need to forward the `preview-start` reply (ip+ports) into
   the native lib via some path we haven't identified — possibly via
   the `peer` arg of a different method we're not calling.

### Next-session entry points
- Inspect AIWIT's `/proc/<pid>/net/tcp` while it's mid-live-view to see
  every endpoint the lib touches.
- `strace` the native lib's connect/sendto calls if we can get a
  rooted-debug build.
- Read `m1.f.F()` end-to-end + the thread runners (`a/b/d/e`) to see
  exactly which JNI calls AIWIT makes between connect and frame-receive.

### Repo state at session 2 end
- Last commit: `b935d25` (wakeup-first sequence).
- Build green at `26423282639`.
- APK at `/sdcard/Download/aiwit-viewer-debug.apk` works for everything
  except live frames (login + cameras list + recordings playback all
  device-verified).

---

## 2026-05-25 — Session 2 cont'd: AIWIT cmd-server protocol MITM'd, last mile blocked

### What landed this session
- **MITM'd AIWIT's cmd-server** via iptables NAT REDIRECT + Python TLS proxy.
  AIWIT uses raw `SSLSocket` (not OkHttp), so the WiFi proxy doesn't apply —
  needed kernel-level redirect with UID exemption for the proxy itself.
  Script + cert at `/data/data/com.termux/files/home/aiwit-re/captures/`,
  staged to `/sdcard/Download/` so it could run as UID 0 via the bridge.
- **Captured AIWIT's actual live-view cmd-server flow** (different from
  what we'd been doing):
  ```
  app-login (platform_id=1, pushToken="")
  heartbeat
  wakeup peer:<sn>
  -- server: wakeup REPLY with err_no:0, pk, ip, video_port, audio_port,
             speak_port  ← session params arrive on the wakeup reply
  devices-state
  wakeup (retry)
  -- server: preview-start NOTIFICATION (msg_type:"notification", state:1)
             ← arrives ~3s after wakeup reply, signals camera is fully
               online and registered with relay
  -- server: fast-streaming NOTIFICATION ← THE actual streaming trigger
  ping every 10s (keepalive — without this the relay drops the peer)
  preview-finish (on close)
  ```
- **Implemented all of it**: corrected `CloudMessagingClient` (platform_id=1,
  pushToken="", new sendPing/sendDevicesState methods), corrected
  `MainActivity.onSelectDevice` to replay AIWIT's exact flow, corrected
  `onMessage` to also use the wakeup reply for pk.
- **P2P side now connects all the way**:
  - `cmd:"connect",err_no:0` from relay (vs prior `peer not exist!`)
  - `state:3` reached (connected)
  - `ready / can-recv / can-schedule` sent by lib
  - `p2pReceiveDataCall` fires (bytes flow from native into Kotlin)
- **`Nat.getNatType()` polled periodically** (kicks lib's NAT machinery
  — was needed to advance state).
- **Routed callbacks via active-camera pk** since the lib passes
  `sn="12345678"` (placeholder) instead of the device SN — AIWIT's own
  listener ignores the sn arg and uses the activity-local pk.

### The remaining gap
Bytes ARE arriving at the JNI callback layer (1300+ callbacks per test
session). But every callback is **1 byte** and never parses as RTP —
they're lib-internal NAT keepalives, not real video frames.

The cloud is **not pushing us** `cmd:"preview-start"` or
`cmd:"fast-streaming"` notifications, even with AIWIT's exact UDID
hijacked. Without `fast-streaming`, the camera firmware never advances
from "handshake mode" to "stream mode" — confirmed by tcpdump showing
only small handshake-sized UDP packets from the camera.

### Candidate explanations (untested)
1. TLS-handshake fingerprint differs (cipher suites, extensions) —
   Android `SSLSocket` defaults vs AIWIT's library's preferences. Cloud
   might pin notifications to fingerprints it recognizes.
2. Cloud-side "latest session wins" stickiness based on TCP source port /
   TLS session ID / connection age — when AIWIT was last active, its
   session was the "primary", and our session is treated as a passive
   observer.
3. Hidden REST call AIWIT makes to api.v2.gdxp.com that primes the cloud
   to expect a cmd-server session. We MITM'd cmd-server but not REST in
   the same window.
4. Some kind of device fingerprinting we don't reproduce.

### Next-session entry points
- Concurrent MITM of BOTH api.v2.gdxp.com (HTTPS REST) AND
  8.222.190.34:8891 (cmd-server) during a SINGLE AIWIT live-view session
  to find any REST call we missed.
- TLS handshake comparison: tshark the AIWIT TLS handshake vs ours
  (Client Hello cipher list, extensions).
- Look for any state on the Android side (SharedPreferences, files in
  `/data/data/com.eken.aiwit/`) that AIWIT writes after first install
  and might be used as device fingerprint.

### Repo state at session 2 end (revised)
- Live-view foundation: all in place (native libs, JNI wrappers,
  n1.* protocol code, decryption pipeline, P2P session orchestration,
  active-camera pk routing, NAT-poll loop, retry-less single
  connectToPeer, ping-keepalive every 10s).
- Cmd-server protocol: 100% matches AIWIT's captured flow.
- Result: relay connect succeeds, bytes arrive at callback, but
  camera never actually streams real frames because the cloud
  doesn't issue `fast-streaming` to our session.
- All cloud-recording playback + browsing features remain functional
  and device-verified.

