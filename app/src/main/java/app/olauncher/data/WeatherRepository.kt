package app.olauncher.data

import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.TimeZone

internal const val WEATHER_REFRESH_INTERVAL = 2 * 60 * 60 * 1_000L

internal data class WeatherReading(val weather: Weather, val fetchedAt: Long) {
    fun refreshDelay(now: Long): Long =
        (listOf(fetchedAt + WEATHER_REFRESH_INTERVAL, weather.currentValidTo, weather.adviceValidTo)
            .filter { it > 0 }.min() - now).coerceIn(0L, WEATHER_REFRESH_INTERVAL)

    companion object {
        fun fromCache(json: JSONObject, config: WeatherConfig, zone: String): WeatherReading? {
            if (json.getDouble("latitude") != config.latitude ||
                json.getDouble("longitude") != config.longitude ||
                json.getString("zone") != zone
            ) return null
            return WeatherReading(Weather.parse(json.getJSONObject("response")), json.getLong("fetchedAt"))
        }
    }
}

// Keep just the latest response in noBackupFilesDir: coordinates must not enter Android backups.
internal class WeatherRepository(directory: File) {
    private val cache = AtomicFile(File(directory, "weather.json"))

    suspend fun load(config: WeatherConfig): WeatherReading? = withContext(Dispatchers.IO) {
        try {
            val json = synchronized(cacheLock) {
                cache.openRead().bufferedReader().use { JSONObject(it.readText()) }
            }
            WeatherReading.fromCache(json, config, TimeZone.getDefault().id)
        } catch (_: IOException) {
            null
        } catch (_: JSONException) {
            null
        }
    }

    suspend fun fetch(config: WeatherConfig): WeatherReading = withContext(Dispatchers.IO) {
        val response = WeatherApi.fetchJson(config)
        val reading = WeatherReading(Weather.parse(response), System.currentTimeMillis())
        ensureActive()
        val json = JSONObject()
            .put("latitude", config.latitude)
            .put("longitude", config.longitude)
            .put("zone", TimeZone.getDefault().id)
            .put("fetchedAt", reading.fetchedAt)
            .put("response", response)
        try {
            synchronized(cacheLock) {
                val stream = cache.startWrite()
                try {
                    stream.write(json.toString().toByteArray(Charsets.UTF_8))
                    cache.finishWrite(stream)
                } catch (e: IOException) {
                    cache.failWrite(stream)
                    throw e
                }
            }
        } catch (e: IOException) {
            // A storage failure must not discard a successful network response.
            Log.w("WeatherRepository", "Could not persist weather", e)
        }
        reading
    }

    private companion object {
        // A cancelled fetch can still finish disk I/O while a new fragment starts loading.
        val cacheLock = Any()
    }
}
