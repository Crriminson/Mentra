# Mentra - Digital Wellbeing App

A modular Android application designed to help users manage their smartphone usage through intelligent nudging and behavioral analysis.

## Project Structure

The project is organized into the following modules:

- `:app` - Main application module containing the app entry point and DI setup
- `:telemetry` - Usage data collection and event tracking
- `:analysis` - Time-series analysis and usage pattern detection
- `:model` - On-device ML for state classification using TensorFlow Lite
- `:nudge` - Visual and haptic feedback delivery system
- `:gaze` - Optional gaze tracking using ML Kit Face Detection
- `:ui` - Jetpack Compose UI components and screens

## Tech Stack

- Kotlin
- Jetpack Compose for UI
- TensorFlow Lite for on-device ML
- ML Kit for face detection
- Kotlin Coroutines & Flow
- Material Design 3
- AndroidX libraries

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run the app module

## Development Setup

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 8
- Android SDK 34
- Gradle 8.2

## License

TBD 