package com.wtcb.myprompter

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "FirebaseAuthManager"
        const val RC_SIGN_IN = 9001
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val googleSignInClient: GoogleSignInClient

    var onSignInSuccess: ((FirebaseUser) -> Unit)? = null
    var onSignInFailure: ((String) -> Unit)? = null
    var onSignOutSuccess: (() -> Unit)? = null

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent() = googleSignInClient.signInIntent

    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign in failed", e)
            onSignInFailure?.invoke("Google sign-in failed: ${e.message}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        Log.d(TAG, "signInWithCredential:success")
                        createOrUpdateUserInFirestore(it)
                        onSignInSuccess?.invoke(it)
                    }
                } else {
                    Log.e(TAG, "signInWithCredential:failure", task.exception)
                    onSignInFailure?.invoke("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun createOrUpdateUserInFirestore(user: FirebaseUser) {
        val userRef = firestore.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                // New user - create document
                val userData = hashMapOf(
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "photoUrl" to user.photoUrl?.toString(),
                    "points" to 0,
                    "wordLimitExtension" to 0,
                    "videosWatchedToday" to 0,
                    "lastVideoWatchDate" to "",
                    "createdAt" to System.currentTimeMillis()
                )

                userRef.set(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "User document created successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error creating user document", e)
                    }
            } else {
                Log.d(TAG, "User already exists in Firestore")
            }
        }
    }

    fun syncPointsToFirestore(points: Int, wordLimitExtension: Int, videosWatched: Int, lastDate: String) {
        val user = auth.currentUser ?: return

        val updates = hashMapOf<String, Any>(
            "points" to points,
            "wordLimitExtension" to wordLimitExtension,
            "videosWatchedToday" to videosWatched,
            "lastVideoWatchDate" to lastDate,
            "lastUpdated" to System.currentTimeMillis()
        )

        firestore.collection("users").document(user.uid)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Points synced to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error syncing points", e)
            }
    }

    fun loadPointsFromFirestore(onLoaded: (Int, Int, Int, String) -> Unit) {
        val user = auth.currentUser ?: return

        firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val points = document.getLong("points")?.toInt() ?: 0
                    val wordLimit = document.getLong("wordLimitExtension")?.toInt() ?: 0
                    val videosWatched = document.getLong("videosWatchedToday")?.toInt() ?: 0
                    val lastDate = document.getString("lastVideoWatchDate") ?: ""

                    onLoaded(points, wordLimit, videosWatched, lastDate)
                    Log.d(TAG, "Points loaded from Firestore: $points")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading points", e)
            }
    }

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            onSignOutSuccess?.invoke()
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isSignedIn(): Boolean = auth.currentUser != null
}