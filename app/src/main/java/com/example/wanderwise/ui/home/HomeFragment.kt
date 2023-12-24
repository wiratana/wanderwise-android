package com.example.wanderwise.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.wanderwise.ui.detailcity.DetailInfoCityActivity
import com.example.wanderwise.ui.home.emergency.EmergencyActivity
import com.example.wanderwise.ui.home.morecity.ExploreCityMoreActivity
import com.example.wanderwise.ui.home.favorite.FavoriteActivity
import com.example.wanderwise.ui.home.notification.NotificationActivity
import com.example.wanderwise.R
import com.example.wanderwise.data.database.City
import com.example.wanderwise.data.database.Information
import com.example.wanderwise.data.database.Score
import com.example.wanderwise.data.database.ScoreCurrent
import com.example.wanderwise.data.database.Weather
import com.example.wanderwise.data.local.database.CityFavorite
import com.example.wanderwise.data.preferences.UserModel
import com.example.wanderwise.data.response.CreatedAt
import com.example.wanderwise.data.response.PostsItem
import com.example.wanderwise.databinding.FragmentHomeBinding
import com.example.wanderwise.ui.ViewModelFactory
import com.example.wanderwise.ui.adapter.CityExploreAdapter
import com.example.wanderwise.ui.adapter.PostHomeAdapter
import com.example.wanderwise.utils.MyLocation
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var cityAdapter: CityExploreAdapter

    private lateinit var _binding: FragmentHomeBinding
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var postAdapter: PostHomeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        lifecycleScope.launch{
            (requireActivity().application as MyLocation).sharedData = ""
            var currentLoc = ""
            var disableDetailAccess = false

            homeViewModel = ViewModelFactory.getInstance(requireContext()).create(HomeViewModel::class.java)

            val db = FirebaseDatabase.getInstance("https://wanderwise-application-default-rtdb.asia-southeast1.firebasedatabase.app")

            homeViewModel.getSessionUser().observe(viewLifecycleOwner) { cityUser ->
                currentLoc = cityUser.userLocation
                Log.d("current-location-log", cityUser.userLocation)

                lifecycleScope.launch(Dispatchers.IO) {
                    val ref = db.getReference("cities/${currentLoc}")
                    val cityListener = object : ValueEventListener {
                        @SuppressLint("SetTextI18n")
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                if (dataSnapshot.exists()) {
                                    binding.locationName.text = dataSnapshot.key.toString()
                                    Glide.with(requireActivity())
                                        .load(dataSnapshot.getValue<City>()!!.image.toString())
                                        .transform(CenterCrop(), RoundedCorners(40))
                                        .into(binding.cityImage)
                                } else {
                                    binding.locationName.text = "Unlisted"
                                    binding.cityImage.setImageResource(R.drawable.baseline_warning_image)
                                    disableDetailAccess = true
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.w("TAG", "loadPost:onCancelled", databaseError.toException())
                        }
                    }
                    ref.addValueEventListener(cityListener)
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    val currentTime = System.currentTimeMillis()
                    val oneDayAgo = currentTime - (24 * 60 * 60 * 1000)
                    val refNotifications =
                        db.getReference("notifications/${currentLoc}").orderByChild("timestamp")
                            .startAt(oneDayAgo.toString().toDouble())
                    val notificationListener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                binding.notifAmount.text = snapshot.childrenCount.toString()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.w("TAG", "loadPost:onCancelled", error.toException())
                        }
                    }
                    refNotifications.addValueEventListener(notificationListener)
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    val refWeathers = db.getReference("weathers/${currentLoc}")
                    val weatherListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.childrenCount > 0) {
                                val temperature =
                                    dataSnapshot.getValue<Weather>()!!.temperature.toString()
                                val formattedTemperature = getString(R.string._29_c, temperature)

                                lifecycleScope.launch(Dispatchers.Main) {
                                    binding.temperature.text = formattedTemperature

                                    when (dataSnapshot.getValue<Weather>()!!.weather.toString()) {
                                        "rain" -> {
                                            binding.weatherIcon.setImageResource(R.drawable.rainy)
                                        }

                                        "sunny" -> {
                                            binding.weatherIcon.setImageResource(R.drawable.sunny)
                                        }

                                        "stormy" -> {
                                            binding.weatherIcon.setImageResource(R.drawable.stormy)
                                        }

                                        "cloudy" -> {
                                            binding.weatherIcon.setImageResource(R.drawable.cloudy)
                                        }

                                        "windy" -> {
                                            binding.weatherIcon.setImageResource(R.drawable.windy)
                                        }
                                    }
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.w("TAG", "loadPost:onCancelled", databaseError.toException())
                        }
                    }
                    refWeathers.addValueEventListener(weatherListener)
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    val refScores = db.getReference("scores/$currentLoc").limitToLast(1)
                    var scoreCurrent: Any? = null
                    val scoreCurrentListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.childrenCount > 0) {
                                dataSnapshot.children.map {
                                    scoreCurrent = it.getValue<ScoreCurrent>()!!.score
                                }
                            } else {
                                scoreCurrent = 0
                            }

                            lifecycleScope.launch(Dispatchers.Main) {
                                if (scoreCurrent.toString().toDouble() <= 33) {
                                    binding.safetyLevelText.text = getString(R.string.danger)
                                    binding.safetyIcon.setImageResource(R.drawable.danger_icon_small)
                                } else if (scoreCurrent.toString().toDouble() <= 70) {
                                    binding.safetyLevelText.text = getString(R.string.warning)
                                    binding.safetyIcon.setImageResource(R.drawable.warning_icon_small)
                                } else if (scoreCurrent.toString().toDouble() <= 100) {
                                    binding.safetyLevelText.text = getString(R.string.safe)
                                    binding.safetyIcon.setImageResource(R.drawable.safe_icon_small)
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.w("TAG", "loadPost:onCancelled", databaseError.toException())
                        }
                    }
                    refScores.addValueEventListener(scoreCurrentListener)
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    val refInformation =
                        db.getReference("informations/${currentLoc}").limitToLast(1)
                    val infoListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                if (dataSnapshot.exists()) {
                                    dataSnapshot.children.map {
                                        binding.destinationsAmount.text =
                                            it.getValue<Information>()!!.numberOfDestinations.toString()
                                        binding.hospitalAmount.text =
                                            it.getValue<Information>()!!.numberOfHospitals.toString()
                                        binding.policeAmount.text =
                                            it.getValue<Information>()!!.numberOfPoliceStations.toString()
                                    }
                                } else {
                                    binding.destinationsAmount.text = "0"
                                    binding.hospitalAmount.text = "0"
                                    binding.policeAmount.text = "0"
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.w("TAG", "loadPost:onCancelled", databaseError.toException())
                        }
                    }
                    refInformation.addValueEventListener(infoListener)
                }
            }

            lifecycleScope.launch(Dispatchers.IO) {
                var cities = ArrayList<City>()
                var scores = ArrayList<Double>()

                val citiesSnapshot = db.getReference("cities").get().await()

                citiesSnapshot.children.forEach { city ->
                    cities.add(
                        City(
                            city.key,
                            city.getValue<City>()!!.area,
                            city.getValue<City>()!!.capital,
                            city.getValue<City>()!!.country,
                            city.getValue<City>()!!.description,
                            city.getValue<City>()!!.image,
                            city.getValue<City>()!!.location
                        )
                    )
                }

                val deferredScores = cities.map { city ->
                    async {
                        val scoreSnapshot =
                            db.getReference("scores/${city.key}").limitToLast(1).get().await()

                        if (scoreSnapshot.childrenCount > 0) {
                            scoreSnapshot.children.first().getValue<Score>()?.score?.toString()
                                ?.toDouble() ?: 0.0
                        } else {
                            0.0
                        }
                    }
                }

                scores.addAll(deferredScores.map { it.await() })

                lifecycleScope.launch(Dispatchers.Main) {
                    cityAdapter = CityExploreAdapter(
                        requireActivity(),
                        cities,
                        scores,
                        homeViewModel,
                        CityFavorite(id = 0, key = "", isLoved = false),
                        viewLifecycleOwner
                    )

                    binding.exploreCityRv.layoutManager =
                        LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
                    binding.exploreCityRv.setHasFixedSize(true)
                    binding.exploreCityRv.adapter = cityAdapter

                    cities = ArrayList()
                    scores = ArrayList()
                }
            }

            lifecycleScope.launch (Dispatchers.IO){
                val dbPost = Firebase.firestore
                var userAllPosts = ArrayList<PostsItem>()


                val postsDataSnapshot = dbPost.collection("posts").get().await()

                postsDataSnapshot.documents.forEach{doc ->
                    userAllPosts.add(
                        PostsItem(
                            image = doc.getString("image"),
                            createdAt = CreatedAt(
                                seconds = doc.getTimestamp("createdAt")!!.seconds,
                                nanoseconds = doc.getTimestamp("createdAt")!!.nanoseconds
                            ),
                            caption = doc.getString("caption"),
                            id = doc.getString("userId"),
                            title = doc.getString("city"),
                            idPost =  doc.getString("idPost"),
                            name = doc.getString("name")
                        )
                    )
                }

                lifecycleScope.launch (Dispatchers.Main){
                    postAdapter = PostHomeAdapter(requireActivity(), userAllPosts)
                    binding.popularPostRv.layoutManager = LinearLayoutManager(requireActivity(),  LinearLayoutManager.HORIZONTAL, false)
                    binding.popularPostRv.setHasFixedSize(true)
                    binding.popularPostRv.adapter = postAdapter
                    userAllPosts = ArrayList()
                }
            }



            binding.notificationButton.setOnClickListener {
                val intentNotif = Intent(activity, NotificationActivity::class.java)
                intentNotif.putExtra("cityKey", currentLoc)
                startActivity(intentNotif)
            }

            binding.cardDetailCity.setOnClickListener {
                if (!disableDetailAccess) {
                    val intent = Intent(activity, DetailInfoCityActivity::class.java)
                    intent.putExtra(DetailInfoCityActivity.KEY_CITY, currentLoc)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Your Location is Not Listed in Our Database",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            homeViewModel.getSessionUser().observe(viewLifecycleOwner) {
                binding.usernameUser.text = it.name
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            binding.favoriteButton.setOnClickListener {
                val intentFavorite = Intent(activity, FavoriteActivity::class.java)
                startActivity(intentFavorite)
            }

            binding.seeDetailButton.setOnClickListener {
                val intentExplore = Intent(activity, ExploreCityMoreActivity::class.java)
                startActivity(intentExplore)
            }

            binding.emergencyButton.setOnClickListener {
                val intentEmergency = Intent(activity, EmergencyActivity::class.java)
                startActivity(intentEmergency)
            }

            binding.seeAll.setOnClickListener {
                findNavController().popBackStack(R.id.homeFragment, false)
                findNavController().navigate(R.id.postFragment)
            }
        }

        return binding.root
    }
}