package com.informatlux.test


// These data classes are the "blueprints" for the JSON response.
// Gson will use them to automatically parse the data.

data class PhotonResponse(
    val features: List<PhotonFeature>
)

data class PhotonFeature(
    val geometry: Geometry,
    val properties: Map<String, Any>
)

data class Geometry(
    val coordinates: List<Double>
)