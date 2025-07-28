package com.informatlux.test

import com.informatlux.test.model.OverpassResponse
import retrofit2.http.*

interface OverpassApiService {
    @FormUrlEncoded
    @POST("interpreter")
    suspend fun getOverpassData(
        @Field("data") query: String
    ): OverpassResponse
}
