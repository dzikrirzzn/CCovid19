package com.example.covid19

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.example.covid19.model.CountriesItem
import com.example.covid19.model.InfoNegara
import com.example.covid19.network.InfoService
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.android.synthetic.main.activity_detail_chart_country.*
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class DetailChartCountry : AppCompatActivity() {

    companion object{
        const val EXTRA_COUNTRY = "EXTRA_COUNTRY"
        lateinit var simpanDataNegara: String
        lateinit var simpanDataFlag: String
    }

    private val sharedProfile = "kotlinsharedpreference"
    private lateinit var sharedPreference : SharedPreferences
    private var dayCases = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_chart_country)

        sharedPreference = this.getSharedPreferences(sharedProfile, Context.MODE_PRIVATE)
        val editor : SharedPreferences.Editor = sharedPreference.edit()

        val data = intent.getParcelableExtra<CountriesItem>(EXTRA_COUNTRY)
        val formatter : NumberFormat = DecimalFormat("#,###")

        data?.let {
            txt_name_country.text = data.country
            latest_update.text = data.date
            hasil_new_confirm_currently.text = formatter.format(data.newConfirmed?.toDouble())
            hasil_total_confirm_currently.text = formatter.format(data.totalConfirmed?.toDouble())
            hasil_new_recover_currently.text = formatter.format(data.newRecovered?.toDouble())
            hasil_total_recover_currently.text = formatter.format(data.totalRecovered?.toDouble())
            hasil_new_death_currently.text = formatter.format(data.newDeaths?.toDouble())
            hasil_total_death_currently.text = formatter.format(data.totalDeaths?.toDouble())
            Glide.with(this).load("https://www.countryflags.io/" + data.countryCode + "/flat/64.png").into(img_flag_country)

            editor.putString(data.country, data.country)
            editor.apply()
            editor.commit()

            val simpanNegara = sharedPreference.getString(data.country, data.country)
            val simpanFlag = sharedPreference.getString(data.countryCode, data.countryCode)
            simpanDataNegara = simpanNegara.toString()
            simpanDataFlag = simpanFlag.toString() + "/flat/64.png"

            if (simpanFlag != null){
                Glide.with(this).load("https://www.countryflags.io/$simpanDataFlag")
                    .into(img_flag_country)
            } else{
                Toast.makeText(this,"Gambar tidak ketemu", Toast.LENGTH_SHORT).show()
            }

            getChart()

        }
    }

    private fun getChart(){
        val okhttp = OkHttpClient().newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.covid19api.com/dayone/country/")
            .client(okhttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(InfoService::class.java)
        api.getInfoService(simpanDataNegara).enqueue(object : Callback<List<InfoNegara>>{
            @SuppressLint("SimpleDateFormat")
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onFailure(call: Call<List<InfoNegara>>, t: Throwable) {
                Toast.makeText(this@DetailChartCountry,"Error", Toast.LENGTH_SHORT).show()
            }

            @SuppressLint("SimpleDateFormat")
            override fun onResponse(
                call: Call<List<InfoNegara>>, response: Response<List<InfoNegara>>) {
                val getListDataCorona : List<InfoNegara> = response.body()!!
                if (response.isSuccessful){
                    val barEnteries : ArrayList<BarEntry> = ArrayList()
                    val barEnteries2 : ArrayList<BarEntry> = ArrayList()
                    val barEnteries3 : ArrayList<BarEntry> = ArrayList()
                    val barEnteries4 : ArrayList<BarEntry> = ArrayList()
                    var i = 0

                    while (i < getListDataCorona.size){
                        for (s in getListDataCorona){
                            val barEntry = BarEntry(i.toFloat(), s.Confirmed?.toFloat() ?: 0f)
                            val barEntry2 = BarEntry(i.toFloat(), s.Deaths?.toFloat() ?: 0f)
                            val barEntry3 = BarEntry(i.toFloat(), s.Recovered?.toFloat() ?: 0f)
                            val barEntry4 = BarEntry(i.toFloat(), s.Active?.toFloat() ?: 0f)

                            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SS'Z'")
                            val outputFormat = SimpleDateFormat("dd-MM-yyyy")
                            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") val date: Date = inputFormat.parse(s.Date!!)
                            val formattedDate: String = outputFormat.format(date!!)
                            dayCases.add(formattedDate)

                            barEnteries.add(barEntry)
                            barEnteries2.add(barEntry2)
                            barEnteries3.add(barEntry3)
                            barEnteries4.add(barEntry4)
                            i++
                        }
                        val xAxis: XAxis = barchartView.xAxis
                        xAxis.valueFormatter = IndexAxisValueFormatter(dayCases)
                        barchartView.axisLeft.axisMinimum = 0f
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.granularity = 1f
                        xAxis.setCenterAxisLabels(true)
                        xAxis.isGranularityEnabled = true
                        val barDataSet = BarDataSet(barEnteries, "Confirmed")
                        val barDataSet2 = BarDataSet(barEnteries2, "Deaths")
                        val barDataSet3 = BarDataSet(barEnteries3, "Recovered")
                        val barDataSet4 = BarDataSet(barEnteries4, "Active")
                        //setColorBAr
                        barDataSet.setColors(Color.parseColor("#F44336"))
                        barDataSet2.setColors(Color.parseColor("#FFEB3B"))
                        barDataSet3.setColors(Color.parseColor("#03DAC5"))
                        barDataSet4.setColors(Color.parseColor("#2196F3"))

                        val data = BarData(barDataSet, barDataSet2, barDataSet3, barDataSet4)
                        barchartView.data = data
                        //ukuran grafik atau bar
                        val barSpace = 0.02f
                        val groupSpace = 0.3f
                        val groupCount = 4f
                        data.barWidth = 0.15f
                        barchartView.invalidate()
                        barchartView.setNoDataTextColor(R.color.colorBlack)
                        barchartView.setTouchEnabled(true)
                        barchartView.description.isEnabled = false
                        barchartView.xAxis.axisMinimum = 0f
                        barchartView.setVisibleXRangeMaximum(
                            0f + barchartView.barData.getGroupWidth(
                                groupSpace,
                                barSpace
                            ) * groupCount
                        )
                        barchartView.groupBars(0f,groupSpace, barSpace)
                    }
                }
            }
        })
    }
}