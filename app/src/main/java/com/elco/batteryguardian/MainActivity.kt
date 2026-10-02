package com.elco.batteryguardian

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.elco.batteryguardian.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.refreshButton.setOnClickListener { refresh() }

        binding.powerSaverButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        }

        binding.optimizationButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        binding.usageAccessButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val snapshot = BatteryAnalyzer.read(this)

        binding.levelText.text = getString(R.string.percent_value, snapshot.level)
        binding.scoreText.text = getString(R.string.score_value, snapshot.score)
        binding.healthText.text = snapshot.healthLabel
        binding.temperatureText.text = getString(R.string.temperature_value, snapshot.temperatureC)
        binding.voltageText.text = getString(R.string.voltage_value, snapshot.voltageMv)
        binding.chargeText.text = if (snapshot.charging) {
            getString(R.string.charging_yes)
        } else {
            getString(R.string.charging_no)
        }
        binding.adviceText.text = snapshot.statusMessage

        if (!UsageAnalyzer.hasUsageAccess(this)) {
            binding.usageText.text = getString(R.string.usage_permission_needed)
            binding.usageAccessButton.isEnabled = true
            return
        }

        val top = UsageAnalyzer.topApps(this)
        binding.usageAccessButton.isEnabled = false
        binding.usageText.text = if (top.isEmpty()) {
            getString(R.string.no_usage_data)
        } else {
            top.joinToString("\n") {
                getString(R.string.usage_row, it.label, it.foregroundMinutes)
            }
        }
    }
}
