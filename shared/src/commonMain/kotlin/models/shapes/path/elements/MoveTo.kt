package models.shapes.path.elements

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class MoveTo(val x: Double, val y: Double) : PathElement {
    override fun toString(): String {
        return "M$x,$y"
    }
}