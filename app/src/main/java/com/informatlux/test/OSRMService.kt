package com.informatlux.test

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Data classes for the OSRM API response
data class OsrmResponse(val routes: List<OsrmRoute>)
data class OsrmRoute(val duration: Double)

// Retrofit service interface for OSRM
interface OsrmService {
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates") coordinates: String, // e.g., "lon1,lat1;lon2,lat2"
        @Query("overview") overview: String = "false"
    ): Response<OsrmResponse>
}