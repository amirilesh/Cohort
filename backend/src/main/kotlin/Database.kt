package com.cohort

import java.sql.Connection
import java.sql.DriverManager

object Database {
    private val url      = System.getenv("DB_URL")      ?: "jdbc:postgresql://localhost:5432/cohort"
    private val user     = System.getenv("DB_USER")     ?: "cohort"
    private val password = System.getenv("DB_PASSWORD") ?: "cohort"

    fun connect(): Connection = DriverManager.getConnection(url, user, password)

    fun checkConnection(): String {
        return try {
            connect().use { "up" }
        } catch (_: Exception) {
            "down"
        }
    }
}
