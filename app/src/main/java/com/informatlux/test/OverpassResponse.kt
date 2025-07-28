package com.informatlux.test.model

data class OverpassResponse(val elements: List<Element>)

data class Element(
    val id: Long,
    val lat: Double?,         // present on nodes
    val lon: Double?,
    val center: Center?,      // present on ways
    val tags: Map<String, String>?
)

data class Center(val lat: Double, val lon: Double)

