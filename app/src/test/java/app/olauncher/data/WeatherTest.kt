package app.olauncher.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WeatherTest {
    @Test fun validatesCoordinatesAndHeaderKey() {
        assertNotNull(WeatherConfig.parse(" key ", "-90", "180"))
        for ((lat, lon) in listOf("91" to "0", "0" to "-181", "NaN" to "0", "0" to "Infinity", "" to "0")) {
            assertNull(WeatherConfig.parse("key", lat, lon))
        }
        assertNull(WeatherConfig.parse("", "60", "24"))
        assertNull(WeatherConfig.parse("bad\nkey", "60", "24"))
    }

    @Test fun parsesCurrentAndRainAdviceWithIndependentExpiry() {
        val weather = Weather.parse(JSONObject("""
            {
              "current": {"icon": "rain", "temp": 8.5, "mmPerH": 0.2, "validTo": "2026-05-01T10:05:00Z"},
              "walk": {"raining": true, "advice": "Raining now.", "validTo": "2026-05-01T10:02:00.000Z"},
              "partial": false
            }
        """))
        assertEquals("rain", weather.icon)
        assertEquals(8.5, weather.temperature!!, 0.0)
        assertEquals(0.2, weather.rainRate!!, 0.0)
        assertEquals(true, weather.raining)
        assertNull(weather.dryDeadline(Weather.timestamp("2026-05-01T10:00:00Z")))
        val now = Weather.timestamp("2026-05-01T10:02:00Z")
        assertTrue(weather.currentIsFresh(now))
        assertFalse(weather.adviceIsFresh(now))
        assertFalse(weather.currentIsFresh(Weather.timestamp("2026-05-01T10:05:00Z")))
    }

    @Test fun missingRainNeverBecomesDryAndInvalidExpiryIsStale() {
        val weather = Weather.parse(JSONObject("""
            {
              "current": {"temp": null, "mmPerH": null, "validTo": null},
              "walk": {"raining": null, "advice": "Dry", "dryUntil": "2026-05-02T10:00:00Z", "validTo": "invalid"},
              "partial": true
            }
        """))
        assertEquals("unknown", weather.icon)
        assertNull(weather.temperature)
        assertNull(weather.rainRate)
        assertNull(weather.raining)
        assertNull(weather.dryDeadline(1))
        assertFalse(weather.currentIsFresh(1))
        assertFalse(weather.adviceIsFresh(1))
        assertTrue(weather.partial)
    }

    @Test fun dryDeadlineRequiresKnownDryRainAndUnexpiredAdvice() {
        val weather = Weather.parse(JSONObject("""
            {
              "current": {"temp": 8.5, "validTo": "2026-05-01T10:05:00Z"},
              "walk": {"raining": false, "dryUntil": "2026-05-02T14:00:00Z", "validTo": "2026-05-01T10:02:00Z"},
              "partial": false
            }
        """))
        val now = Weather.timestamp("2026-05-01T10:00:00Z")
        assertEquals(Weather.timestamp("2026-05-02T14:00:00Z"), weather.dryDeadline(now))
        assertNull(weather.dryDeadline(weather.adviceValidTo))
        assertNull(weather.copy(raining = null).dryDeadline(now))
        assertNull(weather.copy(raining = true).dryDeadline(now))
        assertNull(weather.copy(dryUntil = now).dryDeadline(now))
        assertNull(weather.copy(dryUntil = 0L).dryDeadline(now))
    }

    @Test fun acceptsUtcOffsetsAndRejectsInvalidTimestamps() {
        assertEquals(
            Weather.timestamp("2026-05-01T10:00:00Z"),
            Weather.timestamp("2026-05-01T13:00:00+03:00")
        )
        for (invalid in listOf("", "null", "2026-02-30T10:00:00Z", "2026-05-01T10:00:00Zjunk")) {
            assertEquals(0L, Weather.timestamp(invalid))
        }
    }
}
