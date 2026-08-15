# ExoBoost

ExoBoost is an Android application that provides a floating edge toolbox over other running applications without requiring root privileges. Inspired by the sidebar and video toolbox features found on Xiaomi/MIUI devices, ExoBoost is an independent implementation built entirely on Android's public APIs. The project includes quick utilities such as clean screenshot capture, black screen audio listening, audio gain control, GPU color grading, and per-app profile automation.

## Overview

On custom Android skins like MIUI/HyperOS, sidebar toolboxes allow users to quickly access capture utilities, display adjustments, and audio tools without leaving their active app. Standard Android does not provide a built-in equivalent for third-party apps.

ExoBoost implements this interaction model for standard Android devices using a lightweight overlay service. The application operates strictly within Android's public API boundaries, avoiding root access, hidden APIs, runtime code injection, or accessibility service abuse.

Because Android imposes strict security boundaries between applications, ExoBoost uses capability detection to determine what the underlying hardware and OS version support, falling back gracefully when specific features are restricted by device vendors.

## Features

### Edge Overlay Handle
A customizable vertical pill that rests on the screen edge (left or right). Swiping inward opens the floating toolbox. The handle supports dragging to reposition vertically, opacity adjustment, size customization, and tactile haptic feedback.

### Floating Toolbox Panel
A floating overlay card that appears on top of the active application without resizing or displacing the underlying layout. The toolbox supports four layout modes (4-column grid, 3-column grid, 2-column grid, and a compact horizontal strip) and dismisses upon tapping outside or swiping outward.

### Clean Screenshot Capture
Captures the device screen using Android's MediaProjection API. The floating toolbox and edge handle automatically hide before the frame is acquired so that the resulting image contains only the underlying application content. Captured images are saved to the standard `Pictures/Screenshots` directory via Scoped Storage MediaStore APIs, accompanied by a non-intrusive confirmation card with open, share, and dismiss actions.

### Black Screen Audio Mode
Places an opaque pure black (`#000000`) overlay above the active screen and reduces window brightness to minimum while leaving the device unlocked. This allows users to keep media streaming apps playing without triggering accidental touches in pockets or bags. Exiting is handled through double-tap or swipe gestures.

### Volume Boost (Device-Dependent)
Provides an audio amplification tool with selectable boost targets (100% to 300%). It routes requested gain to Android's global audio session (Session 0) using `DynamicsProcessing` with a brickwall limiter (-1.0 dB threshold) and falls back to `LoudnessEnhancer`. On devices where vendor audio HAL drivers lock Session 0, the app detects this restriction and informs the user.

### Style Engine
A GPU-accelerated image and video color grading processor using GLSL fragment shaders and `ColorMatrixColorFilter`. It includes 11 presets (Original, AI, Outdoor, Cinema, Retro, B&W, Vivid, Warm, Cool, Night, and Custom) with adjustable parameters for contrast, saturation, temperature, gamma, and vignette.

### Live Video Filter (Experimental)
An experimental prototype that captures the display compositor output via MediaProjection, processes incoming frames through the GPU shader pipeline, and renders the graded output in a movable Picture-in-Picture floating viewport with real-time FPS and latency metrics.

### Per-App Profiles and Automation
Allows configuring distinct toolbox behaviors for different installed applications. When granted Usage Access permission, ExoBoost detects foreground application changes and can automatically hide the edge handle on messaging apps (e.g., WhatsApp) or apply custom default styles and volume levels for video apps (e.g., YouTube, VLC).

### Customizable Toolbox Editor
A dedicated visual editor in the main application allowing users to reorder tools, add or remove tools from the active panel, and choose between different grid layouts.

### System Diagnostics
A built-in hardware and capability probe that evaluates the device's overlay permissions, MediaProjection availability, audio effect backend support, GPU shader status, and Usage Access state.

## How It Works

ExoBoost is structured into modular layers that separate background service management, window rendering, and tool processing:

