package backend.utils

import utils.MathUtils.rn

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object Utils {
    // round string to d decimals
    fun rnString(s: String, d: Double = 1.0): String {
        return s.replace(Regex("[\\d.-][\\d.e-]*")) {
            rn(it.value.toDouble(), d).toString()
        }
    }
}