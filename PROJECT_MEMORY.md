# ExoBoost - Project Memory & Implementation Tracker

## 1. Project Overview
- **Application Name**: ExoBoost
- **Tagline**: Universal MIUI-style Edge Video Toolbox for Android
- **Target OS**: Android 13 (API 33) primary target, `minSdk 26` (Android 8.0 Oreo), `compileSdk 35` / `targetSdk 34`
- **Architecture**: Modular Kotlin Architecture with Jetpack Compose & Android WindowManager Overlay Services

---

## 2. Implementation Status Tracker

| Phase | Description | Status | Notes |
| :--- | :--- | :--- | :--- |
| **Phase 1** | **Universal Edge Overlay Foundation** | **COMPLETED & VERIFIED** | Lightweight edge handle, vertical drag, inward swipe, WindowManager service, DataStore persistence. |
| **Phase 2** | **Floating Video Toolbox Panel & Dynamic Tool Model** | **COMPLETED & VERIFIED** | 4x2 original MIUI-inspired floating tool grid, smooth animations, outward swipe & backdrop dismiss, dynamic ToolAction registry, Toolbox Settings UI. |
| **Phase 3** | **Non-Root Screenshot Pipeline & Action Overlay** | **COMPLETED & VERIFIED** | MediaProjection-based frame capture engine, automatic overlay exclusion during capture, modern MediaStore `Pictures/Screenshots` saver, floating preview confirmation card with [Open, Share, Dismiss], DataStore setting `showScreenshotConfirmation`. |
| **Phase 4** | **Black Screen Audio Mode (Screen Off)** | **COMPLETED & VERIFIED** | Fullscreen `#000000` overlay with minimum brightness dimming (`screenBrightness = 0.01f`), display cutout handling, `FLAG_KEEP_SCREEN_ON`, touch interception against accidental pocket clicks, double-tap / swipe exit gestures, and auto-restoring edge handle. |
| **Phase 5** | **Volume Boost (100%–300%) & Capability Detection** | **COMPLETED & VERIFIED** | AudioEffect capability probe, DynamicsProcessing backend with brickwall limiter (-1.0 dB threshold), LoudnessEnhancer fallback, route change listener (headset/bluetooth), floating quick-control card with presets (100%, 125%, 150%, 175%, 200%, 250%, 300%), high gain distortion warnings, honest HAL restriction reporting. |
| **Phase 6** | **ExoBoost Style Engine & GPU Color Grading** | **COMPLETED & VERIFIED** | GPU shader processing pipeline (`ShaderProcessor` GLSL + ColorMatrix), 11 grading presets (Original, AI, Outdoor, Cinema, Retro, B&W, Vivid, Warm, Cool, Night, Custom), 2-column preset grid with rendered thumbnails, live test canvas preview, custom parameter sliders. |
| **Phase 7** | **Experimental Live Video Style Filter (MediaProjection)** | **COMPLETED & VERIFIED** | Real-time GPU style processing pipeline for external video apps via MediaProjection, draggable live PIP viewport (`LiveStylePreviewOverlay`), live FPS/latency HUD, frame-dropping buffer protection, DRM blackout detection, and one-tap clean teardown. |
| **Phase 8** | **Per-App Profiles & Automation** | **COMPLETED & VERIFIED** | JSON DataStore per-app profile manager (`AppProfileManager`), UsageEvents top app detector (`ForegroundAppDetector`), Apps Management screen with search and category filters, modal profile editor with tool toggles, default style & volume boost selectors, and automatic handle hiding on disabled apps. |
| **Phase 9** | **Customizable Toolbox & Grid Layouts** | **COMPLETED & VERIFIED** | Comprehensive `.gitignore` for GitHub; 12 tool actions in `ToolRegistry`; `ToolboxEditorScreen` for visual reordering (Move Up/Down), adding, and removing tools; multi-layout support (4-Column, 3-Column, 2-Column, Compact); dynamic overlay rebuilding without hardcoded buttons; DataStore persistence. |

---

## 3. Verified Phase 9 Capabilities
- **Comprehensive `.gitignore`**:
  - Excludes `.gradle/`, `build/`, `app/build/`, `local.properties`, `.idea/`, `.cxx/`, and OS temp files for clean git pushing.
- **12 Configurable Tool Actions (`ToolRegistry`)**:
  - `Screenshot`, `Record`, `Screen Off`, `Volume Boost`, `Style`, `Audio`, `Brightness`, `Volume`, `Orientation`, `Cast`, `Timer`, `Settings`.
- **Toolbox Visual Editor (`ToolboxEditorScreen`)**:
  - **Layout Mode Switcher**: Seamlessly toggles between *4-Column Grid*, *3-Column Grid*, *2-Column Grid*, and *Compact Strip*.
  - **Active Tools Manager**: Displays active tool count with Move Up `[ ▲ ]`, Move Down `[ ▼ ]`, and Remove `[ ✕ ]` buttons.
  - **Available Tools Drawer**: Allows one-tap `[ + Add ]` of unpinned tools.
  - **Duplicate Prevention & Limits**: Automatically prevents duplicate tool entries.
- **Dynamic Floating Overlay Rebuilding**:
  - `FloatingToolboxView` automatically reads active tool ordering and layout type from DataStore preferences (with per-app filtering support).
  - No hardcoded buttons in the overlay UI.
