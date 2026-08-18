package com.example.test.data.repository

import com.example.test.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore =
        FirebaseFirestore.getInstance()

    private val usersCollection =
        firestore.collection("users")


    // =========================================================
    // Get all users
    // =========================================================

    suspend fun getUsers(): List<User> {

        val snapshot =
            usersCollection
                .get()
                .await()

        return snapshot.documents.mapNotNull { document ->

            document.toObject(User::class.java)
        }
    }


    // =========================================================
    // Get one user
    // =========================================================

    suspend fun getUser(
        uid: String
    ): User? {

        val document =
            usersCollection
                .document(uid)
                .get()
                .await()

        return document.toObject(User::class.java)
    }


    // =========================================================
    // Update user
    // =========================================================

    suspend fun updateUser(
        user: User,
        newName: String,
        newEmail: String
    ) {

        usersCollection
            .document(user.uid)
            .update(
                mapOf(
                    "name" to newName,
                    "email" to newEmail
                )
            )
            .await()
    }


    // =========================================================
    // Update only name
    // =========================================================

    suspend fun updateUserName(
        uid: String,
        newName: String
    ) {

        usersCollection
            .document(uid)
            .update(
                "name",
                newName
            )
            .await()
    }


    // =========================================================
    // Delete user
    // =========================================================

    suspend fun deleteUser(
        uid: String
    ) {

        usersCollection
            .document(uid)
            .delete()
            .await()
    }

    suspend fun deleteUsers(
        users: List<User>
    ) {

        if (users.isEmpty()) {
            return
        }

        val batch =
            firestore.batch()

        users.forEach { user ->

            val userDocument =
                usersCollection.document(user.uid)

            batch.delete(userDocument)
        }

        batch.commit().await()
    }
}