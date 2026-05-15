package com.example.vidhyavahini.ui.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vidhyavahini.R
import com.example.vidhyavahini.databinding.ActivityRoutesBinding
import com.example.vidhyavahini.databinding.ItemRouteBinding
import com.google.firebase.database.*

data class RouteItem(
    val code: String,
    val name: String,
    val status: String,
    val lastPingStop: String = "No updates yet"
)

class RoutesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoutesBinding
    private lateinit var database: DatabaseReference
    private val routeList = mutableListOf<RouteItem>()
    private lateinit var adapter: RoutesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

        adapter = RoutesAdapter(routeList)
        binding.rvRoutes.layoutManager = LinearLayoutManager(this)
        binding.rvRoutes.adapter = adapter

        loadRoutesFromFirebase()
    }

    private fun loadRoutesFromFirebase() {
        database = FirebaseDatabase.getInstance().getReference("vidya-vahini/routes")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                routeList.clear()

                for (routeSnapshot in snapshot.children) {
                    val code = routeSnapshot.key ?: continue
                    val name = routeSnapshot.child("name")
                        .getValue(String::class.java) ?: "Unknown Route"
                    val active = routeSnapshot.child("active")
                        .getValue(Boolean::class.java) ?: false

                    // Get last ping stop
                    val pingsSnapshot = routeSnapshot.child("pings")
                    var lastStop = "No updates yet"
                    var latestTimestamp = 0L

                    for (pingSnapshot in pingsSnapshot.children) {
                        val timestamp = pingSnapshot.child("timestamp")
                            .getValue(Long::class.java) ?: 0L
                        if (timestamp > latestTimestamp) {
                            latestTimestamp = timestamp
                            lastStop = pingSnapshot.child("stop")
                                .getValue(String::class.java) ?: "Unknown"
                        }
                    }

                    // Determine status based on last ping time
                    val currentTime = System.currentTimeMillis()
                    val minutesSincePing = (currentTime - latestTimestamp) / 60000

                    val status = when {
                        latestTimestamp == 0L -> "NO DATA"
                        minutesSincePing > 15 -> "DELAYED"
                        active -> "ACTIVE"
                        else -> "ON TIME"
                    }

                    routeList.add(RouteItem(code, name, status, lastStop))
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@RoutesActivity,
                    "Error loading routes",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}

class RoutesAdapter(private val routes: List<RouteItem>) :
    RecyclerView.Adapter<RoutesAdapter.RouteViewHolder>() {

    inner class RouteViewHolder(private val binding: ItemRouteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(route: RouteItem) {
            binding.tvRouteCode.text = route.code
            binding.tvRouteName.text = route.name
            binding.tvStatus.text = route.status

            // Last ping info
            binding.tvLastPing.text = "Last seen: ${route.lastPingStop}"

            val (bgColor, textColor) = when (route.status) {
                "ACTIVE" -> Pair(
                    android.graphics.Color.parseColor("#4CAF50"),
                    android.graphics.Color.WHITE
                )
                "DELAYED" -> Pair(
                    android.graphics.Color.parseColor("#FF9800"),
                    android.graphics.Color.WHITE
                )
                "ON TIME" -> Pair(
                    android.graphics.Color.parseColor("#2196F3"),
                    android.graphics.Color.WHITE
                )
                "NO DATA" -> Pair(
                    android.graphics.Color.parseColor("#9E9E9E"),
                    android.graphics.Color.WHITE
                )
                else -> Pair(
                    android.graphics.Color.parseColor("#9E9E9E"),
                    android.graphics.Color.WHITE
                )
            }

            binding.tvStatus.setBackgroundColor(bgColor)
            binding.tvStatus.setTextColor(textColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routes[position])
    }

    override fun getItemCount() = routes.size
}