package backend.utils

import backend.generators.delaunay.VoronoiGenerator
import backend.models.Voronoi
import backend.models.delaunay.Triangulation
import kotlinx.coroutines.*
import models.geometry.Vertex
import utils.MathUtils.rn
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.sign
import kotlin.random.Random.Default.nextDouble

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object GraphUtils {
    fun boundaryVertices(width: Double, height: Double, density: Int, spacing: Double, offset: Double = rn(spacing)): Array<Array<MutableList<Vertex>>> {
        if (spacing <= 0.0) throw RuntimeException("Spacing cannot be 0, consider enlarging width or height")

        val boundaryVerticesArray = Array(density) { Array(density) { mutableListOf<Vertex>() } }
        val w = (width / density) + offset * 2
        val h = (height / density) + offset * 2
        val numberX = ceil(w / spacing) - 1
        val numberY = ceil(h / spacing) - 1
        val bottomBoundaryVertices = Array(density) { mutableListOf<Vertex>() }
        val rightBoundaryVertices = mutableListOf<Vertex>()
        val jittering = 0.0//spacing / 2

        for (row in 0 until density) {
            val heightToAdd = h * row

            for (col in 0 until density) {
                var i = 0.5
                val widthToAdd = w * col
                val bottomY = (h * (row + 1)) - offset
                val rightX = (w * (col + 1)) - offset

                if (row != 0) {
                    boundaryVerticesArray[row][col].addAll(bottomBoundaryVertices[col]) //Add all bottom boundary vertices from previous row iteration with col index
                    bottomBoundaryVertices[col].clear()
                }

                repeat(numberX.toInt()) {
                    val xJitter = if (row != (density - 1)) jitter(jittering) else 0.0
                    val yJitter = if (row != (density - 1)) jitter(jittering) else 0.0
                    val x = generateX(w, i, numberX, offset, widthToAdd)

                    if (row == 0) boundaryVerticesArray[row][col].add(Vertex(x, -offset)) //TOP BOUNDARY
                    val bottom = Vertex(x + xJitter, bottomY + yJitter)
                    boundaryVerticesArray[row][col].add(bottom) //BOTTOM BOUNDARY
                    if (row != (density - 1)) bottomBoundaryVertices[col].add(bottom)

                    i++
                }

                i = 0.5
                if (col != 0) {
                    boundaryVerticesArray[row][col].addAll(rightBoundaryVertices) //Add all right boundary vertices from previous col iteration
                    rightBoundaryVertices.clear()
                }

                repeat(numberY.toInt()) {
                    val xJitter = if (col != (density - 1)) jitter(jittering) else 0.0
                    val yJitter = if (col != (density - 1)) jitter(jittering) else 0.0
                    val y = generateY(h, i, numberY, offset, heightToAdd)

                    if (col == 0) boundaryVerticesArray[row][col].add(Vertex(-offset, y)) //LEFT BOUNDARY
                    val right = Vertex(rightX + xJitter, y + yJitter)
                    boundaryVerticesArray[row][col].add(right) //RIGHT BOUNDARY
                    if (col != (density - 1)) rightBoundaryVertices.add(right)

                    i++
                }
            }
        }

        return boundaryVerticesArray
    }

    private fun generateX(w: Double, i: Double, numberX: Double, offset: Double, widthToAdd: Double): Double = ceil((w * i / numberX - offset) + widthToAdd)
    private fun generateY(h: Double, i: Double, numberY: Double, offset: Double, heightToAdd: Double): Double = ceil((h * i / numberY - offset) + heightToAdd)

    private fun widthToAdd(width: Double, offset: Double, col: Int): Double = (width * col) + ((offset * 2) * col)
    private fun heightToAdd(height: Double, offset: Double, row: Int): Double = (height * row) + ((offset * 2) * row)

    fun jitteredGraph(width: Double, height: Double, density: Int, spacing: Double, offset: Double = rn(spacing), jitterMethod: (Double) -> Double = { jittering -> jitter(jittering) }): Array<Array<MutableList<Vertex>>> {
        val verticesArray = Array(density) { Array(density) { mutableListOf<Vertex>() } }
        val singleDensityWidth = width / density
        val singleDensityHeight = height / density

        val deferred = (0 until density).map { row ->
            GlobalScope.async {
                val heightToAdd = heightToAdd(singleDensityHeight, offset, row)
                for (col in 0 until density) {
                    val widthToAdd = widthToAdd(singleDensityWidth, offset, col)
                    verticesArray[row][col] = jitteredGraph(singleDensityWidth, singleDensityHeight, spacing, widthToAdd, heightToAdd, jitterMethod)
                }
                yield()
            }
        }

        runBlocking { deferred.awaitAll() }

        return verticesArray
    }

    //Non asynchronous way of jitteredGraph (slower for large graphs)
    private fun jitteredGraph(width: Double, height: Double, spacing: Double, shiftX: Double, shiftY: Double, jitterMethod: (Double) -> Double = { jittering -> jitter(jittering) }): MutableList<Vertex> {
        val radius = spacing / 2
        val jittering = radius * 0.9
        val vertices = mutableListOf<Vertex>()

        var y = radius
        do {
            var x = radius
            do {
                val v1 = x + jitterMethod(jittering) + shiftX
                val v2 = y + jitterMethod(jittering) + shiftY

                val xj = rn(v1, 2.0)
                val yj = rn(v2, 2.0)

                vertices.add(Vertex(xj, yj))
                x += spacing
            } while (x < width)
            y += spacing
        } while (y < height)

        return vertices
    }

    private fun jitter(jittering: Double): Double {
        return nextDouble() * 2 * jittering - jittering
    }

    fun generateVoronoiGraph(boundaryVertices: List<Vertex>, vertices: List<Vertex>): Voronoi {
        val voronoiGenerator = VoronoiGenerator()
        val all = vertices.toMutableList()
        all.addAll(boundaryVertices)
        
        return voronoiGenerator.generate(Triangulation(all), all, all.size) // all.size (used to be vertices.size) otherwise the boundaries don't connect
    }

    fun generateVoronoiGraphs(boundaryVertices: Array<Array<MutableList<Vertex>>>, graphVertices: Array<Array<MutableList<Vertex>>>, density: Int): Array<Array<Voronoi>> {
        if (boundaryVertices.size != graphVertices.size) throw error("Not enough boundaries")
        val voronoiGenerator = VoronoiGenerator()
        val result = Array(density) { emptyArray<Voronoi>() }

        val rowThreadPool = Executors.newFixedThreadPool(8).asCoroutineDispatcher()
        val deferred = (graphVertices.indices).map { row ->
            GlobalScope.async {
                result[row] = (graphVertices[row].indices).map { col ->
                    GlobalScope.async(rowThreadPool) {
                        val n = graphVertices[row][col].size
                        val all = graphVertices[row][col].toMutableList()
                        all.addAll(boundaryVertices[row][col])
                        voronoiGenerator.generate(Triangulation(all), all, n)
                    }
                }.awaitAll().toTypedArray()
            }
        }

        runBlocking { deferred.awaitAll() }
        rowThreadPool.close()

        return result
    }

    fun calculatePolygonCentroid(points: List<Vertex>): Vertex {
        val n = points.size
        var a: Vertex
        var b = points[n - 1]
        var c: Double
        var x = 0.0
        var y = 0.0
        var k = 0.0

        for (i in 0 until n) {
            a = b
            b = points[i]
            c = (a.x * b.y) - (b.x * a.y)
            k += c
            x += (a.x + b.x) * c
            y += (a.y + b.y) * c
        }

        k *= 3
        return Vertex(x / k, y / k)
    }
}