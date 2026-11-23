package com.example.carparktracker

import android.content.SharedPreferences

/**
 * Minimal key/value surface so the parking storage logic can be unit tested
 * without an Android runtime.
 */
interface KeyValueStore {
    fun getString(key: String, defaultValue: String? = null): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
}

/** [KeyValueStore] backed by the activity's private [SharedPreferences]. */
class SharedPreferencesKeyValueStore(
    private val preferences: SharedPreferences
) : KeyValueStore {

    override fun getString(key: String, defaultValue: String?): String? =
        preferences.getString(key, defaultValue)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
}

/** The parked car's coordinates, independent of the maps SDK value type. */
data class CarCoordinates(val latitude: Double, val longitude: Double)

/**
 * Persists the last parked location (and whether the location permission has
 * already been requested) so both survive the app being killed and reopened.
 */
class CarLocationStore(private val store: KeyValueStore) {

    fun save(coordinates: CarCoordinates) {
        store.putString(KEY_LATITUDE, coordinates.latitude.toString())
        store.putString(KEY_LONGITUDE, coordinates.longitude.toString())
    }

    fun restore(): CarCoordinates? {
        val latitude = store.getString(KEY_LATITUDE)?.toDoubleOrNull() ?: return null
        val longitude = store.getString(KEY_LONGITUDE)?.toDoubleOrNull() ?: return null
        if (!latitude.isFinite() || !longitude.isFinite()) return null
        if (latitude !in MIN_LATITUDE..MAX_LATITUDE) return null
        if (longitude !in MIN_LONGITUDE..MAX_LONGITUDE) return null
        return CarCoordinates(latitude, longitude)
    }

    var hasRequestedPermission: Boolean
        get() = store.getBoolean(KEY_PERMISSION_REQUESTED, false)
        set(value) = store.putBoolean(KEY_PERMISSION_REQUESTED, value)

    private companion object {
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_PERMISSION_REQUESTED = "location_permission_requested"
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
    }
}
