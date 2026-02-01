# GitClock - GitHub Contribution Display for Android

This is a **dedicated dashboard application** that repurposes your Android device into an always-on GitHub contribution tracker. It uses the GitHub GraphQL API to fetch your real-time stats and visualizes them in a beautiful, OLED-friendly interface, providing a premium desk accessory experience without requiring a dedicated complex hardware build.

<div align="center">
  <img src="docs/screenshots/dashboard.png" alt="GitClock Dashboard" style="width:100%; max-width:800px;">
  <br>
  <p><em>GitClock running in fullscreen Landscape Mode.</em></p>
</div>

**Designed for**: Secondary/Old Android Phones (OLED screens recommended)
**Compatibility**: Android 12L and newer

> **My testing configuration**: Sony Xperia 5 II  • Android 15 • OLED 2560x1080

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Setup](#setup)
  - [Installation](#installation)
  - [Initial Configuration](#initial-configuration)
  - [Web Editor (Draggable UI)](#web-editor)
- [Architecture Explained](#architecture-explained)
  - [The Web Server](#the-web-server)
  - [Data Fetching Strategy](#data-fetching-strategy)
  - [OLED Protection](#oled-protection)
- [Known Issues](#known-issues)

## Overview

Main app functionality automates the process of:
1.  **Keeping the screen alive** efficiently (WakeLocks).
2.  **Fetching data** from GitHub (Contributions, Issues, PRs) every minute.
3.  **Rendering** a pixel-perfect contribution graph using Jetpack Compose.
4.  **(BETA) Hosting a local Web Server** to allow remote layout configuration from your PC.

The result is a seamless, set-and-forget desk clock that keeps you motivated to code.

## Prerequisites

-   **Hardware**: Android Phone or Tablet (OLED screen preferred for "True Black" mode).
-   **Software**:
    -   Android 12L or higher (due to Material You theming).
    -   (Optional) GitHub Classic Token for private repository tracking.
-   **Network**: Active WiFi connection.

## Setup

### Installation

You can grab the latest APK from the [Releases](https://github.com/terminal-index/gitclock/releases) page.

**Build from Source:**
```bash
# Clone the repo
git clone https://github.com/terminal-index/gitclock.git

# Open in Android Studio and hit Run (Shift + F10)
```

### Initial Configuration

1.  **Launch** the app.
2.  **Permissions**: Grant Notification permissions (required for foreground service to keep app alive).
3.  **Auth**:
    -   Enter your **GitHub Username**.
    -   (Optional) Enter **OAuth Token** if you want to see stats from private repos.

### Web Editor

GitClock features a unique **Remote Layout Editor**. Instead of fumbling with touch controls on a small screen, you customize the UI from your desktop.

<div align="center">
  <img src="docs/screenshots/webeditor.png" alt="GitClocks Web Editor Interface" style="width:100%; max-width:600px;">
</div>

1.  Go to **Settings** (Gear Icon) -> Enable **Remote Server**.
2.  Note the IP address displayed (e.g., `192.168.1.15:8080`).
3.  Open that URL in your PC browser.
4.  **Drag and Drop** components to rearrange them. Changes appear instantly on the phone!

## Architecture Explained

### The Web Server

Feature in early BETA Stage. Do not expect it to work fine. The app runs an embedded **Ktor** server (`WebServer.kt`) on port 8080.
-   It serves a static HTML/JS frontend to your browser.
-   It uses a WebSocket or REST endpoints to sync coordinates between the browser canvas and the Android Jetpack Compose state.
-   **Why?** Because editing complex layouts on a touch screen is annoying. Mouse control is precise.

### Data Fetching Strategy

We use a hybrid approach in `GitClockRepository.kt`:
1.  **Public Data**: Fetches from `github-contributions-api.jogruber.de` (fast, no token needed).
2.  **Private Data**: If a token is provided, we switch to **GitHub GraphQL API** (`viewer { contributionsCollection ... }`) to get accurate counts including private commits.

### OLED Protection

To prevent burn-in on OLED screens:
-   **True Black Background**: Pixels are turned off (`#000000`).
-   **Pixel Shift**: (Planned/WIP) Elements slightly nudge their position every few minutes to prevent static image retention.

## Known Issues

### ⚠️ 1. App Killed by System (Samsung/Xiaomi/Huawei)

**Issue**: The clock works for a few hours and then the app closes.

**Symptoms**:
-   You look at the phone and it's on the home screen.
-   Stats stopped updating.

**Root Cause**: Aggressive battery management in custom Android skins kills background/foreground services to "save battery".

**Workaround**:
-   Lock the app in the "Recents" view.
-   Go to Settings -> Battery -> Exclude GitClock from optimizations.
-   Check [dontkillmyapp.com](https://dontkillmyapp.com) for your specific device.

### ⚠️ 2. GitHub Rate Limiting

**Issue**: Data stops updating, graphs disappear.

**Symptoms**:
-   Graph becomes empty.
-   Error logs show HTTP 403.

**Root Cause**: Unauthenticated requests to GitHub are limited to 60 per hour. If you have many widgets or refresh too often, you hit the cap.

**Solution**: Add a **Personal Access Token** in settings. This increases your limit to 5,000 requests per hour.

---

## Contributing

If you discover improvements or solutions to known issues, please **open an issue** to discuss them or submit a Pull Request! All contributions are welcome.

## Support My Work

If this project helps you stay productive, consider supporting my work (and energy drink consumption ☕) - especially since I'm a student at **SUT (Silesian University of Technology)**:
-   [GitHub Sponsors](https://github.com/sponsors/terminal-index)

## License

It's MIT! See the `LICENSE` file for details. You can do whatever you want with it, just don't blame me if something breaks. I am not responsible for any damage you may cause to your device. You are on your own. Sorry :C

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=terminal-index/gitclock&type=date&legend=top-left)](https://star-history.com/#terminal-index/gitclock&type=date&legend=top-left)

#### Made with 🤎 in 🇵🇱 by Terminal-Index
