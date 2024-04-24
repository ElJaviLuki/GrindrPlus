package com.grindrplus.commands

import android.net.Uri
import android.widget.Toast
import com.grindrplus.core.Config
import com.grindrplus.core.ModContext
import okhttp3.OkHttpClient
import org.json.JSONArray

class Location(context: ModContext, recipient: String, sender: String) : CommandModule(context, recipient, sender) {
    @Command(name = "tp", aliases = ["tp"], help = "Teleport to a location")
    fun teleport(args: List<String>) {
        /**
         * This command is also used to toggle the teleportation feature.
         * If the user hasn't provided any arguments, just toggle teleport.
         */
        if (args.isEmpty()) {
            val status = (Config.get("current_location", "") as String).isEmpty()
            if (!status) {
                Config.put("current_location", "")
                return context.showToast(Toast.LENGTH_LONG, "Teleportation disabled")
            }
        }

        /**
         * If the user has provided arguments, try to teleport to the location.
         * We currently support different formats for the location:
         * - "lat, lon" (e.g. "37.7749, -122.4194") for latitude and longitude.
         * - "lat" "lon" (e.g. "37.7749" "-122.4194") for latitude and longitude.
         * - "lat lon" (e.g. "37.7749 -122.4194") for latitude and longitude.
         * - "city, country" (e.g. "San Francisco, United States") for city and country.
         */
        when {
            args.size == 1 && args[0].contains(",") -> {
                val (lat, lon) = args[0].split(",").map { it.toDouble() }
                Config.put("current_location", "$lat,$lon")
                return context.showToast(Toast.LENGTH_LONG, "Teleported to $lat, $lon")
            }
            args.size == 2 && args.all { arg -> arg.toDoubleOrNull() != null } -> {
                val (lat, lon) = args.map { it.toDouble() }
                Config.put("current_location", "$lat,$lon")
                return context.showToast(Toast.LENGTH_LONG, "Teleported to $lat, $lon")
            }
            else -> {
                /**
                 * If we reached this point, the user has provided a name of a city.
                 * In this case, it could be either a saved location or an actual city.
                 */
                val location = context.database.getLocation(args.joinToString(" "))
                if (location != null) {
                    Config.put("current_location", "${location.first},${location.second}")
                    return context.showToast(Toast.LENGTH_LONG, "Teleported to ${location.first}" +
                            ", ${location.second}")
                } else {
                    /**
                     * No valid saved location was found, try to get the actual location.
                     * This is done by using Nominatim's API to get the latitude and longitude.
                     */
                    val location = getLocationFromNominatim(args.joinToString(" "))
                    return if (location != null) {
                        Config.put("current_location", "${location.first},${location.second}")
                        context.showToast(Toast.LENGTH_LONG, "Teleported to ${location.first}" +
                                ", ${location.second}")
                    } else {
                        context.showToast(Toast.LENGTH_LONG, "Location not found")
                    }
                }
            }
        }
    }

    @Command(name = "save", aliases = ["sv"], help = "Save the current location")
    fun save(args: List<String>) {
        val location = (Config.get("current_location", "") as String)
        if (location.isEmpty()) {
            return context.showToast(Toast.LENGTH_LONG,
                "No location to save")
        }

        val name = args.joinToString(" ")
        if (context.database.getLocation(name) != null) {
            context.database.updateLocation(
                name, // Location name
                location.split(",")[0].toDouble(),
                location.split(",")[1].toDouble()
            )
            return context.showToast(Toast.LENGTH_LONG,
                "Successfully updated $name")
        }

        context.database.addLocation(
            name, // Location name
            location.split(",")[0].toDouble(),
            location.split(",")[1].toDouble()
        )

        return context.showToast(Toast.LENGTH_LONG,
            "Location saved as $name")
    }

    @Command(name = "delete", aliases = ["del"], help = "Delete a saved location")
    fun delete(args: List<String>) {
        if (args.isEmpty()) {
            return context.showToast(Toast.LENGTH_LONG,
                "Please provide a location to delete")
        }

        val name = args.joinToString(" ")
        val location = context.database.getLocation(name)
        if (location == null) {
            return context.showToast(Toast.LENGTH_LONG,
                "Location not found")
        }

        context.database.deleteLocation(name)
        return context.showToast(Toast.LENGTH_LONG,
            "Location deleted")
    }

    private fun getLocationFromNominatim(location: String): Pair<Double, Double>? {
        val url = "https://nominatim.openstreetmap.org/search?q=${Uri.encode(location)}&format=json"
        val request = okhttp3.Request.Builder().url(url).build()

        return try {
            OkHttpClient().newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (body.isNullOrEmpty()) return null

                val json = JSONArray(body)
                if (json.length() == 0) return null

                val obj = json.getJSONObject(0)
                val lat = obj.getDouble("lat")
                val lon = obj.getDouble("lon")
                Pair(lat, lon)
            }
        } catch (e: Exception) {
            context.logger.log("Error getting location from Nominatim: ${e.message}")
            null
        }
    }
}