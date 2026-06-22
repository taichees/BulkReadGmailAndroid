package com.example.bulkreadgmail.data.api

import com.example.bulkreadgmail.data.model.*
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface GmailApiService {
    @POST("v1/auth/callback")
    suspend fun authCallback(@Body request: AuthCallbackRequest): AuthCallbackResponse

    @Streaming
    @POST("v1/gmail/read-all")
    suspend fun readAll(
        @Header("Authorization") authHeader: String,
        @Body request: ReadAllRequest
    ): ResponseBody

    @POST("v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequest): LogoutResponse

    @POST("https://oauth2.googleapis.com/token")
    @FormUrlEncoded
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String
    ): GoogleTokenResponse
}
