# 🚗 Car Park Tracker (Car Park Finder)

**Car Park Tracker** is an Android application built with **Jetpack Compose** and **Google Maps SDK**. It helps drivers easily save and relocate their parked vehicle. Upon opening, the app displays an interactive map with a custom car marker at your last parked location. With a single tap on **"I’m parked here"**, the app updates your parked spot using your current GPS coordinates and saves it to local storage.

---

## 📱 Screenshots

<p align="center">
  <img src="screenshots/Screenshot 2026-11-23 225144.png" width="42%" alt="Current Location View" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="screenshots/Screenshot 2026-11-23 225020.png" width="42%" alt="Parked Car Location" />
</p>

---

## ✨ Features

- 📍 **One-Tap Parking Location**: Tap **"I’m parked here"** to move the car pin to your current GPS location.
- 🚗 **Custom Car Marker**: Displays a custom car vector icon on the Google Map right where your car is parked.
- 💾 **Automatic Location Persistence**: Restores your parked position automatically after closing or killing the app using `SharedPreferences`.
- 🔐 **SDK-Compliant Location Permissions**: Requests location access with Android-compliant Rationale Dialogs and direct Settings fallback if access is disabled.
- 🗺️ **Interactive Google Maps**: Smooth camera transitions, street-level zoom, and My Location button integration.

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.2
- **UI Framework**: Jetpack Compose (Material 3)
- **Map Services**: Google Maps Compose SDK (`maps-compose:6.4.1`) & Google Play Services Location (`play-services-location:21.3.0`)
- **Storage**: `SharedPreferences`
- **Testing**: JUnit 4 unit tests ([CarLocationStoreTest.kt](app/src/test/java/com/example/carparktracker/CarLocationStoreTest.kt))

---

## 🚀 Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/Asutilla/Car-Park-Finder.git
cd Car-Park-Finder
```

### 2. Add Google Maps API Key
Update your API key in `app/src/main/res/values/google_maps_api.xml`:
```xml
<resources>
    <string name="google_maps_key" translatable="false">YOUR_API_KEY_HERE</string>
</resources>
```

### 3. Build & Run
Open the project in Android Studio and click **Run** (▶).

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
