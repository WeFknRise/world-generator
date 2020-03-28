package utils

import kotlin.math.pow
import kotlin.math.round

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object MathUtils {
    fun rn(v: Double, d: Double = 0.0): Double {
        val m = 10.0.pow(d)
        return round(v * m) / m
    }
}