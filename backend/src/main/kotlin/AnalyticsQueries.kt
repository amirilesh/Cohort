package com.cohort

import kotlinx.serialization.Serializable

@Serializable
data class TopSearch(
    val queryText: String,
    val timesUsed: Int,
)

@Serializable
data class PopularPaper(
    val title: String,
    val doi: String?,
    val publicationYear: Int?,
    val timesReturned: Int,
)

object AnalyticsQueries {

    fun getTopSearches(): List<TopSearch> {
        val sql = """
            SELECT query_text, COUNT(*) AS times_used
            FROM search_queries
            GROUP BY query_text
            ORDER BY times_used DESC
            LIMIT 10
        """.trimIndent()

        Database.connect().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                val results = mutableListOf<TopSearch>()
                while (rs.next()) {
                    results += TopSearch(
                        queryText = rs.getString("query_text"),
                        timesUsed = rs.getInt("times_used"),
                    )
                }
                return results
            }
        }
    }

    fun getPopularPapers(): List<PopularPaper> {
        val sql = """
            SELECT p.title, p.doi, p.publication_year, COUNT(*) AS times_returned
            FROM search_results sr
            JOIN papers p ON p.id = sr.paper_id
            GROUP BY p.id, p.title, p.doi, p.publication_year
            ORDER BY times_returned DESC
            LIMIT 10
        """.trimIndent()

        Database.connect().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                val results = mutableListOf<PopularPaper>()
                while (rs.next()) {
                    results += PopularPaper(
                        title = rs.getString("title"),
                        doi = rs.getString("doi"),
                        publicationYear = rs.getInt("publication_year").takeIf { !rs.wasNull() },
                        timesReturned = rs.getInt("times_returned"),
                    )
                }
                return results
            }
        }
    }
}
