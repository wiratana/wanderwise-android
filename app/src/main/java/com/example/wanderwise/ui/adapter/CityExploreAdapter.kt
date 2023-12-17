package com.example.wanderwise.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.wanderwise.R
import com.example.wanderwise.data.database.City
import com.example.wanderwise.data.database.Score
import com.example.wanderwise.data.database.Weather
import com.example.wanderwise.databinding.ListExploreCityBinding
import com.example.wanderwise.ui.detailcity.DestinationFragment
import com.example.wanderwise.ui.detailcity.DetailInfoCityActivity


class CityExploreAdapter(
    private val context: Context,
    private val cities: ArrayList<City>,
    private val scores: ArrayList<Score>
) : RecyclerView.Adapter<CityExploreAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding = ListExploreCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val city = cities[position]
        val score = scores[position]
        holder.bind(context, score, city)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailInfoCityActivity::class.java)
            intent.putExtra(DetailInfoCityActivity.CITY, city.key.toString())
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return cities.size
    }

    class MyViewHolder(val binding: ListExploreCityBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(context: Context, score: Score, city: City) {

            Glide.with(binding.root)
                .load(city.image)
                .into(binding.imagePreview)

            binding.cityName.text = city.key.toString()

            if (city.key == score.key) {
                if (score.score.toString().toInt() <= 33) {
                    binding.safetyLevel.text = context.getString(R.string.danger)
                    binding.iconSafetyMedium.setImageResource(R.drawable.danger_icon_medium)
                } else if (score.score.toString().toInt() <= 70) {
                    binding.safetyLevel.text = context.getString(R.string.warning)
                    binding.iconSafetyMedium.setImageResource(R.drawable.warning_icon_medium)
                } else if (score.score.toString().toInt() <= 100) {
                    binding.safetyLevel.text = context.getString(R.string.safe)
                    binding.iconSafetyMedium.setImageResource(R.drawable.safe_icon_medium)
                }
            }
        }
    }
}