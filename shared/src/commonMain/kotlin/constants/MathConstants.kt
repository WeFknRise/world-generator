package constants

import kotlin.math.PI

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object MathConstants {
    const val HALF_PI = PI * .5
    const val QUARTER_PI = PI * .25
    const val TAU = PI * 2

    const val DEGREES = 180.0 / PI
    const val RADIANS = PI / 180.0

    const val EPSILON = 1e-6
    const val EPSILON2 = 1e-12
    const val TAU_EPSILON = TAU - EPSILON
}