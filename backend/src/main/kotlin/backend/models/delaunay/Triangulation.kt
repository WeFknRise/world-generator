package backend.models.delaunay

import backend.generators.delaunay.Triangulator
import models.geometry.Vertex

class Triangulation(vertices: List<Vertex>) {
    private val triangulator = Triangulator(vertices)

    val points
        get() = triangulator.points

    val halfEdges
        get() = triangulator.halfEdges

    val triangles
        get() = triangulator.triangles
}