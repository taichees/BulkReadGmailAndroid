package com.example.bulkreadgmail.data.api

import com.example.bulkreadgmail.data.model.*
import retrofit2.http.Body
import retrofit2.http.POST

interface GmailApiService {
    @POST("v1/auth/callback")
    suspend fun authCallback(@Body request: AuthCallbackRequest): AuthCallbackResponse

    @POST("v1/gmail/read-all")
    suspend fun readAll(@Body request: ReadAllRequest): ReadAllResponse

    @POST("v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): LogoutResponse
}
