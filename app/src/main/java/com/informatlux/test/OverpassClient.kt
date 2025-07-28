package com.informatlux.test

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OverpassClient {
    val api: OverpassApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApiService::class.java)
    }
}
