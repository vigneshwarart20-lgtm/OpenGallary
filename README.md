# PureGallery — Premium Offline Android Photo & Video Gallery

**PureGallery** is a native, offline-first, advertisement-free Android photo and video gallery built with Kotlin and Jetpack Compose. It is designed to provide a privacy-focused experience comparable to flagship smartphone galleries.

---

## 🌟 Key Features

### 1. 100% Offline & Ad-Free
- **No Internet Permission**: PureGallery declares zero network permissions in its manifest.
- **Zero Ads & Trackers**: No third-party ad networks, no telemetry SDKs, no cloud analytics.
- **Works in Airplane Mode**: Browse, organize, view, edit, and export media without any connection.

### 2. Native MediaStore Indexing
- Reads real device photos and videos directly via Android's `MediaStore` and `ContentResolver`.
- Automatically categorizes media into:
  - **All Photos & All Videos**
  - **Camera** (DCIM / Camera)
  - **Screenshots**
  - **Screen Recordings**
  - **Downloads**
  - **WhatsApp Images & Videos**
  - **Edited Photos** (Pictures/PureGallery)
  - **Favorites** (Persisted locally with Room Database)
  - **Recently Added & Large Files (>50MB)**
  - **Discovered Device Folders**

### 3. Draggable Fast-Scroll with Date Indicators
- Custom draggable scrollbar thumb along the right edge of every media grid (Photos, Videos, Albums, Favorites, and Search).
- Displays a synchronized date bubble (e.g., "October 2026", "Today", "Yesterday") as you drag.
- Smoothly jumps across thousands of images in reverse chronological order.

### 4. Full-Screen Photo & Video Viewer
- **Photos**:
  - Pinch-to-zoom (up to 5x), double-tap zoom toggle, and pan when zoomed.
  - Smooth horizontal swipe transitions between media items.
  - Immersive tap-to-hide UI controls.
- **Videos**:
  - Full-screen playback with hardware-accelerated video decoding.
  - Play, pause, seek bar, time indicator, and media controls.
- **Actions**:
  - Native Android Sharesheet sharing (single or multiple items).
  - Detailed metadata sheet: resolution, file size, date modified, and folder path.
  - Star / unstar favorites.
  - System-mediated deletion with confirmation.

### 5. Professional Non-Destructive Photo Editor
- **Crop & Transform**: Standard aspect ratios (1:1, 4:3, 3:4, 16:9, 9:16, Original), 90° rotation, fine rotation slider (-45° to +45°), horizontal and vertical flips.
- **Light Adjustments**: Brightness, Exposure, Contrast, Highlights, Shadows.
- **Color Controls**: Saturation, Warmth / Temperature, Tint, Vibrance.
- **Built-in Offline Filter Presets**: Original, Vivid, Warm Sunset, Cool Mist, Vintage 70s, Retro, B&W Classic, Dramatic, Soft Glow, Fade Film, Monochrome, High Contrast, Natural Boost, Sepia Tone with adjustable intensity.
- **Effects**: Vignette and Sharpening sliders.
- **Workflow**: Undo/Redo history stack, Reset All, and non-destructive export to `Pictures/PureGallery/`.

### 6. Local Search & Multi-Select Management
- Instant offline search filtering by filename, album folder, date, and media type (Photos, Videos, Favorites).
- Multi-selection mode: batch share via Sharesheet, batch favorite, and batch delete.

---

## 🏗️ Architecture & Technologies

- **UI**: Jetpack Compose, Material Design 3 (M3)
- **Language**: Kotlin 2.2 with Coroutines & StateFlow
- **Image Loading & Cache**: Coil Compose
- **Local Database**: Room 2.7 (with KSP) for persistent favorites
- **Media Engine**: Android `MediaStore` APIs & `ContentResolver`
- **Target SDK**: Android 16 (API 36) / Minimum SDK: Android 7.0 (API 24)

---

## 🚀 Building & Testing

To compile the application in this environment:
```bash
compile_applet
```

To run local JVM unit tests:
```bash
gradle :app:testDebugUnitTest
```
