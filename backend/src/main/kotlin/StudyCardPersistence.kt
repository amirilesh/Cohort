package com.cohort

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

object StudyCardPersistence {
    private val log = LoggerFactory.getLogger(StudyCardPersistence::class.java)

    suspend fun findByDoi(doi: String): StudyCardResponse? = withContext(Dispatchers.IO) {
        val bare = doi.trim()
            .removePrefix("https://doi.org/")
            .removePrefix("http://doi.org/")
            .removePrefix("doi:")
            .trim()
            .lowercase()

        if (bare.isBlank()) return@withContext null

        try {
            Database.connect().use { conn ->
                conn.prepareStatement(
                    """
                    SELECT sc.source_url, sc.tldr, sc.study_design, sc.limitations,
                           sc.key_findings::text AS key_findings, sc.generation_source, sc.is_saved
                    FROM study_cards sc
                    JOIN papers p ON p.id = sc.paper_id
                    WHERE LOWER(p.doi) = ?
                    ORDER BY sc.created_at DESC
                    LIMIT 1
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, "https://doi.org/$bare")
                    val rs = stmt.executeQuery()
                    if (!rs.next()) return@withContext null
                    log.info("study card cache hit doi={}", bare)
                    rowToStudyCard(rs)
                }
            }
        } catch (e: Exception) {
            log.error("failed to look up study card by doi", e)
            null
        }
    }

    suspend fun findByUrl(url: String): StudyCardResponse? = withContext(Dispatchers.IO) {
        try {
            Database.connect().use { conn ->
                conn.prepareStatement(
                    """
                    SELECT source_url, tldr, study_design, limitations,
                           key_findings::text AS key_findings, generation_source, is_saved
                    FROM study_cards
                    WHERE source_url = ?
                    ORDER BY created_at DESC
                    LIMIT 1
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setString(1, url)
                    val rs = stmt.executeQuery()
                    if (!rs.next()) return@withContext null
                    log.info("study card cache hit url={}", url)
                    rowToStudyCard(rs)
                }
            }
        } catch (e: Exception) {
            log.error("failed to look up study card by url", e)
            null
        }
    }

    private fun rowToStudyCard(rs: ResultSet): StudyCardResponse {
        val keyFindings = try {
            Json.decodeFromString<List<String>>(rs.getString("key_findings") ?: "[]")
        } catch (_: Exception) {
            emptyList()
        }
        val generationSource = rs.getString("generation_source")
        val sourceType = when (generationSource) {
            "llm_abstract" -> "ABSTRACT_ONLY"
            else -> "FULL_TEXT"
        }
        return StudyCardResponse(
            url = rs.getString("source_url"),
            success = true,
            source = generationSource,
            sourceType = sourceType,
            tldr = rs.getString("tldr"),
            studyDesign = rs.getString("study_design"),
            keyFindings = keyFindings,
            limitations = rs.getString("limitations"),
            isSaved = rs.getBoolean("is_saved"),
        )
    }

    suspend fun save(card: StudyCardResponse, doi: String?) = withContext(Dispatchers.IO) {
        try {
            Database.connect().use { conn ->
                val paperId = if (doi != null) ensurePaperByDoi(conn, doi) else null
                insertStudyCard(conn, card, paperId)
            }
        } catch (e: Exception) {
            log.error("failed to save study card", e)
        }
    }

    suspend fun markSaved(url: String): Boolean = withContext(Dispatchers.IO) {
        updateSavedState(url = url, isSaved = true)
    }

    suspend fun unmarkSaved(url: String): Boolean = withContext(Dispatchers.IO) {
        updateSavedState(url = url, isSaved = false)
    }

    private fun ensurePaperByDoi(conn: Connection, doi: String): UUID? {
        val bare = doi.trim()
            .removePrefix("https://doi.org/")
            .removePrefix("http://doi.org/")
            .removePrefix("doi:")
            .trim()
            .lowercase()

        if (bare.isBlank()) return null

        conn.prepareStatement("SELECT id FROM papers WHERE LOWER(doi) = ?").use { stmt ->
            stmt.setString(1, "https://doi.org/$bare")
            val rs = stmt.executeQuery()
            if (rs.next()) return rs.getObject("id", UUID::class.java)
        }

        val paper = OpenAlexService.getPaperByDoi(doi) ?: return null
        return SearchPersistence.upsertPaper(conn, paper)
    }

    private fun insertStudyCard(conn: Connection, card: StudyCardResponse, paperId: UUID?) {
        val sql = """
            INSERT INTO study_cards (paper_id, source_url, tldr, study_design, limitations, key_findings, generation_source)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val keyFindingsJson = PGobject().apply {
            type = "jsonb"
            value = Json.encodeToString(card.keyFindings)
        }

        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, paperId)
            stmt.setString(2, card.url)
            stmt.setString(3, card.tldr)
            stmt.setString(4, card.studyDesign)
            stmt.setString(5, card.limitations)
            stmt.setObject(6, keyFindingsJson)
            stmt.setString(7, card.source)
            stmt.executeUpdate()
        }
    }

    private fun updateSavedState(url: String, isSaved: Boolean): Boolean {
        return try {
            Database.connect().use { conn ->
                conn.prepareStatement(
                    """
                    UPDATE study_cards
                    SET is_saved = ?
                    WHERE source_url = ?
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setBoolean(1, isSaved)
                    stmt.setString(2, url)
                    stmt.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            log.error("failed to update saved state url={} isSaved={}", url, isSaved, e)
            false
        }
    }
}
