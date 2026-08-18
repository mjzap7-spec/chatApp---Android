package com.example.test.data.model

data class UserUiState(
    val users: List<User> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)