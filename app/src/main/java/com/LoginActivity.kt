package com.wtcb.myprompter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnSkipSignIn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var firebaseAuthManager: FirebaseAuthManager

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            firebaseAuthManager.handleSignInResult(task)
        } catch (e: ApiException) {
            progressBar.visibility = View.GONE
            btnGoogleSignIn.isEnabled = true
            Toast.makeText(this, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuthManager = FirebaseAuthManager(this)

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnSkipSignIn = findViewById(R.id.btnSkipSignIn)
        progressBar = findViewById(R.id.progressBar)

        setupFirebaseCallbacks()
        setupButtons()

        // Check if already signed in
        if (firebaseAuthManager.isSignedIn()) {
            navigateToMain()
        }
    }

    private fun setupFirebaseCallbacks() {
        firebaseAuthManager.onSignInSuccess = { user ->
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Welcome ${user.displayName}!", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }

        firebaseAuthManager.onSignInFailure = { error ->
            progressBar.visibility = View.GONE
            btnGoogleSignIn.isEnabled = true
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtons() {
        btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        btnSkipSignIn.setOnClickListener {
            navigateToMain()
        }
    }

    private fun signInWithGoogle() {
        progressBar.visibility = View.VISIBLE
        btnGoogleSignIn.isEnabled = false

        val signInIntent = firebaseAuthManager.getSignInIntent()
        signInLauncher.launch(signInIntent)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onBackPressed() {
        // Prevent going back to welcome screen
        finishAffinity()
    }
}