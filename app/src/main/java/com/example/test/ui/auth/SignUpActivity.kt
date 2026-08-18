package com.example.test.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.test.databinding.ActivitySignUpBinding
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivitySignUpBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setupInputs()

        setupRoleSelection()

        setupButtons()

        observeViewModel()
    }

    private fun setupInputs() {

        binding.nameInput.setLabel("Full Name")

        binding.emailInput.setLabel("Email")

        binding.passwordInput.setLabel("Password")
        binding.passwordInput.setPasswordMode()

        binding.confirmPasswordInput.setLabel(
            "Confirm Password"
        )

        binding.confirmPasswordInput.setPasswordMode()

        binding.companyCodeInput.setLabel(
            "Company Code"
        )
    }

    private fun setupRoleSelection() {

        binding.roleGroup.setOnCheckedChangeListener {
                _, checkedId ->

            when (checkedId) {

                binding.rbUser.id -> {

                    binding.companyCodeInput.visibility =
                        android.view.View.GONE
                }

                binding.rbManager.id -> {

                    binding.companyCodeInput.visibility =
                        android.view.View.VISIBLE
                }
            }
        }
    }

    private fun setupButtons() {

        binding.btnContinue.setOnClickListener {

            val name =
                binding.nameInput.getText().trim()

            val email =
                binding.emailInput.getText().trim()

            val password =
                binding.passwordInput.getText()

            val confirmPassword =
                binding.confirmPasswordInput.getText()

            val role =
                if (binding.rbManager.isChecked) {
                    "MANAGER"
                } else {
                    "USER"
                }

            val companyCode =
                binding.companyCodeInput.getText().trim()

            viewModel.signUp(
                name = name,
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                role = role,
                companyCode = companyCode
            )
        }

        binding.toSignIn.setOnClickListener {

            finish()
        }
    }

    private fun observeViewModel() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    viewModel.isLoading.collect { loading ->

                        binding.btnContinue.isEnabled =
                            !loading

                        binding.btnContinue.text =
                            if (loading) {
                                "Creating Account..."
                            } else {
                                "Sign Up"
                            }
                    }
                }

                launch {

                    viewModel.signUpSuccess.collect { success ->

                        if (success) {

                            Toast.makeText(
                                this@SignUpActivity,
                                "Account created successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            openSignIn()
                        }
                    }
                }

                launch {

                    viewModel.error.collect { error ->

                        if (error != null) {

                            Toast.makeText(
                                this@SignUpActivity,
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

    private fun openSignIn() {

        val intent =
            Intent(
                this,
                SignInActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        startActivity(intent)

        finish()
    }
}