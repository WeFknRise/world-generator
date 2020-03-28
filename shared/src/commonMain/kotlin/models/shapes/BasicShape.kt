package models.shapes

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
abstract class BasicShape {
    var opacity: Double? = null
    var fill: String? = null
    var isSmooth: Boolean? = null
    var stroke: String? = null
    var strokeWidth: Double? = null
    var translateX: Double? = null
    var translateY: Double? = null
}