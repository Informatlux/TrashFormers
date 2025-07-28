package com.informatlux.test

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Data classes for the Nominatim API response
data class NominatimResult(
    val display_name: String,
    val lat: String,
    val lon: String
)

// This is the single, correct definition for your service interface.
interface NominatimService {
    @GET("search")
    suspend fun searchLocation(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1
    ): List<NominatimResult>

    // The companion object creates a singleton instance of the service.
    companion object {
        val api: NominatimService by lazy {
            Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NominatimService::class.java)
        }
    }
}