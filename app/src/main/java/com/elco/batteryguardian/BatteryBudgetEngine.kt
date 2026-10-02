package com.elco.batteryguardian

data class BatteryBudget(
    val targetHours: Int,
    val reservePercent: Int,
    val availablePercent: Int,
    val allowedDrainPerHour: Float,
    val observedDrainPerHour: Float?,
    val status: BudgetStatus,
    val marginPercentPerHour: Float?
)

enum class BudgetStatus {
    LEARNING,
    SAFE,
    TIGHT,
    OVER_BUDGET
}

object BatteryBudgetEngine {

    fun calculate(
        currentLevel: Int,
        targetHours: Int,
        reservePercent: Int,
        observedDrainPerHour: Float?
    ): BatteryBudget {
        val safeHours = targetHours.coerceIn(1, 48)
        val safeReserve = reservePercent.coerceIn(0, 80)
        val available = (currentLevel - safeReserve).coerceAtLeast(0)
        val allowed = available.toFloat() / safeHours.toFloat()

        if (observedDrainPerHour == null) {
            return BatteryBudget(
                targetHours = safeHours,
                reservePercent = safeReserve,
                availablePercent = available,
                allowedDrainPerHour = allowed,
                observedDrainPerHour = null,
                status = BudgetStatus.LEARNING,
                marginPercentPerHour = null
            )
        }

        val margin = allowed - observedDrainPerHour
        val status = when {
            observedDrainPerHour <= allowed * 0.80f -> BudgetStatus.SAFE
            observedDrainPerHour <= allowed -> BudgetStatus.TIGHT
            else -> BudgetStatus.OVER_BUDGET
        }

        return BatteryBudget(
            targetHours = safeHours,
            reservePercent = safeReserve,
            availablePercent = available,
            allowedDrainPerHour = allowed,
            observedDrainPerHour = observedDrainPerHour,
            status = status,
            marginPercentPerHour = margin
        )
    }
}
