package com.example.batterymanager

import CreateInputMutation
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.rx3.rxMutate
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity() {

    val mainHandler = Handler(Looper.getMainLooper())
    val delay: Int = 10 * 1000 //one second = 1000 milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    private val updateBatteryStuff = object : Runnable {
        override fun run() {
            val batteryStatus: Intent? =
                IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    applicationContext.registerReceiver(null, ifilter)
                }

            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1


            val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL

            chargeStatus.text = isCharging.toString()

            val batteryPct: Float? = batteryStatus?.let { intent ->
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level * 100 / scale.toFloat()
            }

            batteryPercentage.text = batteryPct.toString()

            val temperature: Float? = ((batteryStatus?.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE,
                0
            ))?.div(10))?.toFloat()

            batteryTemperature.text = temperature.toString().plus("*C")

            val batteryManager: BatteryManager =
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    applicationContext.getSystemService(Context.BATTERY_SERVICE)
                } else {
                    null
                }) as BatteryManager

            val timeRemaining: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                batteryManager.computeChargeTimeRemaining()
            } else {
                -1
            }

            currentAmps.text = timeRemaining.toString()

            createClient().flatMapCompletable { client: ApolloClient ->
                createEvent(
                    client,
                    timeRemaining.toString(),
                    batteryPct!!.toInt(),
                    temperature.toString(),
                    isCharging,
                    userId = "a1fe4c97-773d-4724-bc3b-379197cd25e9"
                )
            }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .blockingAwait()

            mainHandler.postDelayed(this, delay.toLong())
        }
    }

    private fun createEvent(
        client: ApolloClient, timeRemaining: String,
        batteryPercentage: Int,
        temperature: String,
        isCharging: Boolean,
        userId: String
    ): Completable {
        return client
            .rxMutate(
                CreateInputMutation(
                    timeRemaining,
                    batteryPercentage,
                    temperature,
                    isCharging,
                    userId
                )
            )
            .doOnSuccess { response ->
                Log.i("Apollo", "Created an event: $response")
            }
            .doOnError { failure ->
                Log.w("Apollo", "Failed to create an event.", failure)
            }
            .ignoreElement()
    }

    private fun createClient(): Single<ApolloClient> {
        return Single.just(ApolloClient.builder()
            .serverUrl("https://ptqvxc7if5h5rbvcony6da3ivi.appsync-api.us-east-2.amazonaws.com/graphql")
            .okHttpClient(OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader("x-api-key", secretKey)
                            .build()
                    )
                }
                .build()
            )
            .build())
    }

    override fun onResume() {
        mainHandler.post(updateBatteryStuff)

        super.onResume()
    }

    override fun onPause() {
        mainHandler.removeCallbacks(updateBatteryStuff)
        super.onPause()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
