package com.example.test.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository =
        AuthRepository()

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading

    private val _loginSuccess =
        MutableStateFlow(false)

    val loginSuccess: StateFlow<Boolean> =
        _loginSuccess

    private val _signUpSuccess =
        MutableStateFlow(false)

    val signUpSuccess: StateFlow<Boolean> =
        _signUpSuccess

    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error

    // -------------------------
    // SIGN IN
    // -------------------------

    fun signIn(
        email: String,
        password: String
    ) {

        if (email.isBlank()) {
            _error.value =
                "Please enter your email"
            return
        }

        if (password.isBlank()) {
            _error.value =
                "Please enter your password"
            return
        }

        viewModelScope.launch {

            try {

                _isLoading.value = true
                _error.value = null

                repository.signIn(
                    email = email,
                    password = password
                )

                _loginSuccess.value = true

            } catch (exception: Exception) {

                _error.value =
                    exception.message
                        ?: "Sign in failed"

            } finally {

                _isLoading.value = false
            }
        }
    }

    // -------------------------
    // SIGN UP
    // -------------------------

    fun signUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        role: String,
        companyCode: String
    ) {

        if (name.isBlank()) {
            _error.value =
                "Please enter your name"
            return
        }

        if (email.isBlank()) {
            _error.value =
                "Please enter your email"
            return
        }

        if (password.isBlank()) {
            _error.value =
                "Please enter your password"
            return
        }

        if (confirmPassword.isBlank()) {
            _error.value =
                "Please confirm your password"
            return
        }

        if (password != confirmPassword) {
            _error.value =
                "Passwords do not match"
            return
        }

        if (password.length < 6) {
            _error.value =
                "Password must be at least 6 characters"
            return
        }

        if (role.equals(
                "MANAGER",
                ignoreCase = true
            )
        ) {

            if (companyCode.isBlank()) {
                _error.value =
                    "Please enter the company code"
                return
            }
        }

        viewModelScope.launch {

            try {

                _isLoading.value = true
                _error.value = null

                repository.signUp(
                    name = name,
                    email = email,
                    password = password,
                    role = role
                )

                _signUpSuccess.value = true

            } catch (exception: Exception) {

                _error.value =
                    exception.message
                        ?: "Sign up failed"

            } finally {

                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}