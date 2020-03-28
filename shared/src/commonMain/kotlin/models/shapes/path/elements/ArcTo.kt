package models.shapes.path.elements

import extensions.toInt

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class ArcTo(val radiusX: Double, val radiusY: Double, val xAxisRotation: Double,
            val x: Double, val y: Double, val largeArcFlag: Boolean, val sweepFlag: Boolean) : PathElement {
    override fun toString(): String {
        return "A$radiusX,$radiusY,$xAxisRotation,${largeArcFlag.toInt()},${sweepFlag.toInt()},$x,$y"
    }
}