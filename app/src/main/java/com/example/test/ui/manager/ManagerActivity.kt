package com.example.test.ui.manager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.test.databinding.ActivityManagerBinding
import com.example.test.ui.auth.SignInActivity
import com.example.test.ui.community.CommunityActivity
import com.example.test.ui.user.UserListActivity
import com.example.test.viewmodel.ManagerViewModel
import com.google.firebase.auth.FirebaseAuth
import es.dmoral.toasty.Toasty

class ManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerBinding

    private val viewModel: ManagerViewModel by viewModels()

    private val auth =
        FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityManagerBinding.inflate(layoutInflater)

        setContentView(binding.root)

        observeViewModel()

        verifyManager()

        setupButtons()
    }

    private fun verifyManager() {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            openSignIn()
            return
        }

        viewModel.verifyManager(
            currentUser.uid
        )
    }

    private fun observeViewModel() {

        viewModel.isManager.observe(this) { isManager ->

            if (!isManager) {

                Toasty.error(
                    this,
                    "Manager access only",
                    Toast.LENGTH_SHORT
                ).show()

                auth.signOut()

                openSignIn()
            }
        }

        viewModel.error.observe(this) { message ->

            message ?: return@observe

            Toasty.error(
                this,
                message,
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
}