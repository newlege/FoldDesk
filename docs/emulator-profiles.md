# Emulator profiles for FoldDesk Browser

FoldDesk targets **large screens**: Android tablets (~7–13") and foldable **inner** displays. The app UI uses `sw600dp` / `sw720dp` resource buckets so chrome scales up on wider devices; the WebView is always full-bleed below the toolbar.

These AVD profiles are **approximations** for local testing. DPI values are chosen so layout dp width feels close to physical devices (not exact mm calibration).

| Profile ID | Typical device class | Resolution | `hw.lcd.density` | Notes |
|------------|---------------------|------------|-------------------|--------|
| `tablet_7_1280x800` | 7" tablet | 1280×800 | 213 | Classic WXGA; ~600dp smallest width in landscape |
| `tablet_10_1920x1200` | 10" tablet | 1920×1200 | 240 | Common WUXGA 16:10 |
| `tablet_11_2000x1200` | 11" tablet | 2000×1200 | 240 | Many Android tablets / detachables |
| `tablet_12_2560x1600` | 12–13" tablet | 2560×1600 | 320 | WQXGA high-res slates |
| `fold_magic_v6_inner` | Foldable inner (Honor Magic V6 class) | 2352×2172 | 560 | Nearly square inner canvas |

## Commands

```bash
./scripts/create-avd.sh --list
./scripts/create-avd.sh tablet_10_1920x1200
AVD_NAME=FoldDesk_Tablet_10_1920x1200 ./scripts/run-emulator.sh
./scripts/install-and-launch.sh
```

## Real hardware (sideload)

1. Enable Developer options → USB debugging on the tablet / foldable.
2. `./scripts/build-apk.sh`
3. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
4. Open **FoldDesk Browser** → start page → **httpbin.org/headers** and confirm `User-Agent` is Windows desktop Chrome.

On foldables, test **unfolded inner display**; outer cover screen is not optimized for this MVP but the app still runs.

## DPI rationale (short)

Emulator `hw.lcd.density` is the logical dpi (mdpi=160 baseline). Rough check: smallest width dp ≈ `min(width,height) * 160 / density`. Example: 1920×1200 @ 240 → min side 1200 → ~800dp, which triggers tablet `sw600dp` / `sw720dp` styles.
