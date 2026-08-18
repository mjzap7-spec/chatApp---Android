package com.example.test.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.data.repository.UserRepository
import kotlinx.coroutines.launch

class ManagerViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _isManager = MutableLiveData<Boolean>()
    val isManager: LiveData<Boolean> = _isManager

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun verifyManager(uid: String) {

        viewModelScope.launch {

            try {

                val user = repository.getUser(uid)

                if (user == null) {
                    _isManager.value = false
                    return@launch
                }

                val manager =
                    user.role.equals(
                        "MANAGER",
                        ignoreCase = true
                    )

                _isManager.value = manager

            } catch (exception: Exception) {

                _error.value =
                    exception.message
                        ?: "Could not verify manager access"
            }
        }
    }
}