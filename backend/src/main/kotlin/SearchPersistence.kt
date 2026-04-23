package com.cohort

import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.Types
import java.util.UUID

object SearchPersistence {
    private val log = LoggerFactory.getLogger(SearchPersistence::class.java)

    fun save(result: SearchResult) {
        try {
            Database.connect().use { conn ->
                conn.autoCommit = false
                try {
                    val queryId = insertSearchQuery(conn, result)
                    val paperIds = result.results.map { paper -> upsertPaper(conn, paper) }
                    result.results.zip(paperIds).forEachIndexed { index, (_, paperId) ->
                        insertSearchResult(conn, queryId, paperId, rank = index + 1)
                    }
                    conn.commit()
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                }
            }
        } catch (e: Exception) {
            log.error("failed to save search", e)
        }
    }

    private fun insertSearchQuery(conn: Connection, result: SearchResult): UUID {
        val sql = """
            INSERT INTO search_queries (query_text, page, per_page, total_count)
            VALUES (?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, result.query)
            stmt.setInt(2, result.page)
            stmt.setInt(3, result.perPage)
            stmt.setInt(4, result.totalCount)
            val rs = stmt.executeQuery()
            rs.next()
            return rs.getObject("id", UUID::class.java)
        }
    }

    internal fun upsertPaper(conn: Connection, paper: PaperPreview): UUID {
        val sql = """
            INSERT INTO papers (doi, title, publication_year, abstract, oa_url, oa_status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (doi) WHERE doi IS NOT NULL DO UPDATE SET
                title            = EXCLUDED.title,
                publication_year = EXCLUDED.publication_year,
                abstract         = EXCLUDED.abstract,
                oa_url           = EXCLUDED.oa_url,
                oa_status        = EXCLUDED.oa_status
            RETURNING id
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, paper.doi)
            stmt.setString(2, paper.title)
            if (paper.year != null) stmt.setInt(3, paper.year) else stmt.setNull(3, Types.INTEGER)
            stmt.setString(4, paper.abstractText)
            stmt.setString(5, paper.oaUrl)
            stmt.setString(6, paper.oaStatus)
            val rs = stmt.executeQuery()
            rs.next()
            return rs.getObject("id", UUID::class.java)
        }
    }

    private fun insertSearchResult(conn: Connection, queryId: UUID, paperId: UUID, rank: Int) {
        val sql = """
            INSERT INTO search_results (query_id, paper_id, result_rank)
            VALUES (?, ?, ?)
        """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, queryId)
            stmt.setObject(2, paperId)
            stmt.setInt(3, rank)
            stmt.executeUpdate()
        }
    }
}
