package com.example.vidhyavahini.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.vidhyavahini.R
import com.example.vidhyavahini.databinding.ActivityLoginBinding
import com.example.vidhyavahini.ui.home.HomeActivity
import com.example.vidhyavahini.ui.onboarding.OnboardingActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var isPinVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        auth = FirebaseAuth.getInstance()

        // Auto-login check using Firebase Auth
        if (auth.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        setupListeners()
    }

    private fun setupListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                validateInputs()
            }
            override fun afterTextChanged(p0: Editable?) {}
        }

        binding.etMobile.addTextChangedListener(textWatcher)
        binding.etPin.addTextChangedListener(textWatcher)

        binding.tvShowPin.setOnClickListener {
            isPinVisible = !isPinVisible
            if (isPinVisible) {
                binding.etPin.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.tvShowPin.text = "HIDE"
            } else {
                binding.etPin.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.tvShowPin.text = "SHOW"
            }
            binding.etPin.setSelection(binding.etPin.text.length)
        }

        binding.btnSignIn.setOnClickListener {
            val mobile = binding.etMobile.text.toString().trim()
            val pin = binding.etPin.text.toString().trim()

            // Convert mobile + PIN to fake email for Firebase Auth
            val email = "${mobile}@vidyavahini.app"

            binding.btnSignIn.isEnabled = false
            binding.btnSignIn.text = "Signing in..."

            // Change this line
            auth.signInWithEmailAndPassword(email, pin + "VV")
                .addOnSuccessListener {
                    // Save to SharedPreferences
                    val sharedPref = getSharedPreferences("vidya_vahini_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit().putBoolean("isLoggedIn", true).apply()

                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    binding.btnSignIn.isEnabled = true
                    binding.btnSignIn.text = "Sign In"
                    Toast.makeText(
                        this,
                        "Invalid number or PIN. New user? Create Account.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        binding.btnCreateAccount.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

    private fun validateInputs() {
        val mobile = binding.etMobile.text.toString()
        val pin = binding.etPin.text.toString()
        val isValid = mobile.length == 10 && pin.length == 4
        binding.btnSignIn.isEnabled = isValid

        if (isValid) {
            binding.btnSignIn.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.accent)
            binding.btnSignIn.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            binding.btnSignIn.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.grey)
            binding.btnSignIn.setTextColor(ContextCompat.getColor(this, R.color.black))
        }
    }
}