package com.informatlux.test

import com.informatlux.test.network.RouteResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RoutingService {
    // Basic route call with polyline geometry, between start and end coordinates
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @retrofit2.http.Path("coordinates") coordinates: String,  // "lon1,lat1;lon2,lat2"
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline"
    ): retrofit2.Response<RouteResponse>
}

// RouteResponse classes for OSRM JSON

data class RouteResponse(
    val routes: List<Route>
)

data class Route(
    val geometry: String,
    val duration: Double,
    val distance: Double
)

