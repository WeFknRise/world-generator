package models.shapes.curves.basis

import extensions.isTruthy
import models.shapes.path.Context
import models.shapes.path.PathContext

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class BasisCurve(context: Context = PathContext()) : AbstractBasisCurve(context) {
    var line: Double? = null

    override fun areaStart() {
        line = 0.0
    }

    override fun areaEnd() {
        line = Double.NaN
    }

    override fun lineEnd() {
        when(point) {
            2 -> context.lineTo(x1, y1)
            3 -> {
                point(x1, y1)
                context.lineTo(x1, y1)
            }
        }

        if(line.isTruthy() || (line != 0.0 && point == 1))
            context.closePath()

        line = 1 - (line ?: 0.0)
    }

    override fun point(x: Double, y: Double) {
        when(point) {
            0 -> {
                point = 1
                if(line.isTruthy())
                    context.lineTo(x, y)
                else
                    context.moveTo(x, y)
            }
            1 -> point = 2
            2 -> {
                point = 3
                context.lineTo((5 * x0 + x1) / 6, (5 * y0 + y1) / 6)
                basisPoint(x, y)
            }
            else -> basisPoint(x, y)
        }

        x0 = x1
        x1 = x
        y0 = y1
        y1 = y
    }
}