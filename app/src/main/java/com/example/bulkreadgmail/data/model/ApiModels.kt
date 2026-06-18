package com.example.bulkreadgmail.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthCallbackRequest(
    val code: String,
    val user_id: String
)

@Serializable
data class AuthCallbackResponse(
    val success: Boolean = false,
    val error: String? = null
)

@Serializable
data class ReadAllRequest(
    val user_id: String,
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
