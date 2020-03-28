package utils

import models.geometry.Vertex
import models.shapes.curves.curveBasis
import models.shapes.line

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object ShapeUtils {
    val lineGen = line<Vertex> {
        x { vertex, _, _ -> vertex.x }
        y { vertex, _, _ -> vertex.y }
        curve(curveBasis())
    }

    fun polygonArea(polygon: List<Vertex>): Double {
        val n = polygon.size
        var i = -1
        var a: Vertex
        var b = polygon[n - 1]
        var area = 0.0

        while (++i < n) {
            a = b
            b = polygon[i]
            area += a.y * b.x - a.x * b.y
        }

        return area / 2
    }
}