* **WindowManager Overlay Service**: `OverlayService` runs as an Android foreground service and uses `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` with `FLAG_NOT_FOCUSABLE` to draw floating views directly over other applications.
* **Jetpack Compose in Overlays**: The floating toolbox, style cards, confirmation popups, and main configuration interface use Jetpack Compose hosted inside `ComposeView` wrappers with dedicated `ViewCompositionStrategy` lifecycles.
* **MediaProjection Capture**: Screenshot capture and the experimental live stream filter request screen capture tokens via transparent helper activities (`ScreenshotCaptureActivity`, `LiveStyleCaptureActivity`) and acquire raw pixel buffers via `ImageReader`.
* **AudioEffect Pipeline**: `VolumeBoostController` probes `AudioEffect` descriptors for `DynamicsProcessing` and `LoudnessEnhancer` instances bound to audio session 0.
* **Hardware Shader Pipeline**: `ShaderProcessor` manages GPU color transformations through custom OpenGL ES fragment shaders and hardware `ColorMatrix` operations.
* **Foreground App Detection**: `ForegroundAppDetector` samples Android `UsageStatsManager.queryEvents()` at a 1.5-second interval to identify top-activity switches without high CPU usage.
* **Preferences Storage**: `PreferencesManager` and `AppProfileManager` use Jetpack DataStore Preferences for reactive, asynchronous configuration persistence.

## Black Screen Mode

Black Screen mode is designed for listening to video or audio content without keeping the display visibly lit or vulnerable to pocket touches.

Important operational details:

1. **Not a Hardware Display Lock**: Black Screen mode does not call device lock APIs (`DevicePolicyManager`) and does not physically shut off display power at the hardware controller level. It renders an opaque `#000000` view with `screenBrightness = 0.01f`. On OLED panels, true black pixels draw minimal power, but actual battery consumption will depend on the panel driver and background CPU load.
2. **Underlying Playback Dependency**: ExoBoost cannot force a third-party application to continue playing if that application intentionally pauses itself when hidden or unfocused. The underlying app must support active playback while running in the foreground.

## Volume Boost

Volume Boost operates within Android's public `AudioEffect` framework:

1. **Requested vs. Physical Loudness**: The percentage values (100% to 300%) represent ExoBoost's requested amplification gain curve (0.0 dB up to +9.5 dB), not a guarantee of three times physical speaker loudness.
2. **Audio Architecture Constraints**: Android does not provide a universal public API to intercept and modify the digital audio streams of arbitrary third-party apps directly. ExoBoost attaches effects to global audio session 0.
3. **Vendor HAL Differences**: Certain device manufacturers and audio firmware configurations (such as specific Dolby Atmos or custom DSP implementations) restrict Session 0 effects. On such devices, ExoBoost displays a "HAL Restricted" notice.
4. **Limiter Protection**: When `DynamicsProcessing` is available, ExoBoost configures a limiter stage with a -1.0 dB ceiling to mitigate digital clipping and distortion at higher gain levels.

## Style and Video Processing

The Style Engine provides color grading tools:

1. **Direct Content Processing**: The primary Style Engine grades internal media and reference test canvases directly on the GPU with full frame rate and zero latency.
2. **Experimental External App Processing**: Because Android's security architecture prevents one application from injecting shaders into another app's View hierarchy, processing external video requires capturing the display output through MediaProjection and displaying the result in a floating PIP window.
3. **DRM and Protected Content**: MediaProjection cannot capture DRM-protected content (such as Widevine L1 video streams in Netflix or apps using `FLAG_SECURE`). On protected surfaces, Android returns a black frame. ExoBoost detects this state and displays a notice.

## Android Permissions

ExoBoost requests only the permissions necessary for its overlay and capture tools:

* `SYSTEM_ALERT_WINDOW`: Required to draw the edge handle, floating toolbox, black screen surface, and confirmation cards over other applications.
* `FOREGROUND_SERVICE`: Required to maintain the overlay service lifecycle in the background.
* `FOREGROUND_SERVICE_SPECIAL_USE`: Declared for Android 14+ compliance for the floating overlay manager.
* `FOREGROUND_SERVICE_MEDIA_PROJECTION`: Required on Android 14+ for screen capture and experimental live streaming.
* `POST_NOTIFICATIONS`: Required on Android 13+ to display the ongoing foreground service notification.
* `VIBRATE`: Used for tactile haptic feedback during edge handle drag gestures and button presses.
* `RECEIVE_BOOT_COMPLETED`: Optional permission used to restore the edge handle after a device restart if enabled in settings.
* `PACKAGE_USAGE_STATS`: Optional permission used by Per-App Profiles to detect when the active foreground application changes.

