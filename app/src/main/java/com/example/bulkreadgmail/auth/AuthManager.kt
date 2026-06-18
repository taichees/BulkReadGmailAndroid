package com.example.bulkreadgmail.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

class AuthManager(context: Context) {
    private val webClientId = "725405052696-binf7nh361nd97rt21ilpsd3v7b7q5jd.apps.googleusercontent.com"

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestServerAuthCode(webClientId, true) // forceCodeForRefreshToken = true を追加
        .requestEmail()
        // Gmailの既読化（ラベル操作）に必要なスコープを追加
        .requestScopes(Scope("https://www.googleapis.com/auth/gmail.modify"))
        .build()

    private val googleSignInClient = GoogleSignIn.getClient(context, gso)

    fun getSignInIntent() = googleSignInClient.signInIntent

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            onComplete()
        }
    }
}
