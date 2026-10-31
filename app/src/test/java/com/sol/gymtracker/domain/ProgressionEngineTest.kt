package com.sol.gymtracker.domain

import com.sol.gymtracker.data.db.LoggedSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {
    @Test
    fun `progression is suggested only when every work set reaches top with rir two`() {
        val sets = listOf(
            set(0, 12, 2),
            set(1, 12, 2),
            set(2, 12, 3),
        )
        val result = ProgressionEngine.decide(sets, targetSets = 3, repMax = 12, stepKg = 0.1)
        assertTrue(result.ready)
        assertEquals(0.1, result.suggestedIncreaseKg, 0.001)
    }

    @Test
    fun `progression stays off when one set is below top or rir`() {
        val sets = listOf(set(0, 12, 2), set(1, 11, 2), set(2, 12, 1))
        val result = ProgressionEngine.decide(sets, targetSets = 3, repMax = 12, stepKg = 2.5)
        assertFalse(result.ready)
        assertEquals(0.0, result.suggestedIncreaseKg, 0.001)
    }

    private fun set(index: Int, reps: Int, rir: Int) = LoggedSetEntity("$index", "exercise", index, 80.0, reps, rir, completed = true, tonnage = 80.0 * reps)
}