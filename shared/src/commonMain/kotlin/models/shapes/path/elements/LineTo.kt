package models.shapes.path.elements

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class LineTo(val x: Double, val y: Double) : PathElement {
    override fun toString(): String {
        return "L$x,$y"
    }
}