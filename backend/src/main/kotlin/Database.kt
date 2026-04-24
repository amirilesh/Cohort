package com.cohort

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import java.sql.Connection

object Database {
    private val log = LoggerFactory.getLogger(Database::class.java)
    private val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl         = System.getenv("DB_URL")      ?: "jdbc:postgresql://localhost:5432/cohort"
            username        = System.getenv("DB_USER")     ?: "cohort"
            password        = System.getenv("DB_PASSWORD") ?: "cohort"
            maximumPoolSize = 10
        }
    )

    fun connect(): Connection = dataSource.connection

    fun initSchema() {
        val sql = Database::class.java.getResourceAsStream("/schema.sql")
            ?.bufferedReader()
            ?.readText()
            ?: error("schema.sql not found in resources")

        connect().use { conn ->
            sql.split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { statement ->
                    conn.createStatement().execute(statement)
                }
        }
        log.info("schema initialized")
    }

    fun checkConnection(): String {
        return try {
            connect().use { "up" }
        } catch (_: Exception) {
            "down"
        }
    }
}
