package com.example.test.ui.auth

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.ui.manager.ManagerActivity
import com.example.test.ui.user.UserListActivity
import com.example.test.databinding.ActivitySignInBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import es.dmoral.toasty.Toasty

class SignInActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Connect to firebase Authentication
        auth = FirebaseAuth.getInstance()
        //Connect to Cloud Firestore
        firestore = FirebaseFirestore.getInstance()

        configureInputs()
        configureLogo()
        configureButtons()
    }

    private fun configureInputs(){
        binding.apply {
            nameInput.setLabel("Full Name")

            emailInput.setLabel("Email")

            passwordInput.setLabel("Password")
            passwordInput.setPasswordMode()
        }
    }

    private fun configureLogo() {
        val text = "digital\ngrading\ncompany"
        val spannable = SpannableString(text)

        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            14,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvLogoName.text = spannable
    }

    private fun configureButtons(){
        binding.toSignup.setOnClickListener {
            startActivity(
                Intent(this, SignUpActivity::class.java)
            )
        }

        binding.btnContinue.setOnClickListener {
            validateAndSignIn()
        }
    }

    private fun validateAndSignIn(){
        val name = binding.nameInput.getText().trim()
        val email = binding.emailInput.getText().trim()
        val password = binding.passwordInput.getText()

        when{
            name.isEmpty() -> { signInErrorToast("Please enter your name") }
            email.isEmpty() -> { signInErrorToast("Please enter your email")}
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                signInErrorToast("Please enter your password")
            }

            else -> {
                signInWithFirebase(
                    name = name,
                    email = email,
                    password = password
                )
            }
        }
    }

    private fun signInWithFirebase(
        name: String,
        email: String,
        password: String
    ) {
        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser

                    if (firebaseUser == null) {
                        setLoading(false)
                        signInErrorToast(
                            "Firebase user information is unavailable"
                        )
                        return@addOnCompleteListener
                    }

                    loadUserProfile(
                        uid = firebaseUser.uid,
                        enteredName = name
                    )
                } else {
                    setLoading(false)

                    val exception = task.exception

                    Log.e(
                        "FirebaseSignIn",
                        "Sign-in failed",
                        exception
                    )

                    when (exception) {
                        is FirebaseNetworkException -> {
                            signInErrorToast(
                                "Cannot connect to Firebase. Check your internet connection."
                            )
                        }

                        is FirebaseAuthException -> {
                            Log.e(
                                "FirebaseSignIn",
                                "Firebase error code: ${exception.errorCode}"
                            )

                            handleAuthError(exception.errorCode)
                        }

                        else -> {
                            signInErrorToast(
                                exception?.localizedMessage
                                    ?: "Login failed"
                            )
                        }
                    }
                }
            }
    }

    private fun loadUserProfile(uid: String, enteredName: String){
        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                setLoading(false)
                if(!document.exists()){
                    auth.signOut()
                    signInErrorToast("Your user profile was not found")
                    return@addOnSuccessListener
                }

                val savedName = document.getString("name") ?: ""
                val email = document.getString("email") ?: ""

                val role = document.getString("role") ?: "USER"
                if (!savedName.equals(
                        enteredName,
                        ignoreCase = true
                    )
                ) {
                    auth.signOut()

                    signInErrorToast(
                        "The entered name does not match this account"
                    )
                    return@addOnSuccessListener
                }

                signInSuccessToast("Login successful")
                openUserListScreen(
                    uid = uid,
                    name = savedName,
                    email = email,
                    role = role
                )
            }
            .addOnFailureListener { exception ->

                setLoading(false)
                auth.signOut()

                Log.e(
                    "FirestoreProfile",
                    "Could not load user profile",
                    exception
                )

                signInErrorToast(
                    exception.localizedMessage
                        ?: "Could not load your profile"
                )
            }
    }

    private fun openUserListScreen(
        uid: String,
        name: String,
        email: String,
        role: String
    ) {

        val targetActivity = if (role.equals("MANAGER", ignoreCase = true)){
            ManagerActivity::class.java
        }else{
            UserListActivity::class.java
        }


        val intent = Intent(this, targetActivity)

        intent.putExtra("ROLE", role)
        intent.putExtra("USER_ID", uid)
        intent.putExtra("USER_NAME", name)
        intent.putExtra("USER_EMAIL", email)
        intent.putExtra("USER_ROLE", role)

        startActivity(intent)
        finish()
    }

    private fun handleAuthError(errorCode: String) {
        when (errorCode) {
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD",
            "ERROR_USER_NOT_FOUND" -> {
                signInErrorToast(
                    "Incorrect email or password"
                )
            }

            "ERROR_INVALID_EMAIL" -> {
                signInErrorToast(
                    "Please enter a valid email address"
                )
            }

            "ERROR_USER_DISABLED" -> {
                signInErrorToast(
                    "This account has been disabled"
                )
            }

            "ERROR_TOO_MANY_REQUESTS" -> {
                signInErrorToast(
                    "Too many attempts. Please try again later"
                )
            }

            else -> {
                signInErrorToast(
                    "Login failed: $errorCode"
                )
            }
        }
    }

    private fun setLoading(isLoading: Boolean){
        binding.btnContinue.isEnabled = !isLoading
        binding.btnContinue.text = if(isLoading){"Signing in..."} else{"Continue"}
    }

    private fun signInErrorToast(message: String){
        Toasty.error(this,message, Toast.LENGTH_SHORT).show()
    }

    private fun signInSuccessToast(message: String){
        Toasty.success(this,message, Toast.LENGTH_SHORT).show()
    }
}