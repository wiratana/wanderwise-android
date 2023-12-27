package com.example.wanderwise.ui.home.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wanderwise.R
import com.example.wanderwise.data.database.City
import com.example.wanderwise.data.database.Notification
import com.example.wanderwise.databinding.ActivityNotificationBinding
import com.example.wanderwise.ui.ViewModelFactory
import com.example.wanderwise.ui.adapter.DestinationAdapter
import com.example.wanderwise.ui.adapter.NotificationAdapter
import com.example.wanderwise.ui.home.HomeViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FFFFFF")))
        supportActionBar?.title = "Notification"

        lifecycleScope.launch(Dispatchers.IO) {
            val cityKey = intent.getStringExtra("cityKey")
            val db = FirebaseDatabase.getInstance("https://wanderwise-application-default-rtdb.asia-southeast1.firebasedatabase.app")
            val currentTime = System.currentTimeMillis()
            val oneDayAgo = currentTime - (24 * 60 * 60 * 1000)
            val refNotification = db.getReference("notifications/${cityKey}").orderByChild("timestamp").startAt(oneDayAgo.toString().toDouble())
            val notificationListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val notifications = ArrayList<Notification>()
                    dataSnapshot.children.map {
                        notifications.add(
                            Notification(
                                key = it.key,
                                subject = it.getValue<Notification>()!!.subject,
                                message = it.getValue<Notification>()!!.message,
                                level = it.getValue<Notification>()!!.level,
                                timestamp = it.getValue<Notification>()!!.timestamp
                            )
                        )
                    }

                    notifications.forEach() {
                        sendNotification(it.subject.toString(), it.message.toString())
                    }

                    val notificationAdapter = NotificationAdapter(notifications)
                    binding.rvNotification.layoutManager = LinearLayoutManager(this@NotificationActivity)
                    binding.rvNotification.setHasFixedSize(true)
                    binding.rvNotification.adapter = notificationAdapter
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("TAG", "loadPost:onCancelled", databaseError.toException())
                }
            }
            refNotification.addValueEventListener(notificationListener)
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notifDetailIntent = Intent(this, NotificationActivity::class.java)

        val pendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(notifDetailIntent)
            getPendingIntent(NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.baseline_notifications_active)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            builder.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "channel_01"
        private const val CHANNEL_NAME = "dicoding channel"
    }
}