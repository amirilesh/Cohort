package com.cohort

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.UUID

object StudyCardPersistence {
    private val log = LoggerFactory.getLogger(StudyCardPersistence::class.java)

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
}