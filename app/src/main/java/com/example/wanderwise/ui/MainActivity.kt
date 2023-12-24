package com.example.wanderwise.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat.requestLocationUpdates
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.wanderwise.R
import com.example.wanderwise.data.preferences.UserModel
import com.example.wanderwise.databinding.ActivityMainBinding
import com.example.wanderwise.ui.home.HomeFragment
import com.example.wanderwise.ui.profile.ProfileViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks.await
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var profileViewModel: ProfileViewModel
    private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO){
            profileViewModel = getViewModel()

            fusedLocationClient = withContext(Dispatchers.IO){
                LocationServices.getFusedLocationProviderClient(this@MainActivity)
            }

            withContext(Dispatchers.Main){
                checkPlayServices()

                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    requestLocationUpdates()
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }

            withContext(Dispatchers.Main){
                binding = ActivityMainBinding.inflate(layoutInflater)
                setContentView(binding.root)

                val navView: BottomNavigationView = binding.navView

                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                val appBarConfiguration = AppBarConfiguration(
                    setOf(
                        R.id.homeFragment, R.id.postFragment, R.id.rankMapsFragment, R.id.profileFragment
                    )
                )
                setupActionBarWithNavController(navController, appBarConfiguration)
                navView.setupWithNavController(navController)

                navView.setOnNavigationItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.homeFragment -> {
                            findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.homeFragment)
                            true
                        }
                        R.id.rankMapsFragment -> {
                            findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.rankMapsFragment)
                            true
                        }
                        R.id.postFragment -> {
                            findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.postFragment)
                            true
                        }
                        R.id.profileFragment -> {
                            findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.profileFragment)
                            true
                        }
                        else -> false
                    }
                }
            }
        }
        supportActionBar?.hide()
    }

    private suspend fun requestLocationUpdates() {
        //Check if the necessary location permissions are granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                getLocationRequest(),
                locationCallback,
                null
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun getLocationRequest(): LocationRequest {
        var interval = 6000*5
        var onlyOnce = false
        profileViewModel.getSessionUser().observe(this@MainActivity) {
            if (it.userLocation.isEmpty() && !onlyOnce) {
                onlyOnce = true
                interval = 0
            }
        }
        return LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(interval.toLong()) //Update location every 5 minutes
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            Log.d("location-log", "Location: $location")

            // Now you have the latitude and longitude
            val latitude = location?.latitude
            val longitude = location?.longitude
            Log.d("location-log", "Latitude: $latitude, Longitude: $longitude")

            if (latitude != null && longitude != null) {
                getCityNameFromLatLng(latitude, longitude)
            }
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            // Handle location availability changes if needed
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        lifecycleScope.launch {
            if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, request location updates
                    Log.d("location-log", "permission granted")
                    requestLocationUpdates()
                } else {
                    // Permission denied, handle accordingly
                    Log.d("location-log","Location permission denied")
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)

        if (currentFragment is HomeFragment) {
            finishAffinity()
        }
    }

    @Suppress("DEPRECATION")
    private fun getCityNameFromLatLng(latitude: Double, longitude: Double) {
        lifecycleScope.launch(Dispatchers.IO){
            var current_user_location = ""
            var onlyOnce = false
            val addresses = async { Geocoder(this@MainActivity, Locale.getDefault()).getFromLocation(latitude, longitude, 1) }
            val cityName = async {
                addresses.await()?.get(0)?.subAdminArea?.replace(Regex("(Kota|City|Kabupaten)"), "")
                    ?.replace(Regex("\\s+"), "") ?: ""
            }

            lifecycleScope.launch(Dispatchers.Main){
                cityName.join()
                profileViewModel.getSessionUser().observe(this@MainActivity) { user ->
                    if (!onlyOnce){
                        lifecycleScope.launch(Dispatchers.IO){
                            profileViewModel.editUserModel(UserModel(
                                name = user.name,
                                token = user.token,
                                email = user.email,
                                uid = user.uid,
                                userLocation = cityName.await(),
                                currentActivity = "profile",
                                isLogin = true
                            )).also {
                                current_user_location = user.userLocation
                                onlyOnce = true
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun getViewModel(): ProfileViewModel {
        return ViewModelFactory.getInstance(this@MainActivity).create(ProfileViewModel::class.java)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private fun checkPlayServices() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                val errorDialog: Dialog = apiAvailability.getErrorDialog(
                    this,
                    resultCode,
                    PLAY_SERVICES_RESOLUTION_REQUEST
                )!!
                errorDialog.show()
            }
        }
    }
}
