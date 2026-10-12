package app.olauncher.ui

import app.olauncher.R
import java.util.Locale

internal fun weatherIcon(icon: String): Pair<Int, Int>? {
    val symbol = icon.lowercase(Locale.ROOT).replace("-", "").replace("_", "")
    val night = symbol.endsWith("night") || symbol.endsWith("polartwilight")
    return when {
        "thunder" in symbol -> R.drawable.ic_weather_thunderstorm to R.string.weather_condition_thunder
        "snow" in symbol || "sleet" in symbol -> R.drawable.ic_weather_snowy to R.string.weather_condition_snow
        "rain" in symbol || "drizzle" in symbol -> R.drawable.ic_weather_rainy to R.string.weather_condition_rain
        "fog" in symbol || "mist" in symbol -> R.drawable.ic_weather_foggy to R.string.weather_condition_fog
        "partlycloudy" in symbol || symbol.startsWith("fair") ->
            (if (night) R.drawable.ic_weather_partly_cloudy_night else R.drawable.ic_weather_partly_cloudy_day) to
                R.string.weather_condition_partly_cloudy
        symbol.startsWith("clear") || symbol.startsWith("sunny") ->
            (if (night) R.drawable.ic_weather_bedtime else R.drawable.ic_weather_sunny) to R.string.weather_condition_clear
        symbol.startsWith("cloud") || symbol.startsWith("overcast") ->
            R.drawable.ic_weather_cloud to R.string.weather_condition_cloudy
        else -> null
    }
}
