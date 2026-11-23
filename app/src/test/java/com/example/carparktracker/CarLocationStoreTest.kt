package com.example.carparktracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarLocationStoreTest {

    private class FakeKeyValueStore : KeyValueStore {
        val values = mutableMapOf<String, Any>()

        override fun getString(key: String, defaultValue: String?): String? =
            values[key] as? String ?: defaultValue

        override fun putString(key: String, value: String) {
            values[key] = value
        }

        override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
            values[key] as? Boolean ?: defaultValue

        override fun putBoolean(key: String, value: Boolean) {
            values[key] = value
        }
    }

    @Test
    fun `restore returns null when nothing was parked`() {
        val store = CarLocationStore(FakeKeyValueStore())

        assertNull(store.restore())
    }

    @Test
    fun `saved coordinates survive a restore`() {
        val store = CarLocationStore(FakeKeyValueStore())

        store.save(CarCoordinates(37.4220, -122.0841))

        assertEquals(CarCoordinates(37.4220, -122.0841), store.restore())
    }

    @Test
    fun `saving again replaces the previous parked location`() {
        val store = CarLocationStore(FakeKeyValueStore())

        store.save(CarCoordinates(37.4220, -122.0841))
        store.save(CarCoordinates(48.8584, 2.2945))

        assertEquals(CarCoordinates(48.8584, 2.2945), store.restore())
    }

    @Test
    fun `restore still works after the backing data is reopened`() {
        val backing = FakeKeyValueStore()
        CarLocationStore(backing).save(CarCoordinates(-33.8688, 151.2093))

        val reopened = CarLocationStore(backing)

        assertEquals(CarCoordinates(-33.8688, 151.2093), reopened.restore())
    }

    @Test
    fun `restore returns null when a value is missing`() {
        val backing = FakeKeyValueStore()
        backing.putString("latitude", "37.4220")
        val store = CarLocationStore(backing)

        assertNull(store.restore())
    }

    @Test
    fun `restore returns null when a value is not a number`() {
        val backing = FakeKeyValueStore()
        backing.putString("latitude", "not-a-number")
        backing.putString("longitude", "2.2945")
        val store = CarLocationStore(backing)

        assertNull(store.restore())
    }

    @Test
    fun `restore returns null when coordinates are out of range`() {
        val backing = FakeKeyValueStore()
        backing.putString("latitude", "120.0")
        backing.putString("longitude", "2.2945")
        val store = CarLocationStore(backing)

        assertNull(store.restore())
    }

    @Test
    fun `restore returns null when a value is not finite`() {
        val backing = FakeKeyValueStore()
        backing.putString("latitude", "NaN")
        backing.putString("longitude", "2.2945")
        val store = CarLocationStore(backing)

        assertNull(store.restore())
    }

    @Test
    fun `permission already requested is remembered across instances`() {
        val backing = FakeKeyValueStore()

        assertFalse(CarLocationStore(backing).hasRequestedPermission)

        CarLocationStore(backing).hasRequestedPermission = true

        assertTrue(CarLocationStore(backing).hasRequestedPermission)
    }
}
