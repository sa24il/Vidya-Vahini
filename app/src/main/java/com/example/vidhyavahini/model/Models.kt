package com.example.vidhyavahini.model

data class Ping(
    val studentId: String = "",
    val studentName: String = "",
    val stop: String = "",
    val timestamp: Long = 0L
)

data class Route(
    val id: String = "",
    val name: String = "",
    val stops: List<String> = emptyList(),
    val stopTimes: Map<String, Int> = emptyMap()
)
