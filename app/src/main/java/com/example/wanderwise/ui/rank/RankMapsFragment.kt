package com.example.wanderwise.ui.rank

import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.wanderwise.R
import com.example.wanderwise.data.database.City
import com.example.wanderwise.data.database.LocationCity
import com.example.wanderwise.data.database.Marker
import com.example.wanderwise.data.database.Score
import com.example.wanderwise.ui.ViewModelFactory
import com.example.wanderwise.ui.home.HomeViewModel
import com.example.wanderwise.ui.profile.ProfileViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds.Builder
import com.google.android.gms.maps.model.LatLngBounds.builder
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException

class RankMapsFragment : Fragment() {
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mMap: GoogleMap

    private val boundsBuilder = Builder()

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        mMap = googleMap

        val rank = listOf<Any>(
            BitmapDescriptorFactory.fromResource(R.drawable.rank1),
            BitmapDescriptorFactory.fromResource(R.drawable.rank2),
            BitmapDescriptorFactory.fromResource(R.drawable.rank3))

        val db = FirebaseDatabase.getInstance("https://wanderwise-application-default-rtdb.asia-southeast1.firebasedatabase.app")

        var location: Pair<Double, Double>? = null
        var location_label = ""

        lifecycleScope.launch(Dispatchers.Main) {
            homeViewModel = ViewModelFactory.getInstance(requireContext()).create(HomeViewModel::class.java)


            homeViewModel.getSessionUser().observe(viewLifecycleOwner) { user ->
                lifecycleScope.launch(Dispatchers.IO) {
                    location = getLatLngFromCityName(requireContext(), user.userLocation)

                    if (location != null){
                        boundsBuilder.include(LatLng(location!!.first, location!!.second))
                        location_label = user.userLocation
                    }
                }
            }

            var markers: ArrayList<Marker> = arrayListOf()
            val refCities = db.getReference("cities").get().await()

            lifecycleScope.launch(Dispatchers.IO) {
                refCities.children.forEach { city ->
                    val refScores =
                        db.getReference("scores/${city.key}").limitToLast(1).get().await()

                    refScores.children.forEach { score ->
                        markers.add(
                            Marker(
                                key = city.key,
                                score = score.getValue<Score>()!!.score,
                                location = city.child("location").getValue<LocationCity>()!!
                            )
                        )
                    }
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    val sortedMarkers =
                        markers.toList().sortedByDescending { it.score.toString().toDouble() }
                    val top3Markers = sortedMarkers.take(3)
                    var topMarkers = LatLng(0.0,0.0)
                    var user_location = LatLng(0.0, 0.0)

                    for (i in 0 until 3) {
                        val location: LocationCity = top3Markers[i].location as LocationCity
                        val markerLatLng = LatLng(location.lat as Double, location.lon as Double)

                        if (i == 0){
                            topMarkers = markerLatLng
                        }

                        mMap.addMarker(
                            MarkerOptions()
                                .position(markerLatLng)
                                .title(top3Markers[i].key.toString())
                                .snippet(top3Markers[i].score.toString())
                                .icon(rank[i] as? BitmapDescriptor)
                        ).also { marker ->
                            if (marker != null) {
                                marker.tag = top3Markers[i].key.toString()
                            }
                        }
                    }

                    if (location != null){
                        user_location = LatLng(location!!.first, location!!.second)
                        mMap.addMarker(
                            MarkerOptions()
                                .position(user_location)
                                .title("Your Current Location")
                                .snippet(location_label)
                        ).also { marker ->
                            if (marker != null) {
                                marker.tag = location_label
                            }
                        }
                    } else {
                        boundsBuilder.include(topMarkers)
                        user_location = topMarkers
                    }

                    val bound = boundsBuilder.build()
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            bound,
                            resources.displayMetrics.widthPixels,
                            resources.displayMetrics.heightPixels,
                            resources.displayMetrics.widthPixels/2
                        )
                    )

                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            user_location,
                            10.0f)
                    )

                    setMapStyle()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_rank_maps, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)


    }

    private fun setMapStyle() {
        try {
            val success =
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),
                    R.raw.map_style
                ))
            if (!success) {
                Log.e(TAG, getString(R.string.map_parsing))
            }
        } catch (exception: Resources.NotFoundException) {
            Log.e(TAG, getString(R.string.map), exception)
        }
    }

    @Suppress("DEPRECATION")
    suspend fun getLatLngFromCityName(context: Context, cityName: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO){
            val geocoder = Geocoder(context)
            val addresses: Deferred<MutableList<Address>> = async { geocoder.getFromLocationName(cityName, 1)!! }
            if (addresses.await().isNotEmpty()){
                val latitude = addresses.await().first().latitude
                val longitude = addresses.await().first().longitude
                Pair(latitude, longitude)
            } else {
                null
            }
        }
    }
}