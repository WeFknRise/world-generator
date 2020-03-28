package models.shapes.curves.basis

import models.shapes.curves.Curve
import models.shapes.path.Context
import models.shapes.path.PathContext

/**
 * Class: AbstractBasisCurve
 * Explanation:
 *
 * @author Jarno Michiels
 */
abstract class AbstractBasisCurve(val context: Context = PathContext()) : Curve {
    var point = 0
    var x0 = Double.NaN
    var y0 = Double.NaN
    var x1 = Double.NaN
    var y1 = Double.NaN

    override fun lineStart() {
        x0 = Double.NaN
        y0 = Double.NaN
        x1 = Double.NaN
        y1 = Double.NaN
        point = 0
    }

    fun basisPoint(x: Double, y: Double) {
        context.bezierCurveTo(
                (2 * x0 + x1) / 3,
                (2 * y0 + y1) / 3,
                (x0 + 2 * x1) / 3,
                (y0 + 2 * y1) / 3,
                (x0 + 4 * x1 + x) / 6,
                (y0 + 4 * y1 + y) / 6
        )
    }
}