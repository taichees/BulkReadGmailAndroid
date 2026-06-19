package com.example.bulkreadgmail.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bulkreadgmail.data.api.GmailApiService
import com.example.bulkreadgmail.data.model.*
import com.example.bulkreadgmail.util.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.MediaType.Companion.toMediaType

class MainViewModel(private val sessionManager: SessionManager) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

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

    fun handleAuthCode(code: String) {
        viewModelScope.launch {
            Log.d("MainViewModel", "Sending auth code to backend.")
            _uiState.value = UiState.Loading("ログイン処理中...")
            try {
                val clientId = "725405052696-binf7nh361nd97rt21ilpsd3v7b7q5jd.apps.googleusercontent.com"
                val response = apiService.authCallback(
                    AuthCallbackRequest(
                        code = code,
                        client_id = clientId
                    )
                )
                if (response.success && response.user_id != null) {
                    Log.d("MainViewModel", "Backend auth success: ${response.user_id}")
                    sessionManager.saveUserId(response.user_id)
                    _uiState.value = UiState.LoggedIn(response.user_id)
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
            _uiState.value = UiState.Loading("メール取得中...")
            try {
                val clientId = "725405052696-binf7nh361nd97rt21ilpsd3v7b7q5jd.apps.googleusercontent.com"
                val responseBody = apiService.readAll(
                    ReadAllRequest(
                        user_id = userId,
                        client_id = clientId,
                        stream = true
                    )
                )
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    var total = 0
                    responseBody.byteStream().bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            Log.d("MainViewModel", "Stream line: $line")
                            if (line.isBlank()) return@forEach
                            try {
                                val update = json.decodeFromString<ProgressUpdate>(line)
                                viewModelScope.launch {
                                    when (update.type) {
                                        "count" -> {
                                            total = update.total ?: 0
                                            _uiState.value = UiState.Loading("既読処理中\n(0 / $total 件完了)")
                                        }
                                        "progress" -> {
                                            val completed = update.completed ?: 0
                                            _uiState.value = UiState.Loading("既読処理中\n($completed / $total 件完了)")
                                        }
                                        "result" -> {
                                            val count = update.processed_count ?: 0
                                            _toastEvent.emit("${count}件のメールを既読にしました")
                                            _uiState.value = UiState.LoggedIn(userId)
                                        }
                                        "error" -> {
                                            _toastEvent.emit(update.error ?: "エラーが発生しました")
                                            _uiState.value = UiState.LoggedIn(userId)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to parse line: $line", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "readAllEmails exception", e)
                val errorMessage = if (e is retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    "HTTP ${e.code()}: $errorBody"
                } else {
                    e.localizedMessage ?: "Unknown error"
                }
                _toastEvent.emit("通信エラーが発生しました: $errorMessage")
                _uiState.value = UiState.LoggedIn(userId)
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
    data class Loading(val progressText: String) : UiState()
    data class LoggedIn(val userId: String) : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}
