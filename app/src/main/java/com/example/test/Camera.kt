package com.example.test

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageView
import android.content.Intent
import android.widget.TextView
import com.example.test.databinding.ActivityCameraBinding
class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            println("Arrow clicked")
            finish()
        }
        val denyButton = findViewById<TextView>(R.id.denyButton)
        val allowButton = findViewById<TextView>(R.id.allowButton)
        binding.denyButton.setOnClickListener {
            denyButton.isSelected = true
            allowButton.isSelected = false

        }
        binding.allowButton.setOnClickListener {
            allowButton.isSelected = true
            denyButton.isSelected = false
        }
    }
}