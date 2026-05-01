package com.cohort

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class RecentStudyCard(
    val createdAt: String,
    val generationSource: String,
    val sourceUrl: String,
    val tldr: String,
    val studyDesign: String,
    val isSaved: Boolean,
    val doi: String?,
    val title: String?,
)

@Serializable
data class SearchHistoryEntry(
    val queryText: String,
    val page: Int,
    val perPage: Int,
    val totalCount: Int?,
    val executedAt: String,
)

object HistoryQueries {

    suspend fun getRecentStudyCards(): List<RecentStudyCard> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT
                sc.created_at,
                sc.generation_source,
                sc.source_url,
                sc.tldr,
                sc.study_design,
                sc.is_saved,
                p.doi,
                p.title
            FROM study_cards sc
            LEFT JOIN papers p ON p.id = sc.paper_id
            ORDER BY sc.created_at DESC
            LIMIT 20
        """.trimIndent()

        Database.connect().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                val results = mutableListOf<RecentStudyCard>()
                while (rs.next()) {
                    results += RecentStudyCard(
                        createdAt = rs.getTimestamp("created_at").toInstant().toString(),
                        generationSource = rs.getString("generation_source"),
                        sourceUrl = rs.getString("source_url"),
                        tldr = rs.getString("tldr"),
                        studyDesign = rs.getString("study_design"),
                        isSaved = rs.getBoolean("is_saved"),
                        doi = rs.getString("doi"),
                        title = rs.getString("title"),
                    )
                }
                results
            }
        }
    }

    suspend fun getSavedStudyCards(): List<RecentStudyCard> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT DISTINCT ON (sc.source_url)
                sc.created_at,
                sc.generation_source,
                sc.source_url,
                sc.tldr,
                sc.study_design,
                sc.is_saved,
                p.doi,
                p.title
            FROM study_cards sc
            LEFT JOIN papers p ON p.id = sc.paper_id
            WHERE sc.is_saved = TRUE
            ORDER BY sc.source_url, sc.created_at DESC
        """.trimIndent()

        Database.connect().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                val results = mutableListOf<RecentStudyCard>()
                while (rs.next()) {
                    results += RecentStudyCard(
                        createdAt = rs.getTimestamp("created_at").toInstant().toString(),
                        generationSource = rs.getString("generation_source"),
                        sourceUrl = rs.getString("source_url"),
                        tldr = rs.getString("tldr"),
                        studyDesign = rs.getString("study_design"),
                        isSaved = rs.getBoolean("is_saved"),
                        doi = rs.getString("doi"),
                        title = rs.getString("title"),
                    )
                }
                results.sortedByDescending { it.createdAt }
            }
        }
    }

    suspend fun getSearchHistory(): List<SearchHistoryEntry> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT query_text, page, per_page, total_count, executed_at
            FROM search_queries
            ORDER BY executed_at DESC
            LIMIT 20
        """.trimIndent()

        Database.connect().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                val results = mutableListOf<SearchHistoryEntry>()
                while (rs.next()) {
                    results += SearchHistoryEntry(
                        queryText = rs.getString("query_text"),
                        page = rs.getInt("page"),
                        perPage = rs.getInt("per_page"),
                        totalCount = rs.getInt("total_count").takeIf { !rs.wasNull() },
                        executedAt = rs.getTimestamp("executed_at").toInstant().toString(),
                    )
                }
                results
            }
        }
    }
}
