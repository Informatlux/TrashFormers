package com.informatlux.test

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object OverpassService {
    private const val BASE_URL = "https://overpass-api.de/api/"

    val api: OverpassAPI by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(OverpassAPI::class.java)
    }
}
