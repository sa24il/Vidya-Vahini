package com.example.vidhyavahini.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vidhyavahini.databinding.ActivityAlertsBinding
import com.example.vidhyavahini.databinding.ItemAlertBinding
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.vidhyavahini.R

data class AlertItem(val title: String, val message: String, val time: String)

class AlertsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure the window fits system windows to prevent status bar overlap
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

        val alerts = listOf(
            AlertItem("Bus Started", "VV-07 has started from Village Stand.", "07:30 AM"),
            AlertItem("Delay Alert", "VV-03 is running 15 mins late due to traffic.", "08:10 AM"),
            AlertItem("New Ping", "A student reported VV-07 crossing School Stop.", "08:15 AM"),
            AlertItem("Weather Update", "Heavy rain expected. Expect minor delays.", "Yesterday")
        )

        binding.rvAlerts.layoutManager = LinearLayoutManager(this)
        binding.rvAlerts.adapter = AlertsAdapter(alerts)
    }
}

class AlertsAdapter(private val alerts: List<AlertItem>) : RecyclerView.Adapter<AlertsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAlertBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alert = alerts[position]
        holder.binding.tvAlertTitle.text = alert.title
        holder.binding.tvAlertMessage.text = alert.message
        holder.binding.tvAlertTime.text = alert.time
    }

    override fun getItemCount() = alerts.size
}
