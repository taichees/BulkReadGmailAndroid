package com.example.bulkreadgmail

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bulkreadgmail.auth.AuthManager
import com.example.bulkreadgmail.ui.theme.BulkReadGmailTheme
import com.example.bulkreadgmail.ui.viewmodel.MainViewModel
import com.example.bulkreadgmail.ui.viewmodel.UiState
import com.example.bulkreadgmail.util.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")
        enableEdgeToEdge()
        val sessionManager = SessionManager(this)
        val authManager = AuthManager(this)

        setContent {
            BulkReadGmailTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return MainViewModel(sessionManager) as T
                        }
                    }
                )
                MainScreen(viewModel, authManager)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("MainActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, authManager: AuthManager) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Log.d("MainActivity", "MainScreen composed, state: $uiState")

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "Launcher callback triggered. Result: ${result.resultCode}")
        Toast.makeText(context, "Sign-In Result: ${result.resultCode}", Toast.LENGTH_SHORT).show()
        
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(Exception::class.java)
            Log.d("MainActivity", "Google Sign-In success: ${account?.email}")
            account?.serverAuthCode?.let { code ->
                Log.d("MainActivity", "Server Auth Code obtained: $code")
                // メールアドレスを user_id として利用する（バックエンドの期待に合わせて調整可能）
                val userId = account.email ?: "unknown_user"
                viewModel.handleAuthCode(code, userId)
            } ?: run {
                Log.e("MainActivity", "Server Auth Code is null! Check Web Client ID.")
                Toast.makeText(context, "Error: Server Auth Code is null", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Google Sign-In failed", e)
            Toast.makeText(context, "Sign-In Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Success -> Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            is UiState.Error -> Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            else -> {}
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (val state = uiState) {
                    is UiState.Initial -> {
                        Button(
                            onClick = {
                                Log.d("MainActivity", "Sign-in button clicked")
                                launcher.launch(authManager.getSignInIntent())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Googleにサインイン", style = MaterialTheme.typography.titleLarge)
                        }
                    }

                    is UiState.Loading -> {
                        CircularProgressIndicator()
                    }

                    is UiState.LoggedIn -> {
                        Text("ログイン中: ${state.userId}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // 既読処理ボタン (大きく表示)
                        Button(
                            onClick = { viewModel.readAllEmails() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.9f) // 9:1 の割合のためのウェイト設定（Column全体に効かせるため以下調整）
                                .padding(vertical = 16.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text("未読メールを一括で既読にする", style = MaterialTheme.typography.headlineSmall)
                        }

                        // ログアウトボタン
                        TextButton(
                            onClick = { viewModel.logout { authManager.signOut {} } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.1f)
                                .padding(vertical = 8.dp)
                        ) {
                            Text("ログアウト/アカウントを切り替える", color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    is UiState.Success -> {
                        Text(state.message, style = MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.resetToLoggedIn() },
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        ) {
                            Text("戻る")
                        }
                    }

                    is UiState.Error -> {
                        Text("エラーが発生しました", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                        Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.resetToLoggedIn() },
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        ) {
                            Text("リトライ")
                        }
                    }
                }
            }
        }
    }
}
