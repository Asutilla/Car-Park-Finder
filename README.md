# 🚗 Car Park Tracker

**Car Park Tracker** is a modern Android application built with Jetpack Compose and Google Maps SDK. It helps users easily store and relocate their last parked vehicle position. When launched, the app restores the saved parking location on an interactive map and allows users to update their parked position with a single tap.

---

## 📱 Screenshots

<p align="center">
  <img src="screenshots/Screenshot 2026-11-23 225144.png" width="42%" alt="Current Location View" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="screenshots/Screenshot 2026-11-23 225020.png" width="42%" alt="Parked Car Location" />
</p>

> *(Replace `screenshots/screenshot1.png` and `screenshots/screenshot2.png` with your 2 screenshot image files in the `screenshots/` directory)*

---

## ✨ Features

- 📍 **One-Tap Parking Spot Saving**: Tap **"I’m parked here"** to update your car's location to your current GPS coordinates.
- 🚗 **Custom Car Marker**: Displays a custom car icon on the map exactly where your car is parked.
- 💾 **Persistent Location Storage**: Automatically restores your car's location from `SharedPreferences` when you reopen the app after killing it.
- 🔐 **Smart Permission Management**: Handles location permissions with SDK-compliant Rationale Dialogs and direct links to App Settings if permissions are blocked.
- 🗺️ **Interactive Google Maps**: Built using Google Maps Compose SDK with camera zoom animations and location tracking.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/) (2.2.10)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Map & Location Services**: 
  - [Google Maps Compose SDK](https://github.com/googlemaps/android-maps-compose) (`maps-compose:6.4.1`)
  - [FusedLocationProviderClient](https://developer.android.com/training/location) (`play-services-location:21.3.0`)
- **Persistence**: `SharedPreferences` with custom state store abstraction ([CarLocationStore.kt](app/src/main/java/com/example/carparktracker/CarLocationStore.kt))
- **Unit Testing**: JUnit unit test suite verifying persistence and bounds validation ([CarLocationStoreTest.kt](app/src/test/java/com/example/carparktracker/CarLocationStoreTest.kt))

---

## 🚀 Getting Started

### Prerequisites
- Android Studio 2024.1+ (or Android Studio Ladybug/newer)
- JDK 11 or higher
- Android Device or Emulator running Android 7.0 (API level 24) or higher

### Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/CarParkTracker.git
   cd CarParkTracker
   ```

2. **Configure Google Maps API Key**:
   Obtain a Maps API key from the [Google Cloud Console](https://console.cloud.google.com/) and update `app/src/main/res/values/google_maps_api.xml`:
   ```xml
   <resources>
       <string name="google_maps_key" translatable="false">YOUR_GOOGLE_MAPS_API_KEY</string>
   </resources>
   ```

3. **Build & Run**:
   Open the project in Android Studio and click **Run** (▶) on your device or emulator.

---

## 🧪 Running Unit Tests

Run unit tests directly from the command line:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
