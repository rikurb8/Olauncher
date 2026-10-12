package app.olauncher.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class WeatherConfig(val apiKey: String, val latitude: Double, val longitude: Double) {
    companion object {
        fun parse(key: String, latitude: String, longitude: String): WeatherConfig? {
            val lat = latitude.toDoubleOrNull() ?: return null
            val lon = longitude.toDoubleOrNull() ?: return null
            val trimmedKey = key.trim()
            if (trimmedKey.isEmpty() || trimmedKey.any { it.code !in 33..126 } ||
                !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0
            ) return null
            return WeatherConfig(trimmedKey, lat, lon)
        }
    }
}

data class Weather(
    val icon: String,
    val temperature: Double?,
    val rainRate: Double?,
    val raining: Boolean?,
    val dryUntil: Long,
    val currentValidTo: Long,
    val adviceValidTo: Long,
    val partial: Boolean,
) {
    fun currentIsFresh(now: Long) = now < currentValidTo
    fun adviceIsFresh(now: Long) = now < adviceValidTo
    fun dryDeadline(now: Long): Long? =
        dryUntil.takeIf { raining == false && adviceIsFresh(now) && it > now }

    companion object {
        fun parse(json: JSONObject): Weather {
            val current = json.getJSONObject("current")
            val walk = json.getJSONObject("walk")
            fun number(name: String): Double? =
                current.optDouble(name).takeIf { it.isFinite() }
            return Weather(
                current.optString("icon", "unknown"),
                number("temp"), number("mmPerH"),
                if (walk.isNull("raining")) null else walk.getBoolean("raining"),
                timestamp(walk.optString("dryUntil")),
                timestamp(current.optString("validTo")),
                timestamp(walk.optString("validTo")),
                json.optBoolean("partial", true),
            )
        }

        internal fun timestamp(value: String): Long {
            val normalized = value.replace(Regex("\\.\\d+(?=Z|[+-])"), "")
            val position = ParsePosition(0)
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = format.parse(normalized, position)
            return if (position.index == normalized.length) parsed?.time ?: 0L else 0L
        }
    }
}

class WeatherApiException(val status: Int) : IOException("Weather HTTP $status")

object WeatherApi {
    fun fetch(config: WeatherConfig): Weather {
        val zone = URLEncoder.encode(TimeZone.getDefault().id, "UTF-8")
        val connection = URL(
            "https://weather.goodrnd.xyz/v1/weather?lat=${config.latitude}&lon=${config.longitude}&tz=$zone"
        ).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) throw WeatherApiException(status)
            return connection.inputStream.bufferedReader().use { Weather.parse(JSONObject(it.readText())) }
        } finally {
            connection.disconnect()
        }
    }
}
