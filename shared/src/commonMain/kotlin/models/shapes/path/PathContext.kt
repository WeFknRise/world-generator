package models.shapes.path

import constants.MathConstants.EPSILON
import constants.MathConstants.TAU
import constants.MathConstants.TAU_EPSILON
import extensions.isFalsy
import models.shapes.path.elements.*
import kotlin.math.*

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
fun path(): PathContext = PathContext()

open class PathContext : Context {
    private var x0 = Double.NaN
    private var y0 = Double.NaN
    private var x1 = Double.NaN
    private var y1 = Double.NaN

    private val elements = ArrayList<PathElement>()
    private var pathString = "";

    override fun moveTo(x: Double, y: Double): PathContext {
        x0 = x
        x1 = x
        y0 = y
        y1 = y

        addElement(MoveTo(x, y))

        return this
    }

    override fun lineTo(x: Double, y: Double): PathContext {
        x1 = x
        y1 = y

        addElement(LineTo(x, y))

        return this
    }

    override fun quadraticCurveTo(controlX: Double, controlY: Double, x: Double, y: Double): PathContext {
        x1 = x
        y1 = y

        addElement(QuadCurveTo(controlX, controlY, x, y))

        return this
    }

    override fun bezierCurveTo(controlX: Double, controlY: Double, controlX2: Double, controlY2: Double, x: Double, y: Double): PathContext {
        x1 = x
        y1 = y

        addElement(CubicCurveTo(controlX, controlY, controlX2, controlY2, x, y))

        return this
    }

    override fun arcTo(x1: Double, y1: Double, x2: Double, y2: Double, radius: Double): PathContext {
        // Is the radius negative? Error
        if (radius < 0) throw IllegalArgumentException("""negative radius $radius""")

        val x0 = this.x1
        val y0 = this.y1
        val x21 = x2 - x1
        val y21 = y2 - y1
        val x01 = x0 - x1
        val y01 = y0 - y1
        val l01_2 = x01 * x01 + y01 * y01

        // Is this path empty? Move to (x1, y1)
        if (this.x1.isNaN()) {
            this.x1 = x1
            this.y1 = y1
            addElement(MoveTo(x1, y1))
        }
        // Is (x1,y1) coincident with (x0,y0)?
        else if (l01_2 <= EPSILON) {
            // Do nothing
        }
        // Or, are (x0,y0), (x1,y1) and (x2,y2) collinear?
        // Equivalently, is (x1,y1) coincident with (x2,y2)?
        // Or, is the radius zero? models.visuals.shapes.lines.Line to (x1,y1).
        else if (abs(y01 * x21 - y21 * x01) <= EPSILON || radius.isFalsy()) {
            this.x1 = x1
            this.y1 = y1
            addElement(LineTo(x1, y1))
        }
        // Otherwise draw an arc
        else {
            val x20 = x2 - x0
            val y20 = y2 - y0
            val l21_2 = x21 * x21 + y21 * y21
            val l20_2 = x20 * x20 + y20 * y20
            val l21 = sqrt(l21_2)
            val l01 = sqrt(l01_2)
            val l = radius * tan((PI - acos((l21_2 + l01_2 - l20_2) / (2 * l21 * l01))) / 2.0)
            val t01 = l / l01
            val t21 = l / l21

            // If the start tangent is not coincident with (x0,y0), line to
            if (abs(t01 - 1) > EPSILON) {
                addElement(LineTo(x1 + t01 * x01, y1 + t01 * y01))
            }

            this.x1 = x1 + t21 * x21
            this.y1 = y1 + t21 * y21
            addElement(ArcTo(radius, radius, 0.0, x1, y1, false, (y01 * x20 > x01 * y20)))
        }

        return this
    }

    override fun arc(x: Double, y: Double, r: Double, a0: Double, a1: Double, ccw: Boolean): PathContext {
        // Is the radius negative? Error
        if (r < 0) throw IllegalArgumentException("""negative radius $r""")

        val dx = r * cos(a0)
        val dy = r * sin(a0)
        val x0 = x + dx
        val y0 = y + dy
        val cw = true xor ccw
        var da = if (ccw) a0 - a1 else a1 - a0

        // Is this path emptySelection? Move to (x0,y0).
        if (x1.isNaN()) {
            addElement(MoveTo(x0, y0))
        }

        // Or, is (x0, y0) not coincident with the previous point? models.visuals.shapes.lines.Line to (x0, y0).
        else if (abs(x1 - x0) > EPSILON || abs(y1 - y0) > EPSILON) {
            addElement(LineTo(x0, y0))
        }

        // Is this arc empty? We’re done.
        if (r.isFalsy()) return this

        // Does the angle go the wrong way? Flip the direction.
        if (da < 0) da = da % TAU + TAU

        // Is this a complete circle? Draw two arcs to complete the circle.
        if (da > TAU_EPSILON) {
            addElement(ArcTo(r, r, 0.0, x - dx, y - dy, true, cw))

            x1 = x0
            y1 = y0
            addElement(ArcTo(r, r, 0.0, x1, y1, true, cw))
        }

        // Is this arc non-empty? Draw an arc
        else if (da > EPSILON) {
            x1 = x + r * cos(a1)
            y1 = y + r * sin(a1)
            addElement(ArcTo(r, r, 0.0, x1, y1, (da >= PI), cw))
        }

        return this
    }

    override fun rect(x: Double, y: Double, w: Double, h: Double): PathContext {
        x0 = x
        x1 = x
        y0 = y
        y1 = y

        addElement(MoveTo(x, y))
        addElement(HLineTo(w))
        addElement(VLineTo(h))
        addElement(HLineTo(-w))
        addElement(ClosePath())

        return this
    }

    override fun closePath(): PathContext {
        if (!x1.isNaN()) {
            x1 = x0
            y1 = y0

            addElement(ClosePath())
        }

        return this
    }

    override operator fun invoke(): Path {
        val path = Path(elements, pathString)
        elements.clear()
        pathString = ""
        return path
    }

    private fun addElement(element: PathElement) {
        elements += element
        pathString += element.toString()
    }
}