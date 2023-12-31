package com.example.wanderwise.ui.home.favorite

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wanderwise.data.database.City
import com.example.wanderwise.data.database.DataNeed
import com.example.wanderwise.data.database.Information
import com.example.wanderwise.data.database.Score
import com.example.wanderwise.data.database.Weather
import com.example.wanderwise.data.local.database.CityFavorite
import com.example.wanderwise.databinding.ActivityFavoriteBinding
import com.example.wanderwise.ui.ViewModelFactory
import com.example.wanderwise.ui.adapter.DetailListCityAdapter
import com.example.wanderwise.ui.home.HomeViewModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class FavoriteActivity : AppCompatActivity() {

    private lateinit var homeViewModel:HomeViewModel

    private lateinit var binding: ActivityFavoriteBinding

    private lateinit var detailListCityAdapter: DetailListCityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch(Dispatchers.Main){
            homeViewModel = ViewModelFactory.getInstance(this@FavoriteActivity).create(HomeViewModel::class.java)

            val db = FirebaseDatabase.getInstance("https://wanderwise-application-default-rtdb.asia-southeast1.firebasedatabase.app")

            homeViewModel.getAllCity().observe(this@FavoriteActivity) { cityLove ->
                lifecycleScope.launch(Dispatchers.IO) {
                    var cities:MutableMap<String, DataNeed> = mutableMapOf()
                    try {
                        cityLove.forEach() {
                            val favCityJob = async {
                                val citySnapshot = db.getReference("cities/${it.key}").get().await()

                                if (citySnapshot.exists()){
                                    cities.put(
                                        it.key.toString(),
                                        DataNeed(
                                            key = it.key,
                                            image = citySnapshot.getValue<City>()!!.image.toString(),
                                            area = citySnapshot.getValue<City>()!!.area
                                        ))
                                } else {
                                    null
                                }

                                val informationSnapshot = db.getReference("informations/${it.key}").limitToLast(1).get().await()

                                if (informationSnapshot.hasChildren()){
                                    informationSnapshot.children.forEach{information ->
                                        cities[it.key]?.numberOfDestination = information.getValue<Information>()!!.numberOfDestinations
                                        cities[it.key]?.numberOfHospitals = information.getValue<Information>()!!.numberOfHospitals
                                        cities[it.key]?.numberOfPoliceStations = information.getValue<Information>()!!.numberOfPoliceStations
                                    }
                                }

                                val weatherSnapshot = db.getReference("weathers/${it.key}").get().await()

                                if (weatherSnapshot.exists()){
                                    cities[it.key]?.weather = weatherSnapshot.getValue<Weather>()!!.weather
                                    cities[it.key]?.temperature = weatherSnapshot.getValue<Weather>()!!.temperature
                                }

                                val scoreSnapshot = db.getReference("scores/${it.key}").limitToLast(1).get().await()

                                if (scoreSnapshot.hasChildren()){
                                    scoreSnapshot.children.forEach{
                                        cities[it.key]?.score = scoreSnapshot.getValue<Score>()!!.score
                                    }
                                }
                            }

                            favCityJob.await()
                        }


                        val cityFavorite: CityFavorite = CityFavorite(
                            id = 0,
                            key = "",
                            isLoved = false
                        )

                        Log.d("favorite-city-data", "$cities")
                        lifecycleScope.launch(Dispatchers.Main) {
                            detailListCityAdapter = DetailListCityAdapter(homeViewModel,this@FavoriteActivity, cities, cityFavorite, lifecycleScope)
                            binding.rvCityLove.layoutManager = LinearLayoutManager(this@FavoriteActivity)
                            binding.rvCityLove.setHasFixedSize(true)
                            binding.rvCityLove.adapter = detailListCityAdapter
                        }
                    }  catch (e: Exception){
                        Log.e("Error", "Failed to fetch data: ${e.message}")
                    }
                }
            }
        }

        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FFFFFF")))
        supportActionBar?.title = "Favorite city"
    }

}
