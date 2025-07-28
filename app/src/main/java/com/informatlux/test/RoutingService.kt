package com.informatlux.test

import com.informatlux.test.network.RouteResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RoutingService {
    /**
     * coordString is "lon1,lat1;lon2,lat2"
     * overview=full to get the entire route geometry
     * geometries=polyline6 for encoded polyline. You can also use "polyline" or "geojson".
     */
    @GET("route/v1/driving/{coords}")
    suspend fun getRoute(
        @retrofit2.http.Path("coords") coords: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geoms: String = "polyline"
    ): Response<RouteResponse>
}
