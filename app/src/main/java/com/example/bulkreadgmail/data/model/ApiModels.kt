package com.example.bulkreadgmail.data.model

import kotlinx.serialization.Serializable

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
    val error: String? = null
)

@Serializable
data class ReadAllRequest(
    val user_id: String,
    val client_id: String,
    val stream: Boolean = false,
    val limit: Int? = null
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
