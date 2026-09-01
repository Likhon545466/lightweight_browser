# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-09-01

### Added
- **Native Ad & Tracker Blocker (`ContentBlocker`)**: Zero-overhead request interceptor that blocks advertising domains, telemetry beacons, and tracking pixels out-of-the-box.
- **Material You Dynamic Theming**: Real-time wallpaper color extraction on Android 12+, with Light, Dark, and true AMOLED Pitch-Black (`#000000`) color palettes.
- **Multi-Profile Isolation**: Partitioned browser workspaces (e.g. Work, Personal, Shopping) with isolated cookies, web cache, bookmarks, and browsing history.
- **Incognito & Ephemeral Browsing**: One-tap private tabs with auto-purging session state on exit.
- **Modern Jetpack Compose UI**: Fast-out-slow-in smooth animations, collapsible address bar, and comprehensive bottom navigation bar.
- **Multi-Engine Search Selector**: Fast switcher between Google, DuckDuckGo, Bing, and Brave Search.
- **Productivity & Navigation Features**:
  - In-page text search with live match navigation (`FindInPageBar`).
  - Desktop site user-agent toggling.
  - Interactive tabs manager with swipe-to-dismiss and grid overview.
  - Privacy Shield sheet displaying real-time blocked ads/trackers counters.
  - Bookmark & History managers with full-text search.
  - Integrated downloads dialog with quick file opening.
- **Persistent Storage**: Room database integration for multi-profile bookmarks, history records, and browser preferences.
- **Automated CI/CD**: GitHub Actions workflows for continuous integration and automated release APK packaging with SHA256 checksums.
