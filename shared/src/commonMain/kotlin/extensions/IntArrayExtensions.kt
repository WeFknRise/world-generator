package extensions

/**
 * Explanation: Extension methods for the IntArray class
 *
 * @author Jarno Michiels
 */

fun IntArray.swap(i: Int, j: Int) {
    val temp = this[i]
    this[i] = this[j]
    this[j] = temp
}