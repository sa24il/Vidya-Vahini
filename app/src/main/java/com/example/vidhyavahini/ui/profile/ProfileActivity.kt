package com.example.vidhyavahini.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.vidhyavahini.R
import com.example.vidhyavahini.databinding.ActivityProfileBinding
import com.example.vidhyavahini.ui.home.HomeActivity
import com.example.vidhyavahini.ui.login.LoginActivity
import com.example.vidhyavahini.ui.delay.DelayAdvisorActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        auth = FirebaseAuth.getInstance()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

        loadProfileFromFirebase()
        setupListeners()
    }

    private fun loadProfileFromFirebase() {
        // Show loading state first — don't load from SharedPreferences
        binding.tvProfileNameText.text = "Loading..."
        binding.tvProfileInitial.text = "..."
        binding.tvProfileEmail.text = "..."
        binding.tvPingCount.text = "0"
        binding.tvDaysTracked.text = "0"

        val uid = auth.currentUser?.uid

        if (uid == null) {
            loadFromSharedPrefs()
            return
        }

        FirebaseDatabase.getInstance()
            .getReference("vidya-vahini/users/$uid")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name")
                            .getValue(String::class.java) ?: "Student"
                        val mobile = snapshot.child("mobile")
                            .getValue(String::class.java) ?: ""
                        val college = snapshot.child("college")
                            .getValue(String::class.java) ?: ""
                        val routeCode = snapshot.child("routeCode")
                            .getValue(String::class.java) ?: "VV-07"
                        val parentContact = snapshot.child("parentContact")
                            .getValue(String::class.java) ?: ""

                        // Update SharedPreferences with correct data
                        val sharedPref = getSharedPreferences(
                            "vidya_vahini_prefs", Context.MODE_PRIVATE
                        )
                        with(sharedPref.edit()) {
                            putString("userName", name)
                            putString("mobile", mobile)
                            putString("college", college)
                            putString("routeCode", routeCode)
                            putString("parentContact", parentContact)
                            apply()
                        }

                        loadPingCount(uid, name, mobile, college, routeCode, parentContact)
                    } else {
                        loadFromSharedPrefs()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    loadFromSharedPrefs()
                }
            })
    }

    private fun loadPingCount(
        uid: String,
        name: String,
        mobile: String,
        college: String,
        routeCode: String,
        parentContact: String
    ) {
        // Count actual pings from Firebase
        val sharedPref = getSharedPreferences("vidya_vahini_prefs", Context.MODE_PRIVATE)
        val androidId = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        FirebaseDatabase.getInstance()
            .getReference("vidya-vahini/routes/$routeCode/pings")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var userPingCount = 0
                    for (pingSnapshot in snapshot.children) {
                        val pingStudentId = pingSnapshot.child("studentId")
                            .getValue(String::class.java)
                        if (pingStudentId == androidId) {
                            userPingCount++
                        }
                    }

                    // Update ping count in Firebase
                    FirebaseDatabase.getInstance()
                        .getReference("vidya-vahini/users/$uid/pingCount")
                        .setValue(userPingCount)

                    // Update UI
                    updateUI(name, mobile, college, routeCode, userPingCount)
                }

                override fun onCancelled(error: DatabaseError) {
                    val savedPings = sharedPref.getInt("pingCount", 0)
                    updateUI(name, mobile, college, routeCode, savedPings)
                }
            })
    }

    private fun updateUI(
        name: String,
        mobile: String,
        college: String,
        routeCode: String,
        pingCount: Int
    ) {
        binding.tvProfileNameText.text = name
        binding.tvProfileEmail.text = mobile.ifEmpty { "No mobile saved" }
        binding.tvProfileInitial.text = name.take(1).uppercase()
        binding.tvRouteBadge.text = "Route $routeCode"
        binding.tvPingCount.text = pingCount.toString()
        binding.tvDaysTracked.text = "142"
    }

    private fun loadFromSharedPrefs() {
        val sharedPref = getSharedPreferences("vidya_vahini_prefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("userName", "Student") ?: "Student"
        val mobile = sharedPref.getString("mobile", "") ?: ""
        val routeCode = sharedPref.getString("routeCode", "VV-07") ?: "VV-07"
        val pings = sharedPref.getInt("pingCount", 0)
        updateUI(name, mobile, "", routeCode, pings)
    }

    private fun setupListeners() {
        binding.btnSavedRoutes.setOnClickListener {
            Toast.makeText(this, "Saved Routes coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            // Sign out from Firebase
            auth.signOut()

            // Clear SharedPreferences
            val sharedPref = getSharedPreferences("vidya_vahini_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        binding.bottomNav.selectedItemId = R.id.nav_profile
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }
                R.id.nav_alerts -> {
                    val intent = Intent(this, DelayAdvisorActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }
                else -> true
            }
        }
    }
}