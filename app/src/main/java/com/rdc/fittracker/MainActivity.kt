package com.rdc.fittracker

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.HealthDataTypes
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.DataUpdateListenerRegistrationRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.rdc.fittracker.adapters.BloodPressureAdapter
import com.rdc.fittracker.databinding.ActivityMainBinding
import com.rdc.fittracker.model.BloodPressureFitReadingData
import com.rdc.fittracker.receivers.BloodPressureUpdateReceiver
import com.rdc.fittracker.utils.TAG
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private val dateFormat = SimpleDateFormat("dd/MMM/yy HH:mm:aa", Locale.US)
    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(HealthDataTypes.TYPE_BLOOD_PRESSURE, FitnessOptions.ACCESS_READ)
            .addDataType(HealthDataTypes.AGGREGATE_BLOOD_PRESSURE_SUMMARY, FitnessOptions.ACCESS_READ)
            .build()
    }

    private lateinit var binding: ActivityMainBinding
    private val bloodPressureData: MutableList<BloodPressureFitReadingData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.enableGoogleFitBtn.setOnClickListener {
            checkGoogleAccount()
        }

        if (GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)) {
            readBloodPressureData()
        }
    }

    /**
     * Gets a Google account for use in creating the Fitness client. This is achieved by either
     * using the last signed-in account, or if necessary, prompting the user to sign in.
     * `getAccountForExtension` is recommended over `getLastSignedInAccount` as the latter can
     * return `null` if there has been no sign in before.
     */
    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

    private fun readBloodPressureData(): Task<DataReadResponse?>? {
        // Begin by creating the query.
        val readRequest = queryBloodPressureData()

        // Invoke the History API to fetch the data with the query
        return Fitness.getHistoryClient(this, getGoogleAccount())
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                // For the sake of the sample, we'll print the data so we can see what we just
                // added. In general, logging fitness information should be avoided for privacy
                // reasons.
                printData(dataReadResponse)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "There was a problem reading the data.", e)
            }
    }

    /** Returns a [DataReadRequest] for all step count changes in the past week.  */
    private fun queryBloodPressureData(): DataReadRequest {
        // [START build_read_data_request]
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val now = Date()
        calendar.time = now
        val endTime = calendar.timeInMillis
        //Update last Sync date label
        binding.syncDate.text = resources.getString(R.string.last_sync_date, dateFormat.format(endTime))
        //Set time to start of day so we can retrieve complete data
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        val startTime = calendar.timeInMillis

        Log.i(TAG, "Range Start: ${dateFormat.format(startTime)}")
        Log.i(TAG, "Range End: ${dateFormat.format(endTime)}")

        return DataReadRequest.Builder()
            // The data request can specify multiple data types to return, effectively
            // combining multiple data queries into one call.
            // In this example, it's very unlikely that the request is for several hundred
            // datapoints each consisting of a few steps and a timestamp.  The more likely
            // scenario is wanting to see how many steps were walked per day, for 7 days.
            .aggregate(HealthDataTypes.TYPE_BLOOD_PRESSURE)
            // Analogous to a "Group By" in SQL, defines how data should be aggregated.
            // bucketByTime allows for a time span, whereas bucketBySession would allow
            // bucketing by "sessions", which would need to be defined in code.
                /**
                 * In this moment I'm setting interval by hour in case there is more than one registry
                 * per day, if we want to reduce the amount of dataPoints we can easily change to
                 * TimeUnit.DAYS, this will only return 30 DataPoints but if we have multiple records
                 * on one day it will give us the average measures.
                 */
            .bucketByTime(1, TimeUnit.HOURS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()
    }

    private fun checkGoogleAccount() {
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(HealthDataTypes.TYPE_BLOOD_PRESSURE, FitnessOptions.ACCESS_READ)
            .addDataType(HealthDataTypes.AGGREGATE_BLOOD_PRESSURE_SUMMARY, FitnessOptions.ACCESS_READ)
            .build()

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    account,
                    fitnessOptions
            )
        } else {
            Snackbar.make(binding.root, "Permission is already granted", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                readBloodPressureData()
            }
        }
    }

    /**
     * Logs a record of the query result. It's possible to get more constrained data sets by
     * specifying a data source or data type, but for demonstrative purposes here's how one would
     * dump all the data. In this sample, logging also prints to the device screen, so we can see
     * what the query returns, but your app should not log fitness information as a privacy
     * consideration. A better option would be to dump the data you receive to a local data
     * directory to avoid exposing it to other applications.
     */
    private fun printData(dataReadResult: DataReadResponse) {
        // [START parse_read_data_result]
        //We clear previous data retrieved
        bloodPressureData.clear()
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.buckets.isNotEmpty()) {
            Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
            for (bucket in dataReadResult.buckets) {
                bucket.dataSets.forEach { dumpDataSet(it) }
            }
        } else if (dataReadResult.dataSets.isNotEmpty()) {
            Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.dataSets.size)
            dataReadResult.dataSets.forEach { dumpDataSet(it) }
        }
        // [END parse_read_data_result]
        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(this, 0,
                Intent(this, BloodPressureUpdateReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)

        Fitness.getHistoryClient(this, getGoogleAccount())
                .registerDataUpdateListener(DataUpdateListenerRegistrationRequest.Builder()
                        .setDataType(HealthDataTypes.TYPE_BLOOD_PRESSURE)
                        .setPendingIntent(pendingIntent)
                        .build())
                .addOnSuccessListener {
                    Log.i(TAG, "Subscription to data updates successful")
                }
                .addOnFailureListener {
                    Log.i(TAG, "Failed to Subscribe: " + it.message)
                }

        binding.bloodPressureRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = BloodPressureAdapter(bloodPressureData)
            addItemDecoration(DividerItemDecoration(this@MainActivity, requestedOrientation))
        }
    }

    // [START parse_dataset]
    private fun dumpDataSet(dataSet: DataSet) {
        Log.i(TAG, "Data returned for Data type: ${dataSet.dataType.name}")

        for (dp in dataSet.dataPoints) {
            var bloodPressureInfo = BloodPressureFitReadingData()
            Log.i(TAG, "Data point:")
            Log.i(TAG, "\tType: ${dp.dataType.name}")
            val dateAndTime = dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)).split(" ")
            bloodPressureInfo.date = dateAndTime[0]
            bloodPressureInfo.time = dateAndTime[1]
            Log.i(TAG, "\tStart: ${dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS))}")
            Log.i(TAG, "\tEnd: ${dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS))}")
            dp.dataType.fields.forEach {
                when (it.name){
                    SYSTOLIC_PARAM -> {
                        val systolic = dp.getValue(it).toString().split(".")[0].toInt()
                        bloodPressureInfo.systolic = systolic
                        //Using 130 on systolic measure as limit to determine if blood pressure
                        //is high or elevated
                        if (systolic > 130) bloodPressureInfo.isHigh = true
                    }
                    DIASTOLIC_PARAM -> {
                        val diastolic = dp.getValue(it).toString().split(".")[0].toInt()
                        bloodPressureInfo.diastolic = diastolic
                        //Using 80 on diastolic measure as limit to determine if blood pressure
                        //is high or elevated
                        if (diastolic > 80) bloodPressureInfo.isHigh = true
                    }
                }
                Log.i(TAG, "\tField: ${it.name} Value: ${dp.getValue(it)}")
            }
            bloodPressureData.add(bloodPressureInfo)
        }
    }
    // [END parse_dataset]

    companion object{
        private const val SYSTOLIC_PARAM = "blood_pressure_systolic_average"
        private const val DIASTOLIC_PARAM = "blood_pressure_diastolic_average"

        private const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 142
    }
}