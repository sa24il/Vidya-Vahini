package com.example.vidhyavahini.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.vidhyavahini.R
import com.example.vidhyavahini.databinding.ActivityOnboardingBinding
import com.example.vidhyavahini.ui.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var auth: FirebaseAuth
    private var currentStep = 1

    private val colleges = arrayOf(
        "Govt Science College",
        "Rural Tech Institute",
        "Village Arts Academy"
    )
    private val routes = arrayOf(
        "VV-07 (Village Road → College Gate)",
        "VV-03 (River Bank → Tech Park)",
        "VV-12 (Main Stand → Science Block)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        auth = FirebaseAuth.getInstance()

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        val collegeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            colleges
        )
        binding.spinnerCollege.adapter = collegeAdapter

        val routeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            routes
        )
        binding.spinnerRoute.adapter = routeAdapter
    }

    private fun setupListeners() {
        binding.btnStep1Next.setOnClickListener {
            if (validateStep1()) {
                currentStep = 2
                updateUiForStep()
            }
        }

        binding.btnStep2Next.setOnClickListener {
            currentStep = 3
            updateUiForStep()
        }

        binding.tvSkipStep2.setOnClickListener {
            currentStep = 3
            updateUiForStep()
        }

        binding.btnFinishOnboarding.setOnClickListener {
            registerAndSave()
        }
    }

    private fun validateStep1(): Boolean {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etParentPhone.text.toString().trim()
        val mobile = binding.etMobile.text.toString().trim()
        val pin = binding.etPin.text.toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = "Name required"
            return false
        }
        if (mobile.length != 10) {
            binding.etMobile.error = "Valid 10-digit mobile required"
            return false
        }
        if (pin.length != 4) {
            binding.etPin.error = "PIN must be 4 digits"
            return false
        }
        if (phone.length != 10) {
            binding.etParentPhone.error = "Valid parent phone required"
            return false
        }
        return true
    }

    private fun updateUiForStep() {
        binding.onboardingFlipper.displayedChild = currentStep - 1
        binding.tvStepCount.text = "Step $currentStep of 3"

        binding.step1Indicator.background = ContextCompat.getDrawable(
            this, if (currentStep >= 1) R.drawable.circle_orange else R.drawable.circle_grey
        )
        binding.step2Indicator.background = ContextCompat.getDrawable(
            this, if (currentStep >= 2) R.drawable.circle_orange else R.drawable.circle_grey
        )
        binding.step3Indicator.background = ContextCompat.getDrawable(
            this, if (currentStep >= 3) R.drawable.circle_orange else R.drawable.circle_grey
        )

        if (currentStep == 3) {
            val routeFull = binding.spinnerRoute.selectedItem.toString()
            binding.tvSelectedRoute.text = "Tracking Route ${routeFull.split(" ")[0]}"
            binding.tvSummaryCollege.text = "College: ${binding.spinnerCollege.selectedItem}"
            binding.tvSummaryPhone.text = "Parent: +91 ${binding.etParentPhone.text}"
        }
    }

    private fun registerAndSave() {
        val name = binding.etName.text.toString().trim()
        val parentPhone = binding.etParentPhone.text.toString().trim()
        val college = binding.spinnerCollege.selectedItem.toString()
        val routeFull = binding.spinnerRoute.selectedItem.toString()
        val routeCode = routeFull.split(" ")[0]


        // Get mobile and PIN from onboarding step 1
        val mobile = binding.etMobile?.text?.toString()?.trim() ?: ""
        val pin = binding.etPin?.text?.toString()?.trim() ?: ""

        if (mobile.length != 10 || pin.length != 4) {
            Toast.makeText(this, "Please enter valid mobile and PIN", Toast.LENGTH_SHORT).show()
            return
        }

        val email = "${mobile}@vidyavahini.app"

        binding.btnFinishOnboarding.isEnabled = false
        binding.btnFinishOnboarding.text = "Setting up..."

        // Create Firebase Auth account
        auth.createUserWithEmailAndPassword(email, pin + "VV")
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener

                // Save user data to Firebase Database
                val userData = mapOf(
                    "name" to name,
                    "mobile" to mobile,
                    "parentContact" to parentPhone,
                    "college" to college,
                    "routeCode" to routeCode,
                    "routeName" to routeFull,
                    "pingCount" to 0,
                    "createdAt" to System.currentTimeMillis()
                )

                FirebaseDatabase.getInstance()
                    .getReference("vidya-vahini/users/$uid")
                    .setValue(userData)
                    .addOnSuccessListener {
                        // Save to SharedPreferences as well
                        val sharedPref = getSharedPreferences(
                            "vidya_vahini_prefs",
                            Context.MODE_PRIVATE
                        )
                        with(sharedPref.edit()) {
                            putString("userName", name)
                            putString("parentContact", parentPhone)
                            putString("college", college)
                            putString("routeCode", routeCode)
                            putString("routeName", routeFull)
                            putString("uid", uid)
                            putString("mobile", mobile)
                            putBoolean("isLoggedIn", true)
                            putBoolean("isSetupDone", true)
                            putInt("pingCount", 0)
                            apply()
                        }

                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                binding.btnFinishOnboarding.isEnabled = true
                binding.btnFinishOnboarding.text = "Start Tracking"
                Toast.makeText(
                    this,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}