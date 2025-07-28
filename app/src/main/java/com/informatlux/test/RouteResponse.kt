package com.informatlux.test.network

data class RouteResponse(
    val routes: List<Route>
)

data class Route(
    val geometry: String,    // polyline
    val duration: Double     // seconds
)
