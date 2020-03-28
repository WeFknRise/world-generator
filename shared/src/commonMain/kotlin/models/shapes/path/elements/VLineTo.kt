package models.shapes.path.elements

import models.shapes.path.elements.PathElement

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class VLineTo(val y: Double) : PathElement {
    override fun toString(): String {
        return "V$y"
    }
}