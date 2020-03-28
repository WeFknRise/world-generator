package backend.models

import backend.globals.WorldGlobals.cellsMultiplier
import backend.globals.WorldGlobals.worldHeight
import backend.globals.WorldGlobals.worldMultiplier
import backend.globals.WorldGlobals.worldWidth
import backend.utils.GraphUtils.boundaryVertices
import backend.utils.GraphUtils.jitteredGraph
import models.geometry.Vertex
import utils.MathUtils.rn
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class Graph {
    var width: Double = 0.0
    var height: Double = 0.0
    var density: Int = 1

    lateinit var boundary: Array<Array<MutableList<Vertex>>>
    lateinit var vertices: Array<Array<MutableList<Vertex>>>
    var spacing: Double = 0.0
    var cellsX: Int = 0
    var cellsY: Int = 0

    fun initialize(width: Double, height: Double, density: Int) {
        this.width = width
        this.height = height
        this.density = density

        val cellsDesired = (10_000 * (density.toDouble().pow(worldMultiplier))) * cellsMultiplier
        val v = sqrt(worldWidth * worldHeight / cellsDesired)

        spacing = rn(v, 2.0)
        boundary = boundaryVertices(width, height, density, spacing)
        vertices = jitteredGraph(width, height, density, spacing)
        cellsX = floor((width + 0.5 * spacing) / spacing).toInt()
        cellsY = floor((height + 0.5 * spacing) / spacing).toInt()
    }
}