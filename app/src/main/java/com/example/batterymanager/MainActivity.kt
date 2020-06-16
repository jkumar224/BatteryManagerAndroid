package com.example.batterymanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    val mainHandler = Handler(Looper.getMainLooper())
    val delay: Int = 10*1000 //one second = 1000 milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    private val updateBatteryStuff = object: Runnable {
        override fun run() {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
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

            val temperature: Float? = ((batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0))?.div(10))?.toFloat()

            batteryTemperature.text = temperature.toString().plus("*C")

            val batteryManager: BatteryManager = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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

            //TODO: update aws database with this info

            mainHandler.postDelayed(this, delay.toLong())
        }
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
