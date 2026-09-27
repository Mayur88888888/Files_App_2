# ES File Explorer for Android

A modern, high-performance rewrite of the classic **ES File Explorer** (`com.estrongs.android.pop`) built for Android using Kotlin and Jetpack Compose with Material 3 design.

## Features

- **Dashboard & Storage Overview:** Real-time internal storage and SD card usage meters, capacity progress indicators, and fast navigation.
- **Hierarchical File Explorer:**
  - Full tree folder browsing with breadcrumb navigation.
  - Multi-view options (Grid view & detailed List view).
  - Sorting by Name (A–Z, Z–A), Date (Newest, Oldest), and Size (Ascending, Descending).
  - Search engine for real-time recursive file and folder discovery.
  - Show / hide hidden dotfiles.
- **File Management & Operations:**
  - Create new folders and text files.
  - Rename, copy, cut, and paste with persistent clipboard bar.
  - Multi-file selection mode for batch deletion, copying, cutting, and archiving.
  - File properties and metadata inspection (file size, paths, child items, permissions, last modified dates).
- **Integrated Viewers & Tools:**
  - **Text Editor:** Built-in plain text, JSON, Markdown, and configuration editor with save capabilities.
  - **Image Viewer:** Full-screen photo previewer with clean UI overlay.
  - **Archive Tool:** Built-in ZIP creator and ZIP viewer with one-tap extraction.
- **Space Cleaner:**
  - Scans storage for cache junk, temporary log files, empty directories, obsolete APKs, and large files (>10MB).
  - Customizable cleanup options with one-tap disk space recovery.
- **App Manager:**
  - Inspect installed applications and system packages.
  - One-tap APK backup to the `ESBackups/` directory.
  - Direct application launch and system settings access.
- **Recycle Bin:**
  - Safe file deletion preserving original path metadata.
  - Instant file restoration and permanent wipe.
- **Storage Analyzer:**
  - Visual disk usage distribution chart across Images, Videos, Audio, Documents, APKs, and Archives.

## Tech Stack & Architecture

- **Language:** Kotlin 2.2.10
- **UI Framework:** Jetpack Compose with Material Design 3 (M3)
- **Build System:** Gradle 9.3.1 with Android Gradle Plugin (AGP) 9.1.1
- **Target SDK:** Android 36 (minSdk 26)
- **Image Loading:** Coil Compose
- **Architecture:** Clean MVVM with Coroutines and StateFlow
