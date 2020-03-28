package backend.generators

import backend.extensions.StopwatchExtensions.prettyPrintMS
import backend.models.Voronoi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.util.StopWatch
import visuals.CellLayer

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class CellLayerGenerator(private val voronoiGraphs: Array<Array<Voronoi>>) {
    private val stopWatch = StopWatch("Cell layer generator")

    fun generate(): CellLayer {
        val cellLayer = CellLayer()
        stopWatch.start("Generate path")
        for (row in voronoiGraphs.indices) {
            for (col in voronoiGraphs[row].indices) {
                cellLayer.path += generatePathAsync(voronoiGraphs[row][col])
            }
        }
        stopWatch.stop()
        println(stopWatch.prettyPrintMS())

        return cellLayer
    }

    private fun generatePathAsync(voronoi: Voronoi): String {
        var path = ""

        runBlocking {
            path = voronoi.v.map { (_, cell) ->
                GlobalScope.async {
                    val cellVertices = cell.map { voronoi.p[it]!! }.map { "${it.x},${it.y}" }
                    var cellPath = "M"
                    cellVertices.forEach { cellPath += "$it " }
                    cellPath
                }
            }.awaitAll().joinToString("")
        }

        return path
    }
}