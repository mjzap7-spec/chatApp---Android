package com.example.test.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.test.databinding.ActivitySignInBinding
import com.example.test.ui.manager.ManagerActivity
import kotlinx.coroutines.launch

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    private val viewModel: AuthViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivitySignInBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        setupInputs()

        setupButtons()

        observeViewModel()
    }


    // =========================================================
    // INPUTS
    // =========================================================

    private fun setupInputs() {

        binding.emailInput.setLabel(
            "Email"
        )

        binding.passwordInput.setLabel(
            "Password"
        )

        // Enables password masking + eye button
        binding.passwordInput.setPasswordMode()
    }


    // =========================================================
    // BUTTONS
    // =========================================================

    private fun setupButtons() {

        binding.btnContinue.setOnClickListener {

            val email =
                binding.emailInput
                    .getText()
                    .trim()

            val password =
                binding.passwordInput
                    .getText()

            viewModel.signIn(
                email = email,
                password = password
            )
        }


        binding.toSignup.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    SignUpActivity::class.java
                )
            )
        }
    }


    // =========================================================
    // VIEWMODEL
    // =========================================================

    private fun observeViewModel() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                // -----------------------------
                // Loading
                // -----------------------------

                launch {

                    viewModel.isLoading.collect { loading ->

                        binding.btnContinue.isEnabled =
                            !loading

                        binding.btnContinue.text =
                            if (loading) {
                                "Signing In..."
                            } else {
                                "Sign In"
                            }
                    }
                }


                // -----------------------------
                // Login Success
                // -----------------------------

                launch {

                    viewModel.loginSuccess.collect { success ->

                        if (success) {

                            openManager()
                        }
                    }
                }


                // -----------------------------
                // Error
                // -----------------------------

                launch {

                    viewModel.error.collect { error ->

                        if (error != null) {

                            Toast.makeText(
                                this@SignInActivity,
                                error,
                                Toast.LENGTH_LONG
                            ).show()

                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }


    // =========================================================
    // OPEN MANAGER
    // =========================================================

    private fun openManager() {

        val intent =
            Intent(
                this,
                ManagerActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }
}