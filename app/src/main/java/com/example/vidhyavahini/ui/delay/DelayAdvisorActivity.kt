package com.example.vidhyavahini.ui.delay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.vidhyavahini.R
import com.example.vidhyavahini.api.Content
import com.example.vidhyavahini.api.GeminiRequest
import com.example.vidhyavahini.api.Part
import com.example.vidhyavahini.api.RetrofitClient
import com.example.vidhyavahini.databinding.ActivityDelayAdvisorBinding
import kotlinx.coroutines.launch
import retrofit2.HttpException

class DelayAdvisorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDelayAdvisorBinding
    // Ensure this API Key is valid and has "Generative Language API" enabled in Google Cloud Console
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private var parentPhone: String? = null
    private var apiCalled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityDelayAdvisorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        val route = intent.getStringExtra("ROUTE_CODE") ?: "VV-07"
        val lastStop = intent.getStringExtra("LAST_STOP") ?: "Market"
        val minutes = intent.getIntExtra("DELAY_MINUTES", 18)
        parentPhone = intent.getStringExtra("PARENT_CONTACT")

        binding.tvTimeInfo.text = "No ping received for $minutes minutes."

        if (!apiCalled) {
            apiCalled = true
            fetchAiSuggestion(route, lastStop, minutes)
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnMessageParent.setOnClickListener {
            if (parentPhone.isNullOrEmpty()) {
                Toast.makeText(this, "Parent contact not found", Toast.LENGTH_SHORT).show()
            } else {
                checkSmsPermissionAndSend(route, minutes)
            }
        }
    }

    private fun fetchAiSuggestion(route: String, stop: String, minutes: Int) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvAiResponse.visibility = View.GONE

        val prompt = """
        A student is waiting for bus $route. 
        The bus was last seen at $stop stop.
        No update has been received for $minutes minutes.
        Alternative routes available: 
        1) VV-03 via River Bank (22 mins, short walk)
        2) Shared auto from Market Stand (15 mins)
        Give a SHORT 2 sentence helpful suggestion for the student.
        Be friendly and simple.
    """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        // Wait 2 seconds before calling to avoid rate limits
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.generateContent(apiKey, request)
                    val suggestion = response.candidates
                        ?.firstOrNull()
                        ?.content?.parts
                        ?.firstOrNull()
                        ?.text

                    binding.progressBar.visibility = View.GONE
                    binding.tvAiResponse.visibility = View.VISIBLE
                    binding.tvAiResponse.text = suggestion
                        ?: "Bus appears delayed. Consider Route VV-03 via River Bank or a shared auto from Market Stand."

                } catch (e: HttpException) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvAiResponse.visibility = View.VISIBLE

                    when (e.code()) {
                        429 -> {
                            // Rate limited — show fallback suggestion
                            binding.tvAiResponse.text =
                                "Bus $route appears delayed by $minutes minutes. " +
                                        "Consider Route VV-03 via River Bank or a shared auto from Market Stand. " +
                                        "Please inform your college."
                        }
                        403 -> binding.tvAiResponse.text =
                            "API key issue. Please check your Gemini API key."
                        else -> binding.tvAiResponse.text =
                            "Bus appears delayed. Try Route VV-03 or a shared auto from Market Stand."
                    }

                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvAiResponse.visibility = View.VISIBLE
                    binding.tvAiResponse.text =
                        "Bus $route appears delayed. Consider Route VV-03 via River Bank " +
                                "or a shared auto from Market Stand as alternatives."
                }
            }
        }, 3000)
    }

    private fun checkSmsPermissionAndSend(route: String, minutes: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), 101)
        } else {
            sendSms(route, minutes)
        }
    }

    private fun sendSms(route: String, minutes: Int) {
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val message = "Update: Bus $route is delayed by $minutes mins. I am taking an alternative route."
            smsManager.sendTextMessage(parentPhone, null, message, null, null)
            Toast.makeText(this, "Message sent to parent", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show()
        }
    }
}
