package models.shapes

import models.shapes.path.Context

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
abstract class Shape<out S : Shape<S, T>, T> {
    var opacity: ((T, Int) -> Double)? = null
    var fill: ((T, Int) -> String?)? = null
    var smooth: ((T, Int) -> Boolean)? = null
    var stroke: ((T, Int) -> String?)? = null
    var strokeWidth: ((T, Int) -> Double)? = null
    var translateX: ((T, Int) -> Double)? = null
    var translateY: ((T, Int) -> Double)? = null

    fun opacity(opacity: Double) = opacity { _, _ -> opacity }
    private fun opacity(opacity: (T, Int) -> Double): S {
        this.opacity = opacity
        return this as S
    }

    fun fill(color: String?) = fill { _, _ -> color }
    private fun fill(color: (T, Int) -> String?): S {
        fill = color
        return this as S
    }

    fun smooth(value: Boolean) = smooth { _, _ -> value }
    private fun smooth(value: (T, Int) -> Boolean): S {
        smooth = value
        return this as S
    }

    fun stroke(color: String?) = stroke { _, _ -> color }
    private fun stroke(color: (T, Int) -> String?): S {
        stroke = color
        return this as S
    }

    fun strokeWidth(width: Double) = strokeWidth { _, _ -> width }
    private fun strokeWidth(width: (T, Int) -> Double): S {
        strokeWidth = width
        return this as S
    }

    fun translateX(x: Double) = translateX { _, _ -> x }
    private fun translateX(x: (T, Int) -> Double): S {
        this.translateX = x
        return this as S
    }

    fun translateY(y: Double) = translateY { _, _ -> y }
    private fun translateY(y: (T, Int) -> Double): S {
        this.translateY = y
        return this as S
    }

    internal fun apply(d: T, i: Int, shape: BasicShape) {
        shape.opacity = opacity?.invoke(d, i) ?: shape.opacity

        if(fill != null) shape.fill = fill?.invoke(d, i)
        if(stroke != null) shape.stroke = stroke?.invoke(d, i)
        shape.isSmooth = smooth?.invoke(d, i) ?: shape.isSmooth
        shape.strokeWidth = strokeWidth?.invoke(d, i) ?: shape.strokeWidth

        shape.translateX = translateX?.invoke(d, i) ?: shape.translateX
        shape.translateY = translateY?.invoke(d, i) ?: shape.translateY
    }

    operator fun invoke(d: T) = invoke(d, -1)
    //abstract operator fun invoke(d: T, i: Int): BasicShape

    operator fun invoke(d: T, i: Int): BasicShape {
        val context = generate(d, i) ?: throw IllegalStateException("Context must not be null")
        val shapeContext = context()
        apply(d, i, shapeContext)
        return shapeContext
    }

    abstract fun generate(d: T, i: Int): Context?
}