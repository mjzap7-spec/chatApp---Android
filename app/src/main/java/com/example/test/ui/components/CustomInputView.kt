package com.example.test.ui.components

import android.content.Context
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.test.R
import com.example.test.databinding.ViewCustomInputBinding
import android.view.View

class CustomInputView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(
    context,
    attrs,
    defStyleAttr
) {

    private val binding: ViewCustomInputBinding

    private var passwordVisible = false

    init {

        binding =
            ViewCustomInputBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
            )

        setupPasswordButton()
    }


    // =========================================================
    // Label
    // =========================================================

    fun setLabel(label: String) {

        binding.tvLabel.text = label
    }


    // =========================================================
    // Hint
    // =========================================================

    fun setHint(hint: String) {

        binding.etInput.hint = hint
    }


    // =========================================================
    // Get text
    // =========================================================

    fun getText(): String {

        return binding.etInput.text
            .toString()
    }


    // =========================================================
    // Set text
    // =========================================================

    fun setText(text: String) {

        binding.etInput.setText(text)
    }


    // =========================================================
    // Password mode
    // =========================================================

    fun setPasswordMode() {

        binding.etInput.inputType =
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        binding.btnTogglePassword.visibility =
            View.VISIBLE

        var passwordVisible = false

        binding.btnTogglePassword.setOnClickListener {

            passwordVisible = !passwordVisible

            if (passwordVisible) {

                binding.etInput.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

                binding.btnTogglePassword.setImageResource(
                    R.drawable.ic_visibility_off
                )

            } else {

                binding.etInput.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

                binding.btnTogglePassword.setImageResource(
                    R.drawable.ic_visibility
                )
            }

            binding.etInput.setSelection(
                binding.etInput.text.length
            )
        }
    }


    // =========================================================
    // Password eye
    // =========================================================

    private fun setupPasswordButton() {

        binding.btnTogglePassword.setOnClickListener {

            passwordVisible =
                !passwordVisible

            if (passwordVisible) {

                // Show password

                binding.etInput.transformationMethod =
                    null

                binding.btnTogglePassword.setImageResource(
                    R.drawable.ic_visibility_off
                )

                binding.btnTogglePassword.contentDescription =
                    "Hide password"

            } else {

                // Hide password

                binding.etInput.transformationMethod =
                    PasswordTransformationMethod.getInstance()

                binding.btnTogglePassword.setImageResource(
                    R.drawable.ic_visibility
                )

                binding.btnTogglePassword.contentDescription =
                    "Show password"
            }

            binding.etInput.setSelection(
                binding.etInput.text.length
            )
        }
    }
}