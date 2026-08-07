package com.example.test

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.databinding.ActivityManagerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.dmoral.toasty.Toasty

class ManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        verifyManager()
    }

    private fun verifyManager() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            openSignIn()
            return
        }

        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->

                val role = document.getString("role") ?: "USER"

                if (!role.equals("MANAGER", ignoreCase = true)) {
                    Toasty.error(
                        this,
                        "Manager access only",
                        Toast.LENGTH_SHORT
                    ).show()

                    auth.signOut()
                    openSignIn()
                    return@addOnSuccessListener
                }

                setupButtons()
            }
            .addOnFailureListener {
                Toasty.error(
                    this,
                    "Could not verify manager access",
                    Toast.LENGTH_SHORT
                ).show()

                openSignIn()
            }
    }

    private fun setupButtons() {
        binding.btnManageUsers.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    UserListActivity::class.java
                )
            )
        }

        binding.btnCommunity.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    CommunityActivity::class.java
                )
            )
        }
    }

    private fun openSignIn() {
        val intent = Intent(
            this,
            SignInActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}