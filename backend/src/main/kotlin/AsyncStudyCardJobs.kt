package com.cohort

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
enum class JobStatus {
    @SerialName("processing")
    PROCESSING,

    @SerialName("completed")
    COMPLETED,

    @SerialName("failed")
    FAILED,
}

@Serializable
data class StudyCardJobResponse(
    val jobId: String,
    val status: JobStatus,
)

@Serializable
data class AsyncJobResult(
    val status: JobStatus,
    val result: StudyCardResponse? = null,
    val error: String? = null,
)

@Serializable
data class ProcessingJobResult(
    val status: JobStatus,
)

@Serializable
data class CompletedJobResult(
    val status: JobStatus,
    val result: StudyCardResponse,
)

@Serializable
data class FailedJobResult(
    val status: JobStatus,
    val error: String,
)

object InMemoryJobStore {
    private val jobs = ConcurrentHashMap<String, AsyncJobResult>()

    fun createJob(): String {
        val jobId = UUID.randomUUID().toString()
        jobs[jobId] = AsyncJobResult(status = JobStatus.PROCESSING)
        return jobId
    }

    fun getJob(jobId: String): AsyncJobResult? = jobs[jobId]

    fun completeJob(jobId: String, result: StudyCardResponse) {
        jobs[jobId] = AsyncJobResult(
            status = JobStatus.COMPLETED,
            result = result,
        )
    }

    fun failJob(jobId: String, error: String) {
        jobs[jobId] = AsyncJobResult(
            status = JobStatus.FAILED,
            error = error,
        )
    }
}

object StudyCardJobService {
    private val log = LoggerFactory.getLogger(StudyCardJobService::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start(doi: String?, url: String?): StudyCardJobResponse {
        val jobId = InMemoryJobStore.createJob()

        scope.launch {
            try {
                val result = generateStudyCard(doi = doi, url = url)
                if (result.success) {
                    InMemoryJobStore.completeJob(jobId, result)
                } else {
                    InMemoryJobStore.failJob(jobId, result.reason ?: "study_card_generation_failed")
                }
            } catch (e: Exception) {
                log.error("study card job failed jobId={}", jobId, e)
                InMemoryJobStore.failJob(jobId, e.message ?: "study_card_generation_failed")
            }
        }

        return StudyCardJobResponse(
            jobId = jobId,
            status = JobStatus.PROCESSING,
        )
    }

    private suspend fun generateStudyCard(doi: String?, url: String?): StudyCardResponse {
        if (doi != null) {
            val cached = StudyCardPersistence.findByDoi(doi)
            if (cached != null) return cached

            val result = StudyCardService.generateFromDoi(doi)
            if (result.success) StudyCardPersistence.save(result, doi)
            return result
        }

        val paperUrl = requireNotNull(url) { "missing_doi_or_url" }
        val cached = StudyCardPersistence.findByUrl(paperUrl)
        if (cached != null) return cached

        val result = StudyCardService.generate(paperUrl)
        if (result.success) StudyCardPersistence.save(result, doi = null)
        return result
    }
}
