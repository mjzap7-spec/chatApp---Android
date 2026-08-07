package com.example.test
import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.dmoral.toasty.Toasty
class SignUpActivity : AppCompatActivity(){
    private lateinit var binding: ActivitySignUpBinding

    private lateinit var auth : FirebaseAuth
    private lateinit var firestore : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        configureInputs()
        configureRoleSelection()
        configureLogo()
        configureButtons()
    }

    private fun configureInputs() {
        binding.apply {
            nameInput.setLabel("Full name")
            emailInput.setLabel("Email")
            passwordInput.setLabel("Password")
            passwordInput.setPasswordMode()
            confirmPasswordInput.setLabel("Confirm Passeord")
            confirmPasswordInput.setPasswordMode()

            companyCodeInput.visibility = if(rbManager.isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun configureRoleSelection() {
        binding.rbUser.setOnClickListener {
            binding.companyCodeInput.visibility = View.GONE
        }
        binding.rbManager.setOnClickListener {
            binding.companyCodeInput.visibility = View.VISIBLE
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

    private fun configureButtons() {
        binding.toSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
        binding.btnContinue.setOnClickListener {
            validateAndCreateAccount()
        }
    }

    private fun validateAndCreateAccount(){
        val name = binding.nameInput.getText().trim()
        val email = binding.emailInput.getText().trim()
        val password = binding.passwordInput.getText()
        val confirmPassword = binding.confirmPasswordInput.getText()
        val companyCode = binding.companyCodeInput.getText().trim()

        when{
            name.isEmpty() -> {
                signUpErrorToast("Please enter your name")
            }
            email.isEmpty() -> {
                signUpErrorToast("Please enter your email")
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                signUpErrorToast("Please enter your password")
            }
            password.isEmpty() -> {
                signUpErrorToast("Please enter your password")
            }
            !isValidPassword(password) -> {
                signUpErrorToast("Password bust be at least 8 characters")
            }
            confirmPassword.isEmpty() -> {
                signUpErrorToast("Please confirm your password")
            }
            password != confirmPassword -> {
                signUpErrorToast("Passwords do not match")
            }
            binding.rbManager.isChecked && companyCode.isEmpty() -> {
                signUpErrorToast("Please enter your company code")
            }
            binding.rbManager.isChecked && companyCode != "AAA" -> {
                signUpErrorToast("Company code does not match")
            }

            else -> {
                val role = "USER"

                createFirebaseAccount(
                    name = name,
                    email = email,
                    password = password,
                    role = role
                )
            }
        }
    }

    private fun createFirebaseAccount(name: String, email: String, password: String, role: String){
        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user != null) {
                        saveUserProfile(
                            uid = user.uid,
                            name = name,
                            email = email,
                            role = role
                        )
                    } else {
                        setLoading(false)
                        signUpErrorToast("Firebase user is unavailable")
                    }
                } else {
                    setLoading(false)

                    val exception = task.exception

                    Log.e(
                        "FirebaseSignUp",
                        "Sign-up failed: ${exception?.javaClass?.name}",
                        exception
                    )

                    when (exception) {
                        is FirebaseNetworkException -> {
                            signUpErrorToast(
                                "Cannot connect to Firebase. Check the device internet connection."
                            )
                        }

                        is FirebaseAuthException -> {
                            Log.e(
                                "FirebaseSignUp",
                                "Firebase error code: ${exception.errorCode}"
                            )

                            signUpErrorToast(
                                "${exception.errorCode}: ${exception.localizedMessage}"
                            )
                        }

                        else -> {
                            signUpErrorToast(
                                exception?.localizedMessage
                                    ?: "Could not create the account"
                            )
                        }
                    }
                }
            }
    }

    private fun saveUserProfile(
        uid: String,
        name: String,
        email: String,
        role: String
    ) {
        val userProfile = hashMapOf(
            "uid" to uid,
            "name" to name.trim(),
            "email" to email.trim().lowercase(),
            "role" to "USER",
            "createdAt" to
                    com.google.firebase.firestore
                        .FieldValue
                        .serverTimestamp()
        )

        firestore.collection("users")
            .document(uid)
            .set(userProfile)
            .addOnSuccessListener {
                setLoading(false)
                signUpSuccessToast(
                    "Account created successfully"
                )

                auth.signOut()

                val intent =
                    Intent(
                        this,
                        SignInActivity::class.java
                    )

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                setLoading(false)

                auth.currentUser
                    ?.delete()

                Log.e(
                    "FirebaseSignUp",
                    "Could not save Firestore profile",
                    exception
                )

                signUpErrorToast(
                    exception.localizedMessage
                        ?: "Could not save user profile"
                )
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnContinue.isEnabled = !isLoading
        binding.btnContinue.text =
            if(isLoading){
                "Creating account..."
            }else {
                "Continue"
            }
    }

    private fun signUpSuccessToast(message: String) {
        Toasty.success(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun signUpErrorToast(message: String) {
        Toasty.error(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

}