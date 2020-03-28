package models.shapes.path.elements

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class HLineTo(val x: Double) : PathElement {
    override fun toString(): String {
        return "H$x"
    }
}