package com.elco.batteryguardian

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.elco.batteryguardian.databinding.ActivityMainBinding
import java.util.Locale

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
        val efficiency = PowerEfficiencyEngine.analyze(
            context = this,
            level = snapshot.level,
            charging = snapshot.charging,
            voltageMv = snapshot.voltageMv,
            currentNowUa = snapshot.currentNowUa,
            chargeCounterUah = snapshot.chargeCounterUah
        )
        val history = BatteryHistoryStore.recordAndEstimate(
            context = this,
            level = snapshot.level,
            charging = snapshot.charging
        )

        binding.levelText.text = getString(R.string.percent_value, snapshot.level)
        binding.scoreText.text = getString(R.string.score_value, snapshot.score)
        binding.healthText.text = getString(R.string.health_value, snapshot.healthLabel)
        binding.temperatureText.text = getString(R.string.temperature_value, snapshot.temperatureC)
        binding.voltageText.text = getString(R.string.voltage_value, snapshot.voltageMv)
        binding.chargeText.text = if (snapshot.charging) {
            getString(R.string.charging_yes)
        } else {
            getString(R.string.charging_no)
        }

        binding.currentText.text = efficiency.instantCurrentMa?.let {
            getString(R.string.current_value, it)
        } ?: getString(R.string.current_unavailable)

        binding.powerText.text = efficiency.instantPowerW?.let {
            getString(R.string.power_value, it)
        } ?: getString(R.string.power_unavailable)

        binding.thermalText.text = getString(
            R.string.thermal_value,
            efficiency.thermalStatusLabel
        )

        binding.brightnessText.text = efficiency.brightnessPercent?.let {
            getString(R.string.brightness_value, it)
        } ?: getString(R.string.brightness_unavailable)

        binding.powerSaverStateText.text = if (efficiency.powerSaverActive) {
            getString(R.string.power_saver_on)
        } else {
            getString(R.string.power_saver_off)
        }

        binding.efficiencyText.text = getString(
            R.string.efficiency_value,
            efficiency.efficiencyLabel
        )

        binding.autonomyText.text = efficiency.estimatedHoursRemaining?.let {
            getString(R.string.autonomy_value, formatOneDecimal(it))
        } ?: getString(R.string.autonomy_learning)

        binding.drainText.text = history.percentPerHour?.let {
            getString(R.string.drain_value, formatOneDecimal(it))
        } ?: getString(R.string.drain_learning)

        binding.adviceText.text = efficiency.recommendation
        binding.zeroWakeText.text = getString(R.string.zero_wake_active)

        updateUsageSection()
        BatteryWidgetRenderer.updateAll(this)
    }

    private fun updateUsageSection() {
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

    private fun formatOneDecimal(value: Float): String =
        String.format(Locale.getDefault(), "%.1f", value)
}
