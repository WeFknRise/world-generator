package models.shapes.path.elements

import models.shapes.path.elements.PathElement

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class QuadCurveTo(val controlX: Double, val controlY: Double, val x: Double, val y: Double) : PathElement {
    override fun toString(): String {
        return "Q$controlX,$controlY,$x,$y"
    }
}