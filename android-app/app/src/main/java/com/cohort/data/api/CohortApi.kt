package com.cohort.data.api

import com.cohort.data.model.RecentStudyCard
import com.cohort.data.model.SearchResult
import com.cohort.data.model.StudyCardResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CohortApi {

    @GET("search")
    suspend fun search(
        @Query("q") q: String,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 10,
    ): SearchResult

    @GET("studycard")
    suspend fun studyCardByDoi(
        @Query("doi") doi: String,
    ): StudyCardResponse

    @GET("studycard")
    suspend fun studyCardByUrl(
        @Query("url") url: String,
    ): StudyCardResponse

    @GET("studycards/recent")
    suspend fun getRecentStudyCards(): List<RecentStudyCard>

    @POST("studycards/save")
    suspend fun saveStudyCard(
        @Query("url") url: String,
    ): Response<Unit>

    @DELETE("studycards/save")
    suspend fun deleteSavedStudyCard(
        @Query("url") url: String,
    ): Response<Unit>

    @GET("studycards/saved")
    suspend fun getSavedStudyCards(): List<RecentStudyCard>
}
