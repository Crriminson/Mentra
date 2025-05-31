# 📱 Mentra

---

## 🛑 Problem Statement

Most people lose **6+ hours daily** to unproductive phone use. Existing trackers show *how much* time is spent but not *why* or *how* to reclaim it. Mentra combines passive tracking, simple nudges, and free learning suggestions so users can turn distraction into growth.

---

## 💡 Solution Overview

- **Passive Tracking**: Logs app sessions (app name, start/end).  
- **Automatic Tagging**: Marks sessions as **Productive** or **Distracting** (no manual customization).  
- **Nudge Engine**: Sends a notification when a user exceeds time limits on distracting apps (preset values).  
- **Skill Assistant**: Fetches and ranks free YouTube tutorials (via YouTube Data API) for fixed interest categories—no custom tags.

---

## ✨ Features

- **Usage Logging**  
  - Records start/end timestamps for every app.  
  - Built-in “Productive” vs. “Distracting” tags (no user edits).

- **Nudges**  
  - Preset daily limits for distracting apps.  
  - Push and local notifications when limits are reached.

- **Insights Dashboard**  
  - Simple animated charts (daily/weekly trends) using **fl_chart**.  
  - Heatmap showing peak distraction hours.

- **Skill Exploration**  
  - Fixed set of interest categories (e.g., “Programming,” “Music”).  
  - Retrieves top free tutorials from YouTube.  
  - Displays thumbnail, title, channel, and duration.

---

## 🛠 Tech Stack

- **Frontend**: Flutter (Dart), [fl_chart](https://pub.dev/packages/fl_chart)  
- **Backend**: Firebase Firestore (logs, preferences), Firebase Authentication, Firebase Cloud Messaging  
- **Notifications**: Flutter Local Notifications  
- **Skill Content**: YouTube Data API v3  

---

## 📸 Screenshots

> _(Add screenshots in `screenshots/`)_  
- **Home Dashboard**: `screenshots/home_dashboard.png`  
- **Usage Trends**: `screenshots/usage_charts.png`  
- **Nudge Example**: `screenshots/nudge_notification.png`  
- **Skill Cards**: `screenshots/skill_cards.png`  

---

## 🚀 Run Instructions

1. **Download the Beta APK**  
   - Get the v1.0-beta APK from GitHub Releases:  
     [Mentra v1.0-beta APK](https://github.com/Crriminson/Mentra/releases/tag/v1.0-beta)  
   - Install it on your Android device to try Mentra immediately.
