package utils

import backend.extensions.StopwatchExtensions.prettyPrintMS
import backend.utils.GraphUtils.boundaryVertices
import backend.utils.GraphUtils.generateVoronoiGraph
import backend.utils.GraphUtils.generateVoronoiGraphs
import backend.utils.GraphUtils.jitteredGraph
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.util.StopWatch
import utils.MathUtils.rn
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
@TestInstance(PER_CLASS)
class GraphUtilsTest {
    private val stopwatch = StopWatch("GraphUtilsTest")

    @Test
    fun testJitteredLargeGraph() {
        //GIVEN
        val density = 25.0
        val cellsDesired = 10_000 * (density.pow(2))
        val width = 48_000.0
        val height = 23_425.0
        val spacing = rn(sqrt(width * height / cellsDesired), 2.0)

        //WHEN
        stopwatch.start("testJitteredLargeGraph")
        val vertices = jitteredGraph(width, height, density.toInt(), spacing) { 0.0 }
        stopwatch.stop()

        //THEN
        assertThat(vertices).isNotEmpty
        assertThat(vertices.size).isEqualTo(density.toInt())
        assertThat(vertices.all { it.size == density.toInt() }).isEqualTo(true)
        assertThat(vertices.all { it.all { list -> list.size >= 10_000 } }).isEqualTo(true)
    }

    /**
     * Boundary Tests
     */
    @Test
    fun testBoundaryVertices() {
        //GIVEN
        val density = 5.0
        val width = 5_000.0
        val height = 1_000.0
        val spacing = 10.0
        val minX = -10.0
        val minY = -10.0
        val maxX = 5010.0
        val maxY = 1010.0

        //WHEN
        stopwatch.start("testBoundaryVertices")
        val boundary = boundaryVertices(width, height, density.toInt(), spacing)
        stopwatch.stop()

        //THEN
        assertThat(boundary).isNotEmpty
        assertThat(boundary.size).isEqualTo(density.toInt())
        assertThat(boundary).allMatch { it.size == density.toInt() }
        assertThat(boundary).allSatisfy { it.all { list -> list.all { v -> v.x == minX || v.y == minY || v.x == maxX || v.y == maxY } } }
    }

    @Test
    fun testMediumBoundaryVertices() {
        //GIVEN
        val density = 3.0
        val cellsDesired = 10_000 * (density.pow(2))
        val width = 5_760.0
        val height = 2_811.0
        val spacing = rn(sqrt(width * height / cellsDesired), 2.0)

        //WHEN
        stopwatch.start("testLargeBoundaryVertices")
        val boundary = boundaryVertices(width, height, density.toInt(), spacing)
        stopwatch.stop()

        //THEN
        assertThat(boundary).isNotEmpty
        assertThat(boundary).allMatch { it.size == density.toInt() }

        //Row 1
        assertThat(boundary[0][0]).allMatch { it.x == -13.0 || it.x == 1933.0 || it.y == -13.0 || it.y == 950.0 }
        assertThat(boundary[0][1]).allMatch { it.x == 1933.0 || it.x == 3879.0 || it.y == -13.0 || it.y == 950.0 }
        assertThat(boundary[0][2]).allMatch { it.x == 3879.0 || it.x == 5825.0 || it.y == -13.0 || it.y == 950.0 }

        //Row 2
        assertThat(boundary[1][0]).allMatch { it.x == -13.0 || it.x == 1933.0 || it.y == 950.0 || it.y == 1913.0 }
        assertThat(boundary[1][1]).allMatch { it.x == 1933.0 || it.x == 3879.0 || it.y == 950.0 || it.y == 1913.0 }
        assertThat(boundary[1][2]).allMatch { it.x == 3879.0 || it.x == 5825.0 || it.y == 950.0 || it.y == 1913.0 }

        //Row 3
        assertThat(boundary[2][0]).allMatch { it.x == -13.0 || it.x == 1933.0 || it.y == 1913.0 || it.y == 2876.0 }
        assertThat(boundary[2][1]).allMatch { it.x == 1933.0 || it.x == 3879.0 || it.y == 1913.0 || it.y == 2876.0 }
        assertThat(boundary[2][2]).allMatch { it.x == 3879.0 || it.x == 5825.0 || it.y == 1913.0 || it.y == 2876.0 }
    }

    @Test
    fun testLargeBoundaryVertices() {
        //GIVEN
        val density = 25.0
        val cellsDesired = 10_000 * (density.pow(2))
        val width = 48_000.0
        val height = 23_425.0
        val spacing = rn(sqrt(width * height / cellsDesired), 2.0)

        //WHEN
        stopwatch.start("testLargeBoundaryVertices")
        val boundary = boundaryVertices(width, height, density.toInt(), spacing)
        stopwatch.stop()

        //THEN
        assertThat(boundary).isNotEmpty
    }

    @Test
    fun testGenerateVoronoiGraphs() {
        //GIVEN
        val density = 1.0
        val cellsDesired = 10_000.0 * (density.pow(2))
        val width = 1_920.0
        val height = 937.0
        val spacing = rn(sqrt(width * height / cellsDesired), 2.0)
        val boundaryVertices = boundaryVertices(width, height, density.toInt(), spacing)
        val graphVertices = jitteredGraph(width, height, density.toInt(), spacing) { 0.0 }

        //WHEN
        stopwatch.start("testGenerateVoronoiGraphs")
        val voronoiGraphs = generateVoronoiGraphs(boundaryVertices, graphVertices, density.toInt())
        stopwatch.stop()

        //THEN
        assertThat(voronoiGraphs).isNotEmpty
    }

    @Test
    fun testGenerateVoronoiGraphsMedium() {
        //GIVEN
        val density = 5.0
        val cellsDesired = 10_000 * (density.pow(2))
        val width = 1920.0 * density
        val height = 937.0 * density
        val spacing = rn(sqrt(width * height / cellsDesired), 2.0)
        val boundaryVertices = boundaryVertices(width, height, density.toInt(), spacing).map { row -> row.toList().flatten() }.toList().flatten()
        val graphVertices = jitteredGraph(width, height, density.toInt(), spacing) { 0.0 }.map { row -> row.toList().flatten() }.toList().flatten()

        //WHEN
        stopwatch.start("testGenerateVoronoiGraphs")
        val voronoiGraph = generateVoronoiGraph(boundaryVertices, graphVertices)
        stopwatch.stop()

        //THEN
        assertThat(voronoiGraph).isNotNull
    }

    @Test
    fun testGenerateVoronoiGraphsLarge() {
        //GIVEN
        val density = 25.0
        val cellsDesired = 10_000 * (density.pow(2))
        val width = 48_000.0
        val height = 23_425.0
        val spacing = rn(sqrt(width * height / cellsDesired), 2.0)
        val boundaryVertices = boundaryVertices(width, height, density.toInt(), spacing)
        val graphVertices = jitteredGraph(width, height, density.toInt(), spacing) { 0.0 }

        //WHEN
        stopwatch.start("testGenerateVoronoiGraphsLarge")
        val voronoiGraphs = generateVoronoiGraphs(boundaryVertices, graphVertices, density.toInt())
        stopwatch.stop()

        //THEN
        assertThat(voronoiGraphs).isNotEmpty
    }

    @AfterAll
    fun afterAllTests() {
        println(stopwatch.prettyPrintMS())
    }
}