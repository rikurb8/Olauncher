package app.olauncher.ui

import app.olauncher.R
import org.junit.Assert.*
import org.junit.Test

class WeatherIconTest {
    @Test fun mapsMetAndNormalizedWeatherKeys() {
        val examples = mapOf(
            "clearsky_day" to R.drawable.ic_weather_sunny,
            "clear-day" to R.drawable.ic_weather_sunny,
            "clearsky_night" to R.drawable.ic_weather_bedtime,
            "clear-night" to R.drawable.ic_weather_bedtime,
            "clearsky_polartwilight" to R.drawable.ic_weather_bedtime,
            "fair_day" to R.drawable.ic_weather_partly_cloudy_day,
            "partlycloudy_night" to R.drawable.ic_weather_partly_cloudy_night,
            "partly-cloudy-day" to R.drawable.ic_weather_partly_cloudy_day,
            "CLOUDY" to R.drawable.ic_weather_cloud,
            "overcast-night" to R.drawable.ic_weather_cloud,
            "heavyrainshowers_day" to R.drawable.ic_weather_rainy,
            "drizzle-night" to R.drawable.ic_weather_rainy,
            "lightsnowshowers_night" to R.drawable.ic_weather_snowy,
            "sleet-day" to R.drawable.ic_weather_snowy,
            "lightrainshowersandthunder_day" to R.drawable.ic_weather_thunderstorm,
            "snowandthunder" to R.drawable.ic_weather_thunderstorm,
            "fog" to R.drawable.ic_weather_foggy,
            "mist-night" to R.drawable.ic_weather_foggy,
        )
        for ((symbol, drawable) in examples) {
            assertEquals(symbol, drawable, weatherIcon(symbol)?.first)
        }
        assertEquals(R.string.weather_condition_cloudy, weatherIcon("cloudy")?.second)
    }

    @Test fun hidesUnknownConditionsRatherThanGuessing() {
        for (symbol in listOf("", "unknown", "null", "unmapped")) {
            assertNull(symbol, weatherIcon(symbol))
        }
    }
}
