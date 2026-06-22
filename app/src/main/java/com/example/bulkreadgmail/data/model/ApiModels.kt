package com.example.bulkreadgmail.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class AuthCallbackRequest(
    val code: String,
    val client_id: String,
    val redirect_uri: String? = null,
    val user_id: String? = null
)

@Serializable
data class AuthCallbackResponse(
    val success: Boolean = false,
    val user_id: String? = null,
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Long? = null,
    val error: String? = null
)

@Serializable
data class ReadAllRequest(
    val stream: Boolean = false,
    val limit: Int? = null
)

@Serializable
data class GoogleTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("scope") val scope: String? = null,
    @SerialName("token_type") val tokenType: String? = null
)

@Serializable
data class ReadAllResponse(
    val success: Boolean = false,
    val processed_count: Int = 0,
    val error: String? = null
)

@Serializable
data class LogoutRequest(
    val user_id: String
)

@Serializable
data class LogoutResponse(
    val success: Boolean = false,
    val error: String? = null
)

@Serializable
data class ProgressUpdate(
    val type: String,
    val total: Int? = null,
    val completed: Int? = null,
    val processed_count: Int? = null,
    val success: Boolean? = null,
    val error: String? = null
)
