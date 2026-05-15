package com.example.vidhyavahini.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.vidhyavahini.R
import com.example.vidhyavahini.databinding.ActivityHomeBinding
import com.example.vidhyavahini.model.Ping
import com.example.vidhyavahini.ui.delay.DelayAdvisorActivity
import com.example.vidhyavahini.ui.profile.ProfileActivity
import com.google.firebase.database.*
import com.example.vidhyavahini.ui.routes.RoutesActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var database: DatabaseReference
    private var routeCode: String? = null
    private var parentContact: String? = null
    private var userName: String? = null

    // ← New variables for delay tracking
    private var lastKnownStop: String? = null
    private var lastPingTimestamp: Long = 0L

    private val stops = arrayOf("Village", "School", "Market", "Stand", "College")
    private val stopDistances = mapOf(
        "Village" to 8.0,
        "School" to 5.8,
        "Market" to 2.4,
        "Stand" to 1.2,
        "College" to 0.0
    )
    private val stopTimes = mapOf(
        "Village" to 0,
        "School" to 7,
        "Market" to 14,
        "Stand" to 19,
        "College" to 22
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("vidya-vahini/users/$uid")
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        if (snapshot.exists()) {
                            val name = snapshot.child("name")
                                .getValue(String::class.java) ?: "Student"
                            val college = snapshot.child("college")
                                .getValue(String::class.java) ?: ""
                            val route = snapshot.child("routeCode")
                                .getValue(String::class.java) ?: "VV-07"
                            val routeName = snapshot.child("routeName")
                                .getValue(String::class.java) ?: "Village Road → College Gate"
                            val parent = snapshot.child("parentContact")
                                .getValue(String::class.java) ?: ""

                            val sharedPref = getSharedPreferences(
                                "vidya_vahini_prefs", Context.MODE_PRIVATE
                            )
                            with(sharedPref.edit()) {
                                putString("userName", name)
                                putString("college", college)
                                putString("routeCode", route)
                                putString("routeName", routeName)
                                putString("parentContact", parent)
                                apply()
                            }

                            userName = name
                            routeCode = route
                            parentContact = parent
                            binding.tvGreeting.text = "Namaste, $name!"
                            binding.tvUserSub.text = "$college • $route"
                            binding.tvRouteName.text = routeName

                            setupFirebase()
                        }
                    }

                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        loadFromSharedPrefs()
                    }
                })
        } else {
            loadFromSharedPrefs()
        }
    }

    private fun loadFromSharedPrefs() {
        val sharedPref = getSharedPreferences("vidya_vahini_prefs", Context.MODE_PRIVATE)
        userName = sharedPref.getString("userName", "Student")
        routeCode = sharedPref.getString("routeCode", "VV-07")
        val college = sharedPref.getString("college", "Govt Degree College")
        parentContact = sharedPref.getString("parentContact", "")
        val routeName = sharedPref.getString("routeName", "Village Road → College Gate")

        binding.tvGreeting.text = "Namaste, $userName!"
        binding.tvUserSub.text = "$college • $routeCode"
        binding.tvRouteName.text = routeName

        setupFirebase()
    }

    private fun setupFirebase() {
        database = FirebaseDatabase.getInstance().getReference("vidya-vahini")

        routeCode?.let { code ->
            database.child("routes").child(code).child("pings")
                .limitToLast(1)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val lastPing = snapshot.children.firstOrNull()?.getValue(Ping::class.java)
                        lastPing?.let { updateUIWithPing(it) }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@HomeActivity, "Error loading pings", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun updateUIWithPing(ping: Ping) {
        val lastStop = ping.stop
        // ← Save last known stop and timestamp
        lastKnownStop = lastStop
        lastPingTimestamp = ping.timestamp
        binding.tvEtaValue.text = "~${calculateEta(lastStop)} mins"
        binding.tvDistance.text = "${stopDistances[lastStop] ?: 0.0}km away"
        updateRouteTracker(lastStop)
    }

    // ← New function to calculate delay minutes
    private fun calculateDelayMinutes(): Int {
        if (lastPingTimestamp == 0L) return 0
        val currentTime = System.currentTimeMillis()
        return ((currentTime - lastPingTimestamp) / 60000).toInt()
    }

    private fun updateRouteTracker(currentStop: String) {
        val stopIndex = stops.indexOf(currentStop)

        val stopViews = listOf(
            binding.stop1,
            binding.stop2,
            binding.stop3,
            binding.stop4,
            binding.stop5
        )

        stopViews.forEachIndexed { index, view ->
            when {
                index < stopIndex -> {
                    view.setImageResource(R.drawable.circle_green)
                    view.layoutParams.width = 20.dpToPx()
                    view.layoutParams.height = 20.dpToPx()
                }
                index == stopIndex -> {
                    view.setImageResource(R.drawable.circle_orange)
                    view.layoutParams.width = 32.dpToPx()
                    view.layoutParams.height = 32.dpToPx()
                }
                else -> {
                    view.setImageResource(R.drawable.circle_grey)
                    view.layoutParams.width = 20.dpToPx()
                    view.layoutParams.height = 20.dpToPx()
                }
            }
            view.requestLayout()
        }

        val labels = listOf(
            binding.label1,
            binding.label2,
            binding.label3,
            binding.label4,
            binding.label5
        )

        labels.forEachIndexed { i, label ->
            label.setTextColor(
                if (i == stopIndex)
                    ContextCompat.getColor(this, R.color.accent)
                else
                    ContextCompat.getColor(this, R.color.black)
            )
            label.setTypeface(
                null,
                if (i == stopIndex)
                    android.graphics.Typeface.BOLD
                else
                    android.graphics.Typeface.NORMAL
            )
        }
    }

    fun Int.dpToPx(): Int {
        return (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun calculateEta(lastPingedStop: String): Int {
        val studentStop = "College"
        val targetTime = stopTimes[studentStop] ?: 22
        val lastTime = stopTimes[lastPingedStop] ?: 0
        return (targetTime - lastTime).coerceAtLeast(0)
    }

    private fun setupListeners() {
        binding.fabPing.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Bus crossed which stop?")
            builder.setItems(stops) { _, index ->
                sendPing(stops[index])
            }
            builder.show()
        }

        binding.btnBreakdown.setOnClickListener {
            sendSmsToParent("Alert: Your child's bus $routeCode appears to have a breakdown. Please wait for updates.")
            Toast.makeText(this, "Breakdown report sent to parent", Toast.LENGTH_LONG).show()
        }

        binding.btnReachedSafely.setOnClickListener {
            sendSmsToParent("I have reached college safely. - $userName")
            Toast.makeText(this, "Safe reach notification sent", Toast.LENGTH_SHORT).show()
        }

        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true

                R.id.nav_routes -> {
                    val intent = Intent(this, RoutesActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }

                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }

                // ← Fixed: now passes dynamic data to DelayAdvisorActivity
                R.id.nav_alerts -> {
                    val intent = Intent(this, DelayAdvisorActivity::class.java)
                    intent.putExtra("ROUTE_CODE", routeCode ?: "VV-07")
                    intent.putExtra("LAST_STOP", lastKnownStop ?: "Unknown")
                    intent.putExtra("DELAY_MINUTES", calculateDelayMinutes())
                    intent.putExtra("PARENT_CONTACT", parentContact ?: "")
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    true
                }

                else -> true
            }
        }
    }

    private fun sendPing(stop: String) {
        val pingId = database.child("routes").child(routeCode!!).child("pings").push().key
        val ping = Ping(
            studentId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID),
            studentName = userName ?: "Anonymous",
            stop = stop,
            timestamp = System.currentTimeMillis()
        )

        pingId?.let {
            database.child("routes").child(routeCode!!).child("pings").child(it).setValue(ping)
                .addOnSuccessListener {
                    val sharedPref = getSharedPreferences("vidya_vahini_prefs", Context.MODE_PRIVATE)
                    val currentPings = sharedPref.getInt("pingCount", 0)
                    sharedPref.edit().putInt("pingCount", currentPings + 1).apply()
                    Toast.makeText(this, "Ping Sent! Everyone updated.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun sendSmsToParent(message: String) {
        if (!parentContact.isNullOrEmpty()) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(parentContact, null, message, null, null)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$parentContact")
                    putExtra("sms_body", message)
                }
                startActivity(intent)
            }
        }
    }
}