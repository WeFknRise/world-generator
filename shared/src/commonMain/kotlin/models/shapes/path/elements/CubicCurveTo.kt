package models.shapes.path.elements

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class CubicCurveTo(val controlX: Double, val controlY: Double, val controlX2: Double, val controlY2: Double, val x: Double, val y: Double) : PathElement {
    override fun toString(): String {
        return "C$controlX,$controlY,$controlX2,$controlY2,$x,$y"
    }
}