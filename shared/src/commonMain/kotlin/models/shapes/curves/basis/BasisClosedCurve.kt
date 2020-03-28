package models.shapes.curves.basis

import models.shapes.path.Context
import models.shapes.path.PathContext

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class BasisClosedCurve(context: Context = PathContext()) : AbstractBasisCurve(context) {
    var x2 = Double.NaN
    var y2 = Double.NaN
    var x3 = Double.NaN
    var y3 = Double.NaN
    var x4 = Double.NaN
    var y4 = Double.NaN

    override fun areaStart() {}
    override fun areaEnd() {}

    override fun lineEnd() {
        when(point) {
            1 -> {
                context.moveTo(x2, y2)
                context.closePath()
            }
            2 -> {
                context.moveTo((x2 + 2 * x3) / 3, (y2 + 2 * y3) / 3)
                context.lineTo((x3 + 2 * x2) / 3, (y3 + 2 * y2) / 3)
                context.closePath()
            }
            3 -> {
                point(x2, y2)
                point(x3, y3)
                point(x4, y4)
            }
        }
    }

    override fun point(x: Double, y: Double) {
        when(point) {
            0 -> {
                point = 1
                x2 = x
                y2 = y
            }
            1 -> {
                point = 2
                x3 = x
                y3 = y
            }
            2 -> {
                point = 3
                x4 = x
                y4 = y
                context.moveTo((x0 + 4 * x1 + x) / 6, (y0 + 4 * y1 + y) / 6)
            }
            else -> basisPoint(x, y)
        }

        x0 = x1
        x1 = x
        y0 = y1
        y1 = y
    }
}