package com.example.weather.Activities

import android.content.Context
import android.Manifest
//import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
//import android.media.tv.AdRequest
import android.location.Location
import android.net.DnsResolver.Callback
import android.os.Build
import android.os.PersistableBundle
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.weather.ApiKey
import com.example.weather.Models.WeatherModel
import com.example.weather.R
import com.example.weather.Utilites.ApiUtilities
import com.example.weather.databinding.ActivityMain2Binding
import com.example.weather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import java.text.SimpleDateFormat
import retrofit2.Response
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMain2Binding

    private lateinit var currentlocation: Location
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 101

    private val apiKey=ApiKey.API_KEY
    private var mInterstitialAd: InterstitialAd?=null

    lateinit var database:Locationdatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar()?.hide();

        binding = DataBindingUtil.setContentView(this,R.layout.activity_main2)


        MobileAds.initialize(this){}
        val adRequest=AdRequest.Builder().build()
        binding.bannerAds.loadAd(adRequest)
        //mAdView.loadAd(adRequest)
        loadAds()

        fusedLocationProvider=LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
        binding.citySearch.setOnEditorActionListener{
            textView,i,keyEvent->
            if(i== EditorInfo.IME_ACTION_SEARCH){
                getCityWeather(binding.citySearch.text.toString())
                val view=this.currentFocus
                if(view!=null){
                    val imm:InputMethodManager=getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken,0)
                    binding.citySearch.clearFocus()
                }
                return@setOnEditorActionListener true
            }
            else{
                return@setOnEditorActionListener false
            }
        }
        binding.currentLocation.setOnClickListener{
            getCurrentLocation()
        }


        database= Room.databaseBuilder(applicationContext,
        Locationdatabase::class.java,
        "locations.db").build()

    }

    private fun seefavloc() {
        val intent = Intent(this, MainActivityFav::class.java)
        startActivity(intent)
    }

    private fun addloc() {

        val locationName = binding.citySearch.text.toString()
       GlobalScope.launch{
           database.LocationDao().insertLocation(LocationEntity(locationName))
       }
        Toast.makeText(this, "Location added: $locationName", Toast.LENGTH_SHORT).show()

    }


    private fun getCityWeather(city:String){
        binding.progressBar.visibility= View.VISIBLE
    ApiUtilities.getApiInterface()?.getCityWeather(city,apiKey)?.enqueue(
        object:retrofit2.Callback<WeatherModel> {
            //@RequiresApi(Build.VERSION_CODES.0)
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call:
             Call<WeatherModel>,
             response: Response<WeatherModel>)
            {
               if(response.isSuccessful){
                   loadAds()
                   if(mInterstitialAd!=null){
                       mInterstitialAd?.show(this@MainActivity)
                   }
                   else{
                   }
                   binding.progressBar.visibility=View.GONE
                   response.body()?.let {weatherModel ->
                   setData(weatherModel)
                   }
               }
                else{
                    Toast.makeText(this@MainActivity, "No City Found",
                    Toast.LENGTH_SHORT).show()
                   binding.progressBar.visibility=View.GONE
                   }
               }
            override fun onFailure(call: Call<WeatherModel>,t:Throwable){
            }
        })
    }

    private fun loadAds(){
        val adRequest=AdRequest.Builder().build()
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712",adRequest,object:
        InterstitialAdLoadCallback(){
            override fun onAdFailedToLoad(p0:LoadAdError){
                mInterstitialAd = null
            }
            override fun onAdLoaded(p0:InterstitialAd){
                mInterstitialAd = p0
            }
        })
    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude,longitude,apiKey)
            ?.enqueue(object :retrofit2.Callback<WeatherModel>{
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<WeatherModel>, response: Response<WeatherModel>) {
                    if (response.isSuccessful){
                        binding.progressBar.visibility= View.GONE
                        response.body()?.let {
                            setData(it)
                        }
                    }
                }
                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                }
            })
    }

        private fun getCurrentLocation(){
            if(checkPermissions()){
                if(isLocationEnabled()){
                    if(ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                        requestPermissions()
                        return
                    }
                    fusedLocationProvider.lastLocation.addOnSuccessListener { location ->
                        if(location!=null){
                            currentlocation=location
                            binding.progressBar.visibility = View.VISIBLE
                            fetchCurrentLocationWeather(
                                location.latitude.toString(),
                                location.longitude.toString()
                            )
                        }
                    }
                }
                else{
                    val intent= Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            }
            else{
                requestPermissions()
            }
        }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_REQUEST_CODE
        )
    }

    private fun isLocationEnabled(): Boolean {
            val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE)
            as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermissions():Boolean{
        if(ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)
        ==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==LOCATION_REQUEST_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                getCurrentLocation()
            }
            else{
            }
        }
    }

    //@SuppressLint("SetTextI18n", "SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setData(body:WeatherModel){
        binding.apply {
            val currentDate=SimpleDateFormat("dd/MM/yyyy hh:mm").format(Date())

            dateTime.text=currentDate.toString()

            maxTemp.text="Max "+k2c(body?.main?.temp_max!!)+"°"

            minTemp.text="Min "+k2c(body?.main?.temp_min!!)+"°"

            temp.text=""+k2c(body?.main?.temp!!)+"°"

            weatherTitle.text=body.weather[0].main

            sunriseValue.text=ts2td(body.sys.sunrise.toLong())

            sunsetValue.text=ts2td(body.sys.sunset.toLong())

            pressureValue.text=body.main.pressure.toString()

            humidityValue.text=body.main.humidity.toString()+"%"

            tempFValue.text=""+(k2c(body.main.temp).times(1.8)).plus(32).roundToInt()+"°"

            citySearch.setText(body.name)

            feelsLike.text= ""+k2c(body.main.feels_like)+"°"

            windValue.text=body.wind.speed.toString()+"m/s"

            groundValue.text=body.main.grnd_level.toString()

            seaValue.text=body.main.sea_level.toString()

            countryValue.text=body.sys.country

        }
        updateUI(body.weather[0].id)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ts2td(ts:Long):String{
        val localTime=ts.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
        return localTime.toString()
    }

    private fun k2c(t:Double):Double{
        var intTemp=t
        intTemp=intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }

    private fun updateUI(id: Int) {
        binding.apply {
            when (id) {
                //Thunderstorm
                in 200..232 -> {
                    weatherImg.setImageResource(R.drawable.ic_storm_weather)
                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)
                }
                //Drizzle
                in 300..321 -> {
                    weatherImg.setImageResource(R.drawable.ic_few_clouds)
                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.drizzle_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.drizzle_bg)
                }
                //Rain
                in 500..531 -> {
                    weatherImg.setImageResource(R.drawable.ic_rainy_weather)
                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.rain_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.rain_bg)
                }
                //Snow
                in 600..622 -> {
                    weatherImg.setImageResource(R.drawable.ic_snow_weather)
                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.snow_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.snow_bg)
                }

                //Atmosphere
                in 701..781 -> {
                    weatherImg.setImageResource(R.drawable.ic_broken_clouds)
                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.atmosphere_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.atmosphere_bg)
                }

                //Clear
                800 -> {
                    weatherImg.setImageResource(R.drawable.ic_clear_day)
                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clear_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clear_bg)
                }

                //Clouds
                in 801..804 -> {
                    weatherImg.setImageResource(R.drawable.ic_cloudy_weather)
                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clouds_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clouds_bg)
                }
                //unknown
                else -> {
                    weatherImg.setImageResource(R.drawable.ic_unknown)
                    mainLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.unknown_bg)
                    optionsLayout.background = ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.unknown_bg)
                }
            }
        }
    }

    }