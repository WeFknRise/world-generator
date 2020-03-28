package models.shapes.lines

import models.shapes.Shape
import models.shapes.curves.Curve
import models.shapes.curves.curveLinear
import models.shapes.path.Context
import models.shapes.path.PathContext

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class Line<T> : Shape<Line<T>, List<T>>() {
    var x: (T, Int, List<T>) -> Double = { _, _, _ -> throw IllegalStateException("x must be specified") }
    var y: (T, Int, List<T>) -> Double = { _, _, _ -> throw IllegalStateException("y must be specified") }
    var defined: (T, Int, List<T>) -> Boolean = { _, _, _ -> true }
    var curve: (Context) -> Curve = curveLinear()

    fun x(value: Double) = x { _, _, _ -> value }
    fun x(func: (T, Int, List<T>) -> Double): Line<T> {
        x = func
        return this
    }

    fun y(value: Double) = y { _, _, _ -> value }
    fun y(func: (T, Int, List<T>) -> Double): Line<T> {
        y = func
        return this
    }

    fun defined(func: (T, Int, List<T>) -> Boolean): Line<T> {
        defined = func
        return this
    }

    fun curve(func: (Context) -> Curve): Line<T> {
        curve = func
        return this
    }

    override fun generate(d: List<T>, i: Int): Context {
        val context = PathContext()
        val output = curve(context)

        var defined0 = false

        val n = d.size
        for(idx in 0..n) { // The n has to be included otherwise the next if statement is obsolete
            if(!(idx < n && defined(d[idx], idx, d)) == defined0) {
                defined0 = !defined0
                if(defined0)
                    output.lineStart()
                else
                    output.lineEnd()
            }
            if(defined0)
                output.point(x(d[idx], idx, d), y(d[idx], idx, d))
        }

        return context
    }
}