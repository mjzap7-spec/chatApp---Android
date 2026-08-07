package com.example.test

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.databinding.ActivityFirstMainBinding

class FirstMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFirstMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityFirstMainBinding.inflate(layoutInflater)


            setContentView(binding.root)

            binding.btnSignin.setOnClickListener {
                startActivity(
                    Intent(this, SignInActivity::class.java)
                )
            }

            binding.btnSignup.setOnClickListener {
                startActivity(
                    Intent(this, SignUpActivity::class.java)
                )
            }
        } catch (exception: Exception) {
            Log.e(
                "APP_TEST",
                "FirstMainActivity failed",
                exception
            )

            Toast.makeText(
                this,
                exception.message ?: "Unknown startup error",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
    }
}