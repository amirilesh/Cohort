package com.cohort

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testStudyCardRequiresUrl() = testApplication {
        application {
            module()
        }

        client.get("/studycard").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertFalse(bodyAsText().contains("\"success\":true"))
        }
    }

    @Test
    fun testStudyCardPostRequiresUrl() = testApplication {
        application {
            module()
        }

        client.post("/studycard").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
            assertFalse(bodyAsText().contains("\"success\":true"))
        }
    }

    @Test
    fun testStudyCardJobCanBePolled() = testApplication {
        application {
            module()
        }

        val jobId = InMemoryJobStore.createJob()

        client.get("/studycard/$jobId").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"status":"processing"}""", bodyAsText())
        }
    }
}
