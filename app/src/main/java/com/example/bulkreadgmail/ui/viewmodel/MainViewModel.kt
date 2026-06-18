package com.example.bulkreadgmail.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bulkreadgmail.data.api.GmailApiService
import com.example.bulkreadgmail.data.model.AuthCallbackRequest
import com.example.bulkreadgmail.data.model.LogoutRequest
import com.example.bulkreadgmail.data.model.ReadAllRequest
import com.example.bulkreadgmail.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

class MainViewModel(private val sessionManager: SessionManager) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://gmail-batch-read.chiaki-621.workers.dev/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val apiService = retrofit.create(GmailApiService::class.java)

    init {
        val userId = sessionManager.getUserId()
        if (userId != null) {
            _uiState.value = UiState.LoggedIn(userId)
        }
    }

    fun handleAuthCode(code: String, userId: String) {
        viewModelScope.launch {
            Log.d("MainViewModel", "Sending auth code to backend. user_id: $userId")
            _uiState.value = UiState.Loading
            try {
                val response = apiService.authCallback(AuthCallbackRequest(code, userId))
                if (response.success) {
                    Log.d("MainViewModel", "Backend auth success")
                    sessionManager.saveUserId(userId)
                    _uiState.value = UiState.LoggedIn(userId)
                } else {
                    Log.e("MainViewModel", "Backend auth failed: ${response.error}")
                    _uiState.value = UiState.Error(response.error ?: "Authentication failed")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Auth callback failed", e)
                _uiState.value = UiState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun readAllEmails() {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            Log.d("MainViewModel", "Executing readAllEmails for user: $userId")
            _uiState.value = UiState.Loading
            try {
                val response = apiService.readAll(ReadAllRequest(userId))
                if (response.success) {
                    Log.d("MainViewModel", "readAllEmails success: ${response.processed_count} emails")
                    _uiState.value = UiState.Success("${response.processed_count}件のメールを既読にしました")
                } else {
                    Log.e("MainViewModel", "readAllEmails failed: ${response.error}")
                    _uiState.value = UiState.Error(response.error ?: "Failed to read emails")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "readAllEmails exception", e)
                val errorMessage = if (e is retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    "HTTP ${e.code()}: $errorBody"
                } else {
                    e.localizedMessage ?: "Unknown error"
                }
                _uiState.value = UiState.Error(errorMessage)
            }
        }
    }

    fun logout(onSignOut: () -> Unit) {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            try {
                apiService.logout(LogoutRequest(userId))
            } catch (e: Exception) {
                Log.e("MainViewModel", "Logout API failed", e)
            }
            sessionManager.clearSession()
            _uiState.value = UiState.Initial
            onSignOut()
        }
    }
    
    fun resetToLoggedIn() {
        val userId = sessionManager.getUserId()
        if (userId != null) {
            _uiState.value = UiState.LoggedIn(userId)
        } else {
            _uiState.value = UiState.Initial
        }
    }
}

sealed class UiState {
    object Initial : UiState()
    object Loading : UiState()
    data class LoggedIn(val userId: String) : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}
