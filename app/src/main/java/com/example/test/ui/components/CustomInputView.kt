package com.example.test.ui.components

import android.content.Context
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.test.databinding.ViewCustomInputBinding

class CustomInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding: ViewCustomInputBinding

    private var isPasswordVisible = false

    init {

        orientation = VERTICAL

        binding = ViewCustomInputBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )

        binding.btnTogglePassword.visibility = GONE

    }


    fun setLabel(text: String) {
        binding.tvLabel.text = text
    }


    fun setHint(text: String) {
        binding.etInput.hint = text
    }


    fun getText(): String {
        return binding.etInput.text.toString()
    }


    fun setPasswordMode() {

        binding.etInput.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD


        binding.btnTogglePassword.visibility = VISIBLE


        binding.btnTogglePassword.setOnClickListener {

            if (isPasswordVisible) {

                binding.etInput.transformationMethod =
                    PasswordTransformationMethod.getInstance()

                isPasswordVisible = false

            } else {

                binding.etInput.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()

                isPasswordVisible = true
            }


            binding.etInput.setSelection(
                binding.etInput.text.length
            )
        }
    }
}