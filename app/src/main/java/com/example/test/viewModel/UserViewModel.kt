package com.example.test.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.data.model.User
import com.example.test.data.model.UserUiState
import com.example.test.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val repository =
        UserRepository()


    // =========================================================
    // UI STATE
    // =========================================================

    private val _uiState =
        MutableStateFlow(UserUiState())

    val uiState: StateFlow<UserUiState> =
        _uiState


    // =========================================================
    // Load users
    // =========================================================

    fun loadUsers() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    error = null
                )

            try {

                val users =
                    repository.getUsers()

                _uiState.value =
                    _uiState.value.copy(
                        users = users,
                        isLoading = false
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        error =
                            e.message
                                ?: "Failed to load users"
                    )
            }
        }
    }


    // =========================================================
    // Load current user
    // =========================================================

    fun loadCurrentUser(
        uid: String
    ) {

        viewModelScope.launch {

            try {

                val user =
                    repository.getUser(uid)

                _uiState.value =
                    _uiState.value.copy(
                        currentUser = user
                    )

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        error =
                            e.message
                                ?: "Failed to load current user"
                    )
            }
        }
    }


    // =========================================================
    // Update user
    // =========================================================

    fun updateUser(
        user: User,
        newName: String,
        newEmail: String
    ) {

        if (newName.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    error = "Name cannot be empty"
                )

            return
        }

        if (newEmail.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    error = "Email cannot be empty"
                )

            return
        }

        viewModelScope.launch {

            try {

                repository.updateUser(
                    user = user,
                    newName = newName,
                    newEmail = newEmail
                )

                loadUsers()

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        error =
                            e.message
                                ?: "Failed to update user"
                    )
            }
        }
    }


    // =========================================================
    // Update name
    // =========================================================

    fun updateUserName(
        user: User,
        newName: String
    ) {

        if (newName.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    error = "Name cannot be empty"
                )

            return
        }

        viewModelScope.launch {

            try {

                repository.updateUserName(
                    uid = user.uid,
                    newName = newName
                )

                loadUsers()

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        error =
                            e.message
                                ?: "Failed to update user"
                    )
            }
        }
    }


    // =========================================================
    // Delete one user
    // =========================================================

    fun deleteUser(
        user: User
    ) {

        viewModelScope.launch {

            try {

                repository.deleteUser(
                    user.uid
                )

                loadUsers()

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        error =
                            e.message
                                ?: "Failed to delete user"
                    )
            }
        }
    }


    // =========================================================
    // Delete multiple users
    // =========================================================

    fun deleteUsers(
        users: List<User>
    ) {

        if (users.isEmpty()) {
            return
        }

        viewModelScope.launch {

            try {

                repository.deleteUsers(
                    users
                )

                loadUsers()

            } catch (e: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        error =
                            e.message
                                ?: "Failed to delete users"
                    )
            }
        }
    }
}