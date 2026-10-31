package com.sol.gymtracker.domain

import com.sol.gymtracker.data.db.LoggedSetEntity
import kotlin.math.max

data class ProgressionDecision(
    val ready: Boolean,
    val suggestedIncreaseKg: Double,
    val reason: String,
)

object ProgressionEngine {
    fun decide(previousSets: List<LoggedSetEntity>, targetSets: Int, repMax: Int, stepKg: Double): ProgressionDecision {
        val ready = previousSets.size >= targetSets && previousSets.take(targetSets).all { it.reps >= repMax && it.rir >= 2 }
        return if (ready) {
            ProgressionDecision(true, max(0.1, stepKg), "Все рабочие подходы достигли верхней границы с RIR ≥ 2")
        } else {
            ProgressionDecision(false, 0.0, "Сначала закрепи верхнюю границу повторений с запасом")
        }
    }
}