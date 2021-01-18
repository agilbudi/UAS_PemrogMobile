package com.agilbudiprasetyo.covid19apps

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.agilbudiprasetyo.covid19apps.network.APIService
import com.agilbudiprasetyo.covid19apps.network.AllCountries
import com.agilbudiprasetyo.covid19apps.network.Countries
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var countryAdapter: CountryAdapter
    private var ascending = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        getCountry()

        btn_sequence.setOnClickListener {
            sequenceListener(ascending)
            ascending = !ascending
        }

        search_view.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                countryAdapter.filter.filter(newText)
                return false
            }
        })

        swipe_refresh.setOnRefreshListener {
            getCountry()
            swipe_refresh.isRefreshing = false
        }
    }

    private fun sequenceListener(ascending: Boolean) {
        rv_country.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
            if (ascending){
                (layoutManager as LinearLayoutManager).reverseLayout = true
                (layoutManager as LinearLayoutManager).stackFromEnd = true
                Toast.makeText(this@MainActivity, "Sorted Z-A", Toast.LENGTH_SHORT).show()
            }else{
                (layoutManager as LinearLayoutManager).reverseLayout = false
                (layoutManager as LinearLayoutManager).stackFromEnd = false
                Toast.makeText(this@MainActivity, "Sorted A-Z", Toast.LENGTH_SHORT).show()
            }
            adapter = countryAdapter
        }
    }

    private fun getCountry() {
        val okHttp = OkHttpClient().newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder().baseUrl("https://api.covid19api.com/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(APIService::class.java)
        api.getAllCountries().enqueue(object : Callback<AllCountries>{
            override fun onResponse(call: Call<AllCountries>, response: Response<AllCountries>) {
                if (response.isSuccessful){
                    // Menampung data JSON dari objek
                    val dataCovid19 = response.body()?.Global
                    val formatter: NumberFormat = DecimalFormat("#,###")
                    txt_confirmed_globe.text = formatter.format(dataCovid19?.TotalConfirmed?.toDouble())
                    txt_deaths_globe.text = formatter.format(dataCovid19?.TotalDeaths?.toDouble())
                    txt_recovered_globe.text = formatter.format(dataCovid19?.TotalRecovered?.toDouble())

                    rv_country.apply {
                        layoutManager = LinearLayoutManager(this@MainActivity)
                        setHasFixedSize(true)

                        countryAdapter = CountryAdapter(response.body()?.Countries as ArrayList<Countries>){
                            itemClicked(it)
                        }
                        adapter = countryAdapter
                        progress_bar.visibility = View.GONE
                    }
                }else{
                    handleError(this@MainActivity)
                }
            }

            override fun onFailure(call: Call<AllCountries>, t: Throwable) {
                getCountry()
            }
        })
    }

    private fun itemClicked(country: Countries) {
        val intent = Intent(this, ChartCountryActivity::class.java)
        intent.putExtra(ChartCountryActivity.EXTRA_COUNTRY, country.Country)
        intent.putExtra(ChartCountryActivity.EXTRA_DATE, country.Date)
        intent.putExtra(ChartCountryActivity.EXTRA_COUNTRY_CODE, country.CountryCode)

        intent.putExtra(ChartCountryActivity.EXTRA_NEW_DEATH, country.NewDeaths)
        intent.putExtra(ChartCountryActivity.EXTRA_NEW_CONFIRMED, country.NewConfirmed)
        intent.putExtra(ChartCountryActivity.EXTRA_NEW_RECOVERED, country.NewRecovered)

        intent.putExtra(ChartCountryActivity.EXTRA_TOTAL_CONFIRMED, country.TotalConfirmed)
        intent.putExtra(ChartCountryActivity.EXTRA_TOTAL_DEATH, country.TotalDeaths)
        intent.putExtra(ChartCountryActivity.EXTRA_TOTAL_RECOVERED, country.TotalRecovered)

        startActivity(intent)
    }

    private fun handleError(mainActivity: MainActivity) {
        TODO("Not yet implemented")
    }
}