ExoBoost does not declare or use:
* `INTERNET`: The application is completely offline.
* `BIND_ACCESSIBILITY_SERVICE`: ExoBoost does not use accessibility services.

## Requirements

* Minimum SDK: Android 8.0 Oreo (API 26)
* Target SDK: Android 14 (API 34)
* Compile SDK: Android 15 (API 35)
* Primary Test Environment: Android 13 (API 33)

## Building

### Prerequisites
* JDK 21
* Android SDK with Build Tools 35.0.0 and Platform API 35
* Gradle 8.8+ (wrapper included)

### Build Commands

On Linux / macOS:
```bash
./gradlew assembleDebug
```

On Windows (PowerShell):
```powershell
.\gradlew.bat assembleDebug
```

To run unit tests:
```bash
./gradlew test
```

To assemble a release build:
```bash
./gradlew assembleRelease
```

### Installing the Debug Build

Using ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```text
app/src/main/java/com/exoboost/app/
├── MainActivity.kt                     Main dashboard with Overview, Toolbox, Apps, and Settings tabs
├── ExoBoostApp.kt                      Application class initializing notification channels
├── core/
│   ├── capability/                     Overlay and notification permission checks
│   ├── common/                         Constants and notification channel IDs
│   ├── datastore/                      DataStore preferences and ExoSettings model
│   ├── designsystem/                   Color tokens, typography, and theme definitions
│   ├── permissions/                    System permission intent helpers
│   └── receiver/                       BootCompletedReceiver for startup restoration
└── feature/
    ├── overlay/                        OverlayService, EdgeHandleView, FloatingToolboxView
    ├── toolbox/                        ToolRegistry, ToolAction models, ToolboxEditorScreen
    ├── profiles/                       AppProfile models, AppProfileManager, ForegroundAppDetector
    ├── settings/                       SettingsScreen and preference management
    ├── diagnostics/                    DiagnosticsProbe and DiagnosticsDialog
    └── tools/
        ├── audio/                      VolumeBoostController and AudioEffectCapabilityDetector
        ├── blackscreen/                BlackScreenOverlayController
        ├── screenshot/                 ScreenCaptureEngine, MediaStoreScreenshotSaver, Activities
        └── style/                      StyleEngine, ShaderProcessor, and LiveStreamCaptureEngine
```

## Current Status

### Stable
* Universal edge handle with vertical repositioning and inward swipe detection.
* Floating toolbox with customizable 4-column, 3-column, 2-column, and compact layouts.
* Clean non-root screenshot capture excluding overlays and saving to `Pictures/Screenshots`.
* Black screen audio listening surface with multi-gesture unlock.
* Reusable GPU style engine with 11 GLSL presets.
* Visual toolbox customization and tool reordering.
* Per-app profiles with automatic handle suppression.
* Production settings hub and system diagnostics probe.

### Device-Dependent
* Global Session 0 Volume Boost (depends on OEM audio HAL support for DynamicsProcessing / LoudnessEnhancer).
* Fullscreen immersive video handle auto-hide (depends on OEM system inset reporting).

### Experimental
* Live Video Stream PIP filter using MediaProjection and real-time GPU grading.

## Limitations

* **Sandbox Boundaries**: Android does not allow normal third-party applications to rewrite or inject shaders into another application's native View hierarchy.
* **DRM Protection**: MediaProjection cannot capture DRM-protected streams (`FLAG_SECURE`).
* **Audio HAL Differences**: Global audio effects on session 0 are subject to vendor audio framework behavior and may not affect all output streams equally.
* **OLED Black Overlay**: An application-level black overlay minimizes pixel power draw on OLED screens but does not turn off the underlying display controller.
* **OEM Background Restrictions**: Certain OEM Android skins (e.g., MIUI/HyperOS, One UI) may require manual battery optimization exemptions to prevent background service termination.

## Privacy

ExoBoost operates entirely on-device:

* **Zero Network Traffic**: The application does not declare the `android.permission.INTERNET` permission and makes no network connections.
* **No Telemetry or Tracking**: No analytics libraries, crash reporters, or tracking identifiers are included.
* **Local Media Processing**: Screen captures and audio processing are handled strictly in local device memory and public storage directories.

## Development Notes

ExoBoost is developed as an independent Android project exploring the capabilities and boundaries of Android's public overlay, capture, and audio effect APIs.

## License

This repository does not currently specify an open-source license.
