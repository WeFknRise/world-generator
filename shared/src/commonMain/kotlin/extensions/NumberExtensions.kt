package extensions

/**
 * Explanation: Extensions for Number classes e.g. Double, Int, ...
 *
 * @author Jarno Michiels
 */

fun Double?.orNull(): Double? = if(isFalsy()) null else this