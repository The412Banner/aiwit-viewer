# Notice: third-party native libraries in `app/src/main/jniLibs/`

This repository bundles a handful of `.so` files extracted from the AIWIT
Android app (package `com.eken.aiwit`, version 3.5.6). They are required at
runtime to decrypt and play the `.ts2` clips your doorbell stores in Alibaba
OSS — the decryption algorithm is implemented inside these binaries and
isn't documented anywhere, so we reuse them rather than re-implement.

## What's bundled

| File | Origin | Purpose |
| --- | --- | --- |
| `libEZMediaPlayer.so` | third-party `cn.coderfly.mediacodec` lib bundled in AIWIT | TS2/TS3/TS4/MP4 cloud-storage player; does the actual `.ts2` decryption |
| `libEZMediaUtils.so` | same | helper used by `EZMediaPlayer` |
| `libavcodec.so`, `libavfilter.so`, `libavformat.so`, `libavutil.so`, `libpostproc.so`, `libswresample.so`, `libswscale.so` | upstream FFmpeg, AIWIT's vendored copy | media decode runtime that `EZMediaPlayer` links against |
| `libc++_shared.so` | NDK runtime | required by every modern Android `.so` |

## Why redistribute

- The binaries are required to fulfill the documented goal of this project:
  giving owners of EKEN-OEM doorbells a usable alternative client.
- They're freely downloadable inside any installation of the official AIWIT
  app on the Play Store.
- This is **interop**, not derivative work — we are not modifying the
  binaries, copying their source, or competing with the vendor app on AIWIT's
  primary distribution channels.

If you are the copyright holder for any of these libraries and want them
removed from this repository, open an issue and we'll switch to a runtime
fetch-from-installed-AIWIT approach instead.
