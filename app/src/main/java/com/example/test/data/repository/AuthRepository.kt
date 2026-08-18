package com.example.test.data.repository

import com.example.test.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth =
        FirebaseAuth.getInstance()

    private val firestore =
        FirebaseFirestore.getInstance()

    suspend fun signIn(
        email: String,
        password: String
    ) {
        auth.signInWithEmailAndPassword(
            email,
            password
        ).await()
    }

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        role: String
    ) {

        val result =
            auth.createUserWithEmailAndPassword(
                email,
                password
            ).await()

        val uid =
            result.user?.uid
                ?: throw Exception("Could not create user")

        val user = User(
            uid = uid,
            name = name,
            email = email,
            role = role
        )

        firestore
            .collection("users")
            .document(uid)
            .set(user)
            .await()
    }

    fun getCurrentUser() =
        auth.currentUser

    fun signOut() {
        auth.signOut()
    }
}