package com.example.weather.Activities

import Recycleradapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weather.R
import com.example.weather.databinding.ActivityMainFavBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivityFav : AppCompatActivity() {

    private lateinit var binding: ActivityMainFavBinding
    private val locationDatabase by lazy { Locationdatabase.getDatabase(this).LocationDao() }
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: Recycleradapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainFavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = Recycleradapter()
        binding.locRec.layoutManager = LinearLayoutManager(this)
        binding.locRec.adapter = adapter
        observeloc()
    }

    private fun observeloc() {
        lifecycleScope.launch {
            locationDatabase.getAllLocations().collect { locationsList ->
                val locationNames = locationsList.map { locationEntity ->
                    locationEntity.locationName
                }
               adapter.submitList(locationNames)
            }
        }
    }

}
