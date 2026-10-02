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

        binding.dnaButton.setOnClickListener {
            binding.dnaCard.requestFocus()
            binding.dnaCard.parent.requestChildFocus(binding.dnaCard, binding.dnaCard)
        }

        binding.leakButton.setOnClickListener {
            binding.premiumCard.requestFocus()
            binding.premiumCard.parent.requestChildFocus(binding.premiumCard, binding.premiumCard)
        }

        binding.budgetButton.setOnClickListener {
            binding.premiumCard.requestFocus()
            binding.premiumCard.parent.requestChildFocus(binding.premiumCard, binding.premiumCard)
        }

        binding.chargeGuardButton.setOnClickListener {
            binding.premiumCard.requestFocus()
            binding.premiumCard.parent.requestChildFocus(binding.premiumCard, binding.premiumCard)
        }

        binding.displayButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
        }

        binding.widgetButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }

        binding.inviteFriendsButton.setOnClickListener {
            val referral = ReferralRewardManager.status(this)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, ReferralRewardManager.shareText(this@MainActivity))
            }
            startActivity(Intent.createChooser(shareIntent, "Invita amici"))
        }

        binding.modeButton.setOnClickListener {
            val current = GuardianPreferences.batteryMode(this)
            val next = when (current) {
                BatteryMode.DAILY -> BatteryMode.NIGHT
                BatteryMode.NIGHT -> BatteryMode.TRAVEL
                BatteryMode.TRAVEL -> BatteryMode.EMERGENCY
                BatteryMode.EMERGENCY -> BatteryMode.DAILY
            }
            GuardianPreferences.saveMode(this, next)
            refresh()
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

        val premium = EntitlementRepository.isPremium(this)
        val mode = GuardianPreferences.batteryMode(this)
        val modeProfile = BatteryModeEngine.profile(mode)
        val budget = BatteryBudgetEngine.calculate(
            currentLevel = snapshot.level,
            targetHours = modeProfile.targetHours,
            reservePercent = modeProfile.reservePercent,
            observedDrainPerHour = history.percentPerHour
        )
        val topApp = if (UsageAnalyzer.hasUsageAccess(this)) {
            UsageAnalyzer.topApps(this, 1).firstOrNull()
        } else null
        val leak = BatteryLeakDetector.diagnose(
            charging = snapshot.charging,
            temperatureC = snapshot.temperatureC,
            brightnessPercent = efficiency.brightnessPercent,
            instantCurrentMa = efficiency.instantCurrentMa,
            observedDrainPerHour = history.percentPerHour,
            budget = budget,
            topApp = topApp
        )
        val chargeGuard = ChargeGuardEngine.evaluate(
            charging = snapshot.charging,
            level = snapshot.level,
            temperatureC = snapshot.temperatureC,
            chargeLimit = modeProfile.chargeLimit
        )

        BatteryDnaStore.addSample(
            context = this,
            drainPerHour = history.percentPerHour,
            temperatureC = snapshot.temperatureC,
            currentMa = efficiency.instantCurrentMa
        )
        val dnaProfile = BatteryDnaStore.profile(this)
        val dnaComparison = BatteryDnaStore.compare(
            context = this,
            currentDrainPerHour = history.percentPerHour
        )
        val dnaAdvice = BatteryDnaAdvisor.advice(
            comparison = dnaComparison,
            efficiency = efficiency,
            temperatureC = snapshot.temperatureC,
            topApp = topApp
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

        binding.dnaStatusText.text = dnaAdvice.title + " · " + dnaComparison.message
        binding.dnaBaselineText.text = dnaProfile.averageDrainPerHour?.let {
            getString(R.string.dna_baseline_value, formatOneDecimal(it))
        } ?: getString(R.string.dna_baseline_learning)
        binding.dnaConfidenceText.text = getString(
            R.string.dna_confidence_value,
            dnaProfile.confidence,
            dnaProfile.samples
        )
        binding.dnaAdviceText.text = dnaAdvice.detail

        val referral = ReferralRewardManager.status(this)
        binding.referralProgress.progress = referral.verifiedFriends
        binding.referralProgressText.text = "${referral.verifiedFriends}/${referral.requiredFriends} amici verificati"
        binding.referralCodeText.text = "Codice invito: ${referral.referralCode}"

        binding.premiumModeText.text = getString(
            R.string.premium_mode_value,
            modeProfile.title
        )

        if (premium) {
            val source = EntitlementRepository.premiumSource(this)
            val rewardDays = EntitlementRepository.rewardDaysRemaining(this)
            binding.premiumStatusText.text = if (rewardDays > 0) {
                "PLUS ATTIVO · $source · $rewardDays giorni rimasti"
            } else {
                "PLUS ATTIVO · $source"
            }
            binding.premiumBudgetText.text = getString(
                R.string.premium_budget_value,
                modeProfile.targetHours,
                modeProfile.reservePercent,
                formatOneDecimal(budget.allowedDrainPerHour)
            )
            binding.premiumLeakText.text = getString(
                R.string.premium_leak_value,
                leak.title,
                leak.likelyCause
            )
            binding.premiumChargeGuardText.text = getString(
                R.string.premium_charge_guard_value,
                chargeGuard.title,
                chargeGuard.message
            )
        } else {
            binding.premiumStatusText.text = getString(R.string.premium_locked)
            binding.premiumBudgetText.text = getString(R.string.premium_locked_budget)
            binding.premiumLeakText.text = getString(R.string.premium_locked_leak)
            binding.premiumChargeGuardText.text = getString(R.string.premium_locked_charge_guard)
        }

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
