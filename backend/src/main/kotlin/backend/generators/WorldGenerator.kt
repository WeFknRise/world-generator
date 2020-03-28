package backend.generators

import FastNoiseKt
import backend.extensions.StopwatchExtensions.prettyPrintMS
import backend.globals.WorldGlobals.density
import backend.globals.WorldGlobals.worldHeight
import backend.globals.WorldGlobals.worldWidth
import backend.models.Graph
import backend.models.Voronoi
import backend.utils.GraphUtils.calculatePolygonCentroid
import backend.utils.GraphUtils.generateVoronoiGraph
import backend.utils.GraphUtils.generateVoronoiGraphs
import enums.TerrainType
import enums.TerrainType.LAND
import enums.TerrainType.OCEAN
import org.springframework.util.StopWatch

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class WorldGenerator {
    private lateinit var stopwatch: StopWatch
    lateinit var voronoiGraphs: Array<Array<Voronoi>>
    lateinit var graph: Graph

    fun generate() {
        stopwatch = StopWatch("WorldGenerator")

        //Generate graph
        generateGraph()

        //Generate voronoi
        generateVoronoi()

        //Print time
        println(stopwatch.prettyPrintMS())
        if (stopwatch.isRunning) stopwatch.stop()
    }

    private fun generateGraph() {
        stopwatch.start("Generating graph")

        graph = Graph()
        graph.initialize(worldWidth, worldHeight, density)

        stopwatch.stop()
    }

    private fun generateVoronoi() {
        stopwatch.start("Generating voronoi")

        //voronoiGraphs = generateVoronoiGraphs(graph.boundary, graph.vertices, density)

        val boundaryVertices = graph.boundary.map { row -> row.toList().flatten() }.toList().flatten()
        val graphVertices = graph.vertices.map { row -> row.toList().flatten() }.toList().flatten()
        voronoiGraphs = Array(1) { Array(1) { generateVoronoiGraph(boundaryVertices, graphVertices) } }

        stopwatch.stop()
    }
}