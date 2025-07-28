package com.informatlux.test

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// This interface defines the API endpoints for the Photon service.
interface PhotonService {
    @GET("api/")
    suspend fun searchNearby( // Renamed for clarity
        @Query("q") query: String,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        // FIX: The unsupported "radius" parameter has been REMOVED.
        @Query("limit") limit: Int
    ): Response<PhotonResponse>
}

