package models.shapes

import models.shapes.lines.Line

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
fun <T> line() = Line<T>()
fun <T> line(init: Line<T>.() -> Unit): Line<T> {
    val line = Line<T>()
    line.init()
    return line
}
