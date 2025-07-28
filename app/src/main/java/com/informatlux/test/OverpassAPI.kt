package com.informatlux.test

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OverpassAPI {
    @Headers("Content-Type: application/json")
    @POST("interpreter")
    suspend fun getRecyclingFacilities(@Body query: OverpassRequest): Response<String>
}
