package backend.generators.delaunay

import backend.models.Voronoi
import backend.models.delaunay.Triangulation
import models.geometry.Vertex
import kotlin.math.floor

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class VoronoiGenerator {
    fun generate(triangulation: Triangulation, vertices: List<Vertex>, nPoints: Int): Voronoi {
        val voronoi = Voronoi(nPoints)

        for (e in triangulation.triangles.indices) {
            val p = triangulation.triangles[nextHalfEdge(e)]
            if (p < nPoints && voronoi.c[p] == null) {
                val edges = edgesAroundPoint(e, triangulation)
                voronoi.v[p] = edges.map { triangleOfEdge(it) }.toIntArray()
                voronoi.c[p] = edges.map { triangulation.triangles[it] }.filter { it < nPoints }.toIntArray()
                voronoi.b[p] = edges.size > voronoi.c[p]!!.size
            }

            val t = triangleOfEdge(e) // Is basically just e / 3
            if (voronoi.p[t] == null) { // If already exists don't do it again (same result)
                voronoi.p[t] = triangleCenter(t, triangulation, vertices)
                voronoi.n[t] = trianglesAdjacentToTriangle(t, triangulation)
                voronoi.a[t] = pointsOfTriangle(t, triangulation).toIntArray()
            }
        }

        return voronoi
    }

    private fun nextHalfEdge(e: Int): Int {
        return if (e % 3 == 2) e - 2 else e + 1
    }

    private fun edgesAroundPoint(start: Int, triangulation: Triangulation): IntArray {
        var incoming = start
        val result = mutableListOf<Int>()

        do {
            result.add(incoming)
            val outgoing = nextHalfEdge(incoming)
            incoming = triangulation.halfEdges[outgoing]
        } while (incoming != -1 && incoming != start && result.size < 20)

        return result.toIntArray()
    }

    private fun pointsOfTriangle(t: Int, triangulation: Triangulation): Array<Int> {
        return edgesOfTriangle(t).map { triangulation.triangles[it] }.toTypedArray()
    }

    private fun trianglesAdjacentToTriangle(t: Int, triangulation: Triangulation): IntArray {
        return edgesOfTriangle(t).map { triangulation.halfEdges[it] }
                .map { triangleOfEdge(it) }
                .toIntArray()
    }

    private fun edgesOfTriangle(t: Int): Array<Int> {
        return arrayOf(3 * t, 3 * t + 1, 3 * t + 2)
    }

    private fun triangleCenter(t: Int, triangulation: Triangulation, vertices: List<Vertex>): Vertex {
        val v = pointsOfTriangle(t, triangulation).map { vertices[it] }.toTypedArray()
        return circumcenter(v[0], v[1], v[2])
    }

    private fun triangleOfEdge(e: Int): Int {
        return floor(e / 3.0).toInt()
    }

    private fun circumcenter(a: Vertex, b: Vertex, c: Vertex): Vertex {
        val ad = a.x * a.x + a.y * a.y
        val bd = b.x * b.x + b.y * b.y
        val cd = c.x * c.x + c.y * c.y

        val d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y))
        val x = floor(1.0 / d * (ad * (b.y - c.y) + bd * (c.y - a.y) + cd * (a.y - b.y)))
        val y = floor(1.0 / d * (ad * (c.x - b.x) + bd * (a.x - c.x) + cd * (b.x - a.x)))

        return Vertex(x, y)
    }
}