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

}
