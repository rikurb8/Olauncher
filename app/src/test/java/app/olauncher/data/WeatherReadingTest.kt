package app.olauncher.data

import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WeatherReadingTest {
    private val config = WeatherConfig("secret", 60.0, 24.0)
    private val now = Weather.timestamp("2026-05-01T10:00:00Z")

    private fun snapshot(): JSONObject = JSONObject("""
        {
          "latitude": 60.0,
          "longitude": 24.0,
          "zone": "Europe/Helsinki",
          "fetchedAt": $now,
          "response": {
            "current": {"temp": 8.5, "mmPerH": 0.2, "validTo": "2026-05-01T11:00:00Z"},
            "walk": {"raining": false, "dryUntil": "2026-05-01T10:30:00Z", "validTo": "2026-05-01T10:02:00Z"},
            "partial": false
          }
        }
    """)

    private fun reading() = WeatherReading.fromCache(snapshot(), config, "Europe/Helsinki")!!

    @Test fun restoresReadingsAndTheirOriginalExpiry() {
        val restored = reading()
        assertEquals(now, restored.fetchedAt)
        assertEquals(8.5, restored.weather.temperature!!, 0.0)
        assertEquals(0.2, restored.weather.rainRate!!, 0.0)
        assertEquals(false, restored.weather.raining)
        assertFalse(restored.weather.partial)
        assertTrue(restored.weather.currentIsFresh(now))
        assertFalse(restored.weather.currentIsFresh(now + 60 * 60 * 1_000L))
    }

    @Test fun rejectsOtherLocationsAndTimeZones() {
        assertNull(WeatherReading.fromCache(snapshot(), config.copy(latitude = 61.0), "Europe/Helsinki"))
        assertNull(WeatherReading.fromCache(snapshot(), config.copy(longitude = 25.0), "Europe/Helsinki"))
        assertNull(WeatherReading.fromCache(snapshot(), config, "UTC"))
        // The API key is neither needed nor duplicated in the response cache.
        assertNotNull(WeatherReading.fromCache(snapshot(), config.copy(apiKey = "new"), "Europe/Helsinki"))
    }

    @Test(expected = JSONException::class) fun malformedCacheCannotBecomeAReading() {
        WeatherReading.fromCache(snapshot().put("response", JSONObject()), config, "Europe/Helsinki")
    }

    @Test fun refreshesAtEarliestExpiryAndImmediatelyWhenCacheIsStale() {
        val reading = reading()
        assertEquals(2 * 60 * 1_000L, reading.refreshDelay(now))
        assertEquals(0L, reading.refreshDelay(now + 2 * 60 * 1_000L))
        assertEquals(0L, reading.refreshDelay(now + 3 * 60 * 1_000L))
    }

    @Test fun capsRefreshAtTwoHoursAndIgnoresMissingExpiry() {
        val reading = reading()
        val noExpiry = reading.copy(weather = reading.weather.copy(currentValidTo = 0L, adviceValidTo = 0L))
        assertEquals(WEATHER_REFRESH_INTERVAL, noExpiry.refreshDelay(now))
        assertEquals(WEATHER_REFRESH_INTERVAL - 1_000L, noExpiry.refreshDelay(now + 1_000L))
        assertEquals(0L, noExpiry.refreshDelay(now + WEATHER_REFRESH_INTERVAL))
        assertEquals(WEATHER_REFRESH_INTERVAL, noExpiry.refreshDelay(now - WEATHER_REFRESH_INTERVAL))
        val farExpiry = noExpiry.copy(weather = noExpiry.weather.copy(currentValidTo = now + 10 * WEATHER_REFRESH_INTERVAL))
        assertEquals(WEATHER_REFRESH_INTERVAL, farExpiry.refreshDelay(now))
    }
}
